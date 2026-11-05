-- =============================================================================
-- consultas/verificacion.sql - EURECABANK / MySQL 8.4
-- Consultas de verificacion y de apoyo para la defensa.
--
-- Este archivo NO se ejecuta al arrancar el contenedor: es material de apoyo
-- para revisar la base en DBeaver (o con el cliente mysql) durante la defensa.
-- Todas las consultas son de solo lectura, salvo la del bloqueo de fila, que
-- se hace dentro de una transaccion explicita y se cierra con COMMIT.
--
-- Conexion sugerida: 127.0.0.1:3307 (Docker) o 127.0.0.1:3306 (MySQL local)
-- Base de datos: eurekabank
-- =============================================================================

USE eurekabank;
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1) NUMERO DE TABLAS DEL ESQUEMA (debe devolver 17)
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS total_tablas
FROM information_schema.tables
WHERE table_schema = 'eurekabank'
  AND table_type = 'BASE TABLE';

-- -----------------------------------------------------------------------------
-- 2) CONTEOS ESPERADOS DE CADA TABLA
--    Valores de la semilla: 14 empleados, 20 clientes, 7 sucursales,
--    2 monedas, 10 tipos de movimiento, 11 asignados, 6 modulos,
--    6 cuentas, 74 movimientos, 2 parametros, 42 permisos, 11 usuarios,
--    7 contadores, 0 sesiones.
-- -----------------------------------------------------------------------------
SELECT 'asignado'           AS tabla, COUNT(*) AS filas, 11 AS esperado FROM asignado
UNION ALL SELECT 'cargomantenimiento',   COUNT(*),  2 FROM cargomantenimiento
UNION ALL SELECT 'cliente',               COUNT(*), 20 FROM cliente
UNION ALL SELECT 'contador',              COUNT(*),  7 FROM contador
UNION ALL SELECT 'costomovimiento',       COUNT(*),  2 FROM costomovimiento
UNION ALL SELECT 'cuenta',                COUNT(*),  6 FROM cuenta
UNION ALL SELECT 'empleado',              COUNT(*), 14 FROM empleado
UNION ALL SELECT 'interesmensual',        COUNT(*),  2 FROM interesmensual
UNION ALL SELECT 'logsession',            COUNT(*),  0 FROM logsession
UNION ALL SELECT 'modulo',                COUNT(*),  6 FROM modulo
UNION ALL SELECT 'moneda',                COUNT(*),  2 FROM moneda
UNION ALL SELECT 'movimiento',            COUNT(*), 74 FROM movimiento
UNION ALL SELECT 'parametro',             COUNT(*),  2 FROM parametro
UNION ALL SELECT 'permiso',               COUNT(*), 42 FROM permiso
UNION ALL SELECT 'sucursal',              COUNT(*),  7 FROM sucursal
UNION ALL SELECT 'tipomovimiento',        COUNT(*), 10 FROM tipomovimiento
UNION ALL SELECT 'usuario',               COUNT(*), 11 FROM usuario;

-- -----------------------------------------------------------------------------
-- 3) DEFINICION REAL DE LAS TABLAS (indices, llaves foraneas y CHECK)
--    Comentar la linea que se quiera revisar.
-- -----------------------------------------------------------------------------
SHOW CREATE TABLE cuenta;
SHOW CREATE TABLE movimiento;

-- -----------------------------------------------------------------------------
-- 4) LISTADO DE CUENTAS: saldo, estado y numero de movimientos
--    int_cuencontmov debe coincidir con el numero real de filas de movimiento.
-- -----------------------------------------------------------------------------
SELECT c.chr_cuencodigo   AS cuenta,
       c.chr_cliecodigo   AS cliente,
       cl.vch_clienombre  AS nombre_cliente,
       c.chr_monecodigo   AS moneda,
       c.vch_cuenestado   AS estado,
       c.dec_cuensaldo    AS saldo,
       c.int_cuencontmov  AS movimientos,
       c.dtt_cuenfechacreacion AS fecha_apertura,
       (SELECT COUNT(*) FROM movimiento m
         WHERE m.chr_cuencodigo = c.chr_cuencodigo) AS movimientos_reales
FROM cuenta c
JOIN cliente cl ON cl.chr_cliecodigo = c.chr_cliecodigo
ORDER BY c.chr_cuencodigo;

-- -----------------------------------------------------------------------------
-- 5) TOTAL DE MOVIMIENTOS POR TIPO (los 10 tipos del catalogo)
--    Se usa LEFT JOIN desde tipomovimiento para que los 10 tipos aparezcan
--    aunque no tengan movimientos en la semilla.
-- -----------------------------------------------------------------------------
SELECT t.chr_tipocodigo       AS codigo,
       t.vch_tipodescripcion  AS descripcion,
       t.vch_tipoaccion      AS accion,
       COUNT(m.chr_cuencodigo) AS total_movimientos,
       COALESCE(SUM(m.dec_moviimporte), 0) AS importe_total
