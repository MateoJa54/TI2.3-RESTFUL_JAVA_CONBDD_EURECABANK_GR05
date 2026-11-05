# ENTREGABLES — EURECABANK RESTful (Java) — Grupo GR03

Proyecto de Arquitectura de Software · TI2.1 · WAR `EUREKABANK_RESTFUL_JAVA` (Payara 7) y base de
datos MySQL 8.4 con el esquema `eurekabank` (17 tablas). Contrato congelado v1.0 del 04/11/2026.

Las cifras de este documento se contaron con `find` y `ls` sobre el repositorio, o se leyeron de
los informes `target\surefire-reports`. Lo que no se pudo comprobar aparece como pendiente.

## 1. Inventario

| Carpeta | Contenido | Ficheros |
|---|---|---|
| `01. UML\00. CONTRATO` | Contrato congelado v1.0 (`CONTRATO_CONGELADO_REST.md`) | 1 |
| `01. UML\01. ERS` | ERS en Word (`ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx`, 990 759 bytes) | 1 |
| `01. UML\02. ECUD` | ECUD en Word (`ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx`, 38 029 bytes) | 1 |
| `01. UML\03. DIAGRAMAS` | 39 modelos (38 `.oom` y 1 `.eam`), 5 proyectos `.prj`, 5 `.pjb`, 38 `.oob`, 1 `.eab` (41 modelos con los de clases de escritorio y web) | 124 |
| `02. MER` | 3 modelos (`.cdm`, `.ldm`, `.pdm`) con sus `.cdb`, `.ldb`, `.pdb`, 3 `.prj`, 3 `.pjb` y `README.txt` | 13 |
| `03. BDD` | `docker-compose.yml`, 6 scripts SQL en `docker/init`, consultas, guía Docker/DBeaver, `cargar_bdd.ps1`, `DIAGRAMA_BDD.md` | 12 |
| `04. SERVIDOR` | Contrato OpenAPI (`contrato\eurekabank-openapi.yaml`) y proyecto Maven: 54 clases de `main`, 27 de `test`, `pom.xml`, `web.xml` y resto | 87 |
| `04. CLICON` | Cliente de consola Maven: 29 clases de `main`, 23 de `test` y `pom.xml` | 53 |
| `04. CLIESC` | Cliente de escritorio Swing y FlatLaf (`EUREKABANK_RESTFUL_JAVA_ESC`, 707 pruebas) y su `README.md` | 60 |
| `04. CLIWEB` | Cliente web con Servlets y JSP (`EUREKABANK_RESTFUL_JAVA_WEB`, 723 pruebas) y su `README.md` | 72 |
| `04. CLIMOV` | Instrucciones para el compañero que haga el cliente móvil (`README.md`) | 1 |
| `05. DOCUMENTACION` | Manual en Word (`MANUAL_EUREKABANK_RESTFUL_JAVA_GR03.docx`) y figuras PNG de apoyo | 65 |
| Raíz | `README.md`, `ENTREGABLES.md`, `CONTEXTO_PROYECTO_REST.md`, `PLAN_TRABAJO.md`, `PROMPT_INICIO.md`, `RESTFUL_JAVA_CONBDD_GR03.sws`, `.gitignore`, `.gitattributes` | 8 |

