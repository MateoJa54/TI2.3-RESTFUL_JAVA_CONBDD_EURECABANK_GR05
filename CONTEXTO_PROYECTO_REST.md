# CONTEXTO — TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03

> **Este documento es la memoria del proyecto.** Una sesión nueva de opencode que abra esta
> carpeta debe leerlo **completo antes de escribir una línea de código**. Todo lo que no esté
> aquí y no se pueda derivar de los proyectos de referencia, se pregunta al responsable.
>
> Versión 1.0 · 03/11/2026 · Grupo GR03

---

## 1. Qué hay que construir

Un sistema de servicios **web RESTful** en Java para el banco ficticio **EURECABANK**, con base de
datos MySQL 8.4, que entrega:

- ERS (Especificación de Requerimientos de Software, IEEE 830)
- ECUD (Especificación de Casos de Uso Detallada)
- Modelo entidad-relación en 3 niveles (conceptual, lógico, físico)
- Diagramas UML en PowerDesigner 16.5: casos de uso, actividad, secuencia, clases y arquitectura
- Servidor REST (Jakarta EE 10 sobre Payara 7) con pruebas
- Cliente de consola con pruebas
- Manual técnico y documentación de entrega

**Alcance:** solo el servidor y el cliente de consola. **No** hay clientes de escritorio, web ni
móvil en este proyecto (a diferencia del proyecto SOAP).

**Es la variante REST del proyecto SOAP ya terminado.** Ver §9.

---

## 2. Decisiones congeladas (no se cambian sin avisar al responsable)

| # | Decisión | Estado |
|---|---|---|
| D1 | Estilo **RESTful** con **JSON**, no SOAP | Aceptado |
| D2 | **La misma base de datos** que el proyecto SOAP: esquema `eurekabank`, 17 tablas, misma semilla | Aceptado |
| D3 | Contrato **OpenAPI 3.0.3** en `04. SERVIDOR/contrato/eurekabank-openapi.yaml` (equivalente REST del WSDL) | Aceptado |
| D4 | Servidor **Jakarta EE 10 / JAX-RS 3.1** sobre **Payara 7**, empaquetado como WAR | Aceptado |
| D5 | El token de sesión viaja en la cabecera **`Authorization: Bearer <token>`**, no en el cuerpo ni en la query | Aceptado |
| D6 | El **sobre de respuesta JSON** es `{estado, codigo, mensaje}` + payload, igual que en SOAP | Aceptado |
| D7 | Los **17 códigos de respuesta** y sus **mensajes literales** son los mismos; ahora el **HTTP status es real** (200/400/401/403/404/409/503/500) | Aceptado |
| D8 | Las capas `servicios`, `modelos`, `datos` y `utilidades` del servidor **se copian literales** del proyecto SOAP; solo cambia la capa de entrada | Aceptado |
| D9 | Cliente de consola con **`java.net.http.HttpClient`** (del JDK, sin framework) + **JSON-B (Yasson)** para mapear POJOs | Aceptado |
| D10 | Los **modelos son POJOs planos**: sin anotaciones JAXB **ni** JSON-B. JSON-B los mapea solo | Aceptado |
| D11 | Nombres de paquetes base `ec.edu.monster` (misma identidad del curso) | Aceptado |
| D12 | Nombre del WAR `EUREKABANK_RESTFUL_JAVA`, context root `/EUREKABANK_RESTFUL_JAVA`, base de la API `/api` | Aceptado |

---

## 3. Las 9 operaciones → endpoints REST

