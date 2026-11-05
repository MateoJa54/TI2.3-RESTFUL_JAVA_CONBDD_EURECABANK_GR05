-- =============================================================================
-- 04_carga_prueba.sql - EURECABANK / MySQL 8.4
-- Procedimiento almacenado sp_cargar_datos_prueba().
--
-- ADVERTENCIA: este procedimiento BORRA los datos de prueba de las tablas
-- `movimiento`, `cuenta` y `logsession` y los vuelve a cargar. Es una utility
-- de laboratorio: NO debe usarse en produccion ni en una base con datos reales.
--
-- Uso:  CALL sp_cargar_datos_prueba();
--
-- Que hace:
--   1) Borra los movimientos, las cuentas y el registro de sesiones.
--   2) Recarga las 6 cuentas (5 ACTIVO + 1 CANCELADO) y los 74 movimientos.
--   3) Recalcula cuenta.dec_cuensaldo (SUMA INGRESO - SUMA SALIDA) y
--      cuenta.int_cuencontmov (numero de movimientos) a partir de los movimientos.
--   4) Verifica que ningun saldo quede negativo (regla RB-03) y, si alguma
--      cuenta quedara negativa, revierte la transaccion con un error.
--
-- Es idempotente: se puede llamar tantas veces como se quiera y el resultado
-- es siempre el mismo estado de la semilla.
--
-- NOTA TECNICA: el cuerpo del procedimiento usa DELIMITER, que solo entienden
-- el cliente de linea de comandos `mysql` y el script de arranque de Docker.
-- Workbench y DBeaver no interpretan DELIMITER al pegar el texto: en esos
-- casos ejecuta este archivo con el cliente `mysql` (ver GUIA_DOCKER_DBEAVER.md).
-- =============================================================================

USE eurekabank;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sp_cargar_datos_prueba;

DELIMITER $$