FROM tipomovimiento t
LEFT JOIN movimiento m ON m.chr_tipocodigo = t.chr_tipocodigo
GROUP BY t.chr_tipocodigo, t.vch_tipodescripcion, t.vch_tipoaccion
ORDER BY t.chr_tipocodigo;

-- -----------------------------------------------------------------------------
-- 6) CONSISTENCIA DEL SALDO: la suma de INGRESO menos SALIDA de los
--    movimientos debe coincidir con cuenta.dec_cuensaldo (0 filas = todo bien)
-- -----------------------------------------------------------------------------
SELECT c.chr_cuencodigo,
       c.dec_cuensaldo AS saldo_guardado,
       COALESCE(SUM(CASE WHEN t.vch_tipoaccion = 'INGRESO'
                         THEN m.dec_moviimporte
                         ELSE -m.dec_moviimporte END), 0) AS saldo_calculado,
       c.dec_cuensaldo -
       COALESCE(SUM(CASE WHEN t.vch_tipoaccion = 'INGRESO'
                         THEN m.dec_moviimporte
                         ELSE -m.dec_moviimporte END), 0) AS diferencia
FROM cuenta c
LEFT JOIN movimiento m ON m.chr_cuencodigo = c.chr_cuencodigo
LEFT JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo
GROUP BY c.chr_cuencodigo, c.dec_cuensaldo
HAVING diferencia <> 0;

-- -----------------------------------------------------------------------------
-- 7) UNA CUENTA CANCELADO NO ADMITE MOVIMIENTOS NUEVOS
--    7.1) Cuentas bloqueadas: el servicio debe responder CTA-002
--         ("La cuenta no se encuentra en estado ACTIVO").
--    7.2) Cuentas que si admiten operaciones.
--    7.3) Prueba de que la base tampoco acepta un estado inventado
--         (CHECK chk_cuenta_chr_cuenestado). La instruccion siguiente esta
--         comentada a proposito: al descomentarla debe fallar con
--         ERROR 3819 (Check constraint 'chk_cuenta_chr_cuenestado' is violated).
-- -----------------------------------------------------------------------------
SELECT chr_cuencodigo AS cuenta_bloqueada, vch_cuenestado AS estado,
       dec_cuensaldo AS saldo, 'CTA-002' AS codigo_error_servicio
FROM cuenta
WHERE vch_cuenestado <> 'ACTIVO';

SELECT chr_cuencodigo AS cuenta_operable, vch_cuenestado AS estado,
       dec_cuensaldo AS saldo
FROM cuenta
WHERE vch_cuenestado = 'ACTIVO'
ORDER BY chr_cuencodigo;

-- INSERT INTO movimiento (chr_cuencodigo, int_movinumero, dtt_movifecha, chr_emplcodigo,
--                          chr_tipocodigo, dec_moviimporte, chr_cuenreferencia)
-- VALUES ('00300001', 4, CURDATE(), '0001', '003', 100.00, NULL);
--   -> la base si lo admite, porque el estado vive en `cuenta`; el rechazo es
--      logica del servicio (CTA-002). Pruebe el estado invalido para ver el CHECK:
-- INSERT INTO cuenta (chr_cuencodigo, chr_monecodigo, chr_sucucodigo, chr_emplcreacuenta,
--                     chr_cliecodigo, dec_cuensaldo, dtt_cuenfechacreacion, vch_cuenestado,
--                     int_cuencontmov, chr_cuenclave)
-- VALUES ('99999999','01','001','0001','00001',10.00,CURDATE(),'INACTIVO',0,'123456');
--   -> ERROR 3819: Check constraint 'chk_cuenta_chr_cuenestado' is violated.

-- -----------------------------------------------------------------------------
-- 8) PARAMETROS DE NEGOCIO: ITF y numero de operaciones sin costo
--    ITF = round(monto * 0.08 / 100, 2)   |   el movimiento N es sin costo si N <= 15
-- -----------------------------------------------------------------------------
SELECT chr_paracodigo     AS codigo,
       vch_paradescripcion AS descripcion,
       vch_paravalor      AS valor,
       vch_paraestado     AS estado,
       CASE chr_paracodigo
            WHEN '001' THEN 'Porcentaje: ITF = ROUND(monto * 0.08 / 100, 2)'
            WHEN '002' THEN 'Numero maximo de operaciones sin costo (N <= 15)'
       END AS interpretacion
FROM parametro
ORDER BY chr_paracodigo;

-- Ejemplo de ITF aplicado a una transferencia de 250.00: 0.20
SELECT ROUND(250.00 * CAST(vch_paravalor AS DECIMAL(10,4)) / 100, 2) AS itf_transferencia_250
FROM parametro
WHERE chr_paracodigo = '001';

-- Costo por movimiento e interes mensual por moneda (proyeccion de CU-08)
SELECT m.vch_monedescripcion AS moneda,
       cm.dec_costimporte    AS costo_por_movimiento,
       cg.dec_cargMontoMaximo AS umbral_mantenimiento,
       cg.dec_cargImporte     AS costo_mantenimiento,
       im.dec_inteimporte     AS interes_mensual_porcentaje
