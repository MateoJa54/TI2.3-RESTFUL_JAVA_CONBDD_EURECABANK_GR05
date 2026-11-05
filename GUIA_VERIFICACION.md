# Guía para comprobar todo el proyecto paso a paso

Todos los comandos son de una sola línea. Los de Maven se ejecutan en PowerShell. Cambia `$R` por la ruta de tu carpeta si la moviste.

## 0. Preparación (una vez)

1. Inicia MySQL: abre PowerShell como administrador y ejecuta `net start MySQL84`.
2. Comprueba que escucha: `netstat -ano | findstr :3306` debe mostrar LISTENING.
3. Variables de uso común (PowerShell):

```bash
$R = "C:\Users\mateo\Downloads\Arquit\Unidad1\BddLocal\TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03"; $MVN = "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
```

Nota: las pruebas con dinero cobran 2.00 por movimiento desde el movimiento 16 de una cuenta (regla RB-09), así que un depósito y un retiro iguales nunca dejan el saldo intacto. Para volver a los datos originales ejecuta `& "$R\03. BDD\cargar_bdd.ps1"`.

## 1. Pruebas automáticas (sin servidor)

Cada comando debe terminar con `BUILD SUCCESS` y 0 fallos. Cifras esperadas hoy: servidor 399, consola 381, escritorio 707, web 723.

```bash
cd "$R\04. SERVIDOR\EUREKABANK_RESTFUL_JAVA"; & $MVN -B -o clean package
```

```bash
cd "$R\04. CLICON\EUREKABANK_RESTFUL_JAVA_CON"; & $MVN -B -o clean package
```

```bash
cd "$R\04. CLIESC\EUREKABANK_RESTFUL_JAVA_ESC"; & $MVN -B -o clean package
```

```bash
cd "$R\04. CLIWEB\EUREKABANK_RESTFUL_JAVA_WEB"; & $MVN -B -o clean package
```

Las 37 pruebas de integración del servidor contra MySQL se saltan solas. Para correrlas necesitas la clave de administrador de MySQL: define `EUREKABANK_DB_URL`, `EUREKABANK_DB_ADMIN_USUARIO` y `EUREKABANK_DB_ADMIN_CLAVE` y repite el primer comando.

## 2. Diagramas y documentos

No hay scripts de verificación en el repositorio. Se comprueban abriendo los archivos (sección 8). Las herramientas que generaron los diagramas y los Word están en la carpeta `_herramientas_REST_respaldo`, al lado del proyecto, por si algún día hacen falta.

## 3. Desplegar el servidor y la web en Payara

Payara 7 está en `C:\payara7`. Usa un dominio que no tenga nada importante. Los comandos de ejemplo usan `domain1` (puerto 8080); si ya tienes otro dominio en el 8080, detenlo o usa el suyo con `--port <puerto de administración>`.

```bash
& "C:\payara7\bin\asadmin.bat" start-domain domain1
```

```bash
& "C:\payara7\bin\asadmin.bat" deploy --name EUREKABANK_RESTFUL_JAVA --contextroot EUREKABANK_RESTFUL_JAVA "$R\04. SERVIDOR\EUREKABANK_RESTFUL_JAVA\target\EUREKABANK_RESTFUL_JAVA.war"
```

```bash
& "C:\payara7\bin\asadmin.bat" deploy --name EUREKABANK_RESTFUL_JAVA_WEB --contextroot EUREKABANK_RESTFUL_JAVA_WEB "$R\04. CLIWEB\EUREKABANK_RESTFUL_JAVA_WEB\target\EUREKABANK_RESTFUL_JAVA_WEB.war"
```

Si tu Payara no usa el puerto 8080, antes de arrancar define `$env:SERVICIO_URL = "http://localhost:<puerto>/EUREKABANK_RESTFUL_JAVA/api"` para que la web encuentre el servidor.

## 4. Servidor REST con curl

Usa Git Bash para estos comandos (el `$` de PowerShell cambia la sintaxis). La clave se envía como SHA-256.

1. Base y hash: `B=http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api; H=$(printf 'Cromero2026' | sha256sum | cut -d' ' -f1)`
2. Login: `curl -s -X POST $B/auth/login -H "Content-Type: application/json" -d "{\"usuario\":\"cromero\",\"clave\":\"$H\"}"` debe dar `200` con un `token`.
3. Guarda el token: `T=<pega el token>`
4. Cuentas del cliente: `curl -s $B/clientes/06914897/cuentas -H "Authorization: Bearer $T"` debe dar `200` con la cuenta `00200002`.
5. Sin cabecera: `curl -i -s $B/cuentas/00200002` debe dar `400 DAT-001`.
6. Token falso: `curl -i -s $B/cuentas/00200002 -H "Authorization: Bearer falso"` debe dar `401 AUTH-003`.
7. Cuenta no activa: `curl -i -s $B/cuentas/00300001 -H "Authorization: Bearer $T"` debe dar `409 CTA-002`.
8. Monto cero: `curl -i -s -X POST $B/cuentas/00200002/depositos -H "Authorization: Bearer $T" -H "Content-Type: application/json" -d '{"monto":0}'` debe dar `400 MON-001`.
9. Monto enorme: igual que el anterior con `{"monto":1e999}` debe dar `400 MON-001`.
10. Orden de validación: `curl -i -s -X POST $B/cuentas/00200002/depositos -H "Authorization: Bearer falso" -H "Content-Type: application/json" -d '{}'` debe dar `400 DAT-001`, no 401.
11. Ruta inexistente: `curl -i -s $B/otro` debe dar `404`.
12. Método incorrecto: `curl -i -s -X PUT $B/cuentas/00200002 -H "Authorization: Bearer $T"` debe dar `405`.
13. Logout: `curl -s -X POST $B/auth/logout -H "Authorization: Bearer $T" -H "Content-Type: application/json" -d '{}'` y después repite el paso 4: ahora debe dar `401 AUTH-003`.