| # | Operación (mismo nombre que en SOAP) | Verbo y ruta | Cuerpo JSON de entrada | Respuesta |
|---|---|---|---|---|
| 1 | `autenticar` | `POST /api/auth/login` | `{usuario, clave}` | `RespuestaAutenticacion` |
| 2 | `cerrarSesion` | `POST /api/auth/logout` | `{}` | `RespuestaBase` |
| 3 | `listarCuentasPorCliente` | `GET /api/clientes/{cedula}/cuentas` | — | `RespuestaCuentas` |
| 4 | `consultarCuenta` | `GET /api/cuentas/{cuenta}` | — | `RespuestaCuenta` |
| 5 | `consultarMovimientos` | `GET /api/cuentas/{cuenta}/movimientos` | — | `RespuestaMovimientos` |
| 6 | `realizarDeposito` | `POST /api/cuentas/{cuenta}/depositos` | `{monto}` | `RespuestaMovimiento` |
| 7 | `realizarRetiro` | `POST /api/cuentas/{cuenta}/retiros` | `{monto}` | `RespuestaMovimiento` |
| 8 | `realizarTransferencia` | `POST /api/transferencias` | `{cuentaOrigen, cuentaDestino, monto}` | `RespuestaTransferencia` |
| 9 | `consultarCostosMantenimiento` | `GET /api/cuentas/{cuenta}/costos` | — | `RespuestaCostos` |

- Las 7 operaciones protegidas exigen la cabecera `Authorization: Bearer <token>`.
- `Content-Type: application/json; charset=utf-8` en peticiones y respuestas.
- Los **parametros de ruta** (`{cedula}`, `{cuenta}`) van en la URL; el **token NO** va en la URL.

---

## 4. Catálogo de respuestas (idéntico al del proyecto SOAP)

| Código | HTTP | Mensaje literal (obligatorio, sin variantes) |
|---|---|---|
| `200` | 200 | *(varía por operación — ver §5)* |
| `DAT-001` | 400 | `Los datos de la petición están incompletos o son incorrectos` |
| `AUTH-001` | 401 | `El usuario o la contraseña son incorrectos` |
| `AUTH-002` | 403 | `El usuario se encuentra inactivo` |
| `AUTH-003` | 401 | `El token de sesión no es válido` |
| `AUTH-004` | 401 | `El token de sesión ha expirado, inicie sesión nuevamente` |
| `CTA-001` | 404 | `La cuenta solicitada no existe` |
| `CTA-002` | 409 | `La cuenta no se encuentra en estado ACTIVO` |
| `CTA-003` | 400 | `La cuenta origen y la cuenta destino no pueden ser iguales` |
| `CTA-004` | 409 | `La cuenta de destino pertenece a una moneda distinta` |
| `CLI-001` | 404 | `El cliente solicitado no existe` |
| `SAL-001` | 409 | `El saldo de la cuenta es insuficiente para la operación` |
| `MON-001` | 400 | `El monto debe ser mayor que cero y con máximo dos decimales` |
| `MON-002` | 409 | `La moneda de la cuenta no está soportada por el sistema` |
| `LIM-001` | 409 | `El monto supera el límite máximo de mantenimiento de la cuenta` |
| `SER-001` | 503 | `El servicio no está disponible en este momento` |
| `ERR-001` | 500 | `Ocurrió un error interno, intente nuevamente` |

Mensajes de éxito por operación:

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

> En REST, el **HTTP status** acompaña siempre al código de negocio: `200` con `codigo=200`,
> `401` con `codigo=AUTH-003`, etc. El cliente debe leer **los dos**: el status para la lógica
> HTTP y el `codigo` para el mensaje de negocio.

---

## 5. Orden de validación (idéntico al del proyecto SOAP — se respeta al pie de la letra)

Si una petición viola dos reglas, **gana la que aparece primero**.

### 5.1 Operaciones autenticadas (3 a 9)

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

### 5.2 `autenticar`

```
1. DAT-001  → usuario o clave vacíos
2. AUTH-001 → el usuario no existe o el hash SHA-256 no coincide
3. AUTH-002 → usuario.vch_emplestado ∈ {ANULADO, CANCELADO}
4. Éxito    → genera token (TTL 30 min) e inserta en logsession
```

### 5.3 `cerrarSesion`

```
1. DAT-001 → falta la cabecera Authorization
2. AUTH-003 → el token no existe
3. AUTH-004 → el token expiró (igualmente se cierra la sesión en logsession)
4. Éxito    → marca logsession.fec_salida y descarta el token
```

---

## 6. Reglas de negocio (idénticas al proyecto SOAP)

