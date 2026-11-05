# CONTRATO CONGELADO — EURECABANK RESTful v1.0

> **Documento de verdad única.** Todo el proyecto (BDD, ERS, ECUD, OpenAPI, diagramas
> PowerDesigner, servidor Java y cliente consola) debe derivar **exclusivamente** de este
> archivo. Si algo discrepa, este archivo gana. Cambiarlo exige actualizar los 5 artefactos
> en el mismo día.
>
> Congelado: **2026-11-04** · Versión: **1.0** · Estado: **APROBADO** · Grupo **GR03**
>
> Este contrato es la **variante RESTful** del contrato SOAP congelado del proyecto
> `TI2.1 SOAP_JAVA_CONBDD_EURECABANK_GR03`. Los cambios son **solo de estilo**: el modelo de
> datos, los 17 códigos con sus mensajes literales, las 14 reglas de negocio, los 11 casos de
> uso y los actores **no cambian**. Cambian la capa de entrada (REST + JSON en vez de
> SOAP + XML), la forma de viajar el token (cabecera `Authorization` en vez de parámetro) y
> el estado HTTP (que ahora es real y acompaña siempre al código de negocio).

---

## 1. Identificación del servicio

| Ítem | Valor |
|---|---|
| Nombre del servicio | `EURECABANK RESTful` |
| Estilo | **RESTful con JSON** (no SOAP) |
| Base de la API | `/api` |
| Contexto (nombre del WAR) | `EUREKABANK_RESTFUL_JAVA` |
| **Endpoint de producción local** | `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api` |
| Puerto HTTP | 8080 |
| Grupo | `GR03` |
| Versión del contrato | **1.0** |
| Archivos del contrato | `04. SERVIDOR/contrato/eurekabank-openapi.yaml` (OpenAPI 3.0.3) y este documento |
| Runtime | Jakarta EE 10 / JAX-RS 3.1 / JSON-B 3.0 sobre **Payara 7**, empaquetado como WAR |
| Base de datos | MySQL 8.4, esquema `eurekabank` (17 tablas, la misma del proyecto SOAP) |

> El OpenAPI es el **equivalente REST del WSDL** del proyecto SOAP: las mismas 9 operaciones,
> los mismos 17 códigos y el mismo sobre de respuesta.

---

## 2. Modelo de datos (17 tablas, heredado de `03.BDD` de la guía)

| Tabla | PK | FK | Propósito |
|---|---|---|---|
| `empleado` | `chr_emplcodigo` char(4) | — | Maestro de empleados (14 filas de prueba) |
| `usuario` | `chr_emplcodigo` char(4) | → `empleado` | Credenciales. `vch_emplusuario` único, `vch_emplclave` = SHA-256 hex (64) |
| `permiso` | (`chr_emplcodigo`,`int_moducodigo`) | → `usuario`, `modulo` | Permisos por módulo (6 módulos) |
| `modulo` | `int_moducodigo` int | — | Catálogo de módulos (Procesos, Tablas, Consultas, Reportes, Util, Seguridad) |
| `cliente` | `chr_cliecodigo` char(5) | — | Clientes del banco (20 filas), `chr_cliedni` char(8) |
| `sucursal` | `chr_sucucodigo` char(3) | — | 7 sucursales |
| `moneda` | `chr_monecodigo` char(2) | — | `01` = Soles, `02` = Dólares |
| `cuenta` | `chr_cuencodigo` char(8) | → `cliente`, `moneda`, `sucursal`, `empleado` | Saldo `dec_cuensaldo`, estado `vch_cuenestado` ∈ {ACTIVO, ANULADO, CANCELADO}, `int_cuencontmov` |
| `movimiento` | (`chr_cuencodigo`,`int_movinumero`) | → `cuenta`, `empleado`, `tipomovimiento` | Libro de movimientos; `chr_cuenreferencia` para transferencias |
| `tipomovimiento` | `chr_tipocodigo` char(3) | — | 10 tipos, `vch_tipoaccion` ∈ {INGRESO, SALIDA} |
| `cargomantenimiento` | `chr_monecodigo` | → `moneda` | `dec_cargMontoMaximo` (umbral), `dec_cargImporte` (costo fijo) |
| `costomovimiento` | `chr_monecodigo` | → `moneda` | `dec_costimporte` (costo por movimiento) |
| `interesmensual` | `chr_monecodigo` | → `moneda` | `dec_inteimporte` (porcentaje mensual) |
| `parametro` | `chr_paracodigo` char(3) | — | `001` = ITF (0.08), `002` = Nº operaciones sin costo (15) |
| `contador` | `vch_conttabla` | — | Secuencias de PK por tabla |
| `logsession` | `ID` autoinc | → `empleado` | `fec_ingreso`, `fec_salida`, `ip`, `hostname` |
| `asignado` | `chr_asigcodigo` char(6) | → `empleado`, `sucursal` | Asignación empleado-sucursal |

### Valores de `tipomovimiento` (congelados)

| Código | Descripción | Acción |
|---|---|---|
| `001` | Apertura de Cuenta | INGRESO |
| `002` | Cancelar Cuenta | SALIDA |
| `003` | Deposito | INGRESO |
| `004` | Retiro | SALIDA |
| `005` | Interes | INGRESO |
| `006` | Mantenimiento | SALIDA |
| `007` | ITF | SALIDA |
| `008` | Transferencia | INGRESO |
| `009` | Transferencia | SALIDA |
| `010` | Cargo por Movimiento | SALIDA |

### Valores de `parametro` (congelados)

| Código | Descripción | Valor | Interpretación |
|---|---|---|---|
| `001` | ITF - Impuesto a las Transacciones Financieras | `0.08` | **Porcentaje**: ITF = round(monto × 0.08 / 100, 2) |
| `002` | Número de Operaciones Sin Costo | `15` | El movimiento N es sin costo si N ≤ 15 |

---

## 3. Sobre de respuesta

Todo lo que devuelve el servicio es un `RespuestaBase` **extendido** con un tipo específico.

**`RespuestaBase`** (atributos, en este orden):

| Atributo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `estado` | `string` | sí | `OK` o `ERROR` |
| `codigo` | `string` | sí | Código de resultado (§5) |
| `mensaje` | `string` | sí | Mensaje literal en español (§5) |

Tipos derivados (todos heredan de `RespuestaBase`):

| Tipo | Atributos propios |
|---|---|
| `RespuestaAutenticacion` | `token` (string), `empleado` (string), `nombreEmpleado` (string), `expiracion` (string ISO-8601), `rol` (string) |
| `RespuestaCuentas` | `cedula` (string), `nombreCliente` (string), `cuentas` (`CuentaDTO[]`, maxOccurs=unbounded) |
| `RespuestaCuenta` | `cuenta` (`CuentaDTO`) |
| `RespuestaMovimientos` | `cuenta` (string), `movimientos` (`MovimientoDTO[]`, maxOccurs=unbounded) |
| `RespuestaMovimiento` | `movimiento` (`MovimientoDTO`), `saldoNuevo` (decimal), `costoAplicado` (decimal), `itfAplicado` (decimal) |
| `RespuestaTransferencia` | `numeroOrigen` (int), `numeroDestino` (int), `saldoOrigen` (decimal), `saldoDestino` (decimal), `itfAplicado` (decimal) |
| `RespuestaCostos` | `saldoActual` (decimal), `costoMantenimiento` (decimal), `costoMovimiento` (decimal), `interesProyectado` (decimal), `limiteMantenimiento` (decimal), `operacionesSinCosto` (int) |