Total: 386 ficheros, sin contar `target\` ni el manual. Lo compilado (`target\`) no se versiona:
se regenera con `mvn -B -o clean package`.

Ejecutables: el WAR está en `04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\target\EUREKABANK_RESTFUL_JAVA.war`
y el JAR de la consola en `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\target\EUREKABANK_RESTFUL_JAVA_CON.jar`.

## 2. Documentos Word

- ERS: `01. UML\01. ERS\ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx`.
- ECUD: `01. UML\02. ECUD\ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx`. No se ha verificado cuántas
  fichas contiene.
- Manual técnico: `05. DOCUMENTACION\MANUAL_EUREKABANK_RESTFUL_JAVA_GR03.docx`. **En generación**:
  no existía al cerrar este inventario.

## 3. Modelos UML y MER

Los 39 modelos UML son 12 de casos de uso (`CU_Sistema` y `CU_01` a `CU_11`), 12 de actividad
(`DA_00` a `DA_11`), 12 de secuencia (`DS_01` a `DS_12`), 2 de clases (`DC_Servidor` y
`DC_Cliente_Consola`) y 1 de arquitectura (`DA_EUREKABANK_RESTFUL_JAVA.eam`). Están en
`01. UML\03. DIAGRAMAS\`.

Los 3 modelos del MER están en `02. MER\`: conceptual (`MC_EUREKABANK`), lógico (`ML_EUREKABANK`) y
físico (`MF_EUREKABANK`).

## 4. Contrato y OpenAPI

- `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md`, v1.0: 9 operaciones, 17 códigos, 14 reglas de
  negocio (RB-01 a RB-14), 11 casos de uso (CU-01 a CU-11) y 7 actores.
- `04. SERVIDOR\contrato\eurekabank-openapi.yaml`, OpenAPI 3.0.3. Sirve la URL
  `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`.

## 5. Pruebas

- Servidor: 384 pruebas, 0 fallos, 0 errores (`04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\target\surefire-reports`).
- Consola: 381 pruebas, 0 fallos, 0 errores (`04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\target\surefire-reports`).

Las pruebas de integración del servidor se saltan si no hay `EUREKABANK_DB_URL`, así que el número
depende del entorno en que se ejecutan.

## 6. Guion de presentación (10 minutos)

1. Portada y alcance (1 min). Decir qué es EURECABANK: un servicio REST con 9 operaciones y 17 tablas.
   Todo el proyecto deriva del contrato congelado. Abrir `ENTREGABLES.md` y el contrato.
2. Modelo de datos (1-2 min). Mostrar los 3 niveles del MER. Explicar que `movimiento` es el libro
   de la cuenta, con clave compuesta, y que el saldo se recalcula en la misma transacción.
3. Casos de uso (2-3 min). Abrir `CU_Sistema.oom` con los actores y la ficha de CU-01 o CU-07 en el ECUD.
4. Contrato REST (3-4 min). Mostrar el OpenAPI, las 9 rutas, la cabecera `Authorization: Bearer` y el
   sobre de respuesta con su código de negocio y su HTTP.
5. Orden de validación (4-5 min). Explicar por qué la capa REST valida cabecera y parámetros antes de
   delegar (contrato §6.5). `FiltroToken` solo extrae el token.
6. Arquitectura (5-6 min). Abrir `DA_EUREKABANK_RESTFUL_JAVA.eam`: tres niveles, clientes por HTTP y
   JSON, y acceso a la base por JDBC.
7. Demo en vivo (6-8 min). Login, consulta de cuentas y una transferencia con `curl`. Provocar un
   error (CTA-003 o AUTH-003) para mostrar código y HTTP.
8. ERS (8-9 min). Mostrar las reglas de negocio, el catálogo de 17 códigos y la trazabilidad.
9. Implementación (9 min). Paquetes `api.rest`, `seguridad`, `servicios`, `datos`. Mencionar 384 pruebas
   del servidor y 381 de la consola.
10. Base de datos y cierre (10 min). `03. BDD\cargar_bdd.ps1` y `01_estructura.sql` con las 17 tablas.
    Cerrar con los pendientes que se decidan.

Plan B: si falla la demo en vivo, mostrar las figuras del manual y repetir los `curl` ya preparados.

## 7. Pendientes y decisiones abiertas

- Integrantes: GARCIA ROMERO EDUARDO DAVID, IZA BARRIONUEVO JUAN MATEO y JACOME NOROÑA BRAYAN JOSUE, ya escritos en el ERS, el ECUD y el manual.
- Nombre del repositorio: la carpeta es `TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03`, pero el workspace
  se llama `RESTFUL_JAVA_CONBDD_GR03.sws`. Decidir el nombre final y renombrar el `.sws`.
- CORS: el contrato §11.1 dice que `EUREKABANK_ORIGENES_CORS` vale `*` por defecto. El código
  (`FiltroCors.java`, línea 27) usa `http://localhost:4200` cuando la variable no existe. Decidir cuál
  gana y dejar el otro igual. El cliente web necesita su origen.
- `FiltroCors` no aparece en el árbol de paquetes del contrato (§10.2). Tampoco aparecen `AplicacionRest`
  y `ServiciosEurekaBank` en `api.rest`. Añadirlos al contrato o documentarlos en el ERS.
- `LIM-001` está reservado: figura en el catálogo (§5) y en el mapa HTTP (§10.3), pero ninguna
  operación lo emite. Documentarlo como reservado en el ERS y en el ECUD.
- Clave de la base por defecto en el código: `ProveedorConexion.java` (línea 54) lleva un valor por
  defecto para la clave del usuario de aplicación, igual que el contrato §11.1. Decidir si se quita y
  se exige por variable o propiedad.
- `EUREKABANK_TOKEN_MINUTOS`: `ServiciosEurekaBank.java` (línea 60) lo lee solo de la variable de
  entorno. El contrato §11.1 fija el orden variable, propiedad, fichero y defecto. Decidir: se deja
  solo la variable (y se corrige §11.1) o se implementa el orden completo.
- Puerto de Docker: `03. BDD\docker-compose.yml` publica MySQL en `127.0.0.1:3307`, mientras el
  contrato §11.1 y la URL por defecto usan el 3306. El README lo explica. Decidir si cambia el Compose
  o el valor por defecto.
- Manual técnico en Word: en generación, sin verificar todavía.
- Cliente móvil (CLIMOV): solo tiene instrucciones; el código lo aporta el compañero que lo haga. Escritorio y web ya están construidos.
