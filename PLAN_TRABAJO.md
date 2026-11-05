# PLAN DE TRABAJO — TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03

> Documento complementario a `CONTEXTO_PROYECTO_REST.md`.
> El CONTEXTO dice **qué** es el sistema; este plan dice **cómo se construye, en qué orden,
> con qué comandos y con qué evidencia**.
>
> Fecha: 2026-11-04 · Fase actual: **0 (ciudades) preparada, lista para arrancar**
> No se ha modificado nada del proyecto todavía: solo análisis y verificación.

---

## 0. Resumen ejecutivo

El proyecto REST está **apenas empezado**: tiene el MER, la BDD y la memoria, y nada más.
Todo lo demás (contrato, ERS, ECUD, diagramas, servidor, consola, manual) está por hacer.

La buena noticia, verificada ejecutando, es que **la estrategia "copiar y adaptar" está
respaldada por evidencia**: el proyecto SOAP compila y pasa sus pruebas en esta misma máquina,
en modo offline, contra el MySQL real.

- Servidor SOAP: **265 pruebas, 0 fallos, 0 errores, 0 saltadas** — `BUILD SUCCESS`.
- Consola SOAP: **330 pruebas, 0 fallos, 0 errores, 0 saltadas** — `BUILD SUCCESS`.
- Los 42 generadores (Python, VBS, PowerShell) y las 66 imágenes PNG del proyecto SOAP **están
  en el commit `2af276d`** y son recuperables con `git show`. No hay que escribir 2500 líneas
  desde cero.

El plan es, por tanto: **recuperar del historial git → adaptar con script (sed/PowerShell)
→ reescribir solo la capa REST → verificar con comandos reales.**

Hay **6 riesgos concretos** (sección 2), uno de ellos (R1) es el que de verdad puede hacer
perder puntos del docente y hay que resolverlo en la fase 1, no en la 6.

---

## 1. Análisis del estado real

### 1.1 Lo que ya existe (OBSERVADO)

Árbol completo del proyecto REST en este momento — **30 ficheros**:

```
.gitattributes
.gitignore
CONTEXTO_PROYECTO_REST.md
PROMPT_INICIO.md
README.md
02. MER/  01. CONCEPTUAL/ (4)  02. LOGICO/ (4)  03. FISICO/ (4)  README.txt
03. BDD/  docker/init/ (6)  docker-compose.yml  cargar_bdd.ps1  consultas/  GUIA...  README.txt
.estacion/tareas/  (vacía)
```

Verificación de integridad de lo copiado (hash SHA-256 contra el proyecto SOAP):
- **21 de 24 ficheros idénticos byte a byte** (12 del MER: 4 por modelo + 9 de la BDD:
  `cargar_bdd.ps1`, `docker-compose.yml`, `consultas/verificacion.sql` y los 6 SQL de
  `docker/init/`).
- **3 ficheros legítimamente distintos**: `02. MER/README.txt`, `03. BDD/README.txt` y
  `03. BDD/GUIA_DOCKER_DBEAVER.md` — solo cambia el nombre del proyecto
  (`SOAP_JAVA_CONBDD_EURECABANK_GR03` → `RESTFUL_JAVA_CONBDD_EURECABANK_GR03`).

→ **El MER y la BDD no hay que tocarlos.** Solo hay que añadirles, si acaso, los scripts de
`02. MER/scripts/` que están en el historial git (para regenerar y validar los modelos).

### 1.2 Lo que falta (OBSERVADO)

Falta **todo** lo demás. En concreto:

- `01. UML/00. CONTRATO/` — vacío. Falta `CONTRATO_CONGELADO_REST.md`.
- `01. UML/01. ERS/` — falta `ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx`.
- `01. UML/02. ECUD/` — falta `ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx`.
- `01. UML/03. DIAGRAMAS/` — faltan las 5 familias (`01_CASOS_USO`, `02_ACTIVIDAD`,
  `03_SECUENCIA`, `04_CLASES`, `05_ARQUITECTURA`), 39 modelos y los scripts.
- `04. SERVIDOR/` — falta el proyecto Maven WAR y `contrato/eurekabank-openapi.yaml`.
- `04. CLICON/` — falta el proyecto Maven JAR de la consola.
- `05. DOCUMENTACION/` — falta el manual.
- `RESTFUL_JAVA_CONBDD_GR03.sws` — falta el workspace de PowerDesigner.
- `ENTREGABLES.md` — falta (el SOAP lo tiene).
- **No es repositorio git** (`.git` no existe) y `git status` no aplica.
- `.estacion/tareas/` está vacía: somos el primer equipo sobre este proyecto.

### 1.3 Baseline del proyecto SOAP — verificado **ejecutando**, no leyendo

Ejecutado hoy en esta máquina, con `-o` (offline) y contra el MySQL local:

```
04. SERVIDOR\EUREKABANK_SOAP_JAVA
  mvn -B -o clean test  ->  Tests run: 265, Failures: 0, Errors: 0, Skipped: 0
                           BUILD SUCCESS  (16,0 s)

04. CLICON\EUREKABANK_SOAP_JAVA_CON
  mvn -B -o clean test  ->  Tests run: 330, Failures: 0, Errors: 0, Skipped: 0
                           BUILD SUCCESS  (16,1 s)
```

Que `Skipped: 0` importa: los tests de integración contra MySQL **sí corrieron** (no se
saltaron por falta de base de datos), así que el entorno de pruebas está operativo.

Inventario del SOAP que se va a copiar:

- Servidor: **45 clases** en 6 paquetes (`controladores.ws` 2, `datos` 21, `modelos` 11,
  `seguridad` 3, `servicios` 4, `utilidades` 4) + 24 ficheros de test (17 clases con `@Test`
  + 7 auxiliares) + `eurekabank.properties` + `webapp/WEB-INF/{web.xml,beans.xml}`.
- Consola: **31 clases** (`Main`, `cliente` 2, `controlador` 5, `modelo` 12, `util` 5,
  `vista` 5, `ws` 1) + 21 ficheros de test (19 clases con `@Test` + 2 auxiliares).
