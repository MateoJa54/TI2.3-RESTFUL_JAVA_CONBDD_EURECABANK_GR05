-- =============================================================================
-- 00_esquema.sql - EURECABANK / MySQL 8.4
-- Crea el esquema `eurekabank` con utf8mb4 y fija la zona horaria del Ecuador.
--
-- Es idempotente: se puede ejecutar varias veces sin efectos secundarios.
-- En Docker este archivo corre dentro de /docker-entrypoint-initdb.d (solo la
-- primera vez que se crea el volumen).
-- =============================================================================

-- 1) Esquema de la base de datos
CREATE DATABASE IF NOT EXISTS eurekabank
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- 2) Contexto de ejecucion
USE eurekabank;

-- 3) Conexion en utf8mb4 (acentos y simbolos correctos en los textos)
SET NAMES utf8mb4;

-- 4) Zona horaria America/Guayaquil (UTC-5, Ecuador no aplica horario de verano).
--    La tabla mysql.time_zone_name solo existe si el servidor tiene cargadas las
--    tablas de zona horaria; si no estan cargadas se usa el desplazamiento fijo
--    equivalente (-05:00) para que el script nunca falle.
SET @tz_guayaquil = (SELECT COUNT(*) FROM mysql.time_zone_name WHERE Name = 'America/Guayaquil');
SET @tz_sql = IF(@tz_guayaquil > 0,
                  'SET time_zone = ''America/Guayaquil''',
                  'SET time_zone = ''-05:00''');
PREPARE stmt_tz FROM @tz_sql;
EXECUTE stmt_tz;
DEALLOCATE PREPARE stmt_tz;

-- 5) Comprobacion de la sesion (debe devolver 1 fila)
SELECT DATABASE() AS base_datos,
       @@character_set_database AS charset_base,
       @@collation_database AS collation_base,
       @@time_zone AS zona_horaria,
       VERSION() AS version_mysql;