CREATE PROCEDURE sp_cargar_datos_prueba()
proc: BEGIN
    DECLARE v_cuentas_negativas INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- 1) Limpieza (primero los hijos por la llave foranea)
    DELETE FROM movimiento;
    DELETE FROM logsession;
    DELETE FROM cuenta;

    -- 2) Cuentas de prueba
    INSERT INTO cuenta (chr_cuencodigo, chr_monecodigo, chr_sucucodigo, chr_emplcreacuenta,
                        chr_cliecodigo, dec_cuensaldo, dtt_cuenfechacreacion, vch_cuenestado,
                        int_cuencontmov, chr_cuenclave) VALUES
        ('00100001', '01', '001', '0004', '00005', 0.00, '2008-01-06', 'ACTIVO',    0, '123456'),
        ('00100002', '02', '001', '0004', '00005', 0.00, '2008-01-08', 'ACTIVO',    0, '123456'),
        ('00200001', '01', '002', '0001', '00008', 0.00, '2008-01-05', 'ACTIVO',    0, '123456'),
        ('00200002', '01', '002', '0001', '00001', 0.00, '2008-01-09', 'ACTIVO',    0, '123456'),
        ('00200003', '02', '002', '0001', '00007', 0.00, '2008-01-11', 'ACTIVO',    0, '123456'),
        ('00300001', '01', '003', '0002', '00010', 0.00, '2008-01-07', 'CANCELADO', 0, '123456');

    -- 3) Movimientos de prueba (74). Los 57 primeros son la replica del dump de
    --    la guia; los 17 siguientes son las pruebas coherentes con las reglas
    --    de negocio (transferencias ida y vuelta entre cuentas de la misma
    --    moneda). La cuenta CANCELADA '00300001' no recibe movimientos nuevos.
    INSERT INTO movimiento (chr_cuencodigo, int_movinumero, dtt_movifecha, chr_emplcodigo,
                            chr_tipocodigo, dec_moviimporte, chr_cuenreferencia) VALUES
        ('00100001',  1, '2008-01-06', '0004', '001', 2800.00, NULL),
        ('00100001',  2, '2008-01-15', '0004', '003', 3200.00, NULL),
        ('00100001',  3, '2008-01-20', '0004', '004',  800.00, NULL),
        ('00100001',  4, '2008-02-14', '0004', '003', 2000.00, NULL),
        ('00100001',  5, '2008-02-25', '0004', '004',  500.00, NULL),
        ('00100001',  6, '2008-03-03', '0004', '004',  800.00, NULL),
        ('00100001',  7, '2008-03-15', '0004', '003', 1000.00, NULL),
        ('00100001',  8, '2024-12-07', '0001', '003',  100.00, NULL),
        ('00100001',  9, '2024-12-07', '0001', '003',  100.00, NULL),
        ('00100001', 10, '2024-12-07', '0001', '003',  100.00, NULL),
        ('00100001', 11, '2024-12-07', '0001', '003',  100.00, NULL),
        ('00100001', 12, '2024-12-07', '0001', '003',   10.00, NULL),
        ('00100001', 13, '2024-12-07', '0001', '003',   10.00, NULL),
        ('00100001', 14, '2024-12-07', '0001', '003',   10.00, NULL),
        ('00100001', 15, '2024-12-07', '0001', '003',   15.00, NULL),
        ('00100001', 16, '2024-12-09', '0001', '003',   15.00, NULL),
        ('00100001', 17, '2024-12-09', '0001', '003',    5.00, NULL),
        ('00100001', 18, '2024-12-09', '0001', '003',   20.00, NULL),
        ('00100001', 19, '2024-12-09', '0001', '003',    7.00, NULL),
        ('00100001', 20, '2024-12-09', '0001', '003',   12.00, NULL),
        ('00100001', 21, '2024-12-10', '0001', '009',    5.00, '00200002'),
        ('00100001', 22, '2024-12-11', '0001', '008',    5.00, '00200002'),
        ('00100001', 23, '2024-12-12', '0001', '008',    6.00, '00200001'),
        ('00100001', 24, '2024-12-13', '0001', '009',    6.00, '00200001'),
        ('00100002',  1, '2008-01-08', '0004', '001', 1800.00, NULL),
        ('00100002',  2, '2008-01-25', '0004', '004', 1000.00, NULL),
        ('00100002',  3, '2008-02-13', '0004', '003', 2200.00, NULL),
        ('00100002',  4, '2008-03-08', '0004', '003', 1500.00, NULL),
        ('00100002',  5, '2024-12-07', '0001', '003',  150.00, NULL),
        ('00100002',  6, '2024-12-07', '0001', '003',  150.00, NULL),
        ('00100002',  7, '2024-12-09', '0001', '003',    6.00, NULL),
        ('00100002',  8, '2024-12-09', '0001', '003',   15.99, NULL),
        ('00100002',  9, '2024-12-09', '0001', '003',   90.99, NULL),
        ('00100002', 10, '2024-12-09', '0001', '003',   89.99, NULL),
        ('00100002', 11, '2024-12-10', '0001', '009',    4.00, '00200003'),
        ('00100002', 12, '2024-12-11', '0001', '008',    4.00, '00200003'),
        ('00200001',  1, '2008-01-05', '0001', '001', 5000.00, NULL),
        ('00200001',  2, '2008-01-07', '0001', '003', 4000.00, NULL),
        ('00200001',  3, '2008-01-09', '0001', '004', 2000.00, NULL),
        ('00200001',  4, '2008-01-11', '0001', '003', 1000.00, NULL),
        ('00200001',  5, '2008-01-13', '0001', '003', 2000.00, NULL),
        ('00200001',  6, '2008-01-15', '0001', '004', 4000.00, NULL),
        ('00200001',  7, '2008-01-19', '0001', '003', 2000.00, NULL),
        ('00200001',  8, '2008-01-21', '0001', '004', 3000.00, NULL),
        ('00200001',  9, '2008-01-23', '0001', '003', 7000.00, NULL),
        ('00200001', 10, '2008-01-27', '0001', '004', 1000.00, NULL),
        ('00200001', 11, '2008-01-30', '0001', '004', 3000.00, NULL),
        ('00200001', 12, '2008-02-04', '0001', '003', 2000.00, NULL),
        ('00200001', 13, '2008-02-08', '0001', '004', 4000.00, NULL),
        ('00200001', 14, '2008-02-13', '0001', '003', 2000.00, NULL),
        ('00200001', 15, '2008-02-19', '0001', '004', 1000.00, NULL),
        ('00200001', 16, '2024-12-12', '0001', '009',    6.00, '00100001'),
        ('00200001', 17, '2024-12-13', '0001', '008',    6.00, '00100001'),
        ('00200001', 18, '2024-12-14', '0001', '003',   30.00, NULL),
        ('00200001', 19, '2024-12-15', '0001', '004',   20.00, NULL),
        ('00200001', 20, '2024-12-15', '0001', '004',   10.00, NULL),
        ('00200002', 1, '2008-01-09', '0001', '001', 3800.00, NULL),
        ('00200002', 2, '2008-01-20', '0001', '003', 4200.00, NULL),
        ('00200002', 3, '2008-03-06', '0001', '004', 1200.00, NULL),
        ('00200002', 4, '2024-12-10', '0001', '008',    5.00, '00100001'),
        ('00200002', 5, '2024-12-11', '0001', '009',    5.00, '00100001'),
        ('00200002', 6, '2024-12-16', '0001', '003',  100.00, NULL),
        ('00200002', 7, '2024-12-17', '0001', '004',  100.00, NULL),
        ('00200003', 1, '2008-01-11', '0001', '001', 2500.00, NULL),
        ('00200003', 2, '2008-01-17', '0001', '003', 1500.00, NULL),
        ('00200003', 3, '2008-01-20', '0001', '004',  500.00, NULL),
        ('00200003', 4, '2008-02-09', '0001', '004',  500.00, NULL),
        ('00200003', 5, '2008-02-25', '0001', '003', 3500.00, NULL),
        ('00200003', 6, '2008-03-11', '0001', '004',  500.00, NULL),
        ('00200003', 7, '2024-12-10', '0001', '008',    4.00, '00100002'),
        ('00200003', 8, '2024-12-11', '0001', '009',    4.00, '00100002'),
        ('00300001', 1, '2008-01-07', '0002', '001', 5600.00, NULL),
        ('00300001', 2, '2008-01-18', '0002', '003', 1400.00, NULL),
        ('00300001', 3, '2008-01-25', '0002', '002', 7000.00, NULL);

    -- 4) Recalculo del saldo y del contador de movimientos de cada cuenta
    UPDATE cuenta c
    SET c.dec_cuensaldo = (
            SELECT COALESCE(SUM(CASE WHEN t.vch_tipoaccion = 'INGRESO'
                                     THEN m.dec_moviimporte
                                     ELSE -m.dec_moviimporte END), 0)
            FROM movimiento m
            JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo
            WHERE m.chr_cuencodigo = c.chr_cuencodigo),
        c.int_cuencontmov = (
            SELECT COUNT(*) FROM movimiento m WHERE m.chr_cuencodigo = c.chr_cuencodigo);

    -- 5) El saldo nunca puede quedar negativo (regla RB-03)
    SELECT COUNT(*) INTO v_cuentas_negativas FROM cuenta WHERE dec_cuensaldo < 0;
    IF v_cuentas_negativas > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sp_cargar_datos_prueba: se genero un saldo negativo, se revierte la carga';
    END IF;

    COMMIT;

    -- 6) Evidencia de la recarga
    SELECT 'sp_cargar_datos_prueba'  AS procedimiento,
           NOW()                     AS fecha_carga,
           (SELECT COUNT(*) FROM movimiento) AS total_movimientos,
           (SELECT COUNT(*) FROM cuenta)     AS total_cuentas,
           (SELECT COUNT(*) FROM logsession) AS total_sesiones,
           (SELECT COUNT(*) FROM cuenta WHERE dec_cuensaldo < 0) AS cuentas_con_saldo_negativo;
END$$

DELIMITER ;