- Contrato SOAP: `WSEUREKABANK.wsdl` (243 líneas) + `WSEUREKABANK_schema.xsd` (804 líneas).
- Diagramas: **39 modelos** (12 CU + 12 DA + 12 DS + 2 DC + 1 EAM), cada uno en `.oom` + `.oob`.

### 1.4 Entorno verificado (OBSERVADO)

| Recurso | Estado verificado hoy |
|---|---|
| Maven | `C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd` → 3.9.16 (**no está en el PATH**) |
| JDK | Temurin 25.0.4.1 (se compila con `maven.compiler.release=17`) |
| Payara 7 | `C:\payara7` presente. Dominios: `domain1` (8080/8181/4848) y `dominio_rest` — **ver R3** |
| MySQL 8.4 | Servicio `MySQL84` **Running**, escuchando en 3306 |
| Base de datos | `eurekabank` cargada: 6 cuentas, 74 movimientos, 11 usuarios. `eurekabank_app`/`EurekaBank2026!` **funciona** |
| Python | 3.12 con `python-docx` 1.2.0 y `lxml` 6.1.3 (necesario para el auditor de coherencia) |
| PowerDesigner 16 | `C:\Program Files (x86)\Sybase\PowerDesigner 16` presente (32 bits) |
| `.m2` offline | `jakarta.jakartaee-api 11.0.0`, `jakarta.ws.rs-api 3.1.0`, `jakarta.json.bind-api 3.0.1`, `jakarta.json-api 2.1.3`, `yasson 3.0.4`, `parsson 1.1.7`, `mysql-connector-j 8.4.0`, `junit-jupiter 5.10.2`, `maven-war-plugin 3.4.0`, `maven-surefire-plugin 3.2.5`, `maven-dependency-plugin 3.6.1`, `maven-jar-plugin 3.4.1`, `exec-maven-plugin 3.5.1` — **todo necesario está** |

`README.md` y `CONTEXTO_PROYECTO_REST.md` están en **UTF-8 válido, sin caracteres corruptos**
(verificado: 0 caracteres de reemplazo U+FFFD). Lo que se ve mal en la consola de Windows es un
efecto de la codificación de la terminal, no un defecto de los ficheros.

### 1.5 Discrepancias entre el CONTEXTO y la realidad (hay que corregir)

**D-1 · El número de pruebas del servidor está mal en la memoria.**
`CONTEXTO_PROYECTO_REST.md:293` y `PROMPT_INICIO.md:45` dicen "**318 pruebas** del servidor y
330 de la consola". El real es **265** del servidor y **330** de la consola.
No es un recorte posterior: el diff entre `2af276d` y `aac5b97` (la "limpieza de la entrega")
**está vacío en `src/test`** — el código de pruebas es el mismo. El 318 viene de un README del
proyecto SOAP que quedó desactualizado.
→ Consecuencia práctica: el criterio de aceptación de la fase 6 ("≥ 300 pruebas") **no se
consigue solo copiando**: hay que escribir ~40-80 pruebas nuevas de la capa REST.
→ Acción: corregir el CONTEXTO y el PROMPT_INICIO a 265/330 antes de que otra sesión los lea.

**D-2 · El CONTEXTO promete recuperar 5 generadores, pero en el historial hay 42.**
`CONTEXTO §9.2` lista 10 ficheros concretos. En `2af276d` hay **42** ficheros de construcción:
- `01. UML/00. CONTRATO/auditar_coherencia.py` ← **el auditor de la fase 5**
- `01. UML/01. ERS/{generar_ers,ensamblar_ers,verificar_ers}.py`
- `01. UML/02. ECUD/{generar_ecud,ensamblar_ecud,verificar_ecud}.py`
- `01. UML/03. DIAGRAMAS/scripts/` → 18 ficheros (`gen_casos_uso.vbs`, `gen_actividad.vbs`,
  `gen_secuencia.vbs`, `gen_clases.vbs`, `exportar_png.vbs`, `verificar_apertura.vbs`,
  `generar_todo.ps1`, `registrar_modelos.ps1`, `ajustar_layout_clases.py`,
  `aplicar_layout_calles.ps1`, `inyectar_simbolos_libres.py`, `ruta_enlaces.py`,
  `volcar_rects.py`, `verificar_entregables.py`, `fichas.txt` + 12 `layout/*.txt`)
- `02. MER/scripts/` → 11 ficheros (`gen_cdm/ldm/pdm.vbs`, `exportar_png.vbs`,
  `completar_ldm.py`, `validar_ldm_pdm.py`, `validar_pdm_sql.py`, `registrar_modelos.ps1`,
  `regenerar_mer.ps1`, `verificar_modelos.vbs`)
- `03. BDD/cargar_bdd.ps1`, `04. SERVIDOR/contrato/{comparar_xsd,reordenar_xsd}.py`,
  `05. DOCUMENTACION/{generar_manual,verificar_manual}.py`, `probar_todo.ps1`

→ Acción: recuperar **los 42**, no solo los 10. Los de PowerDesigner son los que hacen el
trabajo pesado de los diagramas.

**D-3 · Las 66 imágenes PNG del SOAP fueron borradas y hay que rehacerlas para el REST.**
En `2af276d` había `05. DOCUMENTACION/img/` con 66 PNG: **39 de diagramas** (CU/DA/DS/DC/EAM),
3 del MER (MC/ML/MF), **10 capturas de la consola** (CON_*) y **10 figuras de manual**
(fig01-fig10). El commit de limpieza las borró. Son recuperables, pero:
- las 10 capturas de consola **siguen valiendo** (la consola se copia literal → misma interfaz);
- `fig10_stack_soap.png` hay que rehacer como `fig10_stack_rest.png`;
- los 39 PNG de diagramas hay que **reexportar** de los modelos nuevos.

**D-4 · No existe el `.sws` de PowerDesigner en el REST.** Hay que crearlo desde cero con
rutas relativas y registrar los 39 modelos en sus `.prj`.

---

## 2. Riesgos técnicos concretos

### R1 · El orden de validación del REST choca con los servicios copiados ← **el riesgo real**

