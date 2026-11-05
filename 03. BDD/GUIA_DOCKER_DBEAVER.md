# EURECABANK — Base de datos con Docker y DBeaver

Guía paso a paso para levantar la base de datos de EURECABANK (MySQL 8.4) y
conectarla desde DBeaver o desde el servidor REST Java.

- Motor: **MySQL 8.4** (imagen oficial `mysql:8.4`)
- Esquema: **`eurekabank`** con **17 tablas**
- Puerto publicado: **3307** (para no chocar con un MySQL local en 3306)
- Credenciales de la aplicación: **`eurekabank_app` / `EurekaBank2026!`**

---

## 1. Levantar la base con Docker

Desde la carpeta `03. BDD`:

```powershell
docker compose up -d
docker compose ps
docker compose logs -f
```

`docker compose logs -f` muestra la carga de los scripts. En un **volumen
nuevo** Docker ejecuta, en orden alfabético, los archivos de `docker/init`:

| Orden | Archivo | Qué hace |
|---|---|---|
| 1 | `00_esquema.sql` | Crea el esquema `eurekabank` (utf8mb4) y fija `America/Guayaquil` |
| 2 | `01_estructura.sql` | Crea las 17 tablas con PK, FK, índices y CHECK |
| 3 | `02_datos.sql` | Carga la semilla (14 empleados, 20 clientes, 74 movimientos, …) |
| 4 | `03_usuarios_app.sql` | Crea el usuario `eurekabank_app` con privilegio mínimo |
| 5 | `04_carga_prueba.sql` | Crea el procedimiento `sp_cargar_datos_prueba()` |

Espera a que el contenedor indique `healthy` (el *healthcheck* usa
`mysqladmin ping`).

Comandos de uso diario:

```powershell
docker compose up -d      # levantar
docker compose logs -f    # ver la carga
docker compose ps         # estado y salud
docker compose down       # detener (conserva el volumen)
docker compose down -v    # detener y BORRAR el volumen (recarga los scripts)
```

> **Los scripts solo se ejecutan la primera vez que se crea el volumen.**
> Si editas un `.sql` de `docker/init`, para que se aplique tienes que rehacer
> el volumen: `docker compose down -v` y después `docker compose up -d`.
> Con solo `docker compose down` el volumen se conserva y los scripts **no**
> se vuelven a cargar.

---

## 2. Levantar la base SIN Docker (MySQL 8.4 local)

Si ya tienes MySQL 8.4 instalado en Windows, puedes levantar la misma base en
el puerto 3306 con los mismos scripts.

1. Abre **PowerShell** y entra a la carpeta `03. BDD`:

```powershell
cd "C:\ruta\al\proyecto\03. BDD"
```

2. Ejecuta los 5 scripts en orden (una sola línea; el cliente `mysql` pide la
   clave de `root`):

```powershell
foreach ($f in @("00_esquema.sql","01_estructura.sql","02_datos.sql","03_usuarios_app.sql","04_carga_prueba.sql")) { & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 -e "source docker/init/$f" }
```

- `--default-character-set=utf8mb4` es **necesario**: sin él el cliente de
  Windows puede leer los acentos de los comentarios con otra codificación.
- Cada script devuelve `exit=0` si se aplicó bien.
- Si quieres forzar todo desde cero, borra antes el esquema:
  `mysql -u root -p -e "DROP DATABASE IF EXISTS eurekabank;"` y repite el paso 2.