FROM moneda m
LEFT JOIN costomovimiento   cm ON cm.chr_monecodigo = m.chr_monecodigo
LEFT JOIN cargomantenimiento cg ON cg.chr_monecodigo = m.chr_monecodigo
LEFT JOIN interesmensual     im ON im.chr_monecodigo = m.chr_monecodigo
ORDER BY m.chr_monecodigo;

-- -----------------------------------------------------------------------------
-- 9) BLOQUEO DE FILA (SELECT ... FOR UPDATE) - regla RB-03
--    El servicio bloquea la fila de la cuenta antes de operar, de modo que dos
--    operaciones simultaneas sobre la misma cuenta se serialicen y el saldo
--    nunca quede negativo. La consulta se hace dentro de una transaccion: el
--    bloqueo se mantiene hasta el COMMIT o el ROLLBACK.
-- -----------------------------------------------------------------------------
START TRANSACTION;

SELECT c.chr_cuencodigo, c.dec_cuensaldo, c.int_cuencontmov
FROM cuenta c
WHERE c.chr_cuencodigo = '00100001'
FOR UPDATE;

-- Con la fila bloqueada, otra sesion que intente un UPDATE sobre la misma
-- cuenta queda en espera (lock wait) hasta que esta transaccion termine.

UPDATE cuenta
SET dec_cuensaldo = dec_cuensaldo
WHERE chr_cuencodigo = '00100001';

COMMIT;

-- -----------------------------------------------------------------------------
-- 10) NO HAY SALDOS NEGATIVOS (regla RB-03) - debe devolver 0 filas
-- -----------------------------------------------------------------------------
SELECT chr_cuencodigo, dec_cuensaldo
FROM cuenta
WHERE dec_cuensaldo < 0;

-- -----------------------------------------------------------------------------
-- 11) USUARIOS DE LA SEMILLA (hash SHA-256 del CONTRATO CONGELADO)
--     Los usuarios creyes, cvalencia y ediaz conservan el hash SHA-1 de 40
--     caracteres del dump de la guia: no pueden autenticarse (AUTH-001).
-- -----------------------------------------------------------------------------
SELECT u.chr_emplcodigo,
       u.vch_emplusuario,
       u.vch_emplestado,
       LENGTH(u.vch_emplclave) AS largo_hash,
       CASE WHEN LENGTH(u.vch_emplclave) = 64 THEN 'SHA-256 (utilizable)'
            ELSE 'SHA-1 heredado (NO utilizable)' END AS algoritmo
FROM usuario u
ORDER BY u.chr_emplcodigo;

-- -----------------------------------------------------------------------------
-- 12) PERMISOS POR USUARIO (7 primeros empleados x 6 modulos = 42)
-- -----------------------------------------------------------------------------
SELECT u.vch_emplusuario AS usuario,
       SUM(CASE WHEN p.vch_permestado = 'ACTIVO'    THEN 1 ELSE 0 END) AS modulos_activos,
       SUM(CASE WHEN p.vch_permestado = 'CANCELADO' THEN 1 ELSE 0 END) AS modulos_cancelados
FROM usuario u
JOIN permiso p ON p.chr_emplcodigo = u.chr_emplcodigo
GROUP BY u.vch_emplusuario
ORDER BY u.vch_emplusuario;

-- -----------------------------------------------------------------------------
-- 13) USUARIO DE APLICACION: privilegios vigentes
--     Debe mostrar solo USAGE (nivel global) y los 4 permisos sobre eurekabank.*
-- -----------------------------------------------------------------------------
SHOW GRANTS FOR 'eurekabank_app'@'%';

-- -----------------------------------------------------------------------------
-- 14) TRANSFERENCIAS DE LA SEMILLA (movimientos con cuenta referida)
--     Muestran la regla RB-07: 009 SALIDA en el origen y 008 INGRESO en el
--     destino, ambos con chr_cuenreferencia apuntando a la otra cuenta.
-- -----------------------------------------------------------------------------
SELECT m.chr_cuencodigo     AS cuenta,
       m.int_movinumero     AS numero,
       m.dtt_movifecha      AS fecha,
       m.chr_tipocodigo     AS tipo,
       t.vch_tipoaccion     AS accion,
       m.dec_moviimporte    AS importe,
       m.chr_cuenreferencia AS cuenta_referida
FROM movimiento m
JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo
WHERE m.chr_cuenreferencia IS NOT NULL
ORDER BY m.chr_cuencodigo, m.int_movinumero;

-- -----------------------------------------------------------------------------
-- 15) RECARGAR LOS DATOS DE PRUEBA (utilidad de laboratorio)
--     Equivale a: CALL sp_cargar_datos_prueba();
--     Borra y recarga movimiento, cuenta y logsession, y recalcula
--     dec_cuensaldo e int_cuencontmov. NO usar en produccion.
-- -----------------------------------------------------------------------------
-- CALL sp_cargar_datos_prueba();