**Tipos de entidad:**

| Tipo | Atributos |
|---|---|
| `CuentaDTO` | `codigo` (string), `moneda` (string), `descripcionMoneda` (string), `saldo` (decimal), `estado` (string), `codigoCliente` (string), `nombreCliente` (string), `codigoSucursal` (string), `fechaCreacion` (string ISO-8601 `yyyy-MM-dd`), `clave` (string), `operacionesRealizadas` (int) |
| `MovimientoDTO` | `numero` (int), `fecha` (string ISO-8601 `yyyy-MM-dd`), `tipoCodigo` (string), `tipoDescripcion` (string), `tipoAccion` (string), `importe` (decimal), `referencia` (string), `empleado` (string) |

> **Correspondencia JSON ↔ Java (obligatoria, no negociable):**
> - `string` ↔ JSON `string` ↔ Java `String`.
> - `decimal` ↔ JSON `number` (`format: double`) ↔ Java `BigDecimal` con **escala 2 y
>   redondeo `HALF_UP`** (RB-01). Nunca `double` ni `float`.
> - `int` ↔ JSON `integer` (`format: int32`) ↔ Java `Integer`.
> - **Las fechas y fechas-hora son `string` en formato ISO-8601** (`yyyy-MM-dd` para
>   `fechaCreacion` y `fecha`; fecha-hora completa para `expiracion`), nunca un tipo
>   numérico ni un objeto de fecha estructurado.
>   Razón: los modelos son POJOs planos sin anotaciones (regla del docente: en `modelos` solo
>   atributos, constructores, getters y setters) y el mapeo por defecto de JSON-B no
>   deserializa un `LocalDate` a un campo `String`.
> - Los **atributos son públicos en el JSON** (no hay `private` ni `@JsonbTransient`): el
>   cliente lee `codigo`, `saldo`, `numero`, etc. directamente.
> - Los **tipos derivados heredan** los atributos del sobre: en el OpenAPI eso se declara con
>   `allOf` + `$ref` a `RespuestaBase`. En el JSON, el objeto derivado **no** lleva una
>   clave anidada: es el mismo objeto plano con `estado`, `codigo`, `mensaje` y sus
>   atributos propios.
> - **Verificado**: JSON-B (Yasson) serializa y deserializa estos POJOs planos **sin una
>   sola anotación** y toma el nombre de la propiedad del getter (`getCodigo` → `codigo`),
>   que ya coincide con el contrato.
> - Los **atributos nulos se omiten** del JSON: un sobre de error solo lleva `estado`,
>   `codigo` y `mensaje`.
> - Los **cuerpos de petición** son los tres POJOs del §4: `{usuario, clave}`,
>   `{monto}` y `{cuentaOrigen, cuentaDestino, monto}`. `POST /auth/logout` recibe `{}`.

### Tabla código → HTTP status (los 17 códigos)

| Código | HTTP status | Qué significa |
|---|---|---|
| `200` | 200 | Operación correcta |
| `DAT-001` | 400 | Los datos de la petición están incompletos o son incorrectos |
| `AUTH-001` | 401 | El usuario o la contraseña son incorrectos |
| `AUTH-002` | 403 | El usuario se encuentra inactivo |
| `AUTH-003` | 401 | El token de sesión no es válido |
| `AUTH-004` | 401 | El token de sesión ha expirado, inicie sesión nuevamente |
| `CTA-001` | 404 | La cuenta solicitada no existe |
| `CTA-002` | 409 | La cuenta no se encuentra en estado ACTIVO |
| `CTA-003` | 400 | La cuenta origen y la cuenta destino no pueden ser iguales |
| `CTA-004` | 409 | La cuenta de destino pertenece a una moneda distinta |
| `CLI-001` | 404 | El cliente solicitado no existe |
| `SAL-001` | 409 | El saldo de la cuenta es insuficiente para la operación |
| `MON-001` | 400 | El monto debe ser mayor que cero y con máximo dos decimales |
| `MON-002` | 409 | La moneda de la cuenta no está soportada por el sistema |
| `LIM-001` | 409 | El monto supera el límite máximo de mantenimiento de la cuenta |
| `SER-001` | 503 | El servicio no está disponible en este momento |
| `ERR-001` | 500 | Ocurrió un error interno, intente nuevamente |

> El **HTTP status acompaña siempre al código de negocio**: `200` con `codigo=200`,
> `401` con `codigo=AUTH-003`, `409` con `codigo=SAL-001`, etc. El cliente lee **los dos**:
> el status para la lógica HTTP y el `codigo` para el mensaje de negocio.
> `LIM-001` está **reservado** en el catálogo: ninguna de las 9 operaciones lo emite; el
> límite de mantenimiento viaja como dato `limiteMantenimiento` de `RespuestaCostos`.

---

## 4. Operaciones (9, congeladas)

Convención REST: el **token no es un parámetro**: viaja en la cabecera
`Authorization: Bearer <token>`. Los **parámetros de ruta** (`{cedula}`, `{cuenta}`) van en la
URL; el cuerpo JSON lleva los datos de la operación.

| # | Operación | Verbo y ruta | Cuerpo JSON de entrada | Respuesta | Cabecera `Authorization: Bearer <token>` | CU |
|---|---|---|---|---|---|---|
| 1 | `autenticar` | `POST /api/auth/login` | `{usuario, clave}` | `RespuestaAutenticacion` | **no se exige** (esta operación produce el token) | CU-01 |
| 2 | `cerrarSesion` | `POST /api/auth/logout` | `{}` | `RespuestaBase` | **obligatoria** | CU-01 |
| 3 | `listarCuentasPorCliente` | `GET /api/clientes/{cedula}/cuentas` | — | `RespuestaCuentas` | **obligatoria** | CU-03 |
| 4 | `consultarCuenta` | `GET /api/cuentas/{cuenta}` | — | `RespuestaCuenta` | **obligatoria** | CU-03 |
| 5 | `consultarMovimientos` | `GET /api/cuentas/{cuenta}/movimientos` | — | `RespuestaMovimientos` | **obligatoria** | CU-04 |
| 6 | `realizarDeposito` | `POST /api/cuentas/{cuenta}/depositos` | `{monto}` | `RespuestaMovimiento` | **obligatoria** | CU-05 |
| 7 | `realizarRetiro` | `POST /api/cuentas/{cuenta}/retiros` | `{monto}` | `RespuestaMovimiento` | **obligatoria** | CU-06 |
| 8 | `realizarTransferencia` | `POST /api/transferencias` | `{cuentaOrigen, cuentaDestino, monto}` | `RespuestaTransferencia` | **obligatoria** | CU-07 |
| 9 | `consultarCostosMantenimiento` | `GET /api/cuentas/{cuenta}/costos` | — | `RespuestaCostos` | **obligatoria** | CU-08 |

