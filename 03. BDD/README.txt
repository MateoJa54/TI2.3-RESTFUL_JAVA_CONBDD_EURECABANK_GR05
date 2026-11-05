================================================================================
03. BDD - Base de datos EURECABANK (MySQL 8.4)
================================================================================

QUE ES
  Base de datos de EURECABANK: esquema "eurekabank" con 17 tablas, semilla de
  datos de prueba (personas y documentos ficticios) y el usuario de aplicacion
  con privilegio minimo. Material del proyecto TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03.

CONTENIDO
  docker-compose.yml              MySQL 8.4 en Docker (puerto host 3307)
  docker/init/00_esquema.sql      Crea el esquema y fija America/Guayaquil
  docker/init/01_estructura.sql   Las 17 tablas (PK, FK, indices, CHECK)
  docker/init/02_datos.sql        Semilla: 14 empleados, 20 clientes,
                                  7 sucursales, 2 monedas, 10 tipos de movimiento,
                                  11 asignados, 6 modulos, 6 cuentas,
                                  74 movimientos, 2 parametros, 42 permisos,
                                  11 usuarios y 7 contadores
  docker/init/03_usuarios_app.sql Usuario eurekabank_app (privilegio minimo)
  docker/init/04_carga_prueba.sql Procedimiento sp_cargar_datos_prueba()
  consultas/verificacion.sql      Consultas de verificacion y apoyo para la defensa
  GUIA_DOCKER_DBEAVER.md          Guia paso a paso (Docker, MySQL local, DBeaver)
  DIAGRAMA_BDD.md                 Modelo de datos: diccionario, cardinalidades,
                                  tipos de movimiento y parametros

COMO LEVANTARLA
  Opcion A, con Docker (desde esta carpeta):
      docker compose up -d
      docker compose logs -f
  Opcion B, con MySQL 8.4 local (desde esta carpeta, PowerShell):
      foreach ($f in @("00_esquema.sql","01_estructura.sql","02_datos.sql","03_usuarios_app.sql","04_carga_prueba.sql")) { & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 -e "source docker/init/$f" }
  Los scripts son idempotentes: se pueden volver a ejecutar sin duplicar datos.

CREDENCIALES
  Administrador (Docker):   root / root123
  Administrador (local):    root / (tu clave)
  Aplicacion:               eurekabank_app / EurekaBank2026!
    Privilegios: SELECT, INSERT, UPDATE, DELETE sobre eurekabank.*
    JDBC (Docker):  jdbc:mysql://127.0.0.1:3307/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil
    JDBC (local):   jdbc:mysql://127.0.0.1:3306/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil

PUERTOS
  Docker publica el 3307 del host (el 3306 del contenedor) para no chocar con un
  MySQL local en 3306. Con MySQL local se usa el 3306.

AVISO IMPORTANTE
  Los scripts de docker/init SOLO se ejecutan la primera vez que se crea el
  volumen "eurekabank_data". Si editas un script y quieres que se aplique:
      docker compose down -v
      docker compose up -d
  Con solo "docker compose down" el volumen se conserva y los scripts no se
  vuelven a cargar.

RECARGA RAPIDA DE LOS DATOS DE PRUEBA (no usar en produccion):
      CALL sp_cargar_datos_prueba();
  Borra y recarga movimiento, cuenta y logsession, y recalcula dec_cuensaldo e
  int_cuencontmov a partir de los movimientos.
================================================================================
