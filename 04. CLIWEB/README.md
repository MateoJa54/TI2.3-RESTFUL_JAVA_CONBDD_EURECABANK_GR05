# CLIWEB - Cliente web para EURECABANK

Tu parte del proyecto. No borres ni modifiques nada fuera de esta carpeta, salvo que el responsable lo pida.

## Qué tienes que construir

Una aplicación web en Java que consuma el servicio REST de EURECABANK. Debe ofrecer las nueve operaciones del
contrato: autenticarse, cerrar sesión, listar las cuentas de un cliente, consultar una cuenta, ver sus
movimientos, depositar, retirar, transferir y consultar los costos de mantenimiento. Las páginas que
muestran datos de cuentas deben estar protegidas: sin sesión válida no se muestra nada.

## Qué consumir

Las nueve rutas están bajo la base `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`, con JSON en UTF-8.
Todas menos el login exigen la cabecera `Authorization: Bearer <token>`. El token lo devuelve el login en el
campo `token` y nunca va en la URL ni en el cuerpo.

Las fuentes de verdad son el contrato congelado `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md` y el
contrato OpenAPI `04. SERVIDOR\contrato\eurekabank-openapi.yaml`. Si algo no coincide, manda el contrato.

## CORS: requisito extra del navegador

El navegador aplica CORS y la consola no. La variable `EUREKABANK_ORIGENES_CORS` del servidor define los
orígenes permitidos, separados por comas. Si tu web corre en `http://localhost:4200` u otro puerto, ese origen
debe estar en la lista. Si necesitas algo que el filtro no hace, avisa al responsable para cambiarlo en el
servidor, no lo parches desde el cliente. El valor por defecto está pendiente de decisión (ver ENTREGABLES.md).

## Reglas que no se negocian

- Los importes se manejan con `BigDecimal` a 2 decimales y redondeo `HALF_UP`, nunca con `double`.
- La clave viaja como SHA-256 en hexadecimal.
- La web muestra el `codigo` y el `mensaje` que devuelve el servidor, sin reescribirlos.
- No guardes el token en lugares visibles para otros usuarios del navegador.

## Referencia

La consola `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\` ya está terminada y probada. Úsala como modelo para los
modelos, el manejo de sesión y el tratamiento de errores. Lo que cambia es la capa de presentación.

## Cómo se prueba

Pruebas unitarias con JUnit 5 y un cliente falso, sin red. Para la prueba manual, despliega el servidor en
Payara y comprueba desde el navegador que el origen de tu web recibe respuesta.

## Entregables de tu parte

- Carpeta `04. CLIWEB\` con el proyecto Maven (empaquetado WAR), el código, las páginas y las pruebas.
- El diagrama de clases de tu cliente, en `01. UML\03. DIAGRAMAS\04_CLASES\`, registrado en el proyecto
  `04_CLASES.prj` y en el workspace `RESTFUL_JAVA_CONBDD_GR03.sws`.
- Avisa al responsable para que actualice el diagrama de arquitectura con tu componente.