`CONTEXTO §5.1` pone **DAT-001 antes que AUTH-003**:

```
1. DAT-001  → falta la cabecera Authorization, o no viene en formato "Bearer <token>",
              o falta un parámetro de ruta o del cuerpo
2. AUTH-003 → el token no existe en el registro de sesiones
```

Pero los servicios SOAP, que se copian **literales** (D8), validan **al revés**: primero el
token, después los parámetros.

- `ServicioCuentas.java:59-65` → `validarToken(token)` y solo después `Textos.estaVacio(cedula)`.
- `ServicioMovimientos.java:147-158` → token, luego `cuenta`, luego `monto`.
- `ServicioMovimientos.java:282-295` → token, luego origen, luego destino, luego monto.

**Si el filtro REST de token rechazara con AUTH-003/AUTH-004 antes de que el recurso mirara
los parámetros, se devolvería AUTH-003 donde el contrato exige DAT-001.** Eso es un desfase
visible entre contrato y código, y es exactamente lo que penaliza el docente.

**Solución (decidida en esta fase, se congela en el contrato):**

1. `seguridad.FiltroToken` (`@Provider`, `ContainerRequestFilter`) **solo extrae** el token:
   - cabecera ausente o mal formada → responde **DAT-001 (400)**;
   - cabecera bien formada → guarda el token en una propiedad del request y **no rechaza nada**.
   **Nunca** rechaza con AUTH-003/AUTH-004.
2. El recurso REST comprueba, **en este orden**:
   - ¿viene el token en la cabecera? → si no, DAT-001;
   - ¿vienen los parámetros de ruta y de cuerpo? → si no, DAT-001;
   - y **solo entonces** delega al servicio, que ya hace AUTH-003 → AUTH-004 → MON-001 →
     CTA-003 → CTA-001 → CTA-002 → CTA-004 → SAL-001 → MON-002, en ese orden exacto
     (verificado en `ServicioMovimientos.java:147-169, 210-236, 282-323`).

Detalle fino que hace que esto cuadre: **DAT-001 tiene un único mensaje literal**, así que
"header inválido" y "parámetro faltante" son indistinguibles para el cliente. Lo observable es
que ambos casos devuelven DAT-001, y con el diseño anterior se cumple siempre.

**Comprobación obligatoria en la fase 6:** un test que mande `POST /api/cuentas/00200002/depositos`
con cuerpo `{}` (falta `monto`) y **token inválido**, y verifique que la respuesta es
**400 / DAT-001**, no 401 / AUTH-003.

### R2 · Parámetro de ruta vacío: 404 del enrutador o DAT-001

En JAX-RS, `{cuenta}` no casa con un segmento vacío, así que `GET /api/cuentas//movimientos`
no enrutaría y devolvería un 404 del contenedor, **sin el sobre `{estado,codigo,mensaje}`**.
Eso rompería la regla "todo lo que devuelve el servicio es un sobre" y la regla de DAT-001
("falta un parámetro de ruta").

**Solución recomendada:** usar plantillas de ruta permisivas en los 9 endpoints:
`@Path("/api/cuentas/{cuenta:.*}/movimientos")` y `@Path("/api/clientes/{cedula:.*}/cuentas")`.
Así un segmento vacío **llega al recurso** con `cuenta = ""` y el servicio devuelve
exactamente **DAT-001** (ya es lo que hace: `ServicioCuentas.java:86`,
`ServicioMovimientos.java:152`). Una cuenta inexistente → CTA-001 (404). Un cliente
inexistente → CLI-001 (404). Los tres códigos ya existen y son los correctos.

Alternativa si el docente no la ve bien: dejar `{cuenta}` estricto y documentar explícitamente
en el contrato que una ruta mal formada es un 404 de enrutamiento, fuera del catálogo de
negocio. **Decisión pendiente (ver sección 4).**

### R3 · El dominio `dominio_rest` de Payara está roto

`C:\payara7\glassfish\domains\dominio_rest` existe (125 ficheros) pero su `config/domain.xml`
conserva los **placeholders sin resolver** del template:
`port="%%%HTTP_PORT%%%"`, `port="%%%ADMIN_PORT%%%"`. Ese dominio **no arranca**.
`domain1` sí es válido (8080/8181/4848) y además **no hay conflicto**: el WAR SOAP usa el
context root `/EUREKABANK_SOAP_JAVA` y el REST usará `/EUREKABANK_RESTFUL_JAVA`; los dos
conviven en el 8080.

**Recomendación:** desplegar en `domain1`. Si se quiere un dominio propio, hay que recrear
`dominio_rest` con puertos explícitos (o borrar el directorio roto y crearlo de nuevo).

### R4 · Generar los Word: no se escriben a mano, se recuperan y se adaptan

Los `.docx` del SOAP pesan 1,6 MB (ERS), 0,8 MB (ECUD) y 6,2 MB (manual). Escribirlos a mano
serían 2500+ líneas de Python. Los generadores están en `2af276d`.
El flujo del SOAP es: `generar_*.py` (contenido) → `ensamblar_*.py` (arma el .docx con
`python-docx`) → `verificar_*.py` (comprueba). Los tres de cada documento se recuperan y se
adaptan. Los índices son **campos de Word** (TOC/SEQ) y los `{{IMG:...}}` se sustituyen con
las imágenes exportadas.

### R5 · PowerDesigner: la parte más frágil del proyecto

El COM de PD 16 **solo funciona desde `C:\Windows\SysWOW64\cscript.exe`** (32 bits), nunca
desde PowerShell. Hay que cerrar PowerDesigner antes de correr los scripts y **mirar el PNG
exportado** antes de dar un diagrama por bueno. Por eso existen en el historial
`verificar_apertura.vbs`, `exportar_png.vbs`, `ajustar_layout_clases.py` y los
`layout/*.txt` (coordenadas por diagrama): el SOAP pasó horas arreglando la maquetación y
esos ficheros la tienen guardada. Copiarlos es la diferencia entre rehacer el trabajo de
layout o heredarlo.

