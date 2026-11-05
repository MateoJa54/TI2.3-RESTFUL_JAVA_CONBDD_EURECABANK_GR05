# PROMPT DE ARRANQUE — TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03

Copia el bloque de abajo en un chat nuevo de **opencode abierto en esta carpeta**
(`C:\Users\mateo\Downloads\Arquit\Unidad1\BddLocal\TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03`).

---

## Bloque para copiar y pegar

```
Trabaja en el proyecto de esta carpeta: TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03.

Es la variante RESTful del proyecto SOAP de EURECABANK que ya está terminado y verificado.
Antes de escribir código, lee estos tres ficheros, completos:

1. CONTEXTO_PROYECTO_REST.md          <-- la memoria del proyecto. LÉELO ENTERO, es la fuente de verdad.
2. 01. UML/00. CONTRATO/              (vacío: aquí va el contrato congelado REST que debes escribir)
3. PROMPT_INICIO.md                   (este mismo fichero)

Y después, como referencia directa del trabajo ya hecho, lee el proyecto SOAP:
   C:\Users\mateo\Downloads\Arquit\Unidad1\BddLocal\TI2.1 SOAP_JAVA_CONBDD_EURECABANK_GR03
   - su contrato:  01. UML/00. CONTRATO/CONTRATO_CONGELADO_EUREKABANK.md
   - su servidor:   04. SERVIDOR/EUREKABANK_SOAP_JAVA/src/main/java/ec/edu/monster/
   - su consola:    04. CLICON/EUREKABANK_SOAP_JAVA_CON/src/main/java/ec/edu/monster/consola/
   - su ERS/ECUD:   01. UML/01. ERS/ y 01. UML/02. ECUD/
   - sus diagramas: 01. UML/03. DIAGRAMAS/

El CONTEXTO ya contiene las 12 decisiones congeladas, las 9 operaciones con su verbo y su ruta,
los 17 códigos de respuesta con su HTTP status y su mensaje literal, el orden de validación,
las 14 reglas de negocio, la estructura de paquetes, las dependencias disponibles offline y el
procedimiento de cada fase. NO inventes nada que no esté ahí: si algo falta, pregúntamelo.

Reglas de trabajo:
- Trabaja por fases en el orden del CONTEXTO (sección 11) y NO empieces a codificar antes de
  cerrar el contrato y los documentos. Es el error que penaliza.
- Actúa como orquestador: reparte el trabajo entre subagentes especializados cuando haya partes
  independientes, y revisa tú mismo lo que devuelven antes de darlo por bueno.
- Copia y adapta, no inventes: el servidor SOAP y la consola SOAP ya resuelven el 80% del
  negocio (modelos, DAO, transacciones, seguridad, vistas). Lo único que cambia es la capa de
  entrada REST.
- Recupera los generadores de los documentos Word y los scripts de PowerDesigner desde el
  historial git del proyecto SOAP (commit 2af276d) en vez de escribir 2500 líneas desde cero.
  El CONTEXTO (sección 9.2) tiene los comandos exactos.
- VERIFICA TODO ejecutándolo. Nada de "debería funcionar": pega la salida real del comando.
  El proyecto SOAP tiene 265 pruebas del servidor y 330 de la consola; este debe llegar igual.
- El servidor REST se prueba con MySQL (esquema eurekabank, el mismo del proyecto SOAP:
  03. BDD/cargar_bdd.ps1) y con un cliente HTTP real (curl o el propio cliente de consola).
- No hagas commit ni push salvo que te lo pida. Al final, entrégame un informe con lo hecho,
  los ficheros tocados y la evidencia.

Empieza por la fase 0 (ciimientos) y la fase 1 (contrato), y avísame cuando los tengas.
```

---

## Qué contiene este fichero (para que no tengas que leerlo dos veces)

### Las 3 reglas que evitan los errores más caros

1. **No se codifica antes de cerrar el contrato.** El error típico de estos trabajos es
   documentar con nombres de operación que después cambian, y que el desfase entre ERS, ECUD,
   diagramas y código es justo lo que el docente penaliza.
