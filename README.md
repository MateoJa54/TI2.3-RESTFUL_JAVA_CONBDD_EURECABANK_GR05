# EURECABANK RESTful (Java con base de datos)

Servicio web REST con JSON para el banco ficticio **EURECABANK**, materia Arquitectura de Software
(ESPE). Nueve operaciones bancarias (autenticación, cuentas, movimientos, depósito, retiro,
transferencia y costos de mantenimiento) sobre una base de datos MySQL 8.4 de 17 tablas, con cuatro
clientes: consola, escritorio, web y (a cargo de otro compañero) móvil.

**Integrantes:** GARCIA ROMERO EDUARDO DAVID, IZA BARRIONUEVO JUAN MATEO, JACOME NOROÑA BRAYAN JOSUE.

## Qué hay en el repositorio

```
01. UML/            contrato congelado, ERS, ECUD y 41 modelos de PowerDesigner
02. MER/            modelo conceptual, lógico y físico
03. BDD/            scripts SQL, docker-compose, cargar_bdd.ps1, DIAGRAMA_BDD.md
04. SERVIDOR/       servidor REST (WAR, Jakarta EE) y contrato OpenAPI
04. CLICON/         cliente de consola (JAR)
04. CLIESC/         cliente de escritorio Swing (JAR)
04. CLIWEB/         cliente web Servlets y JSP (WAR)
04. CLIMOV/         instrucciones para el cliente móvil
05. DOCUMENTACION/  manual técnico en Word
```

Fuente de verdad del diseño: `01. UML/00. CONTRATO/CONTRATO_CONGELADO_REST.md` y
`04. SERVIDOR/contrato/eurekabank-openapi.yaml`.

## Requisitos

- **JDK 17 o superior** (`java -version`).
- **Maven 3.9 o superior** (`mvn -v`). Si no lo tienes en el PATH, NetBeans trae uno en
  `C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd`; en los comandos de abajo cámbialo por esa ruta.
- **MySQL 8.4** local, o **Docker Desktop**.
- **Payara Server 7** (carpeta de instalación, que abajo se llama `PAYARA`). En los ejemplos se usa
  el dominio `domain1` y el puerto HTTP 8080.
- Conexión a internet la primera vez, para que Maven descargue las dependencias.

Todos los comandos son de PowerShell y se ejecutan desde la carpeta raíz del repositorio.

## Inicio rápido (5 pasos)

### 1. Base de datos

Con MySQL local (pide la clave de `root` de forma segura, crea el esquema `eurekabank`, carga la semilla
y el usuario de aplicación `eurekabank_app`):

```powershell
.\03. BDD\cargar_bdd.ps1
```

Si tu `mysql.exe` no está en `C:\Program Files\MySQL\MySQL Server 8.4\bin`, pásalo con `-Mysql "ruta\mysql.exe"`.
Con Docker, en su lugar: `cd "03. BDD"; docker compose up -d`. El contenedor publica MySQL en `127.0.0.1:3307`,
así que antes de arrancar Payara define:

```powershell
$env:EUREKABANK_DB_URL = "jdbc:mysql://127.0.0.1:3307/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil"
```

### 2. Compilar los cuatro proyectos

```powershell
cd "04. SERVIDOR\EUREKABANK_RESTFUL_JAVA"; mvn -B clean package; cd ..\..
cd "04. CLICON\EUREKABANK_RESTFUL_JAVA_CON"; mvn -B clean package; cd ..\..
cd "04. CLIESC\EUREKABANK_RESTFUL_JAVA_ESC"; mvn -B clean package; cd ..\..
cd "04. CLIWEB\EUREKABANK_RESTFUL_JAVA_WEB"; mvn -B clean package; cd ..\..
```

Cada uno debe terminar en `BUILD SUCCESS`. Genera `target\EUREKABANK_RESTFUL_JAVA.war`,
`target\EUREKABANK_RESTFUL_JAVA_CON.jar`, `target\EUREKABANK_RESTFUL_JAVA_ESC.jar` y
`target\EUREKABANK_RESTFUL_JAVA_WEB.war`. Las 37 pruebas de integración del servidor se saltan solas si no
defines `EUREKABANK_DB_URL`.

### 3. Arrancar Payara y desplegar servidor y web