| Regla | Enunciado |
|---|---|
| RB-01 | Todo importe con `BigDecimal`, escala 2, redondeo `HALF_UP`. Nunca `double`. |
| RB-02 | Un movimiento es una transacción única: actualiza `cuenta.dec_cuensaldo`, `cuenta.int_cuencontmov` e inserta en `movimiento`. Si algo falla, **rollback completo**. |
| RB-03 | El saldo se bloquea con `SELECT ... FOR UPDATE` antes de operar; nunca queda negativo. |
| RB-04 | `int_movinumero` = `cuenta.int_cuencontmov` anterior + 1. |
| RB-05 | **Depósito**: saldo += monto; movimiento tipo `003` (INGRESO); `chr_cuenreferencia` = NULL. |
| RB-06 | **Retiro**: saldo −= monto; movimiento tipo `004` (SALIDA); `chr_cuenreferencia` = NULL; exige saldo suficiente. |
| RB-07 | **Transferencia**: saldo origen −= monto, destino += monto, **en una sola transacción**. Movimientos `009` (SALIDA) en el origen con `chr_cuenreferencia` = cuenta destino, y `008` (INGRESO) en el destino con `chr_cuenreferencia` = cuenta origen. Cada cuenta usa su propio contador. |
| RB-08 | **ITF**: solo en transferencias. `itf = round(monto × valor(parametro '001') / 100, 2)`. Si `itf > 0`, movimiento `007` (SALIDA) adicional en el origen. |
| RB-09 | **Costo por movimiento**: si el número de movimiento N supera `parametro '002'` (15), se cobra `costomovimiento.dec_costimporte` y se registra movimiento `010` (SALIDA). El movimiento N es sin costo si N ≤ 15. |
| RB-10 | **Costo de mantenimiento**: se proyecta cuando `cuenta.dec_cuensaldo ≤ cargomantenimiento.dec_cargMontoMaximo`; importe = `cargomantenimiento.dec_cargImporte`. |
| RB-11 | **Interés mensual (proyectado)**: `round(saldo × interesmensual.dec_inteimporte / 100, 2)`. |
| RB-12 | El **cobro efectivo** del mantenimiento y el **abono** del interés los ejecuta un proceso batch externo **fuera de alcance**; el servicio solo los proyecta. |
| RB-13 | La autenticación compara el hash **SHA-256** recibido contra `usuario.vch_emplclave` con `MessageDigest.isEqual` (tiempo constante). |
| RB-14 | El token es un UUID aleatorio, TTL **30 minutos**, en un `ConcurrentHashMap`. Cada sesión abierta inserta una fila en `logsession`; `cerrarSesion` la cierra con `fec_salida`. |

---

## 7. Modelo de datos (17 tablas, las mismas)

| Tabla | PK | FK |
|---|---|---|
| `empleado` | `chr_emplcodigo` char(4) | — |
| `usuario` | `chr_emplcodigo` char(4) | → `empleado` |
| `permiso` | (`chr_emplcodigo`,`int_moducodigo`) | → `usuario`, `modulo` |
| `modulo` | `int_moducodigo` int | — |
| `cliente` | `chr_cliecodigo` char(5) | — |
| `sucursal` | `chr_sucucodigo` char(3) | — |
| `moneda` | `chr_monecodigo` char(2) | — |
| `cuenta` | `chr_cuencodigo` char(8) | → `cliente`, `moneda`, `sucursal`, `empleado` |
| `movimiento` | (`chr_cuencodigo`,`int_movinumero`) | → `cuenta`, `empleado`, `tipomovimiento` |
| `tipomovimiento` | `chr_tipocodigo` char(3) | — |
| `cargomantenimiento` | `chr_monecodigo` | → `moneda` |
| `costomovimiento` | `chr_monecodigo` | → `moneda` |
| `interesmensual` | `chr_monecodigo` | → `moneda` |
| `parametro` | `chr_paracodigo` char(3) | — |
| `contador` | `vch_conttabla` | — |
| `logsession` | `ID` autoinc | → `empleado` |
| `asignado` | `chr_asigcodigo` char(6) | → `empleado`, `sucursal` |

Tipos de movimiento (`tipomovimiento`): `001` Apertura INGRESO · `002` Cancelar SALIDA · `003` Depósito
INGRESO · `004` Retiro SALIDA · `005` Interés INGRESO · `006` Mantenimiento SALIDA · `007` ITF SALIDA ·
`008` Transferencia INGRESO · `009` Transferencia SALIDA · `010` Cargo por Movimiento SALIDA.

