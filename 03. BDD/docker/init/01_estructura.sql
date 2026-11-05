-- =============================================================================
-- 01_estructura.sql - EURECABANK / MySQL 8.4
-- Esquema de 17 tablas heredado del dump de la guia (03.BDD/Estructura.sql).
--
-- Reglas aplicadas:
--   * Se respetan EXACTAMENTE los nombres de columna, tipos, PK, FK, indices y
--     CHECK del dump de la guia.
--   * Cada tabla lleva ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
--     COLLATE=utf8mb4_0900_ai_ci.
--   * Se agregan COMMENT de tabla y de columna (buena practica UTIC/ESPE).
--   * El orden de creacion respeta la dependencia de llaves foraneas:
--     moneda, empleado, sucursal, cliente, modulo, tipomovimiento, usuario,
--     parametro, contador, permiso, asignado, cargomantenimiento,
--     costomovimiento, interesmensual, cuenta, movimiento, logsession.
--
-- Idempotente: usa CREATE TABLE IF NOT EXISTS, asi que el script puede volver a
-- ejecutarse sin borrar los datos. Si se cambia el DDL hay que recrear la base
-- (DROP DATABASE eurekabank) y volver a correr 00..04.
-- =============================================================================

USE eurekabank;
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1) moneda - catalogo de monedas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS moneda (
    chr_monecodigo      char(2)     NOT NULL COMMENT 'Codigo de moneda: 01 = Soles, 02 = Dolares',
    vch_monedescripcion varchar(20) NOT NULL COMMENT 'Nombre de la moneda',
    PRIMARY KEY (chr_monecodigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Catalogo de monedas manejadas por el banco';

-- -----------------------------------------------------------------------------
-- 2) empleado - maestro de empleados (14 filas de prueba)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS empleado (
    chr_emplcodigo   char(4)     NOT NULL COMMENT 'Codigo del empleado (4 digitos)',
    vch_emplpaterno  varchar(25) NOT NULL COMMENT 'Apellido paterno',
    vch_emplmaterno  varchar(25) NOT NULL COMMENT 'Apellido materno',
    vch_emplnombre   varchar(30) NOT NULL COMMENT 'Nombres',
    vch_emplciudad   varchar(30) NOT NULL COMMENT 'Ciudad de residencia',
    vch_empldireccion varchar(50) DEFAULT NULL COMMENT 'Direccion (opcional)',
    PRIMARY KEY (chr_emplcodigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Maestro de empleados del banco';

-- -----------------------------------------------------------------------------
-- 3) sucursal - catalogo de sucursales
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sucursal (
    chr_sucucodigo     char(3)     NOT NULL COMMENT 'Codigo de sucursal (3 digitos)',
    vch_sucunombre     varchar(50) NOT NULL COMMENT 'Nombre de la sucursal',
    vch_sucuciudad     varchar(30) NOT NULL COMMENT 'Ciudad de la sucursal',
    vch_sucudireccion  varchar(50) DEFAULT NULL COMMENT 'Direccion (opcional)',
    int_sucucontcuenta int        NOT NULL COMMENT 'Numero de cuentas abiertas en la sucursal',
    PRIMARY KEY (chr_sucucodigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Sucursales del banco';

-- -----------------------------------------------------------------------------
-- 4) cliente - clientes del banco
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cliente (
    chr_cliecodigo     char(5)     NOT NULL COMMENT 'Codigo del cliente (5 digitos)',
    vch_cliepaterno    varchar(25) NOT NULL COMMENT 'Apellido paterno',
    vch_cliematerno    varchar(25) NOT NULL COMMENT 'Apellido materno',
    vch_clienombre     varchar(30) NOT NULL COMMENT 'Nombres',
    chr_cliedni        char(8)     NOT NULL COMMENT 'Numero de documento de identidad (8 digitos)',
    vch_clieciudad     varchar(30) NOT NULL COMMENT 'Ciudad de residencia',
    vch_cliedireccion  varchar(50) NOT NULL COMMENT 'Direccion del cliente',
    vch_clietelefono   varchar(20) DEFAULT NULL COMMENT 'Telefono (opcional)',
    vch_clieemail      varchar(50) DEFAULT NULL COMMENT 'Correo electronico (opcional)',
    PRIMARY KEY (chr_cliecodigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Clientes del banco';

-- -----------------------------------------------------------------------------
-- 5) modulo - modulos del sistema a los que se asignan permisos
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS modulo (
    int_moducodigo  int         NOT NULL COMMENT 'Codigo del modulo',
    vch_modunombre  varchar(50) DEFAULT NULL COMMENT 'Nombre del modulo',
    vch_moduestado  varchar(15) NOT NULL DEFAULT 'ACTIVO' COMMENT 'Estado: ACTIVO, ANULADO o CANCELADO',
    PRIMARY KEY (int_moducodigo),
    CONSTRAINT modulo_chk_1 CHECK ((`vch_moduestado` in ('ACTIVO','ANULADO','CANCELADO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Modulos del sistema (Procesos, Tablas, Consultas, Reportes, Util, Seguridad)';

-- -----------------------------------------------------------------------------
-- 6) tipomovimiento - catalogo de tipos de movimiento del libro de cuentas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tipomovimiento (
    chr_tipocodigo   char(3)     NOT NULL COMMENT 'Codigo del tipo de movimiento',
    vch_tipodescripcion varchar(40) NOT NULL COMMENT 'Descripcion del tipo de movimiento',
    vch_tipoaccion   varchar(10) NOT NULL COMMENT 'Afectacion al saldo: INGRESO o SALIDA',
    vch_tipoestado   varchar(15) NOT NULL DEFAULT 'ACTIVO' COMMENT 'Estado: ACTIVO, ANULADO o CANCELADO',
    PRIMARY KEY (chr_tipocodigo),
    CONSTRAINT chk_tipomovimiento_vch_tipoaccion CHECK ((`vch_tipoaccion` in ('INGRESO','SALIDA'))),
    CONSTRAINT chk_tipomovimiento_vch_tipoestado CHECK ((`vch_tipoestado` in ('ACTIVO','ANULADO','CANCELADO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Catalogo de los 10 tipos de movimiento (incluye Apertura y Cancelacion de cuenta)';

-- -----------------------------------------------------------------------------
-- 7) usuario - credenciales de acceso (1:1 con empleado)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    chr_emplcodigo  char(4)     NOT NULL COMMENT 'Codigo del empleado propietario de la cuenta',
    vch_emplusuario varchar(20) NOT NULL COMMENT 'Nombre de usuario para autenticacion',
    vch_emplclave   varchar(64) NOT NULL COMMENT 'Hash de la clave: SHA-256 en hexadecimal (64) o SHA-1 heredado (40)',
    vch_emplestado  varchar(15) DEFAULT 'ACTIVO' COMMENT 'Estado: ACTIVO, ANULADO o CANCELADO',
    PRIMARY KEY (chr_emplcodigo),
    UNIQUE KEY `U_Usuario_vch_emplusuario` (`vch_emplusuario`),
    CONSTRAINT usuario_ibfk_1 FOREIGN KEY (`chr_emplcodigo`) REFERENCES `empleado` (`chr_emplcodigo`),
    CONSTRAINT usuario_chk_1 CHECK ((`vch_emplestado` in ('ACTIVO','ANULADO','CANCELADO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Usuarios del sistema con su clave hasheada';

-- -----------------------------------------------------------------------------
-- 8) parametro - parametros de negocio (ITF y operaciones sin costo)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parametro (
    chr_paracodigo     char(3)     NOT NULL COMMENT 'Codigo del parametro: 001 = ITF, 002 = operaciones sin costo',
    vch_paradescripcion varchar(50) NOT NULL COMMENT 'Descripcion del parametro',
    vch_paravalor      varchar(70) NOT NULL COMMENT 'Valor del parametro (texto)',
    vch_paraestado     varchar(15) NOT NULL DEFAULT 'ACTIVO' COMMENT 'Estado: ACTIVO, ANULADO o CANCELADO',
    PRIMARY KEY (chr_paracodigo),
    CONSTRAINT chk_parametro_vch_paraestado CHECK ((`vch_paraestado` in ('ACTIVO','ANULADO','CANCELADO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Parametros de configuracion del negocio';

-- -----------------------------------------------------------------------------
-- 9) contador - secuencias de codigos primarios por tabla
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contador (
    vch_conttabla     varchar(30) NOT NULL COMMENT 'Nombre de la tabla cuyo codigo primario es numerado',
    int_contitem      int        NOT NULL COMMENT 'Ultimo numero de item generado',
    int_contlongitud  int        NOT NULL COMMENT 'Longitud del codigo generado',
    PRIMARY KEY (vch_conttabla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Secuencias de codigos primarios (ultimo item generado por tabla)';

-- -----------------------------------------------------------------------------
-- 10) permiso - permisos por usuario y modulo
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permiso (
    chr_emplcodigo  char(4)     NOT NULL COMMENT 'Empleado/usuario al que se concede el permiso',
    int_moducodigo  int         NOT NULL COMMENT 'Modulo sobre el que se concede el permiso',
    vch_permestado  varchar(15) NOT NULL DEFAULT 'ACTIVO' COMMENT 'Estado: ACTIVO, ANULADO o CANCELADO',
    PRIMARY KEY (chr_emplcodigo, int_moducodigo),
    KEY `FK_Permiso_Modulo` (`int_moducodigo`),
    CONSTRAINT permiso_ibfk_1 FOREIGN KEY (`int_moducodigo`) REFERENCES `modulo` (`int_moducodigo`),
    CONSTRAINT permiso_ibfk_2 FOREIGN KEY (`chr_emplcodigo`) REFERENCES `usuario` (`chr_emplcodigo`),
    CONSTRAINT permiso_chk_1 CHECK ((`vch_permestado` in ('ACTIVO','ANULADO','CANCELADO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Permisos de cada usuario sobre cada modulo del sistema';

-- -----------------------------------------------------------------------------
-- 11) asignado - asignacion de empleado a sucursal
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asignado (
    chr_asigcodigo   char(6) NOT NULL COMMENT 'Codigo de la asignacion (6 digitos)',
    chr_sucucodigo   char(3) NOT NULL COMMENT 'Sucursal asignada',
    chr_emplcodigo   char(4) NOT NULL COMMENT 'Empleado asignado',
    dtt_asigfechaalta date    NOT NULL COMMENT 'Fecha de alta de la asignacion',
    dtt_asigfechabaja date    DEFAULT NULL COMMENT 'Fecha de baja (NULL = vigente)',
    PRIMARY KEY (chr_asigcodigo),
    KEY `idx_asignado01` (`chr_emplcodigo`),
    KEY `idx_asignado02` (`chr_sucucodigo`),
    CONSTRAINT fk_asignado_empleado FOREIGN KEY (`chr_emplcodigo`) REFERENCES `empleado` (`chr_emplcodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_asignado_sucursal FOREIGN KEY (`chr_sucucodigo`) REFERENCES `sucursal` (`chr_sucucodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Asignacion de empleados a sucursales';

-- -----------------------------------------------------------------------------
-- 12) cargomantenimiento - umbral y costo del cargo por mantenimiento
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cargomantenimiento (
    chr_monecodigo       char(2)       NOT NULL COMMENT 'Moneda del cargo',
    dec_cargMontoMaximo  decimal(12,2) NOT NULL COMMENT 'Saldo maximo que no supera el cargo (inclusive)',
    dec_cargImporte      decimal(12,2) NOT NULL COMMENT 'Importe fijo del cargo por mantenimiento',
    PRIMARY KEY (chr_monecodigo),
    KEY `idx_cargomantenimiento01` (`chr_monecodigo`),
    CONSTRAINT fk_cargomantenimiento_moneda FOREIGN KEY (`chr_monecodigo`) REFERENCES `moneda` (`chr_monecodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Cargo por mantenimiento proyectado por moneda';

-- -----------------------------------------------------------------------------
-- 13) costomovimiento - costo por movimiento a partir de la operacion 16
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS costomovimiento (
    chr_monecodigo   char(2)       NOT NULL COMMENT 'Moneda del costo',
    dec_costimporte  decimal(12,2) NOT NULL COMMENT 'Importe del cargo por movimiento',
    PRIMARY KEY (chr_monecodigo),
    KEY `idx_costomovimiento` (`chr_monecodigo`),
    CONSTRAINT fk_costomovimiento_moneda FOREIGN KEY (`chr_monecodigo`) REFERENCES `moneda` (`chr_monecodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Costo por movimiento ejecutado (se cobra desde la operacion 16)';

-- -----------------------------------------------------------------------------
-- 14) interesmensual - interes mensual proyectado por moneda
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interesmensual (
    chr_monecodigo  char(2)       NOT NULL COMMENT 'Moneda del interes',
    dec_inteimporte decimal(12,2) NOT NULL COMMENT 'Porcentaje mensual de interes',
    PRIMARY KEY (chr_monecodigo),
    KEY `idx_interesmensual01` (`chr_monecodigo`),
    CONSTRAINT fk_interesmensual_moneda FOREIGN KEY (`chr_monecodigo`) REFERENCES `moneda` (`chr_monecodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Interes mensual proyectado por moneda';

-- -----------------------------------------------------------------------------
-- 15) cuenta - cuentas corrientes del cliente
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cuenta (
    chr_cuencodigo        char(8)       NOT NULL COMMENT 'Codigo de la cuenta (8 digitos)',
    chr_monecodigo        char(2)       NOT NULL COMMENT 'Moneda de la cuenta',
    chr_sucucodigo        char(3)       NOT NULL COMMENT 'Sucursal que abrio la cuenta',
    chr_emplcreacuenta    char(4)       NOT NULL COMMENT 'Empleado que abrio la cuenta',
    chr_cliecodigo        char(5)       NOT NULL COMMENT 'Cliente titular de la cuenta',
    dec_cuensaldo         decimal(12,2) NOT NULL COMMENT 'Saldo actual de la cuenta',
    dtt_cuenfechacreacion date          NOT NULL COMMENT 'Fecha de apertura de la cuenta',
    vch_cuenestado        varchar(15)   NOT NULL DEFAULT 'ACTIVO' COMMENT 'Estado: ACTIVO, ANULADO o CANCELADO',
    int_cuencontmov       int           NOT NULL COMMENT 'Numero de movimientos registrados en la cuenta',
    chr_cuenclave         char(6)       NOT NULL COMMENT 'Clave de la cuenta para operar en ventanilla',
    PRIMARY KEY (chr_cuencodigo),
    KEY `idx_cuenta01` (`chr_cliecodigo`),
    KEY `idx_cuenta02` (`chr_emplcreacuenta`),
    KEY `idx_cuenta03` (`chr_sucucodigo`),
    KEY `idx_cuenta04` (`chr_monecodigo`),
    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (`chr_cliecodigo`) REFERENCES `cliente` (`chr_cliecodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuenta_moneda FOREIGN KEY (`chr_monecodigo`) REFERENCES `moneda` (`chr_monecodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuenta_sucursal FOREIGN KEY (`chr_sucucodigo`) REFERENCES `sucursal` (`chr_sucucodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cuente_empleado FOREIGN KEY (`chr_emplcreacuenta`) REFERENCES `empleado` (`chr_emplcodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_cuenta_chr_cuenestado CHECK ((`vch_cuenestado` in ('ACTIVO','ANULADO','CANCELADO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Cuentas de los clientes (5 ACTIVO y 1 CANCELADO en la semilla)';

-- -----------------------------------------------------------------------------
-- 16) movimiento - libro de movimientos de las cuentas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimiento (
    chr_cuencodigo     char(8)       NOT NULL COMMENT 'Cuenta que registra el movimiento',
    int_movinumero     int           NOT NULL COMMENT 'Numero consecutivo del movimiento dentro de la cuenta',
    dtt_movifecha      date          NOT NULL COMMENT 'Fecha del movimiento',
    chr_emplcodigo     char(4)       NOT NULL COMMENT 'Empleado que registro el movimiento',
    chr_tipocodigo     char(3)       NOT NULL COMMENT 'Tipo de movimiento (tipomovimiento)',
    dec_moviimporte    decimal(12,2) NOT NULL COMMENT 'Importe del movimiento (siempre >= 0)',
    chr_cuenreferencia char(8)       DEFAULT NULL COMMENT 'Cuenta referida (transferencias); NULL en el resto',
    PRIMARY KEY (chr_cuencodigo, int_movinumero),
    KEY `idx_movimiento01` (`chr_tipocodigo`),
    KEY `idx_movimiento02` (`chr_emplcodigo`),
    KEY `idx_movimiento03` (`chr_cuencodigo`),
    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY (`chr_cuencodigo`) REFERENCES `cuenta` (`chr_cuencodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_empleado FOREIGN KEY (`chr_emplcodigo`) REFERENCES `empleado` (`chr_emplcodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimiento_tipomovimiento FOREIGN KEY (`chr_tipocodigo`) REFERENCES `tipomovimiento` (`chr_tipocodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_Movimiento_importe4 CHECK ((`dec_moviimporte` >= 0.0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Libro de movimientos de las cuentas (74 movimientos en la semilla)';

-- -----------------------------------------------------------------------------
-- 17) logsession - auditoria de sesiones abiertas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS logsession (
    ID             int          NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la sesion',
    chr_emplcodigo char(4)      NOT NULL COMMENT 'Empleado que inicio la sesion',
    fec_ingreso    datetime     NOT NULL COMMENT 'Fecha y hora de inicio de sesion',
    fec_salida     datetime     DEFAULT NULL COMMENT 'Fecha y hora de cierre de sesion (NULL = abierta)',
    ip             varchar(20)  NOT NULL DEFAULT 'NONE' COMMENT 'Direccion IP del cliente',
    hostname       varchar(100) NOT NULL DEFAULT 'NONE' COMMENT 'Nombre del host del cliente',
    PRIMARY KEY (ID),
    KEY `fk_log_session_empleado` (`chr_emplcodigo`),
    CONSTRAINT fk_log_session_empleado FOREIGN KEY (`chr_emplcodigo`) REFERENCES `empleado` (`chr_emplcodigo`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Auditoria de sesiones del sistema (vacia en la semilla)';