3. Verifica que quedó todo cargado (17 tablas, 74 movimientos):

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 -e "USE eurekabank; SHOW TABLES; SELECT COUNT(*) AS movimientos FROM movimiento; SELECT COUNT(*) AS cuentas FROM cuenta;"
```

> **Nota sobre `04_carga_prueba.sql`:** ese archivo usa `DELIMITER`, que solo
> entienden el cliente de línea de comandos `mysql` y el arranque de Docker.
> Workbench y DBeaver no interpretan `DELIMITER` al pegar el texto: ejecuta
> ese archivo con el cliente `mysql` (o creates el procedimiento a mano).

---

## 3. Conectar en DBeaver

1. **File → New Database Connection → MySQL**.
2. Pestaña **Main**:

| Campo | Con Docker | Con MySQL local |
|---|---|---|
| Host | `127.0.0.1` | `127.0.0.1` |
| Puerto | **`3307`** | **`3306`** |
| Base de datos | `eurekabank` | `eurekabank` |
| Usuario | `root` | `root` |
| Contraseña | `root123` | tu clave de `root` |
| Driver | MySQL 8.x | MySQL 8.x |

3. Pestaña **Driver Properties** (obligatorio en esta máquina):

| Propiedad | Valor | Por qué |
|---|---|---|
| `Create database` | **desactivado** | La base ya existe; el motor no debe crearla |
| `Use SSL` | **desactivado** | MySQL local/Docker sin TLS |
| `Allow public key retrieval` | **activado** | Necesario con el método de autenticación `caching_sha2_password` |

4. **Test Connection** y luego **Finish**.

Conexión alternativa para revisar con el usuario de la aplicación:

| Campo | Valor |
|---|---|
| Host / Puerto | `127.0.0.1` / `3307` (Docker) o `3306` (local) |
| Base de datos | `eurekabank` |
| Usuario | `eurekabank_app` |
| Contraseña | `EurekaBank2026!` |

---

## 4. Credenciales de la aplicación

| Dato | Valor |
|---|---|
| Variable de entorno | `EUREKABANK_DB_USUARIO` |
| Usuario | `eurekabank_app` |
| Variable de entorno | `EUREKABANK_DB_CLAVE` |
| Clave | `EurekaBank2026!` |
| Privilegios | `SELECT, INSERT, UPDATE, DELETE` sobre `eurekabank.*` |

URL JDBC de desarrollo (Docker):

```
jdbc:mysql://127.0.0.1:3307/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil
```

Con MySQL local cambia `3307` por `3306`.

El usuario **no** puede crear bases de datos ni borrar tablas: al intentarlo
el motor responde `ERROR 1044 (Access denied)` y `ERROR 1142 (DROP command
denied)`.

---

## 5. Problemas frecuentes

### `ERROR 1045 (28000): Access denied for user 'root'@'localhost'`

La clave de `root` no es la que pusiste. MySQL 8.4 usa `caching_sha2_password`:
prueba con `mysql -u root -p` (te pide la clave) y, si no la conoces,
reinicia el servicio de MySQL como administrador y usa la opción de contraseña
temporal, o entra como el usuario del sistema con `--skip-grant-tables`.

### `ERROR 1045 (28000): Access denied for user 'eurekabank_app'@'%'`

El usuario de aplicación todavía no existe: ejecuta `03_usuarios_app.sql`
(los scripts de `docker/init` no se vuelven a correr si el volumen ya existe;
usa `docker compose down -v` o ejecuta ese archivo a mano).

### `ERROR 2003 (HY000): Can't connect to MySQL server on '127.0.0.1' (port 3306)`

Estás apuntando al puerto equivocado:

- Con Docker la base está en **3307**.
- Con MySQL local está en **3306**.

Comprueba con `docker compose ps` (columna PORTS) o `netstat -ano | findstr 330`.

### `Bind for 0.0.0.0:3307 failed: port is already allocated`

El puerto 3307 ya está ocupado (típico: una instancia anterior del contenedor o
el MySQL de pruebas). Soluciones:

```powershell
docker compose down          # libera el puerto del contenedor anterior
docker compose ps            # verifica que no quede otro contenedor
netstat -ano | findstr 3307 # identifica el proceso que ocupa el puerto
```

Si el puerto 3307 está ocupado por otro programa, cambia el puerto del host en
`docker-compose.yml` (por ejemplo a `3308`) y actualiza el host/puerto en
DBeaver y en la URL JDBC.

### `Public Key Retrieval is not allowed`

MySQL 8.4 usa `caching_sha2_password` y el cliente JDBC no puede traer la
clave pública del servidor. Dos soluciones:

1. En **DBeaver**: Driver Properties → marcar `Allow public key retrieval` y
   desmarcar `Use SSL`.
2. En la **URL JDBC**, agregar el parámetro:

```
jdbc:mysql://127.0.0.1:3307/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil
```

### Edité un script y no cambia nada

Los scripts de `docker/init` solo se ejecutan con volumen nuevo:

```powershell
docker compose down -v
docker compose up -d
```

Sin la `-v` el volumen `eurekabank_data` se conserva y los scripts no se
vuelven a cargar.

### Recargar los datos de prueba

Para dejar la base como estaba (6 cuentas y 74 movimientos) sin tocar Docker:

```sql
CALL sp_cargar_datos_prueba();
```

Borra y recarga `movimiento`, `cuenta` y `logsession`, y recalcula
`dec_cuensaldo` e `int_cuencontmov` a partir de los movimientos. **No usar en
producción.**
