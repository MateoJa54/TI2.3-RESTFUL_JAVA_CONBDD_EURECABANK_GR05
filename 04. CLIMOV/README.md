# CLIMOV — EurekaBank_RestfulJava

Aplicación Android nativa en **Java**, organizada en MVC y con paquetes bajo `ec.edu.monster`. Consume el contrato REST JSON de EUREKABANK y ofrece las nueve operaciones que incluye el cliente de consola.

## Abrir y compilar

Abre `04. CLIMOV/CLIMOV` en Android Studio. El nombre visible es `EurekaBank_RestfulJava`; la clase de inicio es `ec.edu.monster.MainActivity`.

```powershell
cd "04. CLIMOV\CLIMOV"
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
```

El paquete de la aplicación es `ec.edu.monster.eurekabank.restfuljava` y el mínimo es Android 7 (API 24).

## Conexión con Payara

La pantalla de acceso permite editar la URL base completa; se guarda para el siguiente inicio. El valor inicial funciona en el emulador Android:

`http://10.0.2.2:8080/EUREKABANK_RESTFUL_JAVA/api`

En un teléfono conectado a la misma red Wi-Fi, reemplaza `10.0.2.2` por la IPv4 LAN del equipo donde se ejecuta Payara, por ejemplo `http://192.168.1.25:8080/EUREKABANK_RESTFUL_JAVA/api`. No uses `localhost` desde el teléfono. Payara debe escuchar en la interfaz de red y el firewall debe permitir el puerto configurado (8080 por defecto).

Todas las operaciones menos login envían `Authorization: Bearer <token>`. La contraseña se convierte a SHA-256 hexadecimal antes de enviarse. Los importes usan `BigDecimal`, escala 2 y redondeo `HALF_UP`. Los resultados muestran el código y mensaje que devuelve el servidor, y presentan los datos de cuentas, movimientos, transferencias y costos con etiquetas legibles.

## Operaciones

- Iniciar y cerrar sesión.
- Listar cuentas por cédula y consultar una cuenta.
- Consultar movimientos.
- Realizar depósitos, retiros y transferencias.
- Consultar costos de mantenimiento.

La aplicación usa HTTP para permitir la conexión local de laboratorio a Payara. Para un teléfono físico, cambia la IP desde el campo **Servidor** de la pantalla de acceso.
