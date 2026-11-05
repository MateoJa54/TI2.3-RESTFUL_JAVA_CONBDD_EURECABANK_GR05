# EURECABANK — Modelo de datos (17 tablas)

Documento de referencia para los agentes del **MER** (conceptual, lógico y
físico) y del **ERS**. Todo lo que sigue está verificado contra la base
`eurekabank` creada por `docker/init/01_estructura.sql` y `02_datos.sql` en
**MySQL 8.4**.

- Motor: MySQL 8.4 · Esquema: `eurekabank` · Charset: `utf8mb4` ·
  Collation: `utf8mb4_0900_ai_ci` · Zona horaria: `America/Guayaquil` (UTC-5)
- Fuente de verdad funcional: `01. UML/00. CONTRATO/CONTRATO_CONGELADO_REST.md`
- Origen del esquema: `03.BDD/Estructura.sql` y `03.BDD/Datos.sql` de la guía

---

## 1. Inventario de tablas

| # | Tabla | Clave primaria | Llaves foráneas | Filas en la semilla |
|---|---|---|---|---|
| 1 | `moneda` | `chr_monecodigo` | — | 2 |
| 2 | `empleado` | `chr_emplcodigo` | — | 14 |
| 3 | `sucursal` | `chr_sucucodigo` | — | 7 |
| 4 | `cliente` | `chr_cliecodigo` | — | 20 |
| 5 | `modulo` | `int_moducodigo` | — | 6 |
| 6 | `tipomovimiento` | `chr_tipocodigo` | — | 10 |
| 7 | `usuario` | `chr_emplcodigo` | → `empleado` | 11 |
| 8 | `parametro` | `chr_paracodigo` | — | 2 |
| 9 | `contador` | `vch_conttabla` | — | 7 |
| 10 | `permiso` | (`chr_emplcodigo`,`int_moducodigo`) | → `usuario`, `modulo` | 42 |
| 11 | `asignado` | `chr_asigcodigo` | → `empleado`, `sucursal` | 11 |
| 12 | `cargomantenimiento` | `chr_monecodigo` | → `moneda` | 2 |
| 13 | `costomovimiento` | `chr_monecodigo` | → `moneda` | 2 |
| 14 | `interesmensual` | `chr_monecodigo` | → `moneda` | 2 |
| 15 | `cuenta` | `chr_cuencodigo` | → `cliente`, `moneda`, `sucursal`, `empleado` | 6 |
| 16 | `movimiento` | (`chr_cuencodigo`,`int_movinumero`) | → `cuenta`, `empleado`, `tipomovimiento` | 74 |
| 17 | `logsession` | `ID` (autoincremental) | → `empleado` | 0 (auditoría en runtime) |

---

## 2. Diccionario de datos

### 2.1 `moneda` — catálogo de monedas

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_monecodigo` | char(2) | Sí | — | No | — | Código de moneda: `01` = Soles, `02` = Dólares |
| `vch_monedescripcion` | varchar(20) | No | — | No | — | Nombre de la moneda |

### 2.2 `empleado` — maestro de empleados

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_emplcodigo` | char(4) | Sí | — | No | — | Código del empleado (`0001`..`0014`) |
| `vch_emplpaterno` | varchar(25) | No | — | No | — | Apellido paterno |
| `vch_emplmaterno` | varchar(25) | No | — | No | — | Apellido materno |
| `vch_emplnombre` | varchar(30) | No | — | No | — | Nombres |
| `vch_emplciudad` | varchar(30) | No | — | No | — | Ciudad de residencia |
| `vch_empldireccion` | varchar(50) | No | — | Sí | NULL | Dirección (opcional) |

