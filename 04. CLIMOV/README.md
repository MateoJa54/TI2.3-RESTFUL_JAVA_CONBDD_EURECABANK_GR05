# CLIMOV - Cliente móvil Android para EURECABANK

Tu parte del proyecto. No borres ni modifiques nada fuera de esta carpeta, salvo que el responsable lo pida.

## Qué tienes que construir

Una aplicación Android que consuma el servicio REST de EURECABANK. Debe ofrecer las nueve operaciones del
contrato: autenticarse, cerrar sesión, listar las cuentas de un cliente, consultar una cuenta, ver sus
movimientos, depositar, retirar, transferir y consultar los costos de mantenimiento.

## Qué consumir

Las nueve rutas están bajo la base `http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api`, con JSON en UTF-8.
Todas menos el login exigen la cabecera `Authorization: Bearer <token>`. El token lo devuelve el login en el
campo `token`, y tu app debe enviarlo en cada petición protegida.

Desde el emulador de Android, `localhost` es el propio emulador. La máquina anfitriona se alcanza con
`10.0.2.2`, así que la base es `http://10.0.2.2:8080/EUREKABANK_RESTFUL_JAVA/api`. En un teléfono físico usa
la IP de tu PC en la red local. Android bloquea el tráfico HTTP en claro por defecto: revisa la configuración
de red de la app para permitirlo solo hacia ese host.

Las fuentes de verdad son el contrato congelado `01. UML\00. CONTRATO\CONTRATO_CONGELADO_REST.md` y el
contrato OpenAPI `04. SERVIDOR\contrato\eurekabank-openapi.yaml`. Si algo no coincide, manda el contrato.

## Reglas que no se negocian

- Los importes se manejan con `BigDecimal` a 2 decimales y redondeo `HALF_UP`, nunca con `double`.
- La clave viaja como SHA-256 en hexadecimal.
- La app muestra el `codigo` y el `mensaje` que devuelve el servidor, sin reescribirlos.
- CORS no aplica en Android, porque no es un navegador.

## Referencia

La consola `04. CLICON\EUREKABANK_RESTFUL_JAVA_CON\` ya está terminada y probada. Úsala como modelo para los
modelos, los nombres de las operaciones y el tratamiento de errores.

## Cómo se prueba

Pruebas unitarias con JUnit para la capa de datos y el armado de peticiones, sin red. Para la prueba manual,
arranca el servidor en Payara y ejecuta el flujo completo desde el emulador contra `10.0.2.2`.

## Entregables de tu parte

- Carpeta `04. CLIMOV\` con el proyecto Gradle, el código y las pruebas.
- El diagrama de clases de tu cliente, en `01. UML\03. DIAGRAMAS\04_CLASES\`, registrado en el proyecto
  `04_CLASES.prj` y en el workspace `RESTFUL_JAVA_CONBDD_GR03.sws`.
- Avisa al responsable para que actualice el diagrama de arquitectura con tu componente.
