-- =============================================================================
-- 03_usuarios_app.sql - EURECABANK / MySQL 8.4
-- Usuario de aplicacion con privilegio minimo.
--
-- Credenciales (desarrollo local / laboratorio):
--   usuario : eurekabank_app
--   clave   : EurekaBank2026!
--
-- NO se le concede ALL PRIVILEGES sobre *.* ni se le da el usuario root.
-- Solo puede leer y escribir filas de las 17 tablas del esquema eurekabank.
--
-- Idempotente: CREATE USER IF NOT EXISTS + ALTER USER ... IDENTIFIED BY dejan la
-- clave siempre puesta, y los GRANT no duplican permisos.
-- =============================================================================

-- 1) Crear el usuario si no existe
CREATE USER IF NOT EXISTS 'eurekabank_app'@'%' IDENTIFIED BY 'EurekaBank2026!';

-- 2) Fijar siempre la clave (por si el usuario ya existia con otra clave)
ALTER USER 'eurekabank_app'@'%' IDENTIFIED BY 'EurekaBank2026!';

-- 3) Privilegio minimo sobre el esquema eurekabank
GRANT SELECT, INSERT, UPDATE, DELETE ON eurekabank.* TO 'eurekabank_app'@'%';

-- 4) Limpieza: la imagen oficial de Docker concede ALL PRIVILEGES sobre
--    MYSQL_DATABASE al usuario de MYSQL_USER. Se revocan esos permisos
--    sobrantes para que el usuario de aplicacion quede con el minimo.
--    El GRANT del punto 3 garantiza que la cuenta existe en el nivel de base,
--    de modo que este REVOKE nunca falla.
REVOKE ALL PRIVILEGES ON eurekabank.* FROM 'eurekabank_app'@'%';

-- 5) Se vuelve a conceder el privilegio minimo (SELECT, INSERT, UPDATE, DELETE)
GRANT SELECT, INSERT, UPDATE, DELETE ON eurekabank.* TO 'eurekabank_app'@'%';

-- 6) Aplicar los cambios en la tabla de privilegios
FLUSH PRIVILEGES;

-- 7) Comprobacion: debe listar USAGE y los 4 permisos sobre eurekabank.*
SHOW GRANTS FOR 'eurekabank_app'@'%';
