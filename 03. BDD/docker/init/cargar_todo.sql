-- =============================================================================
-- EURECABANK - Carga completa de la base de datos (5 scripts, en orden)
-- =============================================================================
-- Ejecutar DESDE LA RAIZ DEL PROYECTO (la carpeta que contiene "03. BDD"):
--
--   Get-Content "03. BDD\docker\init\cargar_todo.sql" | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p
--
-- O, mas comodo, el script que lo hace todo y ademas verifica:
--
--   .\03. BDD\cargar_bdd.ps1
--
-- Los scripts usan rutas relativas a la raiz del proyecto, por eso hay que
-- ejecutarlos desde alli. El orden importa: el esquema antes que las tablas,
-- las tablas antes que los datos, y los datos antes que los permisos.
-- =============================================================================

source 03. BDD/docker/init/00_esquema.sql
source 03. BDD/docker/init/01_estructura.sql
source 03. BDD/docker/init/02_datos.sql
source 03. BDD/docker/init/03_usuarios_app.sql
source 03. BDD/docker/init/04_carga_prueba.sql