Parámetros: `001` = ITF = `0.08` (porcentaje) · `002` = Número de Operaciones Sin Costo = `15`.

**Usuarios de prueba** (contraseña en claro; el cliente envía el SHA-256):

| Usuario | Clave | Empleado | Estado |
|---|---|---|---|
| `cromero` | `Cromero2026` | 0001 Carlos Alberto Romero Castillo | ACTIVO |
| `lcastro` | `Castro2026` | 0002 Lidia Castro Vargas | ACTIVO |
| `aramos` | `Ramos2026` | 0004 Angelica Ramos Garibay | ACTIVO |
| `rcruz` | `Cruz2026` | 0006 Ricardo Cruz Tarazona | ACTIVO |
| `lpachas` | `Pachas2026` | 0009 Luis Alberto Pachas Sifuentes | ACTIVO |
| `htello` | `Tello2026` | 0010 Hugo Valentin Tello Alarcon | ACTIVO |
| `pcarrasco` | `Carrasco2026` | 0011 Pedro Hugo Carrasco Vargas | ACTIVO |
| `creyes` | cualquiera | 0003 | ANULADO → `AUTH-002` |
| `csarmiento` | `Sarmiento2026` | 0008 | ANULADO → `AUTH-002` |

Datos de apoyo: DNI `06914897` → Eric Gustavo Coronel Castillo (cuenta `00200002`, Soles, 6800.00).
Cuenta `00300001` está CANCELADA (→ `CTA-002`). Cuentas `00100002` y `00200003` están en Dólares
(→ `CTA-004` en transferencias).

---

## 8. Estructura del proyecto

```
TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03/
├── 01. UML/
│   ├── 00. CONTRATO/      CONTRATO_CONGELADO_REST.md  (la fuente de verdad de ESTE proyecto)
│   ├── 01. ERS/           ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx
│   ├── 02. ECUD/          ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx
│   └── 03. DIAGRAMAS/     01_CASOS_USO/ 02_ACTIVIDAD/ 03_SECUENCIA/ 04_CLASES/ 05_ARQUITECTURA/
├── 02. MER/               (ya copiado del proyecto SOAP: 17 tablas, idéntico)
├── 03. BDD/               (ya copiado del proyecto SOAP: MISMOS scripts, MISMA base de datos)
├── 04. SERVIDOR/
│   ├── contrato/          eurekabank-openapi.yaml
│   └── EUREKABANK_RESTFUL_JAVA/       proyecto Maven (WAR)
├── 04. CLICON/
│   └── EUREKABANK_RESTFUL_JAVA_CON/   proyecto Maven (JAR)
├── 05. DOCUMENTACION/     MANUAL_EUREKABANK_RESTFUL_JAVA_GR03.docx
├── CONTEXTO_PROYECTO_REST.md   este fichero
├── PROMPT_INICIO.md             el prompt de arranque
└── RESTFUL_JAVA_CONBDD_GR03.sws  workspace de PowerDesigner
```

### 8.1 Paquetes del servidor (D8: cuatro capas se copian literales)

```
ec.edu.monster
├── api.rest                          ← LO NUEVO (sustituye a controladores.ws)
│   ├── AuthResource                 @Path("/api/auth")        login, logout
│   ├── ClienteResource              @Path("/api/clientes")     listarCuentasPorCliente
│   ├── CuentaResource               @Path("/api/cuentas")     consultar, movimientos, depositos, retiros, costos
│   ├── TransferenciaResource        @Path("/api/transferencias")
│   └── ManejadorExcepcion           @Provider  (mapea excepciones a códigos del catálogo)
├── seguridad                         ← se adapta
│   ├── TokenServicio                (idéntico al SOAP)
│   ├── ServicioAutenticacion        (idéntico al SOAP)
│   └── FiltroToken                  ← LO NUEVO: @Provider que lee Authorization: Bearer
├── servicios                        ← IDÉNTICO al SOAP (ServicioCuentas, ServicioMovimientos,
│                                      ServicioCostos, FabricaRespuestas)
├── modelos                          ← IDÉNTICO al SOAP (11 POJOs, sin anotaciones)
├── datos                            ← IDÉNTICO al SOAP (ProveedorConexion, UnidadTrabajo, 9 DAO)
└── utilidades                       ← IDÉNTICO al SOAP (Codigos, FormatoDinero, HashClave, Textos)
```