2. **Copiar y adaptar, no inventar.** El proyecto SOAP ya resolvió el negocio entero con 265 + 330
   pruebas en verde. Reutilizarlo baja el riesgo a cero en la parte difícil (transacciones,
   concurrencia, dinero, seguridad) y deja el esfuerzo para lo que de verdad es nuevo: la
   capa REST.
3. **Verificar ejecutando.** Cada criterio de aceptación del CONTEXTO se comprueba con un
   comando real y se pega su salida. Un diagrama se da por bueno solo después de mirar su PNG.

### Lo que cambia y lo que no (resumen del CONTEXTO §12)

| Se copia literal | Se adapta | Se rehace |
|---|---|---|
| Las capas `modelos`, `datos`, `servicios`, `utilidades` del servidor | La capa `seguridad` (el token pasa a cabecera `Authorization`) | La capa `api.rest` (JAX-RS) |
| La consola: `modelo`, `vista`, `controlador`, `util` | El `pom.xml` (JSON-B en vez de JAX-WS) | El `cliente/ClienteEUREKABANK` (HTTP en vez de SOAP) |
| `02. MER` completo (17 tablas) | Los 12 diagramas de casos de uso y los 12 de actividad | Los 12 de secuencia, los 2 de clases y el de arquitectura |
| `03. BDD` completo (misma base de datos) | El ERS y el ECUD (los mensajes y los códigos no cambian; la tecnología sí) | El contrato OpenAPI |
| Las 11 fichas del ECUD y sus 44 flujos alternativos | | |

### Las 3 trampas específicas de esta migración

1. **El token en la URL.** En SOAP iba como último parámetro. En REST va en la cabecera
   `Authorization: Bearer <token>`. Si lo dejas en la query string, se queda en el log del
   servidor: es un fallo de diseño, no un detalle.
2. **El HTTP status y el código de negocio son dos cosas.** El sobre sigue siendo
   `{estado, codigo, mensaje}` con los 17 códigos y sus mensajes literales, y **además** ahora
   hay un status HTTP real (401, 409, 404…). El cliente lee los dos: el status para la lógica
   HTTP y el `codigo` para el mensaje de negocio.
3. **El orden de validación no se toca.** Es el mismo del SOAP, paso a paso. Los flujos
   alternativos del ECUD dependen de él: si dos reglas fallan a la vez, gana la primera de la
   lista.

### El stack, y por qué

- **Servidor**: WAR de Jakarta EE 10 sobre Payara 7. JAX-RS 3.1 y JSON-B 3.0 los da el
  contenedor: no hace falta ninguna dependencia extra aparte de `mysql-connector-j`.
- **Cliente de consola**: `java.net.http.HttpClient` (del JDK, sin framework) + JSON-B (Yasson)
  para mapear los POJOs. **Verificado**: Yasson mapea los POJOs planos sin anotaciones y usa
  como nombre de propiedad el del getter, que ya coincide con el contrato.
- **No uses Jackson**: no está en el repositorio Maven local y no hay red.
- **PowerDesigner**: el COM de PD 16 solo funciona desde el `cscript` de 32 bits
  (`C:\Windows\SysWOW64\cscript.exe`), nunca desde PowerShell. Cierra PowerDesigner antes de
  correr los scripts y mira el PNG exportado antes de dar el diagrama por bueno.
- **Maven** no está en el PATH: `"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"`.
  Desde NetBeans funciona solo.

### Comprobación de que el punto de partida está bien

Antes de escribir nada, verifica que el entorno responde:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -v
& "C:\payara7\bin\asadmin.bat" list-domains
& ".\03. BDD\cargar_bdd.ps1"
```

Si la última pide tu clave de `root`, es lo normal: el script la pide de forma segura y no la
deja en el historial.

### Si te falta algo

Las tres únicas cosas que el CONTEXTO no puede resolver solo (§14) son:

1. Los **nombres de los integrantes** del grupo para la portada de los documentos.
2. El **nombre del repositorio** en GitHub.
3. Si la **clave de `root`** de MySQL hay que solicitarla o si usas la que ya tiene.

El resto está en el CONTEXTO. Si algo no está, **pregunta antes de inventar**.