### R6 · El criterio "≥ 300 pruebas del servidor" hay que ganarlo con tests nuevos

El baseline son 265. La fase 6 tiene que añadir **≥ 35** pruebas de la capa REST. Es
razonable: 4 clases de recurso + filtro + manejador de excepciones + mapa de códigos HTTP +
casos de borde de cabecera y cuerpo dan de sobra 60-80.

---

## 3. Plan por fases

Cada fase lleva: objetivo · qué se toca · cómo se hace · cómo se verifica · criterio de cierre.
**No se avanza a la fase siguiente sin cerrar la anterior** (regla del CONTEXTO §11 y del
PROMPT_INICIO).

---

### FASE 0 · Cimientos

**Objetivo:** que el árbol de carpetas exista y el proyecto quede versionable.

**Qué se hace**
1. Crear el árbol vacío: `01. UML\00. CONTRATO`, `01. UML\01. ERS`, `01. UML\02. ECUD`,
   `01. UML\03. DIAGRAMAS\{01_CASOS_USO,02_ACTIVIDAD,03_SECUENCIA,04_CLASES,05_ARQUITECTURA,scripts}`,
   `04. SERVIDOR\contrato`, `04. SERVIDOR\EUREKABANK_RESTFUL_JAVA`,
   `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON`, `05. DOCUMENTACION\img`.
2. Copiar de `2af276d` los **42 scripts** de construcción a su carpeta destino (con `git show`).
3. Copiar los **66 PNG** de `05. DOCUMENTACION/img/` a `05. DOCUMENTACION/img/` del REST
   (los 39 de diagramas y 3 del MER se regenerarán después; los 10 CON_* y 9 fig* se reutilizan).
4. `git init` en la raíz y `git add` de lo copiado. **Sin commit** hasta que Mateo lo pida.
5. Actualizar `README.md` (ya existe y está bien) y crear `ENTREGABLES.md` al final.

**Comando para recuperar los generadores (uno por línea, todos verificados hoy):**
```
git -C "C:\Users\mateo\Downloads\Arquit\Unidad1\BddLocal\TI2.1 SOAP_JAVA_CONBDD_EURECABANK_GR03" ls-tree -r --name-only 2af276d
```
y luego, para cada fichero:
```
git -C "<SOAP>" show "2af276d:01. UML/01. ERS/generar_ers.py" > "01. UML\01. ERS\generar_ers.py"
```
> Nota: `git show ... > fichero` en PowerShell 5.1 **reescribe el contenido como UTF-16LE** si
> la redirección es de PowerShell. Hay que usar `cmd /c` o `git show ... | Out-File -Encoding utf8`.
> Esto se comprueba en la fase 0 antes de seguir.

**Verificación**
```
Get-ChildItem -Recurse -File | Measure-Object
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -v
& "C:\payara7\bin\asadmin.bat" list-domains
```

**Criterio de cierre:** el árbol existe, los 42 scripts están en su sitio y son legibles, el
README lo describe, y `git status` funciona.

---

### FASE 1 · Contrato congelado REST + OpenAPI  ← **la fase que fija todo lo demás**

**Objetivo:** un contrato único que ERS, ECUD, diagramas, código y auditoría consultarán.
Nada de esto se toca después (salvo erratas, que se reportan).

**Qué se hace**
1. `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md`: partir de
   `CONTRATO_CONGELADO_EUREKABANK.md` (502 líneas, 25 encabezados) y **adaptar**:
   - §3 *Sobre de respuesta*: la tabla de atributos se queda igual; **cambia** la columna
     "Correspondencia con el XSD" por la equivalencia JSON (tipos JSON ↔ Java) y se añade
     la **tabla código → HTTP status** (17 filas, copiada del CONTEXTO §4).
   - §4 *Operaciones*: las 9 filas cambian a **verbo + ruta + cuerpo + respuesta**
     (`POST /api/auth/login`, `GET /api/clientes/{cedula}/cuentas`, …). El token **sale de
     la columna de parámetros** y pasa a una columna aparte "cabecera `Authorization: Bearer`".
   - §5 *Catálogo*: se añade la columna HTTP (ya existe en el SOAP, se verifica que coincide
     con el CONTEXTO §4 — **coincide**, se comprobó uno a uno).
   - §6 *Orden de validación*: se reescribe con DAT-001 agrupando los tres casos (cabecera,
     formato, parámetro) **antes** de AUTH-003, y se **añade una nota explícita de R1**
     explicando por qué la capa REST valida cabecera y parámetros antes de delegar.
   - §7 *Reglas de negocio*: **sin cambios** (RB-01..RB-14).
   - §8 *Casos de uso*: los mismos 11 CU.
   - §10 *Estructura del servidor*: la tabla de paquetes SOAP → REST
     (`controladores.ws` → `api.rest`, `seguridad` gana `FiltroToken`).
   - §13 *Estructura de la consola*: `cliente/ClienteWSEUREKABANK` → `cliente/ClienteEUREKABANK`,
     se **elimina** el paquete `ws/` (ya no hay stub), se documenta que `modelo/`, `vista/`,
     `controlador/` y `util/` son copia literal.
   - §16 *Entorno*: URL base `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`,
     variable de entorno `SERVICIO_URL` (en vez de `WS_URL`).
2. `04. SERVIDOR\contrato\eurekabank-openapi.yaml` (OpenAPI 3.0.3):
   - 9 `paths` con verbo, `operationId` = nombre de la operación SOAP, `parameters` de ruta,
     `requestBody` JSON donde toque, `responses` con **todos** los HTTP status posibles por
     operación y `content: application/json` con el esquema del sobre.
   - `components.schemas`: `RespuestaBase` + los 7 derivados + `CuentaDTO` + `MovimientoDTO`
     + los cuerpos de entrada (`AutenticacionRequest`, `MontoRequest`,
     `TransferenciaRequest`).
   - `components.securitySchemes.bearerAuth`: `type: http`, `scheme: bearer`.
   - `security: - bearerAuth: []` en las 7 operaciones protegidas; las 2 de `/api/auth` sin
     seguridad.
   - Los **17 códigos** con su **HTTP status** y su **mensaje literal** en la descripción.
