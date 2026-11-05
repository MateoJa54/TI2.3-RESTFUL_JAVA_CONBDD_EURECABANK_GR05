# CLIESC - Cliente de escritorio para EURECABANK

Tu parte del proyecto. No borres ni modifiques nada fuera de esta carpeta, salvo que el responsable lo pida.

## Qué tienes que construir

Una aplicación de escritorio en Java que consuma el servicio REST de EURECABANK. Debe ofrecer las nueve
operaciones del contrato: autenticarse, cerrar sesión, listar las cuentas de un cliente, consultar una
cuenta, ver sus movimientos, depositar, retirar, transferir y consultar los costos de mantenimiento.

## Qué consumir

Las nueve rutas están bajo la base `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`, con JSON en UTF-8.
Todas menos el login exigen la cabecera `Authorization: Bearer <token>`. El token lo devuelve el login en
el campo `token` y nunca va en la URL ni en el cuerpo.

Las fuentes de verdad son el contrato congelado `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md` y el
contrato OpenAPI `04. SERVIDOR\contrato\eurekabank-openapi.yaml`. Si algo no coincide, manda el contrato.
No inventes nombres de operaciones, de campos ni de códigos de respuesta.

## Reglas que no se negocian

- Los importes se manejan con `BigDecimal` a 2 decimales y redondeo `HALF_UP`, nunca con `double`.
- La clave viaja como SHA-256 en hexadecimal. El servidor acepta también la clave en claro, pero el cliente
  envía el hash.
- El cliente muestra el `codigo` y el `mensaje` que devuelve el servidor, sin reescribirlos. Usa también el
  HTTP status para la lógica.
- Los modelos son clases planas con atributos, constructores, getters y setters.

## Referencia

La consola `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\` ya está terminada y probada. Úsala como modelo para el
manejo de sesión, los modelos y el tratamiento de errores. Lo que cambia es la vista: ventanas Swing en lugar
de la consola de texto.

## Cómo se prueba

Pruebas unitarias con JUnit 5 y un cliente falso, sin red. Para la prueba manual, arranca el servidor en
Payara y comprueba que la ruta de login responde.

## Entregables de tu parte

- Carpeta `04. CLIESC\` con el proyecto Maven, el código y las pruebas.
- El diagrama de clases de tu cliente, en `01. UML\03. DIAGRAMAS\04_CLASES\`, registrado en el proyecto
  `04_CLASES.prj` y en el workspace `RESTFUL_JAVA_CONBDD_GR03.sws`.
- Avisa al responsable para que actualice el diagrama de arquitectura con tu componente.