Reglas de esta sección:

- Las **7 operaciones protegidas** son las de la 3 a la 9 y todas exigen la cabecera
  `Authorization: Bearer <token>`. `autenticar` no la exige (es la que la produce) y
  `cerrarSesion` sí la exige.
- El **token nunca va en la URL** ni en el cuerpo: quedaría en el log del servidor.
- `Content-Type: application/json; charset=utf-8` en peticiones y en respuestas.
- En el OpenAPI, el esquema de seguridad `bearerAuth` se declara en las **7 operaciones
  protegidas**; las dos de `/auth` quedan sin `security` (la de `cerrarSesion` sigue
  exigiendo la cabecera en tiempo de ejecución, pero su esquema se documenta en la
  descripción de la operación).
- Los **verbos** siguen la convención REST: `GET` para leer, `POST` para las tres
  operaciones que escriben o ejecutan (`depositos`, `retiros`, `transferencias`).

---

## 5. Catálogo de códigos y mensajes (congelado)

**Regla:** el `mensaje` del sobre es **exactamente** la cadena de esta tabla. El ERS, el ECUD,
el `enum` del servidor y el cliente deben usar la misma cadena, sin variantes. El
**HTTP status** acompaña siempre al código.

| Código | Categoría | HTTP | Mensaje literal (obligatorio) |
|---|---|---|---|
| `200` | Éxito | 200 | *(varía por operación — ver abajo)* |
| `DAT-001` | Error de datos | 400 | `Los datos de la petición están incompletos o son incorrectos` |
| `AUTH-001` | Autenticación | 401 | `El usuario o la contraseña son incorrectos` |
| `AUTH-002` | Autenticación | 403 | `El usuario se encuentra inactivo` |
| `AUTH-003` | Autorización | 401 | `El token de sesión no es válido` |
| `AUTH-004` | Autorización | 401 | `El token de sesión ha expirado, inicie sesión nuevamente` |
| `CTA-001` | Cuenta | 404 | `La cuenta solicitada no existe` |
| `CTA-002` | Cuenta | 409 | `La cuenta no se encuentra en estado ACTIVO` |
| `CTA-003` | Cuenta | 400 | `La cuenta origen y la cuenta destino no pueden ser iguales` |
| `CTA-004` | Cuenta | 409 | `La cuenta de destino pertenece a una moneda distinta` |
| `CLI-001` | Cliente | 404 | `El cliente solicitado no existe` |
| `SAL-001` | Saldo | 409 | `El saldo de la cuenta es insuficiente para la operación` |
| `MON-001` | Monto | 400 | `El monto debe ser mayor que cero y con máximo dos decimales` |
| `MON-002` | Moneda | 409 | `La moneda de la cuenta no está soportada por el sistema` |
| `LIM-001` | Límite | 409 | `El monto supera el límite máximo de mantenimiento de la cuenta` |
| `SER-001` | Servicio | 503 | `El servicio no está disponible en este momento` |
| `ERR-001` | Sistema | 500 | `Ocurrió un error interno, intente nuevamente` |

### Mensajes de éxito por operación

| Operación | `mensaje` con `codigo` 200 |
|---|---|
| `autenticar` | `Autenticación exitosa` |
| `cerrarSesion` | `Sesión cerrada correctamente` |
| `listarCuentasPorCliente` | `Cuentas consultadas correctamente` |
| `consultarCuenta` | `Cuenta consultada correctamente` |
| `consultarMovimientos` | `Movimientos consultados correctamente` |
| `realizarDeposito` | `Depósito realizado correctamente` |
| `realizarRetiro` | `Retiro realizado correctamente` |
| `realizarTransferencia` | `Transferencia realizada correctamente` |
| `consultarCostosMantenimiento` | `Costos de mantenimiento consultados correctamente` |

---

## 6. Orden de validación (obligatorio, idéntico en ERS, ECUD, diagramas y código)

Si una petición viola dos reglas, **gana la que aparece primero**.

### 6.1 Operaciones autenticadas (3 a 9)

```
1. DAT-001  → falta la cabecera Authorization, o no viene en formato "Bearer <token>",
              o falta un parámetro de ruta o del cuerpo
2. AUTH-003 → el token no existe en el registro de sesiones
3. AUTH-004 → el token expiró (TTL vencido)
4. MON-001  → monto ≤ 0 o con más de 2 decimales        [solo 6, 7, 8]
5. CTA-003  → cuenta origen = cuenta destino             [solo 8]
6. CTA-001  → la cuenta no existe
7. CTA-002  → la cuenta no está en estado ACTIVO
8. CTA-004  → la cuenta destino es de otra moneda        [solo 8]
9. SAL-001  → saldo insuficiente                         [solo 7, 8]
10. MON-002 → la moneda de la cuenta no está soportada
11. Éxito o error de persistencia → ERR-001
```

> El orden importa: si una petición viola dos reglas, **gana la que aparece primero en la lista**.
> Los flujos alternativos del ECUD deben respetar esta precedencia.

### 6.2 `autenticar`

```
1. DAT-001  → usuario o clave vacíos
2. AUTH-001 → el usuario no existe o el hash SHA-256 no coincide
3. AUTH-002 → `usuario.vch_emplestado` ∈ {ANULADO, CANCELADO}
4. Éxito    → genera token (TTL 30 min), inserta `logsession`
```

### 6.3 `listarCuentasPorCliente`

```
1. DAT-001 → cedula vacía o cabecera Authorization ausente/mal formada
2. AUTH-003 / AUTH-004 → validación de token
3. CLI-001 → no existe cliente con ese DNI
4. Éxito   → devuelve todas sus cuentas (incluidas no ACTIVO, con su estado visible)
```

### 6.4 `cerrarSesion`

```
1. DAT-001 → falta la cabecera Authorization
2. AUTH-003 → el token no existe
3. AUTH-004 → el token expiró (igualmente se cierra la sesión en `logsession`)
4. Éxito    → marca `logsession.fec_salida` y descarta el token
```

### 6.5 Nota de diseño (obligatoria): por qué la capa REST valida la cabecera y los parámetros ANTES de delegar en los servicios

El orden congelado de §6.1 coloca **`DAT-001` (paso 1) antes que `AUTH-003` (paso 2) y
`AUTH-004` (paso 3)**. Los servicios de la capa `servicios` —**copiados literalmente del
proyecto SOAP**— hacen lo contrario: en `ServicioCuentas`, `ServicioMovimientos` y
`ServicioCostos` la **primera** comprobación es `tokenServicio.validarToken(token)`, que ya
devuelve `DAT-001`, `AUTH-003` o `AUTH-004`; la validación de los datos de la operación
(cuenta, cedula, monto, cuenta destino) viene **después**.

Si el recurso REST delegara directamente en el servicio, una petición con **token inválido y
parámetro ausente** respondería `AUTH-003`, violando §6.1 (debería responder `DAT-001`).

Por eso la **capa REST valida cabecera y parámetros antes de delegar**:

1. **`FiltroToken`** (`@Provider`, `ContainerRequestFilter`, en `seguridad`) **solo extrae**
   el token de la cabecera `Authorization: Bearer <token>` y lo deja disponible para el
   recurso. **No consulta el registro de sesiones y nunca responde `AUTH-003` ni
   `AUTH-004`**: esos dos códigos los produce siempre el servicio, nunca el filtro.
2. El **recurso REST** comprueba la presencia y el formato de la cabecera y la presencia de
   los parámetros de ruta y del cuerpo. Si algo falta → `DAT-001` (**400**) con su mensaje
   literal, y **no** se delega.
3. Solo entonces se **delega** en el servicio, que valida el token (`AUTH-003`, `AUTH-004`) y
   continúa con el resto del orden de §6.1.

Consecuencia práctica: `AUTH-003` y `AUTH-004` **nunca** los produce el filtro de token, y
`DAT-001` **siempre** precede a ambos.

**Reglas de enrutado y de parámetros (obligatorias):**

- **Un parámetro de ruta vacío produce `DAT-001` (400).** Las plantillas de ruta son
  permisivas: un segmento vacío o en blanco (p. ej. `/api/cuentas//movimientos`) llega al
  recurso como **cadena vacía**, no como error de enrutado. El recurso lo detecta y
  responde `DAT-001` (400) con su mensaje literal. **No** es `CTA-001`: una cuenta vacía no
  es "una cuenta solicitada que no existe", es un dato de la petición incompleto.
- **Una ruta que no corresponde a ninguna de las 9 operaciones produce el 404 del
  enrutador** (p. ej. `/api/cuentas/00200002/lista`, `/api/otro`). Ese 404 lo genera el
  enrutador de JAX-RS, **sin el sobre JSON**, y **está fuera del catálogo de negocio**: no es
  `CTA-001` ni `CLI-001` y por eso no se documenta en el OpenAPI. Los 404 documentados
  (`CTA-001`, `CLI-001`) sí son de negocio y sí llevan el sobre con `codigo` y `mensaje`.

---

## 7. Reglas de negocio (congeladas)

| Regla | Enunciado | Fuente |
|---|---|---|
| **RB-01** | Todo importe se maneja con `BigDecimal`, escala 2, redondeo `HALF_UP`. Nunca `double`/`float`. | BDD `decimal(12,2)` |
| **RB-02** | Un movimiento es una transacción única: actualiza `cuenta.dec_cuensaldo`, `cuenta.int_cuencontmov` e inserta en `movimiento`. Si algo falla, **rollback completo**. | Integridad referencial |
| **RB-03** | El saldo se bloquea con `SELECT ... FOR UPDATE` antes de operar; el saldo nunca queda negativo. | Consistencia |
| **RB-04** | `int_movinumero` = `cuenta.int_cuencontmov` anterior + 1. | `cuenta.int_cuencontmov` |
| **RB-05** | **Depósito**: saldo += monto; movimiento tipo `003` (INGRESO); `chr_cuenreferencia` = NULL. | `tipomovimiento` |
| **RB-06** | **Retiro**: saldo −= monto; movimiento tipo `004` (SALIDA); `chr_cuenreferencia` = NULL; exige saldo suficiente. | `tipomovimiento` |
| **RB-07** | **Transferencia**: saldo origen −= monto, saldo destino += monto, **en una sola transacción**. Dos movimientos: `009` (SALIDA) en el origen con `chr_cuenreferencia` = cuenta destino, y `008` (INGRESO) en el destino con `chr_cuenreferencia` = cuenta origen. Cada cuenta usa su propio contador. | `tipomovimiento` |
| **RB-08** | **ITF**: solo en transferencias. `itf = round(monto × valor(parametro '001') / 100, 2)`. Si `itf > 0`, se registra un movimiento `007` (SALIDA) adicional en la cuenta origen y se descuenta del saldo. | `parametro 001` |
| **RB-09** | **Costo por movimiento**: si el número de movimiento N supera `parametro '002'` (15), se cobra `costomovimiento.dec_costimporte` de la moneda de la cuenta y se registra movimiento `010` (SALIDA). El movimiento N es sin costo si N ≤ 15. | `parametro 002` + `costomovimiento` |
| **RB-10** | **Costo de mantenimiento**: se aplica (proyecta) cuando `cuenta.dec_cuensaldo ≤ cargomantenimiento.dec_cargMontoMaximo`; importe = `cargomantenimiento.dec_cargImporte`. | `cargomantenimiento` |
| **RB-11** | **Interés mensual (proyectado)**: `round(saldo × interesmensual.dec_inteimporte / 100, 2)`. | `interesmensual` |
| **RB-12** | El **cobro efectivo** del mantenimiento y el **abono** del interés mensual los ejecuta un **proceso batch externo** fuera del alcance de este servicio; el servicio solo los **proyecta** en `consultarCostosMantenimiento`. | Límite de alcance documentado |
| **RB-13** | La autenticación compara el **hash SHA-256** recibido contra `usuario.vch_emplclave` con `MessageDigest.isEqual` (comparación en tiempo constante). | Seguridad |
| **RB-14** | El token es un UUID aleatorio, con TTL de **30 minutos**, almacenado en un `ConcurrentHashMap` en memoria. Cada sesión abierta inserta una fila en `logsession`; `cerrarSesion` la cierra con `fec_salida`. | `logsession` |

---

## 8. Casos de uso (11, congelados)