### 8.2 Paquetes del cliente de consola (D9)

```
ec.edu.monster.consola
├── Main                    arranque + resolución de la URL base
├── cliente
│   └── ClienteEUREKABANK   ← LO ÚNICO que sabe que existe HTTP y JSON
├── controlador             ControladorAutenticacion, ControladorCuentas, ControladorMovimientos,
│                           ControladorCostos, ControladorProtegido
├── modelo                  RespuestaBase + las 7 derivadas, CuentaDTO, MovimientoDTO, SesionUsuario
├── vista                   VistaMenu, VistaAutenticacion, VistaCuentas, VistaMovimientos, VistaCostos
└── util                    Consola, Teclado, Formato, ExcepcionServicio, HashClave
```

---

## 9. Los proyectos de referencia (léelos, no los inventes)

### 9.1 Proyecto SOAP equivalente — **la fuente principal**

`C:\Users\mateo\Downloads\Arquit\Unidad1\BddLocal\TI2.1 SOAP_JAVA_CONBDD_EURECABANK_GR03\`

Ya está **completo, verificado y subido a GitHub**. Contiene:

- `01. UML\00. CONTRATO\CONTRATO_CONGELADO_EUREKABANK.md` → el contrato SOAP completo, con las
  9 operaciones, los 17 códigos, el orden de validación y las 14 RB. **Copia de este contenido a
  `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md` y adapta los §4 (operaciones → endpoints) y
  §16 (entorno).**
- `04. SERVIDOR\EUREKABANK_SOAP_JAVA\src\main\java\ec\edu\monster\` → **el servidor SOAP completo**
  (45 clases, 265 pruebas verdes). Copia `modelos`, `datos`, `servicios`, `utilidades` y `seguridad`
  tal cual, y reescribe solo la capa de entrada.
- `04. CLICON\EUREKABANK_SOAP_JAVA_CON\src\main\java\ec\edu\monster\consola\` → **el cliente de
  consola SOAP completo** (30 clases, 330 pruebas verdes). Copia `modelo`, `vista`, `controlador`,
  `util` y `Main`; reescribe `cliente/`.
- `04. SERVIDOR\contrato\WSEUREKABANK.wsdl` + `WSEUREKABANK_schema.xsd` → el contrato SOAP.
- `01. UML\01. ERS\ERS_*.docx` (60 tablas, 15 figuras), `01. UML\02. ECUD\ECUD_*.docx` (11 fichas),
  `05. DOCUMENTACION\MANUAL_*.docx` (66 figuras), `02. MER\` (3 modelos) y los 39 diagramas.

### 9.2 Los generadores de los Word están en el **historial git** del proyecto SOAP

Se borraron del árbol de trabajo para limpiar la entrega, pero siguen en el commit `2af276d`.
Recupéralos así y **adáptalos** en vez de escribir 2500 líneas desde cero:

```powershell
$soap = "C:\Users\mateo\Downloads\Arquit\Unidad1\BddLocal\TI2.1 SOAP_JAVA_CONBDD_EURECABANK_GR03"
git -C $soap show "2af276d:01. UML/01. ERS/generar_ers.py" > "01. UML\01. ERS\generar_ers.py"
git -C $soap show "2af276d:01. UML/02. ECUD/generar_ecud.py" > "01. UML\02. ECUD\generar_ecud.py"
git -C $soap show "2af276d:01. UML/01. ERS/ensamblar_ers.py" > "01. UML\01. ERS\ensamblar_ers.py"
git -C $soap show "2af276d:01. UML/02. ECUD/ensamblar_ecud.py" > "01. UML\02. ECUD\ensamblar_ecud.py"
git -C $soap show "2af276d:05. DOCUMENTACION/generar_manual.py" > "05. DOCUMENTACION\generar_manual.py"
git -C $soap show "2af276d:01. UML/03. DIAGRAMAS/scripts/gen_casos_uso.vbs" > "01. UML\03. DIAGRAMAS\scripts\gen_casos_uso.vbs"
git -C $soap show "2af276d:01. UML/03. DIAGRAMAS/scripts/gen_actividad.vbs" > "01. UML\03. DIAGRAMAS\scripts\gen_actividad.vbs"
git -C $soap show "2af276d:01. UML/03. DIAGRAMAS/scripts/gen_secuencia.vbs" > "01. UML\03. DIAGRAMAS\scripts\gen_secuencia.vbs"
git -C $soap show "2af276d:01. UML/03. DIAGRAMAS/scripts/gen_clases.vbs" > "01. UML\03. DIAGRAMAS\scripts\gen_clases.vbs"
git -C $soap show "2af276d:01. UML/03. DIAGRAMAS/scripts/verificar_entregables.py" > "01. UML\03. DIAGRAMAS\scripts\verificar_entregables.py"
```

Lo mismo para el resto de scripts de `01. UML\03. DIAGRAMAS\scripts\` y `02. MER\scripts\`
(lista completa: `git -C $soap ls-tree -r --name-only 2af276d`).

### 9.3 Otros proyectos de referencia

- `C:\Users\mateo\Downloads\Arquit\Unidad1\SinBdd\TI.1.1 PAO202651_SOAP_JAVA_SINBDD_NRC36883_GR05\`
  → **el proyecto del docente ya aceptado**: ERS con 32 tablas, ECUD con 11 fichas de 17 filas,
  41 modelos PowerDesigner, estructura de carpetas `01. UML / 02. MER / 03. BDD / 04. SERVIDOR /
  04. CLICON / 05. DOCUMENTACION`. **Copia su estilo de documento y de diagrama.**
- `C:\Users\mateo\.claude\skills\proyecto-arquitectura-java\` → skill con las reglas de la
  arquitectura Java, los scripts de PowerDesigner y las referencias de documentación. **Léela.**

---

## 10. Entorno (hechos verificados en esta máquina)

| Recurso | Dato |
|---|---|
| **Maven** | `"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"` — **NO está en el PATH** |
| **JDK** | Temurin 25 → compilar con `<maven.compiler.release>17</maven.compiler.release>` |
| **Payara** | `C:\payara7\bin\asadmin.bat`, dominio `domain1`, puerto de admin 4848, HTTP 8080 |
| **MySQL 8.4** | `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`. Servicio del usuario en **3306** (su `root` tiene clave) |
| **PowerDesigner** | `C:\Program Files (x86)\Sybase\PowerDesigner 16` (32 bits). Ejecutar VBS con `C:\Windows\SysWOW64\cscript.exe`; **desde PowerShell el COM no funciona** (exporta símbolos sin propiedades) → usa `skills\proyecto-arquitectura-java\scripts\powerdesigner\ejecutar-vbs.ps1` |
| **Python** | 3.12 con `python-docx` (para generar los Word) |
| **Docker** | **NO instalado.** El `docker-compose.yml` se entrega para los compañeros y se valida ejecutando los mismos scripts contra el MySQL local |
| **NetBeans** | `C:\Program Files\Apache NetBeans` — en NetBeans el Maven funciona sin configurar el PATH |

### 10.1 Dependencias disponibles **offline** en `~/.m2` (usar estas, no otras)

| Artefacto | Versiones |
|---|---|
| `jakarta.platform:jakarta.jakartaee-api` | 11.0.0, 11.0.0-M1 |
| `jakarta.ws.rs:jakarta.ws.rs-api` | 3.1.0, 4.0.0 |
| `jakarta.json:jakarta.json-api` | 2.1.0, 2.1.3 |
| `jakarta.json.bind:jakarta.json.bind-api` | 3.0.0, 3.0.1 |
| `org.eclipse:yasson` (impl. JSON-B) | 3.0.4 |
| `org.eclipse.parsson` (impl. JSON-P) | 1.1.7 |
| `org.glassfish.jersey.core:jersey-client` / `jersey-hk2` | 4.0.2 |
| `com.mysql:mysql-connector-j` | 8.4.0 |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 |
| `org.apache.maven.plugins:maven-compiler-plugin` | 3.12.1 |
| `org.apache.maven.plugins:maven-war-plugin` | 3.4.0 |
| `org.apache.maven.plugins:maven-surefire-plugin` | 3.2.5 |
| `org.codehaus.mojo:exec-maven-plugin` | 3.5.1 |

**NO disponibles:** Jackson (`com.fasterxml.jackson.*`), `jersey-media-json-jackson`,
`jersey-media-json-processing`, `microprofile-config-api`, `com.sun.xml.ws:rt`.
→ **No los uses.** El stack REST se monta con JAX-RS (lo da el contenedor) + JSON-B/JSON-P.

### 10.2 Hecho verificado que te ahorra un problema

**JSON-B (Yasson) serializa y deserializa los POJOs planos sin una sola anotación**, y usa
como nombre de propiedad el del getter (`getCodigo` → `codigo`), que ya coincide con el contrato:

```
SERIALIZA:   {"cedula":"06914897","cuentas":[{"codigo":"00200002","saldo":6800.00}]}
DESERIALIZA: codigo=00200002 saldo=6800.00
```

Verificado ejecutando un proyecto Maven mínimo **offline** con
`jakarta.json.bind-api:3.0.1` + `jakarta.json-api:2.1.3` + `yasson:3.0.4` + `parsson:1.1.7`.
→ **No pongas anotaciones JSON en los modelos.**

### 10.3 Base de datos: es la MISMA que la del proyecto SOAP

- Esquema `eurekabank`, 17 tablas, misma semilla. **Los dos servidores (SOAP y REST) comparten
  base de datos.** Como los context roots son distintos (`/EUREKABANK_SOAP_JAVA` y
  `/EUREKABANK_RESTFUL_JAVA`), conviven en el puerto 8080.
- Usuario de aplicación: `eurekabank_app` / `EurekaBank2026!` (privilegio mínimo).
- Cargar la base de datos (pide la clave de `root` de forma segura):
  ```powershell
  & ".\03. BDD\cargar_bdd.ps1"
  ```
- Restaurar los datos de prueba después de las pruebas que mutan la base:
  ```sql
  CALL sp_cargar_datos_prueba();
  ```
- Variables de entorno que lee el servidor (`ProveedorConexion`): `EUREKABANK_DB_URL`,
  `EUREKABANK_DB_USUARIO`, `EUREKABANK_DB_CLAVE`, `EUREKABANK_TOKEN_MINUTOS`,
  `EUREKABANK_ORIGENES_CORS`.
  Defecto por defecto: `jdbc:mysql://127.0.0.1:3306/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil`.