```powershell
& "PAYARA\bin\asadmin.bat" start-domain domain1
& "PAYARA\bin\asadmin.bat" deploy --name EUREKABANK_RESTFUL_JAVA --contextroot EUREKABANK_RESTFUL_JAVA "04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\target\EUREKABANK_RESTFUL_JAVA.war"
& "PAYARA\bin\asadmin.bat" deploy --name EUREKABANK_RESTFUL_JAVA_WEB --contextroot EUREKABANK_RESTFUL_JAVA_WEB "04. CLIWEB\EUREKABANK_RESTFUL_JAVA_WEB\target\EUREKABANK_RESTFUL_JAVA_WEB.war"
```

La API queda en `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api` y la web en
`http://localhost:8080/EUREKABANK_RESTFUL_JAVA_WEB/`. Si tu Payara usa otro puerto, define antes de arrancarlo
`$env:SERVICIO_URL = "http://localhost:<puerto>/EUREKABANK_RESTFUL_JAVA/api"` para que la web y los clientes encuentren la API.

### 4. Probar con un cliente

Usuario de prueba: `cromero` / `Cromero2026`. Cuentas de ejemplo: cédula `06914897`, cuenta `00200002`
(activa) y `00300001` (no activa, para ver el error `CTA-002`).

- **Web:** abre `http://localhost:8080/EUREKABANK_RESTFUL_JAVA_WEB/`.
- **Escritorio:** `java -jar "04. CLIESC\EUREKABANK_RESTFUL_JAVA_ESC\target\EUREKABANK_RESTFUL_JAVA_ESC.jar"`
- **Consola:** `java -jar "04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\target\EUREKABANK_RESTFUL_JAVA_CON.jar"`

Los clientes de escritorio y consola aceptan otra URL como primer argumento, con `-Dservicio.url=...` o con la
variable `SERVICIO_URL`.

### 5. Apagar

```powershell
& "PAYARA\bin\asadmin.bat" undeploy EUREKABANK_RESTFUL_JAVA_WEB
& "PAYARA\bin\asadmin.bat" undeploy EUREKABANK_RESTFUL_JAVA
& "PAYARA\bin\asadmin.bat" stop-domain domain1
```

## Variables de entorno del servidor

- `EUREKABANK_DB_URL`, `EUREKABANK_DB_USUARIO`, `EUREKABANK_DB_CLAVE`: conexión a MySQL (por defecto
  `127.0.0.1:3306`, usuario `eurekabank_app`, la clave de laboratorio del contrato §11).
- `EUREKABANK_TOKEN_MINUTOS`: vigencia del token (30 por defecto).
- `EUREKABANK_ORIGENES_CORS`: orígenes permitidos, separados por comas (por defecto `http://localhost:4200`).

## Las nueve rutas

Todas bajo `/EUREKABANK_RESTFUL_JAVA/api`, con JSON. Las siete rutas protegidas exigen la cabecera
`Authorization: Bearer <token>`; el token nunca va en la URL.

- `POST /auth/login` (sin token): `{"usuario":"...","clave":"<SHA-256 de la clave>"}`.
- `POST /auth/logout`, `GET /clientes/{cedula}/cuentas`, `GET /cuentas/{cuenta}`,
  `GET /cuentas/{cuenta}/movimientos`, `GET /cuentas/{cuenta}/costos`.
- `POST /cuentas/{cuenta}/depositos` y `/retiros`: `{"monto":10.00}`.
- `POST /transferencias`: `{"cuentaOrigen":"...","cuentaDestino":"...","monto":10.00}`.

Ejemplo en Git Bash:

```bash
BASE="http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api"; H=$(printf 'Cromero2026' | sha256sum | cut -d' ' -f1)
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d "{\"usuario\":\"cromero\",\"clave\":\"$H\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s "$BASE/clientes/06914897/cuentas" -H "Authorization: Bearer $TOKEN"
```

Cada movimiento cobra 2.00 de costo después de las primeras operaciones de una cuenta (regla del contrato),
así que probar depósitos y retiros cambia saldos. `.\03. BDD\cargar_bdd.ps1` restaura los datos.

## Códigos de respuesta

El HTTP status acompaña al código de negocio del sobre `{estado, codigo, mensaje}` (mensajes literales en el
contrato §5): `DAT-001` 400, `AUTH-001/003/004` 401, `AUTH-002` 403, `CTA-001` y `CLI-001` 404,
`CTA-002/004`, `SAL-001` y `MON-002` 409, `CTA-003` y `MON-001` 400, `SER-001` 503, `ERR-001` 500.

## Pruebas

Servidor 399, consola 381, escritorio y web con su propio conjunto (`mvn -B clean package` en cada proyecto
las ejecuta; todas deben dar 0 fallos).