## 5. Cliente de consola

```bash
cd "$R\04. CLICON\EUREKABANK_RESTFUL_JAVA_CON"; java -Dfile.encoding=UTF-8 -jar target\EUREKABANK_RESTFUL_JAVA_CON.jar http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api
```

1. Usuario `cromero`, clave `Cromero2026`: debe decir "Acceso concedido".
2. Opción 1 con la cédula `06914897`: muestra la cuenta `00200002`.
3. Opción 7 con `00200002`: muestra los costos.
4. Opción 2 con `00300001`: muestra `[CTA-002]`.
5. Opción 4 con monto `abc`: muestra el aviso de monto y no mueve dinero.
6. Opción 0 para salir. Repite con una URL falsa (`http://localhost:9999/x`): debe avisar que no hay servicio y seguir vivo.

## 6. Cliente de escritorio (lo que solo tú puedes ver)

```bash
cd "$R\04. CLIESC\EUREKABANK_RESTFUL_JAVA_ESC"; java -jar target\EUREKABANK_RESTFUL_JAVA_ESC.jar http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api
```

Revisa a ojo:
1. El login muestra a la izquierda la foto de Sulley y Boo y a la derecha una tarjeta con usuario, clave y un solo botón "Ingresar".
2. Con usuario vacío y la clave escrita aparece un aviso y no se llama al servidor.
3. Con clave incorrecta aparece el mensaje del servidor (AUTH-001) en rojo, junto al botón.
4. Con las credenciales correctas entra a la ventana con pestañas Cuentas, Movimientos, Operaciones y Costos.
5. Cuentas con `06914897`: tabla con importes alineados a la derecha. Cédula vacía: aviso de campo vacío.
6. Operaciones: un depósito de `1.00` muestra el mensaje del servidor. Prueba `abc`, `-5`, `0`, `1,5`, `1e3` y `1.001`: todos deben rechazarse con aviso.
7. Pulsa dos veces seguidas el botón de una operación: solo se envía una vez y el botón dice "Procesando".
8. Detén Payara y repite una consulta: debe aparecer el mensaje de servicio no disponible, sin cerrarse la ventana.
9. Cierra sesión: vuelve al login.
10. Un solo color de acento (teal), esquinas redondeadas parejas y texto legible. Anota cualquier detalle feo para pulirlo.

## 7. Cliente web

Abre `http://localhost:8080/EUREKABANK_RESTFUL_JAVA_WEB/` en el navegador.

1. Debe llevarte a `/login` con la foto de Monster a la izquierda y el formulario a la derecha. Con la ventana estrecha la foto pasa arriba.
2. Entra con `cromero` y `Cromero2026`. Debe abrir `/cuentas`.
3. Cuentas con `06914897`, Movimientos y Costos con `00200002`: tablas y detalle correctos.
4. Operaciones: depósito de `1.00`; recarga la página (F5): no debe repetirse el depósito.
5. Cuenta `<script>alert(1)</script>` en cualquier campo: se muestra como texto, no se ejecuta.
6. Pulsa F12, pestaña Aplicación, Cookies: `JSESSIONID` con HttpOnly y SameSite Strict, y el token del servicio no aparece en ningún lado.
7. Copia la URL de `/cuentas` en una ventana privada: te manda a `/login`.
8. Cierra sesión y pulsa Atrás: no debe mostrar datos.
9. Cambia el tema del sistema a oscuro: la web cambia sola a su modo oscuro.
10. Revisa que un solo color de acento domina y que no hay guiones largos en los textos.

## 8. Diagramas y documentos

1. Abre `RESTFUL_JAVA_CONBDD_GR03.sws` con PowerDesigner 16.5: deben abrir los 41 modelos de UML y los 3 del MER.
2. Los PNG de `05. DOCUMENTACION\img\` son de apoyo para los Word. Los de los clientes de escritorio y web no se exportaron: ábrelos directo en PowerDesigner.
3. Abre en Word `ERS_EUREKABANK_RESTFUL_JAVA_GR03.docx`, `ECUD_EUREKABANK_RESTFUL_JAVA_GR03.docx` y `MANUAL_EUREKABANK_RESTFUL_JAVA_GR03.docx`; pulsa F9 para actualizar los índices.
4. Las portadas del ERS, el ECUD y el manual ya llevan los nombres de los tres integrantes.

## 9. Limpieza

```bash
& "C:\payara7\bin\asadmin.bat" undeploy EUREKABANK_RESTFUL_JAVA_WEB; & "C:\payara7\bin\asadmin.bat" undeploy EUREKABANK_RESTFUL_JAVA; & "C:\payara7\bin\asadmin.bat" stop-domain domain1
```