| CU | Nombre | Operación(es) | Códigos de error propios | Modelos |
|---|---|---|---|---|
| **CU-01** | Autenticarse en el sistema | `autenticar`, `cerrarSesion` | DAT-001, AUTH-001, AUTH-002, AUTH-003, AUTH-004 | `CU_01_Autenticarse.oom` / `DA_01_Autenticarse.oom` / `DS_01_Autenticarse.oom` |
| **CU-02** | Validar la sesión del usuario | transversal (ops 3–9) | AUTH-003, AUTH-004 | `CU_02_ValidarSesion.oom` / `DA_02_ValidarSesion.oom` / `DS_02_TokenInvalido.oom` |
| **CU-03** | Consultar las cuentas de un cliente | `listarCuentasPorCliente`, `consultarCuenta` | CLI-001, CTA-001, CTA-002 | `CU_03_ConsultarCuentas.oom` / `DA_03_ConsultarCuentas.oom` / `DS_03_ConsultarCuentas.oom` |
| **CU-04** | Consultar los movimientos de una cuenta | `consultarMovimientos` | CTA-001, CTA-002 | `CU_04_ConsultarMovimientos.oom` / `DA_04_ConsultarMovimientos.oom` / `DS_04_ConsultarMovimientos.oom` |
| **CU-05** | Realizar un depósito | `realizarDeposito` | MON-001, CTA-001, CTA-002, MON-002 | `CU_05_RealizarDeposito.oom` / `DA_05_RealizarDeposito.oom` / `DS_05_RealizarDeposito.oom` |
| **CU-06** | Realizar un retiro | `realizarRetiro` | MON-001, CTA-001, CTA-002, SAL-001, MON-002 | `CU_06_RealizarRetiro.oom` / `DA_06_RealizarRetiro.oom` / `DS_06_RealizarRetiro.oom` |
| **CU-07** | Realizar una transferencia | `realizarTransferencia` | MON-001, CTA-001, CTA-002, CTA-003, CTA-004, SAL-001, MON-002 | `CU_07_RealizarTransferencia.oom` / `DA_07_RealizarTransferencia.oom` / `DS_07_RealizarTransferencia.oom` |
| **CU-08** | Consultar los costos de mantenimiento | `consultarCostosMantenimiento` | CTA-001, CTA-002, MON-002 | `CU_08_ConsultarCostosMantenimiento.oom` / `DA_08_ConsultarCostosMantenimiento.oom` / `DS_08_ConsultarCostosMantenimiento.oom` |
| **CU-09** | Generar la respuesta estructurada | transversal (todas) | — | `CU_09_RespuestaEstructurada.oom` / `DA_09_RespuestaEstructurada.oom` / `DS_09_RespuestaEstructurada.oom` |
| **CU-10** | Gestionar token inválido o expirado | transversal (ops 3–9) | AUTH-003, AUTH-004 | `CU_10_TokenInvalidoExpirado.oom` / `DA_10_TokenInvalidoExpirado.oom` / `DS_10_SesionExpirada.oom` |
| **CU-11** | Gestionar errores de negocio | transversal (todas) | CTA-001..004, CLI-001, SAL-001, MON-001/002, LIM-001, DAT-001, ERR-001 | `CU_11_ErroresNegocio.oom` / `DA_11_ErroresNegocio.oom` / `DS_11_ErrorNegocio.oom` |

> **Los 11 CU tienen ficha propia en el ECUD y modelo propio en las 3 familias de diagramas.**
> Esto replica exactamente el patrón del proyecto `TI.1.1` del docente (11 CU, 11 fichas, 12 `.oom`
> por familia). Los CU transversales (02, 09, 10, 11) se dibujan con `<<include>>` hacia los CU que
> los usan, pero **sí** tienen ficha y modelo propio.
>
> Además: `CU_Sistema.oom` (diagrama general de casos de uso), `DA_00_Sistema_General.oom` y
> `DS_12_Sistema_General.oom`.

---

## 9. Actores (congelados)

| Actor | Código | Tipo | Descripción |
|---|---|---|---|
| Cliente Bancario | `ACT-001` | humano | Persona con cuenta en EURECABANK; opera por el ATM/ventanilla, represented por los clientes de software |
| Cliente de Software Consola | `ACT-002` | sistema | `04. CLICON/EUREKABANK_SOAP_JAVA_CON` — **responsabilidad de Mateo** |
| Cliente de Software Escritorio | `ACT-003` | sistema | `04. CLIESC/` — responsabilidad del compañero 1 |
| Cliente de Software Web | `ACT-004` | sistema | `04. CLIWEB/` — responsabilidad del compañero 2 |
| Cliente de Software Móvil | `ACT-005` | sistema | `04. CLIMOV/` — responsabilidad del compañero 3 |
| Base de Datos EUREKABANK | `ACT-006` | sistema externo | MySQL 8.4 con el esquema `eurekabank` (17 tablas) |
| Proceso Batch de Mantenimiento | `ACT-007` | sistema externo | Proceso que cobra mantenimiento y abona interés (fuera de alcance, ver RB-12) |

> **Nota de alcance de esta variante REST (CONTEXTO §1).** Los actores se **heredan sin
> cambios** del contrato SOAP, pero en este proyecto **solo existen el servidor REST y el
> cliente de consola**:
> - `ACT-002` (consola) se materializa en `04. CLICON/EUREKABANK_RESTFUL_JAVA_CON`.
> - `ACT-003` (escritorio), `ACT-004` (web) y `ACT-005` (móvil) quedan **registrados como
>   actores de contrato, sin implementación** en esta variante.
> - `ACT-006` y `ACT-007` se mantienen igual (base de datos y proceso batch).

---

## 10. Estructura del servidor (paquetes Java, congelada)

### 10.1 Tabla de equivalencia SOAP → REST

| Paquete SOAP | Paquete REST | Qué cambia |
|---|---|---|
| `controladores.ws` (`WSEUREKABANK`, `WSEUREKABANKImpl`) | **`api.rest`** (`AuthResource`, `ClienteResource`, `CuentaResource`, `TransferenciaResource`, `ManejadorExcepcion`) | La fachada `@WebService` se sustituye por **recursos JAX-RS** (`@Path`, `@GET`, `@POST`, `@PathParam`, `@HeaderParam`, `@Consumes`, `@Produces`). Sin lógica de negocio: validan, delegan y envuelven. |
| `seguridad` (`TokenServicio`, `ServicioAutenticacion`) | `seguridad` (**idéntico**) + **`FiltroToken`** (`@Provider`) | `TokenServicio` y `ServicioAutenticacion` se **copian literales**. `FiltroToken` es lo **nuevo**: `ContainerRequestFilter` que **extrae** el token de `Authorization: Bearer <token>` y **nunca** responde `AUTH-003`/`AUTH-004` (§6.5). |
| `servicios` | `servicios` (**idéntico**) | `ServicioCuentas`, `ServicioMovimientos`, `ServicioCostos`, `FabricaRespuestas`: se copian literales. Sigue validando el token primero (§6.5). |
| `modelos` | `modelos` (**idéntico**) | 11 POJOs sin anotaciones: ni JAXB ni JSON-B. JSON-B los mapea solo. |
| `datos` | `datos` (**idéntico**) | `ProveedorConexion`, `UnidadTrabajo`, `UnidadTrabajoJdbc` y los 9 DAO con sus interfaces. |
| `utilidades` | `utilidades` (**idéntico**) | `Codigos`, `FormatoDinero`, `HashClave`, `Textos`. |

### 10.2 Árbol de paquetes

```
ec.edu.monster
├── api.rest                          ← LO NUEVO (sustituye a controladores.ws)
│   ├── AuthResource                 @Path("/api/auth")        login, logout
│   ├── ClienteResource              @Path("/api/clientes")    listarCuentasPorCliente
│   ├── CuentaResource               @Path("/api/cuentas")    consultar, movimientos, depositos, retiros, costos
│   ├── TransferenciaResource        @Path("/api/transferencias")
│   └── ManejadorExcepcion           @Provider  (mapea excepciones a códigos del catálogo)
├── seguridad
│   ├── TokenServicio                (idéntico al SOAP)
│   ├── ServicioAutenticacion        (idéntico al SOAP)
│   └── FiltroToken                  ← LO NUEVO: @Provider que lee Authorization: Bearer
├── servicios                        ← IDÉNTICO al SOAP
│   ├── ServicioCuentas
│   ├── ServicioMovimientos
│   ├── ServicioCostos
│   └── FabricaRespuestas
├── modelos                          ← IDÉNTICO al SOAP (11 POJOs, sin anotaciones)
│   ├── RespuestaBase, RespuestaAutenticacion, RespuestaCuentas, RespuestaCuenta,
│   │   RespuestaMovimientos, RespuestaMovimiento, RespuestaTransferencia, RespuestaCostos
│   ├── CuentaDTO, MovimientoDTO
│   └── CodigoRespuesta               ← enum con los 17 códigos de §5
├── datos                            ← IDÉNTICO al SOAP
│   ├── ProveedorConexion             ← DataSource por variables de entorno
│   ├── UnidadTrabajo, UnidadTrabajoJdbc
│   ├── ClienteDAO, CuentaDAO, MovimientoDAO, UsuarioDAO, ParametroDAO,
│   │   TipoMovimientoDAO, MonedaDAO, CostoDAO, LogSessionDAO
└── utilidades                       ← IDÉNTICO al SOAP
    ├── Codigos                        ← constantes de mensajes
    ├── FormatoDinero                  ← formateo BigDecimal a 2 decimales
    ├── HashClave                      ← SHA-256 de la clave
    └── Textos                         ← utilidades de texto
```