3. Escribir un test de contrato (en la fase 6) que compare el YAML con el `.md` — pero la
   primera versión del comparador se hace aquí, como script, y luego se evolves a test JUnit.

**Verificación**
```
python -c "import yaml,sys; d=yaml.safe_load(open(r'04. SERVIDOR\contrato\eurekabank-openapi.yaml',encoding='utf-8')); print(len(d['paths']),'paths'); print(sorted(d['components']['schemas'].keys()))"
```
> Ojo: `pyyaml` puede no estar instalado. Si falta, se valida con el parser JSON-B/JSON-P de
> Java o se instala `pyyaml` (decidir con Mateo). **No hay red**, así que hay que comprobar
> `pip list` antes de contar con ello.

**Criterio de cierre:** el OpenAPI declara las 9 operaciones con sus cuerpos, respuestas,
códigos HTTP y el esquema `Bearer`; el `.md` y el `.yaml` dicen lo mismo; y un lector que
sólo lea el contrato puede implementar el cliente y el servidor sin preguntar nada.

---

### FASE 2 · ERS

**Objetivo:** `01. UML\01. ERS\ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx`, ≥ 60 tablas.

**Qué se hace**
1. Recuperar `generar_ers.py`, `ensamblar_ers.py`, `verificar_ers.py` de `2af276d`.
2. Adaptar el texto: cambiar **SOAP → REST** en todo lo tocable. En concreto:
   - §1.2 Alcance: ahora solo servidor + consola (sin clientes escritorio/web/móvil).
   - §2.3 Entorno de ejecución: Payara 7, JAX-RS 3.1, JSON-B 3.0, sin JAX-WS.
   - §3.1 Interfaces: HTTP + JSON + cabecera `Authorization: Bearer` (en vez de WSDL/SOAP).
   - §3.2.4 Operaciones del servicio: las 9 con verbo y ruta.
   - §3.4 Requisitos de BD y §5 Atributos de calidad: **sin cambios**.
   - §6 Reglas de negocio: RB-01..RB-14 **sin cambios**.
   - §7 Catálogo de códigos: añadir la columna HTTP.
   - §8 Casos de uso: los mismos 11.
   - §9 Matriz de trazabilidad: rehacer las filas donde cambió la tecnología.
3. Sustituir las figuras: incrustar los PNG nuevos (figuras de arquitectura REST).
4. **Cero menciones de SOAP/WSDL/XSD fuera de la comparación explícita con el proyecto SOAP**
   (criterio de aceptación del CONTEXTO §11).

**Verificación:** `python "01. UML\01. ERS\verificar_ers.py"` + abrir el `.docx` y mirar que
los índices son campos de Word (TOC), no texto escrito.

**Criterio de cierre:** `.docx` generado, ≥ 60 tablas, índices con campos, 0 menciones
residual de SOAP fuera del capítulo de comparación.

---

### FASE 3 · ECUD

**Objetivo:** `01. UML\02. ECUD\ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx`, 11 fichas de 17 filas.

**Qué se hace**
1. Recuperar `generar_ecud.py`, `ensamblar_ecud.py`, `verificar_ecud.py` de `2af276d`.
2. Adaptar: los **11 CU** y los **44 flujos alternativos** se conservan con sus **mensajes
   literales idénticos**; lo que cambia es la **entrada del token**: de "parámetro token" a
   "cabecera `Authorization: Bearer <token>`". En las fichas, la fila de "Precondiciones" /
   "Entradas" pasa a describir la cabecera, y se añade la fila de HTTP status esperado.
3. Coherencia con R1: la ficha de los flujos alternativos debe reflejar el orden real
   (DAT-001 antes que AUTH-003).

**Verificación:** `python "01. UML\02. ECUD\verificar_ecud.py"` + contrastar los 17 mensajes
literales contra el catálogo del contrato con un script (no a ojo).

**Criterio de cierre:** 11 fichas × 17 filas, 44 flujos alternativos, mensajes idénticos al
catálogo, token en cabecera.

---

### FASE 4 · Diagramas PowerDesigner (39 modelos)

**Objetivo:** 12 CU + 12 DA + 12 DS + 2 DC + 1 EAM, con sus PNG.

**Qué se hace** (usando los scripts recuperados de `2af276d`)

| Familia | Acción | Nota |
|---|---|---|
| `01_CASOS_USO` | **copiar tal cual** los 12 `.oom` del SOAP | un caso de uso no menciona SOAP |
| `02_ACTIVIDAD` | copiar y **cambiar las etiquetas** "servicio web SOAP" → "servicio web REST" | 12 ficheros de `layout/*.txt` a revisar |
| `03_SECUENCIA` | **rehacer** los 12 | cambian los *lifelines* (recurso REST en vez de fachada SOAP) y los mensajes (petición HTTP + JSON en vez de sobre SOAP) |
| `04_CLASES` | **rehacer** los 2 | paquetes nuevos (`api.rest`), anotaciones JAX-RS, sin JAXB |
| `05_ARQUITECTURA` | **rehacer** el `.eam` | nodo REST con sus endpoints; el enlace SOAP → HTTP/JSON |
| `02. MER` | **copiar tal cual** (ya está) | el modelo de datos no depende de la tecnología |

1. Copiar los 12 CU y los 12 DA; editar las etiquetas con `gen_actividad.vbs` + los `layout/*.txt`.
2. Rehacer los 12 DS con `gen_secuencia.vbs`: lifeline = Cliente → [api.rest] → `seguridad.FiltroToken`
   → `servicios.*` → `datos.*` → MySQL; mensaje = `POST /api/auth/login` + `{usuario, clave}` JSON.
3. Rehacer los 2 DC con `gen_clases.vbs`: `DC_Servidor` con los paquetes `api.rest`, `seguridad`,
   `servicios`, `modelos`, `datos`, `utilidades`; `DC_Cliente_Consola` con `cliente`, `controlador`,
   `modelo`, `vista`, `util`.