---

## 11. Fases de trabajo

| # | Fase | Entregable | Criterio de aceptación |
|---|---|---|---|
| 0 | Cimientos | Árbol de carpetas, `.gitignore`, `.gitattributes`, `README.md`, `git init` | El árbol existe y el README lo describe |
| 1 | Contrato | `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md` + `04. SERVIDOR\contrato\eurekabank-openapi.yaml` | El OpenAPI declara las 9 operaciones con sus cuerpos, respuestas, códigos HTTP y el esquema `Bearer`; el contrato Markdown coincide con el YAML |
| 2 | ERS | `01. UML\01. ERS\ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx` (≥ 60 tablas) | Adaptado del SOAP: RF-01..11, RNF-001..014, RB-01..14, 17 códigos, matriz de trazabilidad; **cero** menciones de SOAP/WSDL/XSD fuera de la comparación con el proyecto SOAP |
| 3 | ECUD | `01. UML\02. ECUD\ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx` (11 fichas de 17 filas) | Los mismos 11 CU, los mismos 44 flujos alternativos con los **mensajes literales idénticos**, y las entradas de token ahora en la cabecera `Authorization` |
| 4 | Diagramas | 12 CU + 12 DA + 12 DS + 2 DC + 1 EAM en `01. UML\03. DIAGRAMAS\` | Ver §12 |
| 5 | Auditoría | `01. UML\00. CONTRATO\AUDITORIA_COHERENCIA.md` | 9/9 comprobaciones OK entre código ↔ contrato ↔ ERS ↔ ECUD ↔ OpenAPI ↔ diagramas |
| 6 | Servidor | `04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\` | `mvn clean package` verde, ≥ 300 pruebas, extremo a extremo real contra MySQL, WSDL→OpenAPI coherente |
| 7 | Consola | `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\` | `mvn clean package` verde **con el servidor apagado**, ≥ 300 pruebas, flujo real contra el servidor desplegado |
| 8 | Documentación | `05. DOCUMENTACION\MANUAL_EUREKABANK_RESTFUL_JAVA_GR03.docx`, `README.md`, `ENTREGABLES.md` | Manual con las figuras incrustadas e índices con campos de Word |

**Orden obligatorio:** BDD/MER → contrato → ERS → ECUD → diagramas → auditoría → servidor →
consola → manual. No se empieza a codificar antes de cerrar el contrato y los documentos.

---

## 12. Qué se copia y qué se rehace en los diagramas

Esto es lo que más engaña en la migración de SOAP a REST. **No es solo el diagrama de clases.**

| Familia | Cantidad | Acción | Por qué |
|---|---|---|---|
| Casos de uso | 12 `.oom` | **COPIAR tal cual** | Un caso de uso no menciona SOAP |
| Actividad | 12 `.oom` | COPIAR y ajustar etiquetas | Solo cambian las palabras "servicio web SOAP" → "servicio web REST" |
| **Secuencia** | 12 `.oom` | **REHACER** | Cambian los lifelines (recurso REST en vez de fachada SOAP) y los mensajes (petición HTTP + JSON en vez de sobre SOAP) |
| **Clases** | 2 `.oom` | **REHACER** | Paquetes nuevos (`api.rest`), anotaciones JAX-RS, sin JAXB |
| **Arquitectura** | 1 `.eam` | **REHACER** | El nodo del servicio pasa a ser REST con sus endpoints; el enlace SOAP se sustituye por HTTP/JSON |
| MER | 3 modelos | **COPIAR tal cual** | El modelo de datos es independiente de la tecnología |

Todos los modelos se registran en su `.prj` y en el workspace `RESTFUL_JAVA_CONBDD_GR03.sws`
con **rutas relativas** (cero rutas absolutas), y sus figuras van incrustadas en los Word.

---

## 13. Reglas que no se negocian

1. **En `modelos` solo** atributos, constructores, getters y setters. Ni lógica, ni anotaciones
   JAXB, ni anotaciones JSON-B.
2. La lógica de negocio vive en `servicios`; el acceso a datos en `datos`; los recursos REST solo
   validan, delegan y envuelven. **El recurso REST no decide nada.**
3. El servidor **nunca** lanza al cliente una excepción por regla de negocio: siempre devuelve
   el sobre con `estado=ERROR` y su código, con el HTTP status correspondiente.
4. Importes con `BigDecimal` y 2 decimales, `HALF_UP`. Nunca `double`.
5. Los **mensajes de error son literales exactos**: los del §4, sin variantes ni sinónimos.
6. El token va en `Authorization: Bearer`, **nunca** en la URL (quedaría en el log del servidor).
7. La contraseña viaja como **SHA-256 hex**; comparación en tiempo constante.
8. Cada operación mueve dinero dentro de una **transacción** con `SELECT ... FOR UPDATE`.
9. En los documentos: los índices son **campos de Word** (TOC / SEQ), no texto escrito a mano.
10. En PowerDesigner: **Cierra PowerDesigner antes de correr los scripts**; el COM de PD 16 solo
    funciona desde el `cscript` de 32 bits; y **mira el PNG exportado antes de dar el diagrama
    por bueno**.

---

## 14. Preguntas abiertas (las responde el responsable)

1. **Integrantes del grupo** para la portada del ERS, el ECUD y el manual (mismo grupo GR03).
2. **Nombre del repositorio** en GitHub (el del SOAP quedó como `TI2.1-SOAP_JAVA_CONBDD_EURECABANK_GR05`).
3. Si la clave de `root` de MySQL 3306 hay que solicitarla o si se deja la que ya tiene.
