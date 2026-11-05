# TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03

Servicio web RESTful con JSON para el banco ficticio **EURECABANK**, grupo **GR03**, materia
Arquitectura de Software (ESPE). Expone nueve operaciones bancarias (autenticación, cuentas,
movimientos, depósito, retiro, transferencia y costos de mantenimiento) sobre una base de datos
MySQL 8.4 de 17 tablas.

Es la variante REST del proyecto hermano de la misma materia. Ambos comparten modelo de datos,
base de datos y reglas de negocio; solo cambia la capa de entrada.

## Documentos de referencia

- Contrato congelado v1.0, fuente de verdad: `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md`
- Contrato OpenAPI 3.0.3: `04. SERVIDOR\contrato\eurekabank-openapi.yaml`
- Inventario de entregables y pendientes: `ENTREGABLES.md`
- Memoria de trabajo: `CONTEXTO_PROYECTO_REST.md` (léela antes de tocar código), `PLAN_TRABAJO.md`, `PROMPT_INICIO.md`

## Requisitos

- JDK 17 o superior (ambos `pom.xml` compilan con `maven.compiler.release` 17).
- Maven 3.9 o superior. En el equipo no está en el PATH: se usa el de NetBeans,
  `C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd`.
- MySQL 8.4 local o Docker Desktop.
- Payara Server 7 en `C:\payara7`, dominio `domain1`, puerto HTTP 8080.

## 1. Base de datos

Carga todo en un comando desde la raíz del proyecto. Pide la clave de `root` de forma segura, crea
el esquema `eurekabank` con sus 17 tablas, carga la semilla y crea el usuario de aplicación.

```powershell
& ".\03. BDD\cargar_bdd.ps1"
```

Parámetros opcionales: `-Usuario root -Puerto 3306` (valores por defecto).

Con Docker, desde `03. BDD`: `docker compose up -d`. El contenedor publica MySQL en
`127.0.0.1:3307` (no en 3306, para no chocar con un MySQL local). Si usas Docker, define
`EUREKABANK_DB_URL` con el puerto 3307.

Credenciales de laboratorio, según el contrato §11: usuario de aplicación `eurekabank_app` con
clave `EurekaBank2026!`.

Consultas de verificación: `03. BDD\consultas\verificacion.sql`.

## 2. Compilar

Servidor, genera `target\EUREKABANK_RESTFUL_JAVA.war`:

```powershell
cd "04. SERVIDOR\EUREKABANK_RESTFUL_JAVA"
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -B -o clean package
```

Consola, genera `target\EUREKABANK_RESTFUL_JAVA_CON.jar`:

```powershell
cd "04. CLICON\EUREKABANK_RESTFUL_JAVA_CON"
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -B -o clean package
```

Las pruebas de integración del servidor solo corren si `EUREKABANK_DB_URL` está definida; si no,
se saltan (`Assumptions`) sin fallar.

## 3. Desplegar el servidor en Payara 7

Todo en la misma ventana de PowerShell, para que la variable llegue al dominio al arrancarlo:

```powershell
$env:EUREKABANK_DB_URL = "jdbc:mysql://127.0.0.1:3306/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil"
& "C:\payara7\bin\asadmin.bat" start-domain domain1
& "C:\payara7\bin\asadmin.bat" deploy --name EUREKABANK_RESTFUL_JAVA --contextroot EUREKABANK_RESTFUL_JAVA "04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\target\EUREKABANK_RESTFUL_JAVA.war"
```

Variables opcionales, leídas por el servidor: `EUREKABANK_DB_USUARIO` y `EUREKABANK_DB_CLAVE`
(por defecto `eurekabank_app`), `EUREKABANK_TOKEN_MINUTOS` (por defecto 30, solo desde variable de
entorno en el código actual) y `EUREKABANK_ORIGENES_CORS` (orígenes permitidos; ver
`ENTREGABLES.md`, pendientes).

Base de la API: `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`. Página de inicio del WAR:
`http://localhost:8080/EUREKABANK_RESTFUL_JAVA/`.

## 4. Ejecutar la consola

```powershell
java -jar "04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\target\EUREKABANK_RESTFUL_JAVA_CON.jar"
```

Por defecto apunta a `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`. Para otro servidor, pásalo
como primer argumento, o usa `-Dservicio.url` o la variable `SERVICIO_URL`.

Usuario de prueba del contrato §12: `cromero` / `Cromero2026`. El cliente envía el SHA-256 de la
clave, como indica el contrato.

## 5. Las nueve rutas

Todas bajo `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`, con JSON en UTF-8. Las siete rutas
protegidas exigen la cabecera `Authorization: Bearer <token>`. El token nunca va en la URL ni en el
cuerpo.

- `POST /auth/login`, sin token. Cuerpo `{"usuario":"...","clave":"..."}`. Devuelve `token`,
  `empleado`, `nombreEmpleado`, `expiracion` y `rol`.