### 10.3 El paquete `api.rest` y el mapa de códigos HTTP

`ManejadorExcepcion` es el único punto donde se decide el **HTTP status**; los recursos
devuelven el sobre ya construido por `FabricaRespuestas` y el `status` que le corresponde.

| Código | HTTP status | Dónde se produce |
|---|---|---|
| `200` | 200 | `FabricaRespuestas` (mensaje propio de cada operación) |
| `DAT-001` | 400 | `api.rest` (recurso) y `seguridad` (token vacío) |
| `AUTH-001` | 401 | `seguridad.ServicioAutenticacion` |
| `AUTH-002` | 403 | `seguridad.ServicioAutenticacion` |
| `AUTH-003` | 401 | `seguridad.TokenServicio.validarToken` |
| `AUTH-004` | 401 | `seguridad.TokenServicio.validarToken` |
| `CTA-001` | 404 | `servicios` (consulta de cuenta inexistente) |
| `CTA-002` | 409 | `servicios` (cuenta no ACTIVO) |
| `CTA-003` | 400 | `servicios.ServicioMovimientos` (origen = destino) |
| `CTA-004` | 409 | `servicios.ServicioMovimientos` (moneda distinta) |
| `CLI-001` | 404 | `servicios.ServicioCuentas` (DNI inexistente) |
| `SAL-001` | 409 | `servicios.ServicioMovimientos` (saldo insuficiente) |
| `MON-001` | 400 | `servicios.ServicioMovimientos` (monto ≤ 0 o con más de 2 decimales) |
| `MON-002` | 409 | `servicios` (moneda no soportada) |
| `LIM-001` | 409 | **reservado**: ninguna de las 9 operaciones lo emite |
| `SER-001` | 503 | `api.rest.ManejadorExcepcion` (base de datos no disponible) |
| `ERR-001` | 500 | `api.rest.ManejadorExcepcion` (excepción no prevista) |

**Reglas del docente que no se negocian:**
- En `modelos` **solo** atributos, constructores, getters y setters. Cero lógica. **Ni
  anotaciones JAXB ni anotaciones JSON-B**: JSON-B mapea los POJOs planos solo.
- La lógica de negocio vive en `servicios`; el acceso a datos en `datos`; **el recurso REST
  no decide nada**: valida la cabecera y los parámetros (§6.5), delega y envuelve.
- El servidor **nunca** lanza al cliente una excepción por regla de negocio: siempre devuelve
  el sobre con `estado=ERROR`, su `codigo`, su `mensaje` literal y el **HTTP status
  correspondiente**.
- El **token viaja en la cabecera `Authorization: Bearer`**, nunca en la URL ni en el cuerpo.

---

## 11. Configuración (variables de entorno, congelada)

### 11.1 Variables del servidor

| Variable | Defecto | Uso |
|---|---|---|
| `EUREKABANK_DB_URL` | `jdbc:mysql://127.0.0.1:3306/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil` | URL del JDBC |
| `EUREKABANK_DB_USUARIO` | `eurekabank_app` | Usuario de aplicación |
| `EUREKABANK_DB_CLAVE` | `EurekaBank2026!` | Clave de aplicación |
| `EUREKABANK_TOKEN_MINUTOS` | `30` | TTL del token de sesión |
| `EUREKABANK_ORIGENES_CORS` | `*` | Orígenes permitidos (el cliente web necesita `http://localhost:4200`) |

> La resolución es la misma que en el proyecto SOAP: **variable de entorno → propiedad del
> sistema → fichero `eurekabank.properties` del classpath → valor por defecto**. Una variable
> de entorno *definida* manda aunque su valor esté vacío; en las otras fuentes un valor en
> blanco se ignora.

### 11.2 Variable del cliente de consola

| Variable | Defecto | Uso |
|---|---|---|
| `SERVICIO_URL` | `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api` | URL base del servicio REST |

> La URL del cliente se resuelve así: **argumento `arg0` → propiedad `-Dservicio.url` →
> variable de entorno `SERVICIO_URL` → defecto**. Ver §13.

---

## 12. Semilla de usuarios (congelada) — contraseña = SHA-256 hex de la clave entre paréntesis

| `chr_emplcodigo` | `vch_emplusuario` | `vch_emplclave` (SHA-256) | Clave real | Estado |
|---|---|---|---|---|
| `0001` | `cromero` | `891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4` | `Cromero2026` | ACTIVO |
| `0002` | `lcastro` | `d4b83a0cf9194ca8c9d5cb3f714596cfeb6543df33b618f9b05ef174df87aea4` | `Castro2026` | ACTIVO |
| `0003` | `creyes` | `32b3491336522e073489725b5daf298cd749007a` | *(sin clave conocida — ANULADO)* | ANULADO |
| `0004` | `aramos` | `f0bd8805123716f1d5237930643ae643b43c7f378ff8d387090eacfa73a128ee` | `Ramos2026` | ACTIVO |
| `0005` | `cvalencia` | `c8a50f632c3c4baf27fc05facb1883104e1d16ef` | *(sin clave conocida — ACTIVO)* | ACTIVO |
| `0006` | `rcruz` | `c6a5a412bea19bfa4133c0a15a97c20ddd49dc519330e07d82bdd9db926cc0ac` | `Cruz2026` | ACTIVO |
| `0007` | `ediaz` | `3718e00ac45cec21633e2211af9b77cd0a193698` | *(sin clave conocida — ANULADO)* | ANULADO |
| `0008` | `csarmiento` | `4503389bee2ee5ed2daf0125d825d7e364215989cce0f15804e67ac0fbce9adc` | `Sarmiento2026` | ANULADO |
| `0009` | `lpachas` | `386796f4e78be3c0ee919f1d97805e891fd076a940ba7e09db5601d94797b088` | `Pachas2026` | ACTIVO |
| `0010` | `htello` | `c35eb4169bb3099e24bf0590f0b3a6f3bab1a1d0cee46b182e8fb639e51b71e0` | `Tello2026` | ACTIVO |
| `0011` | `pcarrasco` | `558c296936404a839676a427bc1e63e94db38f5404d489cbe2bde9ab79a51c85` | `Carrasco2026` | ACTIVO |

