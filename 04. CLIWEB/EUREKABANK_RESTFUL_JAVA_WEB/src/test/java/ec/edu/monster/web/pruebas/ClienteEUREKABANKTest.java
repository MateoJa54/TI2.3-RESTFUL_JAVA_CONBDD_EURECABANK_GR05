package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.cliente.ClienteEUREKABANK;
import ec.edu.monster.web.modelo.CuentaDTO;
import ec.edu.monster.web.modelo.RespuestaAutenticacion;
import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.RespuestaCostos;
import ec.edu.monster.web.modelo.RespuestaCuenta;
import ec.edu.monster.web.modelo.RespuestaCuentas;
import ec.edu.monster.web.modelo.RespuestaMovimiento;
import ec.edu.monster.web.modelo.RespuestaMovimientos;
import ec.edu.monster.web.modelo.RespuestaTransferencia;
import ec.edu.monster.web.util.ExcepcionServicio;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Prueba el adaptador REST con HTTP de verdad: publica, dentro de la misma
 * prueba, un servidor que responde como el del banco y lo consume por la
 * red. No necesita Payara ni la base de datos.
 *
 * <p>Es la prueba que demuestra que los POJOs planos del cliente se
 * serializan y deserializan bien contra el JSON que declara el contrato, y
 * que el token viaja en la cabecera {@code Authorization} y nunca en la URL.
 */
class ClienteEUREKABANKTest {

    private static final String BASE = "/EUREKABANK_RESTFUL_JAVA/api";
    private static final String TOKEN = "TOKEN-REST";

    private static ServidorDePrueba servidor;
    private static String url;

    @BeforeAll
    static void publicarServicio() throws IOException {
        servidor = new ServidorDePrueba();
        url = servidor.url(BASE);

        servidor.registrarDinamico("POST", BASE + "/auth/login", 200, peticion -> {
            if ("mala".equals(ServidorDePrueba.campo(peticion, "clave"))) {
                return ServidorDePrueba.sobre("ERROR", "AUTH-001",
                        "El usuario o la contraseña son incorrectos");
            }
            return ServidorDePrueba.sobre("OK", "200", "Autenticación exitosa",
                    "\"token\":\"" + TOKEN + "\",\"empleado\":\"0001\","
                            + "\"nombreEmpleado\":\"Carlos Romero\","
                            + "\"expiracion\":\"2026-11-03T12:15:00Z\",\"rol\":\"Consultas\"");
        });
        servidor.registrar("POST", BASE + "/auth/logout", 200,
                ServidorDePrueba.sobre("OK", "200", "Sesión cerrada correctamente"));
        servidor.registrar("GET", BASE + "/clientes/06914897/cuentas", 200,
                ServidorDePrueba.sobre("OK", "200", "Cuentas consultadas correctamente",
                        "\"cedula\":\"06914897\",\"nombreCliente\":\"Cliente de Prueba\","
                                + "\"cuentas\":[" + cuentaJson("00200002", "01", "Soles", "6800.00", "ACTIVO", 7)
                                + "," + cuentaJson("00200003", "02", "Dolares", "1234.56", "ANULADO", 0) + "]"));
        servidor.registrar("GET", BASE + "/cuentas/00200002", 200,
                ServidorDePrueba.sobre("OK", "200", "Cuenta consultada correctamente",
                        "\"cuenta\":" + cuentaJson("00200002", "01", "Soles", "6800.00", "ACTIVO", 7)));
        servidor.registrar("GET", BASE + "/cuentas/00200002/movimientos", 200,
                ServidorDePrueba.sobre("OK", "200", "Movimientos consultados correctamente",
                        "\"cuenta\":\"00200002\",\"movimientos\":["
                                + movimientoJson(1, "2026-01-05", "003", "Deposito", "INGRESO", "100.00", "")
                                + "," + movimientoJson(2, "2026-01-06", "004", "Retiro", "SALIDA", "50.50", "00100001")
                                + "]"));
        servidor.registrarDinamico("POST", BASE + "/cuentas/00200002/depositos", 200,
                peticion -> ServidorDePrueba.sobre("OK", "200", "Depósito realizado correctamente",
                        "\"saldoNuevo\":6900.00,\"costoAplicado\":0.50,\"itfAplicado\":0.08,"
                                + "\"movimiento\":" + movimientoJson(8, "2026-11-03", "003", "Deposito",
                                "INGRESO", ServidorDePrueba.campo(peticion, "monto"), "")));
        servidor.registrar("POST", BASE + "/cuentas/00200002/retiros", 200,
                ServidorDePrueba.sobre("OK", "200", "Retiro realizado correctamente",
                        "\"saldoNuevo\":6750.00,\"costoAplicado\":0.00,\"itfAplicado\":0.00,"
                                + "\"movimiento\":" + movimientoJson(9, "2026-11-03", "004", "Retiro", "SALIDA",
                                "50.00", "")));
        servidor.registrar("POST", BASE + "/transferencias", 200,
                ServidorDePrueba.sobre("OK", "200", "Transferencia realizada correctamente",
                        "\"numeroOrigen\":10,\"numeroDestino\":25,\"saldoOrigen\":6775.00,"
                                + "\"saldoDestino\":7429.00,\"itfAplicado\":0.02"));
        servidor.registrar("GET", BASE + "/cuentas/00200002/costos", 200,
                ServidorDePrueba.sobre("OK", "200", "Costos de mantenimiento consultados correctamente",
                        "\"saldoActual\":6800.00,\"costoMantenimiento\":5.00,\"costoMovimiento\":0.50,"
                                + "\"interesProyectado\":54.40,\"limiteMantenimiento\":100.00,"
                                + "\"operacionesSinCosto\":15"));
    }