4. Rehacer el `.eam` con `gen_arquitectura.ps1`.
5. Registrar los 39 modelos en sus `.prj` y en `RESTFUL_JAVA_CONBDD_GR03.sws` con **rutas relativas**.
6. Exportar los PNG con `exportar_png.vbs` (SysWOW64 `cscript`).
7. **Mirar cada PNG** antes de darlo por bueno (regla 10 del CONTEXTO §13).

**Verificación**
```
& "C:\Windows\SysWOW64\cscript.exe" //nologo "01. UML\03. DIAGRAMAS\scripts\verificar_apertura.vbs"
python "01. UML\03. DIAGRAMAS\scripts\verificar_entregables.py"
```

**Criterio de cierre:** 39 modelos + 39 PNG, todos abren en PowerDesigner, todas las rutas del
`.sws` relativas, etiquetas sin "SOAP" (salvo el capítulo de comparación).

---

### FASE 5 · Auditoría de coherencia

**Objetivo:** `01. UML\00. CONTRATO\AUDITORIA_COHERENCIA.md` con las 9 comprobaciones en OK.

**Qué se hace**
1. Recuperar `auditar_coherencia.py` de `2af276d` (ya sabe cruzar código ↔ contrato ↔ ERS ↔
   ECUD ↔ manual ↔ diagramas ↔ BDD; hoy hace 9 comprobaciones C1..C9).