### 2.3 `sucursal` — sucursales del banco

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_sucucodigo` | char(3) | Sí | — | No | — | Código de sucursal (`001`..`007`) |
| `vch_sucunombre` | varchar(50) | No | — | No | — | Nombre de la sucursal |
| `vch_sucuciudad` | varchar(30) | No | — | No | — | Ciudad |
| `vch_sucudireccion` | varchar(50) | No | — | Sí | NULL | Dirección (opcional) |
| `int_sucucontcuenta` | int | No | — | No | — | Número de cuentas abiertas en la sucursal |

### 2.4 `cliente` — clientes del banco

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_cliecodigo` | char(5) | Sí | — | No | — | Código del cliente (`00001`..`00020`) |
| `vch_cliepaterno` | varchar(25) | No | — | No | — | Apellido paterno |
| `vch_cliematerno` | varchar(25) | No | — | No | — | Apellido materno |
| `vch_clienombre` | varchar(30) | No | — | No | — | Nombres |
| `chr_cliedni` | char(8) | No | — | No | — | Documento de identidad (8 dígitos) |
| `vch_clieciudad` | varchar(30) | No | — | No | — | Ciudad de residencia |
| `vch_cliedireccion` | varchar(50) | No | — | No | — | Dirección |
| `vch_clietelefono` | varchar(20) | No | — | Sí | NULL | Teléfono (opcional) |
| `vch_clieemail` | varchar(50) | No | — | Sí | NULL | Correo (opcional) |

### 2.5 `modulo` — módulos del sistema

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `int_moducodigo` | int | Sí | — | No | — | Código del módulo (1..6) |
| `vch_modunombre` | varchar(50) | No | — | Sí | NULL | Nombre del módulo |
| `vch_moduestado` | varchar(15) | No | — | No | `'ACTIVO'` | Estado: `ACTIVO`, `ANULADO`, `CANCELADO` (CHECK `modulo_chk_1`) |

### 2.6 `tipomovimiento` — catálogo de tipos de movimiento

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_tipocodigo` | char(3) | Sí | — | No | — | Código del tipo (`001`..`010`) |
| `vch_tipodescripcion` | varchar(40) | No | — | No | — | Descripción |
| `vch_tipoaccion` | varchar(10) | No | — | No | — | `INGRESO` (suma) o `SALIDA` (resta) — CHECK `chk_tipomovimiento_vch_tipoaccion` |
| `vch_tipoestado` | varchar(15) | No | — | No | `'ACTIVO'` | Estado: `ACTIVO`, `ANULADO`, `CANCELADO` — CHECK `chk_tipomovimiento_vch_tipoestado` |

### 2.7 `usuario` — credenciales de acceso

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_emplcodigo` | char(4) | Sí | → `empleado.chr_emplcodigo` | No | — | Empleado propietario de la cuenta (FK `usuario_ibfk_1`) |
| `vch_emplusuario` | varchar(20) | No | — | No | — | Nombre de usuario; **único** (`U_Usuario_vch_emplusuario`) |
| `vch_emplclave` | varchar(64) | No | — | No | — | Hash de la clave: SHA-256 hexadecimal (64) |
| `vch_emplestado` | varchar(15) | No | — | Sí | `'ACTIVO'` | Estado: `ACTIVO`, `ANULADO`, `CANCELADO` — CHECK `usuario_chk_1` |

> `vch_emplclave` es `varchar(64)` (el dump de la guía traía `varchar(50)`): con
> 50 no cabía el hash SHA-256 de 64 caracteres que exige el CONTRATO §12.

### 2.8 `parametro` — parámetros de negocio

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_paracodigo` | char(3) | Sí | — | No | — | `001` = ITF, `002` = operaciones sin costo |
| `vch_paradescripcion` | varchar(50) | No | — | No | — | Descripción |
| `vch_paravalor` | varchar(70) | No | — | No | — | Valor (texto) |
| `vch_paraestado` | varchar(15) | No | — | No | `'ACTIVO'` | Estado — CHECK `chk_parametro_vch_paraestado` |

### 2.9 `contador` — secuencias de códigos primarios

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `vch_conttabla` | varchar(30) | Sí | — | No | — | Tabla cuyo código se numera |
| `int_contitem` | int | No | — | No | — | Último número de ítem generado |
| `int_contlongitud` | int | No | — | No | — | Longitud del código generado |

Valores de la semilla: `Asignado` 11/6, `Cliente` 20/5, `Empleado` 14/4,
`Moneda` 2/2, `Parametro` 2/3, `Sucursal` 7/3, `TipoMovimiento` 10/3.

### 2.10 `permiso` — permisos por usuario y módulo

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_emplcodigo` | char(4) | Sí (compuesta) | → `usuario.chr_emplcodigo` | No | — | Usuario al que se concede (FK `permiso_ibfk_2`) |
| `int_moducodigo` | int | Sí (compuesta) | → `modulo.int_moducodigo` | No | — | Módulo (FK `permiso_ibfk_1`) |
| `vch_permestado` | varchar(15) | No | — | No | `'ACTIVO'` | Estado — CHECK `permiso_chk_1` |