    @AfterAll
    static void detenerServicio() {
        servidor.close();
    }

    private static String cuentaJson(String codigo, String moneda, String descripcionMoneda,
            String saldo, String estado, int operaciones) {
        return "{\"codigo\":\"" + codigo + "\",\"moneda\":\"" + moneda + "\",\"descripcionMoneda\":\""
                + descripcionMoneda + "\",\"saldo\":" + saldo + ",\"estado\":\"" + estado + "\","
                + "\"codigoCliente\":\"00001\",\"nombreCliente\":\"Cliente de Prueba\","
                + "\"codigoSucursal\":\"001\",\"fechaCreacion\":\"2020-01-01\","
                + "\"clave\":\"ABC123\",\"operacionesRealizadas\":" + operaciones + "}";
    }

    private static String movimientoJson(int numero, String fecha, String tipoCodigo,
            String tipoDescripcion, String tipoAccion, String importe, String referencia) {
        return "{\"numero\":" + numero + ",\"fecha\":\"" + fecha + "\",\"tipoCodigo\":\"" + tipoCodigo
                + "\",\"tipoDescripcion\":\"" + tipoDescripcion + "\",\"tipoAccion\":\"" + tipoAccion
                + "\",\"importe\":" + importe + ",\"referencia\":\"" + referencia
                + "\",\"empleado\":\"0001\"}";
    }