2. Adaptarlo: el WSDL y el XSD se sustituyen por el **OpenAPI YAML** (C5 pasa de "el XSD y el
   WSDL siguen compilando" a "el OpenAPI es válido y declara las 9 operaciones"), y las rutas
   de los documentos cambian de nombre.
3. Ejecutarlo en modo auditoría (sin correcciones) y **revisar a mano** cada línea.

**Verificación:** `python "01. UML\00. CONTRATO\auditar_coherencia.py" --sin-correcciones` → 9/9 OK.

**Criterio de cierre:** `AUDITORIA_COHERENCIA.md` generado con 9/9 comprobaciones OK y 0
diferencias pendientes.

---

### FASE 6 · Servidor REST

**Objetivo:** `04. SERVIDOR\EUREKABANK_RESTFUL_JAVA` — WAR que compila, ≥ 300 pruebas verdes,
verificado de extremo a extremo contra MySQL real.

**Qué se copia (literal) y qué se rehace**

| Paquete | Acción | Ficheros |
|---|---|---|
| `modelos` | **copiar** | 11 POJOs (sin anotaciones) + `CodigoRespuesta` |
| `datos` | **copiar** | 21 ficheros (9 DAO + 9 interfaces + `ProveedorConexion` + `UnidadTrabajo`) |
| `servicios` | **copiar** | `FabricaRespuestas`, `ServicioCuentas`, `ServicioMovimientos`, `ServicioCostos` |
| `utilidades` | **copiar** | `Codigos`, `FormatoDinero`, `HashClave`, `Textos` |
| `seguridad` | **copiar + añadir** | `TokenServicio`, `ServicioAutenticacion` literales; **nuevo** `FiltroToken` |
| `api.rest` | **rehacer** | `AuthResource`, `ClienteResource`, `CuentaResource`, `TransferenciaResource`, `ManejadorExcepcion` (+ el mapa de códigos HTTP) |
| `controladores.ws` | **eliminar** | `WSEUREKABANK` y `WSEUREKABANKImpl` no aplican |

**El `pom.xml` (WAR)**
- `packaging`: `war`; `finalName`: `EUREKABANK_RESTFUL_JAVA`.
- `jakarta.platform:jakarta.jakartaee-api:11.0.0` (**provided**) → trae JAX-RS 3.1 y JSON-B.
- `com.mysql:mysql-connector-j:8.4.0` (**runtime**).
- `org.junit.jupiter:junit-jupiter:5.10.2` (**test**).
- **Fuera** (no están en `.m2`): `jakarta.xml.ws-api`, `jakarta.jws-api`, `jaxws-rt`,
  `com.fasterxml.jackson.*`, `jersey-media-json-jackson`.
- `maven.compiler.release=17`, `maven-war-plugin 3.4.0`, `maven-surefire-plugin 3.2.5`.
- `failOnMissingWebXml` → **false**: en JAX-RS el `web.xml` es opcional y el 8080 del contexto
  lo pone Payara. Hay que decidirlo (ver sección 4).

**El `webapp/WEB-INF/`**
- `web.xml`: solo el `welcome-file` (index.html) y, si se quiere, el filtro CORS.
- `beans.xml`: se conserva (`bean-discovery-mode="all"`).
- `sun-jaxws.xml`: **se elimina**.
- `index.html`: se adapta al contexto REST.

**Los 5 recursos REST** (mapeo exacto de las 9 operaciones)

| Clase | `@Path` | Métodos |
|---|---|---|
| `AuthResource` | `/api/auth` | `POST /login`, `POST /logout` |
| `ClienteResource` | `/api/clientes` | `GET /{cedula}/cuentas` |
| `CuentaResource` | `/api/cuentas` | `GET /{cuenta}`, `GET /{cuenta}/movimientos`, `POST /{cuenta}/depositos`, `POST /{cuenta}/retiros`, `GET /{cuenta}/costos` |
| `TransferenciaResource` | `/api/transferencias` | `POST /` |

Todas devuelven `Response` con el **status HTTP derivado del `codigo` del sobre** y el sobre
como entidad JSON (`produces = "application/json; charset=utf-8"`).
Todos aceptan `consumes = "application/json"` donde hay cuerpo.

**Pruebas (≥ 300)**
- Copiar los 24 ficheros de test del SOAP y adaptarlos (los de `datos`, `servicios`,
  `seguridad`, `utilidades` y `modelos` **se copian tal cual**: son 265 tests que no tocan
  la capa de entrada).
- Añadir los nuevos: `FiltroTokenTest` (cabecera ausente, mal formada, válida),
  `MapeoCodigoHttpTest` (los 17 códigos → su status), `AuthResourceTest`,
  `ClienteResourceTest`, `CuentaResourceTest`, `TransferenciaResourceTest` (los 17 códigos
  alcanzables por operación), `ManejadorExcepcionTest` (JSON mal formado → DAT-001,
  excepción inesperada → ERR-001 500), y `RestApiTest` de extremo a extremo con
  `HttpClient` real contra el WAR desplegado (como el `ExtremoExtremoTest` del SOAP, que usa
  `jaxws-rt`; aquí se usa `HttpClient` + el WAR en un `Payara` local o contra el desplegado).
- **El test de R1** (parámetro faltante + token inválido → DAT-001 y no AUTH-003) es
  obligatorio.

**Verificación**
```
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -B -o clean package
& "C:\payara7\bin\asadmin.bat" start-domain domain1
& "C:\payara7\bin\asadmin.bat" deploy --name EUREKABANK_RESTFUL_JAVA --contextroot EUREKABANK_RESTFUL_JAVA "04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\target\EUREKABANK_RESTFUL_JAVA.war"
curl.exe -i -X POST http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api/auth/login -H "Content-Type: application/json" -d "{\"usuario\":\"cromero\",\"clave\":\"<SHA256 de Cromero2026>\"}"
```
Y después, con el token devuelto:
```
curl.exe -i http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api/clientes/06914897/cuentas -H "Authorization: Bearer <token>"
curl.exe -i -X POST http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api/cuentas/00200002/depositos -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"monto\":100.00}"
```
Y un caso de error, para ver HTTP status + código a la vez:
```
curl.exe -i http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api/cuentas/00300001 -H "Authorization: Bearer <token>"
```
> debe salir **409 / CTA-002 / "La cuenta no se encuentra en estado ACTIVO"**.

**Criterio de cierre:** `mvn clean package` verde con ≥ 300 tests, WAR desplegado, y las
llamadas `curl` de arriba con la salida real pegada.

---

### FASE 7 · Consola

**Objetivo:** `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON` — JAR que compila **con el servidor
apagado**, ≥ 300 tests, flujo real contra el servidor desplegado.

**Qué se copia y qué se rehace**

| Paquete | Acción |
|---|---|
| `modelo` (12 clases) | **copiar literal** — los mismos POJOs, mapeados por JSON-B sin anotaciones |
| `vista` (5), `controlador` (5), `util` (5), `Main` | **copiar literal** |
| `cliente` | **rehacer**: `IClienteEUREKABANK` (idéntica) + `ClienteEUREKABANK` (nuevo) |
| `ws` | **eliminar** (ya no hay stub que escribir a mano) |

**Por qué se puede copiar `controlador/` y `vista/` tal cual:** si `IClienteEUREKABANK` mantiene
las **9 firmas idénticas** a las del SOAP, los controladores no cambian ni una línea. El
cliente REST devuelve el mismo objeto `Respuesta*`, y el `codigo`/`mensaje` del sobre sigue
conduciendo la lógica de la consola (el `ControladorProtegido` del SOAP ya descarta la sesión
ante `AUTH-003`/`AUTH-004`).

**`ClienteEUREKABANK` (lo único nuevo)**
- `java.net.http.HttpClient` del JDK, **sin framework** (D9).
- JSON-B (Yasson) para mapear POJOs: `jakarta.json.bind-api 3.0.1` + `jakarta.json-api 2.1.3` +
  `yasson 3.0.4` + `parsson 1.1.7` (los cuatro están en `.m2`; **sin Jackson**).
- **El token va en la cabecera `Authorization: Bearer <token>`**, nunca en la URL.
- Lee **los dos**: el **status HTTP** (para decidir si es un fallo de transporte → `SER-001`
  vía `ExcepcionServicio`) y el **`codigo`** del sobre (para el mensaje de negocio).
- Timeouts: 5 s de conexión, 30 s de respuesta (mismo criterio que el SOAP).
- Resolución de la URL base: `arg0` → `-Dservicio.url` → env `SERVICIO_URL` → defecto
  `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`.
- **Compila con el servidor apagado**: no descarga nada; el OpenAPI se empaqueta en
  `src/main/resources/contrato/eurekabank-openapi.yaml` como referencia y para un test que
  verifique que el cliente y el contrato coinciden.

**Pruebas (≥ 300)**
- Copiar los 21 ficheros de test del SOAP (330 tests) y **adaptar los del cliente**:
  `ClienteWSEUREKABANKTest` (16) y `ClienteWSEUREKABANKBordeTest` (11) pasan de un stub
  JAX-WS a un servidor HTTP de prueba (el JDK trae `com.sun.net.httpserver.HttpServer`, que
  sirve para simular 200/401/404/409/503 y cuerpo mal formado **sin dependencias nuevas**).
- `ControladorProtegidoTest` y el resto no cambian.
- Añadir: `ClienteEUREKABANKTest` (cabecera `Authorization` presente, URL sin token,
  timeout, respuesta no-JSON, status 503 → `ExcepcionServicio`).

**Verificación**
```
# Con el servidor APAGADO: debe compilar y las pruebas deben pasar
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -B -o clean package
# Con el servidor ENCENDIDO: flujo real
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -B -o exec:java
```
**Criterio de cierre:** verde con el servidor apagado (≥ 300 tests) y flujo real de consola
contra el WAR desplegado, con la salida pegada.

---

### FASE 8 · Documentación y entrega

**Objetivo:** manual, README final, `ENTREGABLES.md`.

**Qué se hace**
1. Recuperar `generar_manual.py`, `verificar_manual.py`, `probar_todo.ps1` de `2af276d`.
2. Adaptar el manual (11 capítulos):
   - cap. 4 "El servidor SOAP" → "El servidor REST": despliegue en Payara, el WAR, las 9
     rutas, el `Authorization: Bearer`, la tabla código ↔ HTTP.
   - cap. 6 "El cliente de consola": la URL base, el nuevo cliente HTTP, las 10 pantallas
     (las capturas CON_* se reutilizan: la interfaz no cambia).
   - cap. 7 "Códigos de respuesta": **añadir la columna HTTP**.
   - cap. 8 "Pruebas": cifras reales (≥ 300 / ≥ 300) y el informe de `mvn test`.
   - cap. 9 "Diagramas": los 39 modelos.
   - cap. 10 "Errores frecuentes": los del SOAP + los nuevos de REST (token en la URL,
     401 vs 403, JSON mal formado).
   - cap. 11 Anexo: el **OpenAPI** y el diccionario de las 17 tablas.
   - `fig10_stack_soap.png` → `fig10_stack_rest.png`.
3. `README.md` final: qué es, cómo se compila, cómo se despliega, cómo se ejecuta, la
   estructura de carpetas, el contrato, las credenciales de prueba y el mapa de la BDD.
4. `ENTREGABLES.md`: inventario de todo lo entregado (adaptando el del SOAP).

**Verificación**
```
python "05. DOCUMENTACION\verificar_manual.py"
& ".\05. DOCUMENTACION\probar_todo.ps1"
```

**Criterio de cierre:** manual con figuras incrustadas e índices con campos de Word, README
describiendo el proyecto, `ENTREGABLES.md` con el inventario completo.

---

## 4. Decisiones que necesito de Mateo

Las del CONTEXTO §14 (siguen abiertas) y las que ha destapado este análisis:

| # | Decisión | Mi recomendación |
|---|---|---|
| 1 | **Nombres de los integrantes** del grupo GR03 para la portada del ERS, el ECUD y el manual | — (necesario, no puedo inventarlo) |
| 2 | **Nombre del repositorio** en GitHub | `TI2.1-RESTFUL_JAVA_CONBDD_EURECABANK_GR03` |
| 3 | **Clave de `root`** de MySQL 3306: ¿se pide o se usa la que ya hay? | se usa la que ya hay; `eurekabank_app` ya funciona |
| 4 | **Dominio de despliegue**: `dominio_rest` está roto (placeholders `%%%HTTP_PORT%%%`) | **`domain1`** (8080). Los dos WAR conviven por context root |
| 5 | **`failOnMissingWebXml`**: en JAX-RS el `web.xml` es opcional | `false` + `web.xml` mínimo, o quitarlo |
| 6 | **Parámetro de ruta vacío**: ¿404 de enrutamiento o DAT-001? (R2) | **plantillas permisivas `{cuenta:.*}`** → DAT-001, y así todo devuelve el sobre |
| 7 | **¿Dónde vive el mapa código → HTTP?** `servicios` y `modelos` se copian literales (D8), así que no puede ser allí | una clase nueva en `api.rest` (misma lista de paquetes, una clase más) |
| 8 | **`pyyaml` disponible para validar el OpenAPI** | comprobar `pip list`; si no está, valida con un test JUnit en vez de Python |
| 9 | **Cifra real de tests**: corregir el CONTEXTO (dice 318; son 265) | corregir a 265/330 y ajustar el criterio de la fase 6 a "≥ 300 = 265 copiados + ≥ 35 nuevos" |
| 10 | **`git init` + primer commit** | `git init` en la fase 0; el primer commit, con tu permiso |

---

## 5. Trazabilidad CONTEXTO §11 → fases de este plan

| Fase del CONTEXTO | Entregable | Fase de este plan | Verificación principal |
|---|---|---|---|
| 0 Cimientos | árbol, `.gitignore`, `README.md`, `git init` | FASE 0 | árbol + 42 scripts recuperados |
| 1 Contrato | `CONTRATO_CONGELADO_REST.md` + `eurekabank-openapi.yaml` | FASE 1 | 9 operaciones en el YAML, coinciden con el `.md` |
| 2 ERS | `ERS_...docx` (≥ 60 tablas) | FASE 2 | `verificar_ers.py` + 0 menciones de SOAP |
| 3 ECUD | `ECUD_...docx` (11 fichas × 17 filas) | FASE 3 | `verificar_ecud.py` + 44 flujos, mensajes idénticos |
| 4 Diagramas | 12 CU + 12 DA + 12 DS + 2 DC + 1 EAM | FASE 4 | `verificar_apertura.vbs` + mirar los PNG |
| 5 Auditoría | `AUDITORIA_COHERENCIA.md` | FASE 5 | 9/9 OK |
| 6 Servidor | WAR, ≥ 300 pruebas, e2e real | FASE 6 | `mvn package` + `curl` con salida real |
| 7 Consola | JAR, ≥ 300 pruebas con el servidor apagado | FASE 7 | `mvn package` + flujo real |
| 8 Documentación | manual, `README.md`, `ENTREGABLES.md` | FASE 8 | `verificar_manual.py` + `probar_todo.ps1` |

---

## 6. Qué NO se hace (y por qué)

- **No se toca `02. MER/` ni `03. BDD/`**: son copia verificada del SOAP y el modelo de datos
  es independiente de la tecnología. Solo se les añade, si hace falta, `02. MER/scripts/`.
- **No se modifica `modelos`, `datos`, `servicios`, `utilidades`**: son copia literal (D8) y
  concentran las 265 pruebas que ya pasan.
- **No se usa Jackson**: no está en `.m2` y no hay red.
- **No se usa `jaxws-rt` ni `jakarta.xml.ws-api`**: no aplican a JAX-RS.
- **No se escriben a mano los `.docx` ni los 39 diagramas**: están en el historial git.
- **No se hace commit ni push** hasta que lo pidas.
- **No se codifica antes de cerrar la fase 1**: es el error que penaliza, y el CONTEXTO lo dice
  dos veces.

---

## 7. Cómo se retoma

Todo lo que hace falta está en este documento y en `CONTEXTO_PROYECTO_REST.md`. Si otra sesión
de opencode abre esta carpeta:

1. Leer este plan y el CONTEXTO.
2. Comprobar en qué fase se está (`git log`, y el estado de los entregables de cada fase).
3. Retomar la fase en curso con su criterio de cierre.

La pizarra del equipo (`.estacion/`) se usa para repartir las fases entre subagentes: las
fases 2, 3 y 8 (documentos) y la fase 4 (diagramas) son independientes entre sí y se pueden
paralelizar; las fases 6 y 7 son secuenciales por diseño (el servidor antes que la consola).