### 2.11 `asignado` — asignación empleado–sucursal

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_asigcodigo` | char(6) | Sí | — | No | — | Código de asignación (`000001`..`000011`) |
| `chr_sucucodigo` | char(3) | No | → `sucursal.chr_sucucodigo` | No | — | Sucursal (FK `fk_asignado_sucursal`) |
| `chr_emplcodigo` | char(4) | No | → `empleado.chr_emplcodigo` | No | — | Empleado (FK `fk_asignado_empleado`) |
| `dtt_asigfechaalta` | date | No | — | No | — | Fecha de alta |
| `dtt_asigfechabaja` | date | No | — | Sí | NULL | Fecha de baja (NULL = vigente) |

### 2.12 `cargomantenimiento` — cargo por mantenimiento (proyectado)

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_monecodigo` | char(2) | Sí | → `moneda.chr_monecodigo` | No | — | Moneda (FK `fk_cargomantenimiento_moneda`) |
| `dec_cargMontoMaximo` | decimal(12,2) | No | — | No | — | Saldo máximo (inclusive) que no supera el cargo |
| `dec_cargImporte` | decimal(12,2) | No | — | No | — | Importe fijo del cargo |

Semilla: Soles 3500.00 / 7.00 · Dólares 1200.00 / 2.50.

### 2.13 `costomovimiento` — costo por movimiento

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_monecodigo` | char(2) | Sí | → `moneda.chr_monecodigo` | No | — | Moneda (FK `fk_costomovimiento_moneda`) |
| `dec_costimporte` | decimal(12,2) | No | — | No | — | Importe del cargo por movimiento |

Semilla: Soles 2.00 · Dólares 0.60.

### 2.14 `interesmensual` — interés mensual proyectado

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_monecodigo` | char(2) | Sí | → `moneda.chr_monecodigo` | No | — | Moneda (FK `fk_interesmensual_moneda`) |
| `dec_inteimporte` | decimal(12,2) | No | — | No | — | Porcentaje mensual de interés |

Semilla: Soles 0.70 % · Dólares 0.60 %.