    private static void assertImporte(String esperado, BigDecimal real) {
        assertNotNull(real);
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "Se esperaba el importe " + esperado + " y llegó " + real);
    }

    private static ClienteEUREKABANK cliente() {
        return new ClienteEUREKABANK(url);
    }

    // ---------- las nueve operaciones del contrato ----------

    @Test
    void laAutenticacionLlegaConTokenEmpleadoYExpiracion() {
        RespuestaAutenticacion respuesta = cliente().autenticar("cromero", "clave-hash");

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Autenticación exitosa", respuesta.getMensaje());
        assertEquals(TOKEN, respuesta.getToken());
        assertEquals("0001", respuesta.getEmpleado());
        assertEquals("Carlos Romero", respuesta.getNombreEmpleado());
        assertEquals("2026-11-03T12:15:00Z", respuesta.getExpiracion());
        assertEquals("Consultas", respuesta.getRol());
    }

    @Test
    void unErrorDeNegocioLlegaComoDatosYNoComoExcepcion() {
        RespuestaAutenticacion respuesta = cliente().autenticar("cromero", "mala");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("AUTH-001", respuesta.getCodigo());
        assertEquals("El usuario o la contraseña son incorrectos", respuesta.getMensaje());
        assertNull(respuesta.getToken());
    }

    @Test
    void elCierreDeSesionDevuelveElSobreComun() {
        RespuestaBase respuesta = cliente().cerrarSesion(TOKEN);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Sesión cerrada correctamente", respuesta.getMensaje());
    }

    @Test
    void elListadoDeCuentasLlegaCompletoYEnOrden() {
        RespuestaCuentas respuesta = cliente().listarCuentasPorCliente("06914897", TOKEN);

        assertEquals("06914897", respuesta.getCedula());
        assertEquals("Cliente de Prueba", respuesta.getNombreCliente());
        assertEquals(2, respuesta.getCuentas().size());
        assertEquals("00200002", respuesta.getCuentas().get(0).getCodigo());
        assertEquals("00200003", respuesta.getCuentas().get(1).getCodigo());
        assertImporte("6800.00", respuesta.getCuentas().get(0).getSaldo());
        assertImporte("1234.56", respuesta.getCuentas().get(1).getSaldo());
    }

    @Test
    void elDetalleDeUnaCuentaLlegaConTodosSusCampos() {
        RespuestaCuenta respuesta = cliente().consultarCuenta("00200002", TOKEN);

        assertNotNull(respuesta.getCuenta());
        CuentaDTO cuenta = respuesta.getCuenta();
        assertEquals("00200002", cuenta.getCodigo());
        assertEquals("01", cuenta.getMoneda());
        assertEquals("Soles", cuenta.getDescripcionMoneda());
        assertEquals("ACTIVO", cuenta.getEstado());
        assertEquals("00001", cuenta.getCodigoCliente());
        assertEquals("Cliente de Prueba", cuenta.getNombreCliente());
        assertEquals("001", cuenta.getCodigoSucursal());
        assertEquals("2020-01-01", cuenta.getFechaCreacion());
        assertEquals("ABC123", cuenta.getClave());
        assertEquals(7, cuenta.getOperacionesRealizadas());
        assertImporte("6800.00", cuenta.getSaldo());
    }

    @Test
    void losMovimientosLleganCompletosYEnOrden() {
        RespuestaMovimientos respuesta = cliente().consultarMovimientos("00200002", TOKEN);

        assertEquals("00200002", respuesta.getCuenta());
        assertEquals(2, respuesta.getMovimientos().size());
        assertEquals(1, respuesta.getMovimientos().get(0).getNumero());
        assertEquals("003", respuesta.getMovimientos().get(0).getTipoCodigo());
        assertEquals("Deposito", respuesta.getMovimientos().get(0).getTipoDescripcion());
        assertEquals("INGRESO", respuesta.getMovimientos().get(0).getTipoAccion());
        assertEquals("0001", respuesta.getMovimientos().get(0).getEmpleado());
        assertImporte("50.50", respuesta.getMovimientos().get(1).getImporte());
        assertEquals("00100001", respuesta.getMovimientos().get(1).getReferencia());
    }

    @Test
    void elDepositoDevuelveSaldoCostoItfYMovimiento() {
        RespuestaMovimiento respuesta = cliente()
                .realizarDeposito("00200002", new BigDecimal("100.00"), TOKEN);

        assertEquals("OK", respuesta.getEstado());
        assertImporte("6900.00", respuesta.getSaldoNuevo());
        assertImporte("0.50", respuesta.getCostoAplicado());
        assertImporte("0.08", respuesta.getItfAplicado());
        assertEquals("003", respuesta.getMovimiento().getTipoCodigo());
        assertImporte("100.00", respuesta.getMovimiento().getImporte());
    }

    @Test
    void elRetiroDevuelveElSaldoNuevo() {
        RespuestaMovimiento respuesta = cliente()
                .realizarRetiro("00200002", new BigDecimal("50.00"), TOKEN);

        assertImporte("6750.00", respuesta.getSaldoNuevo());
        assertEquals(0, respuesta.getCostoAplicado().compareTo(BigDecimal.ZERO));
    }

    @Test
    void laTransferenciaDevuelveNumerosSaldosYItf() {
        RespuestaTransferencia respuesta = cliente()
                .realizarTransferencia("00200002", "00100001", new BigDecimal("25.00"), TOKEN);

        assertEquals(10, respuesta.getNumeroOrigen());
        assertEquals(25, respuesta.getNumeroDestino());
        assertImporte("6775.00", respuesta.getSaldoOrigen());
        assertImporte("7429.00", respuesta.getSaldoDestino());
        assertImporte("0.02", respuesta.getItfAplicado());
    }

    @Test
    void losCostosDeMantenimientoLleganConTodosSusImportes() {
        RespuestaCostos respuesta = cliente().consultarCostosMantenimiento("00200002", TOKEN);

        assertImporte("6800.00", respuesta.getSaldoActual());
        assertImporte("5.00", respuesta.getCostoMantenimiento());
        assertImporte("0.50", respuesta.getCostoMovimiento());
        assertImporte("54.40", respuesta.getInteresProyectado());
        assertImporte("100.00", respuesta.getLimiteMantenimiento());
        assertEquals(15, respuesta.getOperacionesSinCosto());
    }

    @Test
    void losImportesConMasDeDosDecimalesNoSePierden() {
        RespuestaMovimiento respuesta = cliente()
                .realizarDeposito("00200002", new BigDecimal("0.01"), TOKEN);

        assertImporte("0.01", respuesta.getMovimiento().getImporte());
    }

    @Test
    void elMismoClienteSirveParaMuchasLlamadas() {
        ClienteEUREKABANK cliente = cliente();

        for (int i = 1; i <= 10; i++) {
            assertImporte("100.00", cliente.realizarDeposito("00200002", new BigDecimal("100.00"), TOKEN)
                    .getMovimiento().getImporte());
        }
    }

    // ---------- verbos, rutas y cuerpos del contrato ----------

    @Test
    void elLoginUsaPostYLaRutaDelContrato() {
        cliente().autenticar("cromero", "clave-hash");

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("POST", peticion.getMetodo());
        assertEquals(BASE + "/auth/login", peticion.getRuta());
    }

    @Test
    void elLogoutUsaPostYLaRutaDelContrato() {
        cliente().cerrarSesion(TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("POST", peticion.getMetodo());
        assertEquals(BASE + "/auth/logout", peticion.getRuta());
    }

    @Test
    void elListadoDeCuentasUsaGetYLaRutaDelContrato() {
        cliente().listarCuentasPorCliente("06914897", TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("GET", peticion.getMetodo());
        assertEquals(BASE + "/clientes/06914897/cuentas", peticion.getRuta());
    }

    @Test
    void laConsultaDeCuentaUsaGetYLaRutaDelContrato() {
        cliente().consultarCuenta("00200002", TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("GET", peticion.getMetodo());
        assertEquals(BASE + "/cuentas/00200002", peticion.getRuta());
    }

    @Test
    void laConsultaDeMovimientosUsaGetYLaRutaDelContrato() {
        cliente().consultarMovimientos("00200002", TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("GET", peticion.getMetodo());
        assertEquals(BASE + "/cuentas/00200002/movimientos", peticion.getRuta());
    }

    @Test
    void elDepositoUsaPostYLaRutaDelContrato() {
        cliente().realizarDeposito("00200002", new BigDecimal("100.00"), TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("POST", peticion.getMetodo());
        assertEquals(BASE + "/cuentas/00200002/depositos", peticion.getRuta());
    }

    @Test
    void elRetiroUsaPostYLaRutaDelContrato() {
        cliente().realizarRetiro("00200002", new BigDecimal("50.00"), TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("POST", peticion.getMetodo());
        assertEquals(BASE + "/cuentas/00200002/retiros", peticion.getRuta());
    }

    @Test
    void laTransferenciaUsaPostYLaRutaDelContrato() {
        cliente().realizarTransferencia("00200002", "00100001", new BigDecimal("25.00"), TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("POST", peticion.getMetodo());
        assertEquals(BASE + "/transferencias", peticion.getRuta());
    }

    @Test
    void laConsultaDeCostosUsaGetYLaRutaDelContrato() {
        cliente().consultarCostosMantenimiento("00200002", TOKEN);

        ServidorDePrueba.Peticion peticion = servidor.ultimaPeticion();
        assertEquals("GET", peticion.getMetodo());
        assertEquals(BASE + "/cuentas/00200002/costos", peticion.getRuta());
    }

    @Test
    void elLoginMandaElCuerpoJsonConUsuarioYClave() {
        cliente().autenticar("cromero", "clave-hash");

        String cuerpo = servidor.ultimaPeticion().getCuerpo();
        assertTrue(cuerpo.contains("\"usuario\":\"cromero\""), cuerpo);
        assertTrue(cuerpo.contains("\"clave\":\"clave-hash\""), cuerpo);
    }

    @Test
    void elLogoutMandaUnCuerpoJsonVacio() {
        cliente().cerrarSesion(TOKEN);

        assertEquals("{}", servidor.ultimaPeticion().getCuerpo());
    }

    @Test
    void elDepositoMandaElCuerpoJsonConElMonto() {
        cliente().realizarDeposito("00200002", new BigDecimal("100.00"), TOKEN);

        String cuerpo = servidor.ultimaPeticion().getCuerpo();
        assertTrue(cuerpo.contains("\"monto\""), cuerpo);
        assertTrue(cuerpo.contains("100.00"), cuerpo);
    }

    @Test
    void elRetiroMandaElCuerpoJsonConElMonto() {
        cliente().realizarRetiro("00200002", new BigDecimal("50.00"), TOKEN);

        assertTrue(servidor.ultimaPeticion().getCuerpo().contains("\"monto\""));
    }

    @Test
    void laTransferenciaMandaElCuerpoJsonConLosTresCampos() {
        cliente().realizarTransferencia("00200002", "00100001", new BigDecimal("25.00"), TOKEN);

        String cuerpo = servidor.ultimaPeticion().getCuerpo();
        assertTrue(cuerpo.contains("\"cuentaOrigen\":\"00200002\""), cuerpo);
        assertTrue(cuerpo.contains("\"cuentaDestino\":\"00100001\""), cuerpo);
        assertTrue(cuerpo.contains("\"monto\""), cuerpo);
    }

    @Test
    void lasPeticionesConCuerpoDeclaranContentTypeJsonConUtf8() {
        cliente().autenticar("cromero", "clave-hash");

        assertEquals("application/json; charset=utf-8",
                servidor.ultimaPeticion().getTipoContenido());
    }

    // ---------- el token viaja en la cabecera, nunca en la URL ----------

    @Test
    void elTokenViajaEnLaCabeceraAuthorizationComoBearer() {
        cliente().consultarCuenta("00200002", TOKEN);

        assertEquals("Bearer " + TOKEN, servidor.ultimaPeticion().getAutorizacion());
    }

    @Test
    void elTokenNoApareceEnLaUrlDeNingunaOperacionProtegida() {
        ClienteEUREKABANK cliente = cliente();

        cliente.listarCuentasPorCliente("06914897", TOKEN);
        cliente.consultarCuenta("00200002", TOKEN);
        cliente.consultarMovimientos("00200002", TOKEN);
        cliente.realizarDeposito("00200002", new BigDecimal("10.00"), TOKEN);
        cliente.realizarRetiro("00200002", new BigDecimal("10.00"), TOKEN);
        cliente.realizarTransferencia("00200002", "00100001", new BigDecimal("10.00"), TOKEN);
        cliente.consultarCostosMantenimiento("00200002", TOKEN);
        cliente.cerrarSesion(TOKEN);

        for (ServidorDePrueba.Peticion peticion : servidor.peticiones()) {
            assertFalse(peticion.getRuta().contains(TOKEN),
                    "El token apareció en la ruta " + peticion.getRuta());
            assertFalse(peticion.getCuerpo() != null && peticion.getCuerpo().contains(TOKEN),
                    "El token apareció en el cuerpo de la petición");
        }
    }

    @Test
    void elLoginNoMandaCabeceraAuthorization() {
        cliente().autenticar("cromero", "clave-hash");

        assertNull(servidor.ultimaPeticion().getAutorizacion());
    }

    @Test
    void elLogoutSiMandaCabeceraAuthorization() {
        cliente().cerrarSesion(TOKEN);

        assertEquals("Bearer " + TOKEN, servidor.ultimaPeticion().getAutorizacion());
    }

    // ---------- fallos del servicio y del formato ----------

    @Test
    void un503ProduceLaExcepcionDelCliente() {
        servidor.registrar("GET", BASE + "/cuentas/NODISPONIBLE", 503,
                ServidorDePrueba.sobre("ERROR", "SER-001", "El servicio no está disponible en este momento"));

        ExcepcionServicio error = assertThrows(ExcepcionServicio.class,
                () -> cliente().consultarCuenta("NODISPONIBLE", TOKEN));

        assertEquals("SER-001", error.getCodigo());
        assertTrue(error.getMessage().contains("SER-001"));
        assertTrue(error.getMessage().contains("El servicio no está disponible en este momento"));
    }

    @Test
    void unCuerpoQueNoEsJsonProduceLaExcepcionDelCliente() {
        servidor.registrar("GET", BASE + "/cuentas/HTML", 200, "text/html; charset=utf-8",
                "<html><body>No soy JSON</body></html>");

        ExcepcionServicio error = assertThrows(ExcepcionServicio.class,
                () -> cliente().consultarCuenta("HTML", TOKEN));

        assertEquals("SER-001", error.getCodigo());
    }

    @Test
    void unJsonSinEstadoNiCodigoProduceLaExcepcionDelCliente() {
        servidor.registrar("GET", BASE + "/cuentas/OTROJSON", 200,
                "{\"otra\":\"cosa\",\"saldo\":100}");

        assertThrows(ExcepcionServicio.class, () -> cliente().consultarCuenta("OTROJSON", TOKEN));
    }

    @Test
    void unCuerpoVacioProduceLaExcepcionDelCliente() {
        servidor.registrar("GET", BASE + "/cuentas/VACIO", 200, "");

        assertThrows(ExcepcionServicio.class, () -> cliente().consultarCuenta("VACIO", TOKEN));
    }

    @Test
    void siLaRutaNoExisteSeLanzaLaExcepcionDelCliente() {
        assertThrows(ExcepcionServicio.class,
                () -> cliente().consultarCuenta("99999999", TOKEN));
    }

    @Test
    void siNoHayServidorEnEsaDireccionSeLanzaLaExcepcionDelCliente() throws IOException {
        String sinServidor = "http://localhost:" + puertoLibre() + BASE;
        ClienteEUREKABANK cliente = new ClienteEUREKABANK(sinServidor);

        ExcepcionServicio error = assertThrows(ExcepcionServicio.class,
                () -> cliente.listarCuentasPorCliente("06914897", TOKEN));

        assertEquals("SER-001", error.getCodigo());
        assertTrue(error.getMessage().contains("SER-001"));
        assertTrue(error.getMessage().contains(sinServidor));
    }

    // ---------- construcción del cliente ----------

    @Test
    void crearElClienteNoNecesitaQueElServidorEsteEncendido() {
        ClienteEUREKABANK cliente = new ClienteEUREKABANK("  http://localhost:1/noexiste  ");

        assertEquals("http://localhost:1/noexiste", cliente.getUrl());
    }

    @Test
    void laDireccionEsObligatoria() {
        assertThrows(IllegalArgumentException.class, () -> new ClienteEUREKABANK(null));
        assertThrows(IllegalArgumentException.class, () -> new ClienteEUREKABANK("  "));
    }

    @Test
    void laDireccionPorDefectoApuntaAlServidorRestLocal() {
        assertEquals("http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api",
                ClienteEUREKABANK.URL_POR_DEFECTO);
    }

    private static int puertoLibre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