> Los usuarios `creyes`, `cvalencia`, `ediaz` conservan el hash del *dump* de la guía (SHA-1 de 40
> caracteres). **No son utilizables** para autenticarse porque el sistema exige SHA-256 de 64
> caracteres (RB-13): la comparación falla y se devuelve `AUTH-001`. Se conservan para que la tabla
> tenga el mismo contenido que la guía. `creyes` y `ediaz` además están `ANULADO`, lo que activa
> `AUTH-002` en caso de que se prueben.

---

## 13. Estructura del cliente de consola (paquetes, congelada)

```
ec.edu.monster.consola
├── Main                              ← arranque, menú principal, resolución de la URL base
├── cliente
│   └── ClienteEUREKABANK            ← LO ÚNICO que sabe que existe HTTP y JSON
├── controlador
│   ├── ControladorAutenticacion
│   ├── ControladorCuentas
│   ├── ControladorMovimientos
│   ├── ControladorCostos
│   └── ControladorProtegido
├── modelo
│   ├── RespuestaBase, RespuestaAutenticacion, RespuestaCuentas, RespuestaCuenta,
│   │   RespuestaMovimientos, RespuestaMovimiento, RespuestaTransferencia, RespuestaCostos
│   ├── CuentaDTO, MovimientoDTO
│   └── SesionUsuario                 ← token, empleado, expiración
├── vista
│   ├── VistaAutenticacion, VistaCuentas, VistaMovimientos, VistaCostos, VistaMenu
└── util
    ├── Consola, Teclado, Formato, ExcepcionServicio, HashClave
```

> **El paquete `ws/` desaparece**: en el proyecto SOAP contenía el stub JAX-WS generado
> (`WS_EUREKABANK`) a partir del WSDL local. Aquí no hay WSDL ni `wsimport`: el cliente habla
> HTTP y JSON con `java.net.http.HttpClient`. **No existe el paquete `ws/`** en
> `04. CLICON/EUREKABANK_RESTFUL_JAVA_CON`.
>
> Recuento de la variante SOAP (30 clases): `Main` (1) + `cliente` (2: `ClienteWSEUREKABANK`,
> `IClienteEUREKABANK`) + `ws` (1) + `controlador` (5) + `modelo` (11) + `vista` (5) + `util` (5).
> En esta variante REST se eliminan `ws/` (1 clase) y el stub, y `cliente/` pasa a contener
> `ClienteEUREKABANK`.

**Reglas:**
- El transporte es **`java.net.http.HttpClient`** (del JDK, sin framework) y el mapeo de
  POJOs es **JSON-B (Yasson)**. **Sin Jackson** (no está en el repositorio Maven local).
- **No hay WSDL local ni generación de stub**: el cliente **compila con el servidor apagado**
  y conoce el contrato por el OpenAPI.
- La URL base del servicio se resuelve así: **argumento `arg0` → propiedad de sistema
  `-Dservicio.url` → variable de entorno `SERVICIO_URL` → defecto
  `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`**.
- Timeouts: **conexión 5 s, lectura 30 s**.
- El cliente envía la cabecera `Authorization: Bearer <token>` en las 8 operaciones que lo
  exigen (las 7 protegidas y `cerrarSesion`) y **nunca** lo pone en la URL.
- Un error HTTP o de transporte → `ExcepcionServicio` con el mensaje de `SER-001`
  (`El servicio no está disponible en este momento`).
- El cliente lee **los dos**: el HTTP status para la lógica y el `codigo`/`mensaje` del
  sobre para lo que se muestra al usuario.

---

## 14. Reglas de los diagramas PowerDesigner

| Aspecto | Regla |
|---|---|
| Casos de uso | `01_CASOS_USO/`: **12 `.oom`** = `CU_Sistema.oom` (general, con todos los actores y las relaciones `<<include>>`) + `CU_01_Autenticarse.oom` … `CU_11_ErroresNegocio.oom` (uno por CU). |
| Actividad | `02_ACTIVIDAD/`: **12 `.oom`** = `DA_00_Sistema_General.oom` + `DA_01_Autenticarse.oom` … `DA_11_ErroresNegocio.oom`. Solo cambian las palabras "servicio web SOAP" → "servicio web REST". |
| Secuencia | `03_SECUENCIA/`: **12 `.oom`** = `DS_01_Autenticarse.oom` … `DS_11_ErrorNegocio.oom` + `DS_12_Sistema_General.oom`. Un modelo por CU con el flujo principal; los CU 02, 09, 10 y 11 muestran el flujo de error. **Cambian los lifelines** (recurso REST en vez de fachada SOAP) y los mensajes (petición HTTP + JSON en vez de sobre SOAP). |
| Clases | `04_CLASES/`: **2 `.oom`** = `DC_Servidor.oom` (paquetes del servidor, sección de clases) + `DC_Cliente_Consola.oom` (paquetes de la consola). Deben ser **verificables contra el código real**: los nombres de clase, atributos y métodos se toman del Java implementado, no inventados. |
| Arquitectura | `05_ARQUITECTURA/`: **1 `.eam`** = `DA_EUREKABANK_RESTFUL_JAVA.eam`: 3 áreas (Consumidores / Controlador-Payara / Modelo-MySQL), aplicaciones cliente, enlace REST (HTTP/JSON) y enlace JDBC. |
| MER | `02. MER/02. LOGICO/ML_EUREKABANK.ldm` (17 entidades con atributos, PK, FK y cardinalidades) + `02. MER/03. FISICO/MF_EUREKABANK.pdm` (17 tablas con tipos MySQL 8.4, PK, FK, índices y CHECK). El conceptual va en `02. MER/01. CONCEPTUAL/`. **Se copia tal cual**: el modelo de datos no depende de la tecnología. |
| Rutas | Todos los `.prj` y el `.sws` con **rutas relativas**. Cero rutas absolutas. Nombre del workspace: `RESTFUL_JAVA_CONBDD_GR03.sws` en la raíz del proyecto. |
| Figuras | Cada modelo se exporta a imagen desde PowerDesigner y se incrusta en el ERS, el ECUD y el manual con su pie numerado. |
| Archivos por carpeta | Cada familia de diagramas lleva su `<FAMILIA>.prj` y `<FAMILIA>.pjb` (proyecto PowerDesigner) al que se registran sus modelos. |

### Plantilla de la ficha del ECUD (obligatoria, replicada del `TI.1.1` del docente)

Cada ficha es **una tabla** con estas filas, en este orden:

| Fila | Contenido |
|---|---|
| Nombre Caso de Uso | Nombre del CU |
| Identificador | `CU-01` … `CU-11` |
| Descripción | Párrafo de 3–5 líneas: qué hace, con qué operación REST, qué devuelve |
| Meta | Objetivo del CU desde el punto de vista del usuario |
| Estado / Versión | `Activo` / `1.0` |
| Autores | Los nombres del grupo (los del `TI.1.1` son *Eduardo García, Brayan Jacome, Mateo Iza*; **confirma con Mateo los de este grupo** y usa esos si no hay respuesta) |
| Fecha creación / Fecha modificación | `DD/MM/AAAA` (usar `04/11/2026`) |
| Datos de entrada | Lista `parámetro: descripción` (en REST, el token va como `Authorization: Bearer <token>`) |
| Datos de salida | Lista `resultado: descripción` (extensión sobre el `TI.1.1`) |
| Precondiciones | Lista de condiciones que deben cumplirse |
| Post condiciones | `Éxito: ...` / `Fallo: ...` |
| Gráfico | `Ilustración N. Caso de Uso de <nombre>` + la imagen del modelo |
| Flujo básico | Tabla de dos columnas `Actor` / `Sistema` con los pasos numerados 1..n |
| Flujo Alternativo | `FA-01 <condición> (paso n): <código> <mensaje literal>` … (un FA por cada código de error del CU) |
| Casos de uso incluidos | `<<include>>` hacia los CU que usa |
| Casos de uso extendidos | `<<extend>>` o vacío |
| Pruebas de aceptación | 2–4 condiciones verificables (extensión sobre el `TI.1.1`) |

Reglas de la plantilla:
- El **orden de los pasos** del flujo básico debe coincidir con el **orden de validación** de §6.
- Los **mensajes literales** de los FA son los de §5, **idénticos**, sin variantes.
- Cada "Gráfico" lleva su modelo en `01_CASOS_USO` y su imagen incrustada en el propio ECUD.
- El documento lleva `Índice de tablas` e `Índice de ilustraciones` como campos de Word (TOC), con los títulos `Tabla N. ECUD – CU-01: <nombre>` e `Ilustración N. Caso de Uso de <nombre>`.

---

## 15. Trazabilidad de requisitos (semilla para el ERS)

### 15.1 Requisitos funcionales (RF-01..RF-11)

| RF | Nombre | CU | Operación REST | Verbo y ruta |
|---|---|---|---|---|
| RF-01 | Autenticar usuario | CU-01 | `autenticar` | `POST /api/auth/login` |
| RF-02 | Cerrar sesión | CU-01 | `cerrarSesion` | `POST /api/auth/logout` |
| RF-03 | Validar token de sesión | CU-02 | transversal (ops 3–9) | cabecera `Authorization: Bearer <token>` |
| RF-04 | Listar cuentas por cliente | CU-03 | `listarCuentasPorCliente` | `GET /api/clientes/{cedula}/cuentas` |
| RF-05 | Consultar una cuenta | CU-03 | `consultarCuenta` | `GET /api/cuentas/{cuenta}` |
| RF-06 | Consultar movimientos | CU-04 | `consultarMovimientos` | `GET /api/cuentas/{cuenta}/movimientos` |
| RF-07 | Realizar depósito | CU-05 | `realizarDeposito` | `POST /api/cuentas/{cuenta}/depositos` |
| RF-08 | Realizar retiro | CU-06 | `realizarRetiro` | `POST /api/cuentas/{cuenta}/retiros` |
| RF-09 | Realizar transferencia | CU-07 | `realizarTransferencia` | `POST /api/transferencias` |
| RF-10 | Consultar costos de mantenimiento | CU-08 | `consultarCostosMantenimiento` | `GET /api/cuentas/{cuenta}/costos` |
| RF-11 | Emitir respuesta estructurada | CU-09 | transversal (todas) | sobre JSON `{estado, codigo, mensaje}` + payload |

### 15.2 Requisitos no funcionales (RNF-001..RNF-014)

| RNF | Categoría | Enunciado (semilla) | Parte del contrato que lo fija |
|---|---|---|---|
| RNF-001 | Rendimiento | Tiempo de respuesta acotado por operación con la base de datos local | §16 (entorno), §4 |
| RNF-002 | Fiabilidad | Todo movimiento de dinero es atómico, con `SELECT ... FOR UPDATE` y rollback completo | §7 RB-02, RB-03 |
| RNF-003 | Fiabilidad | Todos los importes con `BigDecimal`, escala 2 y `HALF_UP`; prohibido `double` | §3, §7 RB-01 |
| RNF-004 | Fiabilidad | El sobre es consistente en las 9 operaciones: `estado` y `codigo` siempre, `mensaje` literal | §3, §5 |
| RNF-005 | Seguridad | Las credenciales se guardan como hash SHA-256 de 64 caracteres, comparación en tiempo constante | §7 RB-13, §12 |
| RNF-006 | Seguridad | El control de acceso usa un token UUID con TTL de 30 min en `ConcurrentHashMap` | §7 RB-14, §4, §11 |
| RNF-007 | Seguridad | El servidor usa un usuario de aplicación de privilegio mínimo (`eurekabank_app`) | §11 |
| RNF-008 | Mantenibilidad | Separación en paquetes congelada; en `modelos` solo atributos, constructores, getters y setters | §10 |
| RNF-009 | Mantenibilidad | Suite de pruebas unitarias que cubra la lógica de negocio esencial | §16 |
| RNF-010 | Usabilidad | Todos los mensajes al usuario final en español, literales e idénticos al catálogo | §5 |
| RNF-011 | Adecuación funcional | El servicio cubre las 9 operaciones funcionales pactadas | §4 |
| RNF-012 | Compatibilidad | El contrato es OpenAPI 3.0.3 consumible por herramientas genéricas de interoperabilidad | §1, `04. SERVIDOR/contrato/eurekabank-openapi.yaml` |
| RNF-013 | Compatibilidad | El acceso desde el navegador funciona con el origen configurado en `EUREKABANK_ORIGENES_CORS` | §11 |
| RNF-014 | Portabilidad | Stack fijo y reproducible: JDK 17 con Maven, Payara 7, MySQL 8.4 reconstruible | §16 |

---

## 16. Entorno de pruebas (congelado para todos los agentes)

| Recurso | Valor |
|---|---|
| **MySQL 8.4 de pruebas** | `127.0.0.1:3306`, esquema `eurekabank`, usuario de aplicación `eurekabank_app` / `EurekaBank2026!` |
| Driver | `com.mysql.cj.jdbc.Driver` (mysql-connector-j 8.4.0) |
| URL de pruebas | `jdbc:mysql://127.0.0.1:3306/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil` |
| Maven | `"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"` (**no** está en el PATH) |
| JDK | Temurin 25 → compilar con `<maven.compiler.release>17</maven.compiler.release>` |
| Payara | `C:\payara7\bin\asadmin.bat`, dominio `domain1`, puerto **8080** |
| PowerDesigner | `C:\Program Files (x86)\Sybase\PowerDesigner 16` (32 bits) → ejecutar los VBS con `C:\Windows\SysWOW64\cscript.exe` |
| **Docker** | **NO instalado.** El `docker-compose.yml` se entrega para los compañeros y se valida por equivalencia de scripts contra MySQL 8.4. |

> La **base de datos es la misma** que la del proyecto SOAP (esquema `eurekabank`, 17 tablas,
> misma semilla): los dos servidores conviven en el puerto 8080 porque los context roots son
> distintos (`/EUREKABANK_SOAP_JAVA` y `/EUREKABANK_RESTFUL_JAVA`).