### 2.15 `cuenta` — cuentas de los clientes (detalle en §3.2)

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_cuencodigo` | char(8) | Sí | — | No | — | Código de cuenta (`00100001`..`00300001`) |
| `chr_monecodigo` | char(2) | No | → `moneda.chr_monecodigo` | No | — | Moneda (FK `fk_cuenta_moneda`) |
| `chr_sucucodigo` | char(3) | No | → `sucursal.chr_sucucodigo` | No | — | Sucursal (FK `fk_cuenta_sucursal`) |
| `chr_emplcreacuenta` | char(4) | No | → `empleado.chr_emplcodigo` | No | — | Empleado que abrió la cuenta (FK `fk_cuente_empleado`) |
| `chr_cliecodigo` | char(5) | No | → `cliente.chr_cliecodigo` | No | — | Cliente titular (FK `fk_cuenta_cliente`) |
| `dec_cuensaldo` | decimal(12,2) | No | — | No | — | Saldo actual (nunca negativo, regla RB-03) |
| `dtt_cuenfechacreacion` | date | No | — | No | — | Fecha de apertura |
| `vch_cuenestado` | varchar(15) | No | — | No | `'ACTIVO'` | `ACTIVO`, `ANULADO` o `CANCELADO` — CHECK `chk_cuenta_chr_cuenestado` |
| `int_cuencontmov` | int | No | — | No | — | Número de movimientos de la cuenta |
| `chr_cuenclave` | char(6) | No | — | No | — | Clave de la cuenta para ventanilla |

Índices: `idx_cuenta01` (`chr_cliecodigo`), `idx_cuenta02`
(`chr_emplcreacuenta`), `idx_cuenta03` (`chr_sucucodigo`), `idx_cuenta04`
(`chr_monecodigo`).

### 2.16 `movimiento` — libro de movimientos (detalle en §3.1)

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_cuencodigo` | char(8) | Sí (compuesta) | → `cuenta.chr_cuencodigo` | No | — | Cuenta que registra el movimiento (FK `fk_movimiento_cuenta`) |
| `int_movinumero` | int | Sí (compuesta) | — | No | — | Consecutivo dentro de la cuenta |
| `dtt_movifecha` | date | No | — | No | — | Fecha del movimiento |
| `chr_emplcodigo` | char(4) | No | → `empleado.chr_emplcodigo` | No | — | Empleado que lo registró (FK `fk_movimiento_empleado`) |
| `chr_tipocodigo` | char(3) | No | → `tipomovimiento.chr_tipocodigo` | No | — | Tipo (FK `fk_movimiento_tipomovimiento`) |
| `dec_moviimporte` | decimal(12,2) | No | — | No | — | Importe, siempre ≥ 0 — CHECK `chk_Movimiento_importe4` |
| `chr_cuenreferencia` | char(8) | No | — | Sí | NULL | Cuenta referida (transferencias) |

Índices: `idx_movimiento01` (`chr_tipocodigo`), `idx_movimiento02`
(`chr_emplcodigo`), `idx_movimiento03` (`chr_cuencodigo`).

> `chr_cuenreferencia` es una **referencia lógica** sin llave foránea (igual que
> en el dump de la guía): apunta a la otra cuenta de una transferencia.

### 2.17 `logsession` — auditoría de sesiones

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `ID` | int | Sí (autoincremental) | — | No | AUTO_INCREMENT | Identificador de la sesión |
| `chr_emplcodigo` | char(4) | No | → `empleado.chr_emplcodigo` | No | — | Empleado que abrió la sesión (FK `fk_log_session_empleado`) |
| `fec_ingreso` | datetime | No | — | No | — | Inicio de sesión |
| `fec_salida` | datetime | No | — | Sí | NULL | Cierre de sesión (NULL = abierta) |
| `ip` | varchar(20) | No | — | No | `'NONE'` | IP del cliente |
| `hostname` | varchar(100) | No | — | No | `'NONE'` | Host del cliente |

En la semilla está **vacía**: se llena en runtime con cada autenticación.

---

## 3. Diccionario de datos detallado de `movimiento` y `cuenta`

### 3.1 `movimiento` — libro de movimientos

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_cuencodigo` | char(8) | Sí | → `cuenta` | No | — | Cuenta del movimiento. Es parte de la PK compuesta: el consecutivo `int_movinumero` es **por cuenta**, no global |
| `int_movinumero` | int | Sí | — | No | — | Consecutivo dentro de la cuenta. Regla RB-04: `int_movinumero = cuenta.int_cuencontmov` anterior + 1 |
| `dtt_movifecha` | date | No | — | No | — | Fecha de operación (no incluye hora: el modelo es de fecha) |
| `chr_emplcodigo` | char(4) | No | → `empleado` | No | — | Empleado que ejecutó la operación. Permite auditar quién generó el movimiento |
| `chr_tipocodigo` | char(3) | No | → `tipomovimiento` | No | — | Tipo de movimiento. **Es la columna que decide si el saldo sube o baja**: `vch_tipoaccion = 'INGRESO'` suma, `'SALIDA'` resta |
| `dec_moviimporte` | decimal(12,2) | No | — | No | — | Importe del movimiento, siempre positivo o cero. El signo lo aporta `tipomovimiento.vch_tipoaccion`; el CHECK `dec_moviimporte >= 0` impide importes negativos |
| `chr_cuenreferencia` | char(8) | No | — | Sí | NULL | Cuenta relacionada. Se usa en transferencias: en el origen (tipo `009`) apunta a la cuenta destino y en el destino (tipo `008`) a la cuenta origen. NULL en depósitos, retiros, intereses, cargos y cancelaciones |

**Invariante del saldo**

```
cuenta.dec_cuensaldo = SUMA(movimiento.dec_moviimporte  donde la acción es INGRESO)
                     - SUMA(movimiento.dec_moviimporte  donde la acción es SALIDA)