- `POST /auth/logout`, con token. Cuerpo `{}`.
- `GET /clientes/{cedula}/cuentas`, con token. Lista las cuentas del cliente, incluidas las no activas.
- `GET /cuentas/{cuenta}`, con token. Consulta una cuenta.
- `GET /cuentas/{cuenta}/movimientos`, con token. Libro de movimientos de la cuenta.
- `POST /cuentas/{cuenta}/depositos`, con token. Cuerpo `{"monto":10.00}`.
- `POST /cuentas/{cuenta}/retiros`, con token. Cuerpo `{"monto":5.00}`.
- `POST /transferencias`, con token. Cuerpo `{"cuentaOrigen":"...","cuentaDestino":"...","monto":10.00}`.
- `GET /cuentas/{cuenta}/costos`, con token. Costos de mantenimiento y de movimiento.

Ejemplos en bash (Git Bash o Linux). Usan la semilla de prueba: cédula `06914897` (cliente `00001`,
titular de la cuenta `00200002`) y la cuenta `00200001`, ambas en soles y activas.

```bash
BASE="http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api"
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"usuario":"cromero","clave":"Cromero2026"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s -X POST "$BASE/auth/logout" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{}'
curl -s "$BASE/clientes/06914897/cuentas" -H "Authorization: Bearer $TOKEN"
curl -s "$BASE/cuentas/00200002" -H "Authorization: Bearer $TOKEN"
curl -s "$BASE/cuentas/00200002/movimientos" -H "Authorization: Bearer $TOKEN"
curl -s -X POST "$BASE/cuentas/00200002/depositos" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"monto":10.00}'
curl -s -X POST "$BASE/cuentas/00200002/retiros" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"monto":5.00}'
curl -s -X POST "$BASE/transferencias" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"cuentaOrigen":"00200002","cuentaDestino":"00200001","monto":10.00}'
curl -s "$BASE/cuentas/00200002/costos" -H "Authorization: Bearer $TOKEN"
```

Los depósitos, retiros y transferencias cambian saldos en la base de laboratorio. En PowerShell 5.1
las comillas dobles del JSON se pierden al pasarlas a `curl.exe`; en ese caso usa Git Bash o pon el
JSON en un fichero y envíalo con `--data-binary @fichero.json`.

## 6. Códigos de respuesta

El HTTP status acompaña siempre al código de negocio. El cuerpo es un sobre con `estado`, `codigo` y
`mensaje`. El mensaje es literal y está en el contrato §5.

| Código | HTTP | Significado |
|---|---|---|
| `200` | 200 | Operación correcta |
| `DAT-001` | 400 | Datos incompletos o incorrectos, o cabecera de token ausente |
| `AUTH-001` | 401 | Usuario o clave incorrectos |
| `AUTH-002` | 403 | Usuario inactivo |
| `AUTH-003` | 401 | Token no válido |
| `AUTH-004` | 401 | Token expirado |
| `CTA-001` | 404 | Cuenta inexistente |
| `CTA-002` | 409 | Cuenta no está en estado ACTIVO |
| `CTA-003` | 400 | Cuenta origen igual a cuenta destino |
| `CTA-004` | 409 | Cuenta destino de otra moneda |
| `CLI-001` | 404 | Cliente inexistente |
| `SAL-001` | 409 | Saldo insuficiente |
| `MON-001` | 400 | Monto no positivo o con más de dos decimales |
| `MON-002` | 409 | Moneda no soportada |
| `LIM-001` | 409 | Reservado: ninguna operación lo emite |
| `SER-001` | 503 | Base de datos no disponible |
| `ERR-001` | 500 | Error interno |

## 7. Cifras del proyecto

- Servidor: 384 pruebas, 0 fallos, 0 errores (`04. SERVIDOR\...\target\surefire-reports`).
- Consola: 381 pruebas, 0 fallos, 0 errores (`04. CLICON\...\target\surefire-reports`).
- 39 modelos UML en PowerDesigner: 38 `.oom` y 1 `.eam`. Más 3 modelos del MER.
- 17 tablas, 9 operaciones, 17 códigos de respuesta.

Las cifras salen de los informes de la última ejecución que hay en `target\`. Para confirmarlas,
vuelve a correr `mvn -o clean test` en cada proyecto.

## 8. Estructura de carpetas

```
TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03/
├─ 01. UML/        contrato congelado, ERS, ECUD y 39 modelos PowerDesigner
├─ 02. MER/        modelo conceptual, lógico y físico
├─ 03. BDD/        docker-compose, scripts SQL, cargar_bdd.ps1, DIAGRAMA_BDD.md
├─ 04. SERVIDOR/   proyecto Maven del WAR y contrato OpenAPI
├─ 04. CLICON/     cliente de consola (JAR)
├─ 04. CLIESC/     cliente de escritorio (en desarrollo por el compañero)
├─ 04. CLIWEB/     cliente web (en desarrollo por el compañero)
├─ 04. CLIMOV/     cliente móvil Android (en desarrollo por el compañero)
├─ 05. DOCUMENTACION/  manual técnico en Word
├─ ENTREGABLES.md  inventario, guion de defensa y pendientes
└─ README.md       este archivo
```