```

Verificado en la semilla: las 6 cuentas cuadran exactamente y ninguna tiene
saldo negativo.

### 3.2 `cuenta` — cuentas de los clientes

| Columna | Tipo | PK | FK | NULL | Valor por defecto | Descripción |
|---|---|---|---|---|---|---|
| `chr_cuencodigo` | char(8) | Sí | — | No | — | Código de cuenta. Prefijo `001`/`002`/`003` = sucursal que la abrió; sufijo `0001`.. = correlativo |
| `chr_monecodigo` | char(2) | No | → `moneda` | No | — | Moneda de la cuenta. Define la moneda de todos sus movimientos; dos cuentas con distinta moneda no pueden transferirse (CTA-004) |
| `chr_sucucodigo` | char(3) | No | → `sucursal` | No | — | Sucursal apertura. Es también la sucursal del empleado que la abrió (`asignado` liga empleado–sucursal) |
| `chr_emplcreacuenta` | char(4) | No | → `empleado` | No | — | Empleado que abrió la cuenta (responsable de la apertura) |
| `chr_cliecodigo` | char(5) | No | → `cliente` | No | — | Cliente titular. Un cliente puede tener 0..N cuentas; la consulta de cuentas del cliente (RF-04) parte de aquí |
| `dec_cuensaldo` | decimal(12,2) | No | — | No | — | Saldo disponible. Se actualiza **en la misma transacción** que inserta el movimiento (RB-02), se bloquea con `SELECT ... FOR UPDATE` (RB-03) y nunca es negativo (RB-03) |
| `dtt_cuenfechacreacion` | date | No | — | No | — | Fecha de apertura. El movimiento tipo `001` "Apertura de Cuenta" de esa fecha es el primer movimiento del libro |
| `vch_cuenestado` | varchar(15) | No | — | No | `'ACTIVO'` | Estado de la cuenta. El CHECK `chk_cuenta_chr_cuenestado` solo admite `ACTIVO`, `ANULADO` o `CANCELADO`. Solo `ACTIVO` admite operaciones nuevas; en cualquier otro estado el servicio responde **CTA-002** |
| `int_cuencontmov` | int | No | — | No | — | Contador de movimientos de la cuenta. Debe ser igual al número de filas de `movimiento` con esa cuenta: es el consecutivo que usa la regla RB-04 |
| `chr_cuenclave` | char(6) | No | — | No | — | Clave de 6 caracteres para operar en ventanilla (no es la clave de autenticación del usuario) |

**Estados de la cuenta en la semilla**

| Cuenta | Cliente | Moneda | Saldo | Movimientos | Estado |
|---|---|---|---|---|---|
| `00100001` | `00005` | 01 Soles | 7404.00 | 24 | `ACTIVO` |
| `00100002` | `00005` | 02 Dólares | 5002.97 | 12 | `ACTIVO` |
| `00200001` | `00008` | 01 Soles | 7000.00 | 20 | `ACTIVO` |
| `00200002` | `00001` | 01 Soles | 6800.00 | 7 | `ACTIVO` |
| `00200003` | `00007` | 02 Dólares | 6000.00 | 8 | `ACTIVO` |
| `00300001` | `00010` | 01 Soles | 0.00 | 3 | `CANCELADO` |

`00300001` es el caso de prueba de **CTA-002**: tiene 3 movimientos (apertura,
depósito y cancelación por el mismo importe) y no admite movimientos nuevos.

---

## 4. Cardinalidades

Notación: `1` = uno, `0..1` = opcional, `0..N` = cero o muchos, `1..N` = uno o
muchos. El lado "padre" es el de la clave foránea.

| Entidad padre | Entidad hija | Cardinalidad | Restricción |
|---|---|---|---|
| `moneda` | `cuenta` | 1 → 0..N | Cada cuenta tiene exactamente una moneda |
| `moneda` | `cargomantenimiento` | 1 → 0..1 | Como máximo un cargo por moneda (PK = FK) |
| `moneda` | `costomovimiento` | 1 → 0..1 | Como máximo un costo por moneda (PK = FK) |
| `moneda` | `interesmensual` | 1 → 0..1 | Como máximo un interés por moneda (PK = FK) |
| `empleado` | `usuario` | 1 → 0..1 | Un empleado puede tener o no credencial de acceso (1:1 opcional) |
| `empleado` | `permiso` | 1 → 0..N | Permisos por módulo del usuario asociado |
| `empleado` | `asignado` | 1 → 0..N | Asignaciones a sucursales |
| `empleado` | `cuenta` | 1 → 0..N | Cuentas que el empleado abrió |
| `empleado` | `movimiento` | 1 → 0..N | Movimientos que el empleado registró |
| `empleado` | `logsession` | 1 → 0..N | Sesiones abiertas por el empleado |
| `sucursal` | `asignado` | 1 → 0..N | Asignaciones de la sucursal |
| `sucursal` | `cuenta` | 1 → 0..N | Cuentas aperturadas en la sucursal |
| `cliente` | `cuenta` | 1 → 0..N | Cuentas del cliente (RF-04) |
| `modulo` | `permiso` | 1 → 0..N | Permisos concedidos sobre el módulo |
| `usuario` | `permiso` | 1 → 0..N | Permisos del usuario (FK a `usuario`, no a `empleado`) |
| `tipomovimiento` | `movimiento` | 1 → 0..N | Movimientos del tipo |
| `cuenta` | `movimiento` | 1 → 0..N | Movimientos de la cuenta (PK compuesta con la cuenta) |
| `cuenta` | `cuenta` (referencia) | 0..1 → 0..N | Referencia lógica por `movimiento.chr_cuenreferencia` en transferencias (sin FK) |

Detalles para el MER:

- La jerarquía de sucursales es plana: `sucursal` no tiene llave padre.
- `usuario` y `permiso` forman la security boundary del sistema: la
  autenticación (RF-01) valida `usuario` y la autorización por módulo valida
  `permiso`.
- `cuenta` es la entidad central del negocio: concentra al cliente, la moneda,
  la sucursal y el empleado apertor, y es el padre del libro de movimientos.
- `movimiento` es una entidad de detalle de `cuenta` con clave compuesta: el
  número de movimiento es por cuenta.

---

## 5. Los 10 tipos de movimiento

| Código | Descripción | Acción | Efecto en el saldo | Cuándo lo usa el servicio |
|---|---|---|---|---|
| `001` | Apertura de Cuenta | `INGRESO` | Suma el saldo inicial | Al abrir la cuenta (primer movimiento, `int_movinumero = 1`) |
| `002` | Cancelar Cuenta | `SALIDA` | Resta el saldo que se cancela | Al cancelar la cuenta; la cuenta queda `CANCELADO` |
| `003` | Deposito | `INGRESO` | Suma el monto | RF-06: `realizarDeposito` (RB-05) |
| `004` | Retiro | `SALIDA` | Resta el monto | RF-07: `realizarRetiro` (RB-06) |
| `005` | Interes | `INGRESO` | Suma el interés | Abono del proceso batch de mantenimiento (RB-12, fuera del alcance del servicio) |
| `006` | Mantenimiento | `SALIDA` | Resta el cargo | Cobro del proceso batch de mantenimiento (RB-12, fuera del alcance del servicio) |
| `007` | ITF | `SALIDA` | Resta el impuesto | RB-08: en transferencias, `itf = round(monto × 0.08 / 100, 2)`; solo se registra si `itf > 0` |
| `008` | Transferencia | `INGRESO` | Suma en la cuenta destino | RB-07: segunda pata de la transferencia, con `chr_cuenreferencia` = cuenta origen |
| `009` | Transferencia | `SALIDA` | Resta en la cuenta origen | RB-07: primera pata de la transferencia, con `chr_cuenreferencia` = cuenta destino |
| `010` | Cargo por Movimiento | `SALIDA` | Resta el costo del movimiento | RB-09: se cobra cuando el número de movimiento supera 15 (operaciones sin costo) |

---

## 6. Los 2 parámetros de negocio

| Código | Descripción | Valor | Interpretación | Uso |
|---|---|---|---|---|
| `001` | ITF - Impuesto a la Transacciones Financieras | `0.08` | **Porcentaje**: `itf = round(monto × 0.08 / 100, 2)`. Ej.: transferencia de 250.00 → ITF 0.20 | RB-08: solo en transferencias; movimiento tipo `007` en la cuenta origen |
| `002` | Número de Operaciones Sin Costo | `15` | El movimiento **N** es sin costo si `N ≤ 15`; a partir del movimiento 16 se cobra | RB-09: cobro de `costomovimiento.dec_costimporte` de la moneda de la cuenta, en movimiento tipo `010` |

Parámetros derivados de la moneda (no son `parametro`, son catálogos):

| Concepto | Soles (`01`) | Dólares (`02`) | Tabla |
|---|---|---|---|
| Costo por movimiento | 2.00 | 0.60 | `costomovimiento.dec_costimporte` |
| Umbral de mantenimiento | 3500.00 | 1200.00 | `cargomantenimiento.dec_cargMontoMaximo` |
| Costo de mantenimiento | 7.00 | 2.50 | `cargomantenimiento.dec_cargImporte` |
| Interés mensual | 0.70 % | 0.60 % | `interesmensual.dec_inteimporte` |

---

## 7. Usuarios de la semilla (hash del CONTRATO §12)

| Empleado | Usuario | Estado | Hash | Longitud |
|---|---|---|---|---|
| `0001` | `cromero` | ACTIVO | SHA-256 | 64 |
| `0002` | `lcastro` | ACTIVO | SHA-256 | 64 |
| `0003` | `creyes` | ANULADO | SHA-1 heredado | 40 |
| `0004` | `aramos` | ACTIVO | SHA-256 | 64 |
| `0005` | `cvalencia` | ACTIVO | SHA-1 heredado | 40 |
| `0006` | `rcruz` | ACTIVO | SHA-256 | 64 |
| `0007` | `ediaz` | ANULADO | SHA-1 heredado | 40 |
| `0008` | `csarmiento` | ANULADO | SHA-256 | 64 |
| `0009` | `lpachas` | ACTIVO | SHA-256 | 64 |
| `0010` | `htello` | ACTIVO | SHA-256 | 64 |
| `0011` | `pcarrasco` | ACTIVO | SHA-256 | 64 |

Los tres usuarios con hash SHA-1 heredado (`creyes`, `cvalencia`, `ediaz`) **no
pueden autenticarse**: el servicio compara contra SHA-256 de 64 caracteres
(RB-13) y devuelve `AUTH-001`. `creyes` y `ediaz` además están `ANULADO`, lo
que por sí solo activaría `AUTH-002`.

---

## 8. Notas para el MER y el ERS

1. **Los importes son `decimal(12,2)`**: nunca `float`/`double` en Java, se
   manejan con `BigDecimal` y redondeo `HALF_UP` (RB-01).
2. **`dec_cuensaldo` se recalcula desde `movimiento`**: cualquier movimiento
   nuevo actualiza saldo y contador en la misma transacción (RB-02).
3. **`movimiento` no tiene FK a `cuenta` por `chr_cuenreferencia`**: es una
   referencia lógica entre cuentas; se documenta como relación recursiva en el
   MER.
4. **`logsession` está fuera del núcleo transaccional del servicio**: es
   auditoría; el token vive en memoria (RB-14) y la tabla solo registra
   ingresos y salidas.
5. **`contador` es un registro de secuencias por tabla**, no una tabla de
   parámetros de negocio.
