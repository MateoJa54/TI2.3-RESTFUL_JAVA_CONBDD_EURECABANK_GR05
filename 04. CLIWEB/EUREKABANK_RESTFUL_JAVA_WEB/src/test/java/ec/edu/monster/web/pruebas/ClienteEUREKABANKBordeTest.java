package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.cliente.ClienteEUREKABANK;
import ec.edu.monster.web.modelo.RespuestaAutenticacion;
import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.RespuestaCuentas;
import ec.edu.monster.web.util.ExcepcionServicio;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Casos borde del adaptador REST: direcciones mal escritas, servidores que
 * contestan mal, respuestas que no son un sobre del banco y textos
 * difíciles.
 */
class ClienteEUREKABANKBordeTest {

    private static final String BASE = "/EUREKABANK_RESTFUL_JAVA/api";

    private static ServidorDePrueba servidor;
    private static String url;

    @BeforeAll
    static void publicar() throws IOException {
        servidor = new ServidorDePrueba();
        url = servidor.url(BASE);

        // Devuelve lo que recibe, para probar el viaje de los datos.
        servidor.registrarDinamico("POST", BASE + "/auth/login", 200,
                peticion -> ServidorDePrueba.sobre("OK", "200",
                        ServidorDePrueba.texto(ServidorDePrueba.campo(peticion, "usuario")),
                        "\"token\":\"" + ServidorDePrueba.texto(ServidorDePrueba.campo(peticion, "clave")) + "\""));
        servidor.registrarDinamico("POST", BASE + "/auth/logout", 200,
                peticion -> ServidorDePrueba.sobre("OK", "200", tokenDe(peticion)));

        StringBuilder cuentas = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            if (i > 0) {
                cuentas.append(',');
            }
            cuentas.append("{\"codigo\":\"CUENTA-").append(i).append("\",\"moneda\":\"01\",")
                    .append("\"descripcionMoneda\":\"Soles\",\"saldo\":10.00,\"estado\":\"ACTIVO\",")
                    .append("\"codigoCliente\":\"00001\",\"nombreCliente\":\"Cliente de Prueba\",")
                    .append("\"codigoSucursal\":\"001\",\"fechaCreacion\":\"2020-01-01\",")
                    .append("\"clave\":\"ABC123\",\"operacionesRealizadas\":").append(i).append('}');
        }
        servidor.registrarDinamico("GET", BASE + "/clientes/06914897/cuentas", 200,
                peticion -> ServidorDePrueba.sobre("OK", "200", tokenDe(peticion),
                        "\"cedula\":\"06914897\",\"cuentas\":[" + cuentas + "]"));

        // Rutas que contestan mal.
        servidor.registrar("GET", BASE + "/cuentas/HTML", 200, "text/html; charset=utf-8",
                "<html><body>Hola</body></html>");
        servidor.registrar("GET", BASE + "/cuentas/VACIO", 200, "application/json; charset=utf-8", "");
        servidor.registrar("GET", BASE + "/cuentas/JSONROTO", 200, "application/json; charset=utf-8",
                "{\"estado\":\"OK\",");
        servidor.registrar("GET", BASE + "/cuentas/OTROJSON", 200, "application/json; charset=utf-8",
                "{\"otra\":\"cosa\"}");
        servidor.registrar("GET", BASE + "/cuentas/LISTA", 200, "application/json; charset=utf-8", "[1,2,3]");
        servidor.registrar("GET", BASE + "/cuentas/ERROR500HTML", 500, "text/html; charset=utf-8",
                "Internal Server Error");
        servidor.registrar("GET", BASE + "/cuentas/ERROR500", 500,
                ServidorDePrueba.sobre("ERROR", "ERR-001", "Ocurrió un error interno, intente nuevamente"));
        servidor.registrar("GET", BASE + "/cuentas/NOEXISTE", 404,
                ServidorDePrueba.sobre("ERROR", "CLI-001", "El cliente solicitado no existe"));
        servidor.registrar("GET", BASE + "/cuentas/CONFLICTO", 409,
                ServidorDePrueba.sobre("ERROR", "SAL-001",
                        "El saldo de la cuenta es insuficiente para la operación"));
        servidor.registrar("GET", BASE + "/cuentas/NOAUTORIZADO", 401,
                ServidorDePrueba.sobre("ERROR", "AUTH-003", "El token de sesión no es válido"));
        servidor.registrar("GET", BASE + "/cuentas/NODISPONIBLE", 503,
                ServidorDePrueba.sobre("ERROR", "SER-001", "El servicio no está disponible en este momento"));
        servidor.registrar("GET", BASE + "/cuentas/NODISPONIBLEVACIO", 503,
                "application/json; charset=utf-8", "");
        servidor.registrarDinamico("GET", BASE + "/cuentas/LENTO", 200, peticion -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ServidorDePrueba.sobre("OK", "200", "Tarde");
        });
        servidor.registrar("GET", BASE + "/cuentas/00 20002", 200,
                ServidorDePrueba.sobre("OK", "200", "Cuenta con espacio"));
        servidor.registrarDinamico("POST", BASE + "/cuentas/00200002/depositos", 200,
                peticion -> ServidorDePrueba.sobre("OK", "200", "Depósito realizado correctamente",
                        "\"saldoNuevo\":6900.00,\"costoAplicado\":0.00,\"itfAplicado\":0.00,"
                                + "\"movimiento\":{\"numero\":8,\"fecha\":\"2026-11-03\","
                                + "\"tipoCodigo\":\"003\",\"tipoDescripcion\":\"Deposito\","
                                + "\"tipoAccion\":\"INGRESO\",\"importe\":"
                                + ServidorDePrueba.campo(peticion, "monto")
                                + ",\"referencia\":\"\",\"empleado\":\"0001\"}"));
    }

    @AfterAll
    static void detener() {
        servidor.close();
    }

    /** El token que llega en la cabecera Authorization, sin el prefijo Bearer. */
    private static String tokenDe(ServidorDePrueba.Peticion peticion) {
        String autorizacion = peticion.getAutorizacion();
        if (autorizacion == null) {
            return "<nulo>";
        }
        return autorizacion.startsWith("Bearer ") ? autorizacion.substring(7) : autorizacion;
    }

    private static ClienteEUREKABANK cliente() {
        return new ClienteEUREKABANK(url);
    }

    // ---------- respuestas que no son un sobre del banco ----------

    @ParameterizedTest
    @ValueSource(strings = {"HTML", "VACIO", "JSONROTO", "OTROJSON", "LISTA", "ERROR500HTML",
        "RUTASINREGISTRAR"})
    void siLaRespuestaNoEsUnSobreDelBancoSeLanzaLaExcepcionDelCliente(String cuenta) {
        ClienteEUREKABANK cliente = cliente();

        ExcepcionServicio error = assertThrows(ExcepcionServicio.class,
                () -> cliente.consultarCuenta(cuenta, "t"));

        assertEquals("SER-001", error.getCodigo());
        assertTrue(error.getMessage().contains("SER-001"));
    }

    @Test
    void un503ProduceLaExcepcionDelClienteAunqueElSobreSeaValido() {
        ExcepcionServicio error = assertThrows(ExcepcionServicio.class,
                () -> cliente().consultarCuenta("NODISPONIBLE", "t"));

        assertEquals("SER-001", error.getCodigo());
        assertTrue(error.getMessage().contains("503"));
    }

    @Test
    void un503SinCuerpoProduceLaExcepcionDelCliente() {
        assertThrows(ExcepcionServicio.class,
                () -> cliente().consultarCuenta("NODISPONIBLEVACIO", "t"));
    }

    // ---------- errores de negocio con sobre válido ----------

    @Test
    void un401ConSobreDeNegocioDevuelveElSobre() {
        RespuestaBase respuesta = cliente().consultarCuenta("NOAUTORIZADO", "t");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("AUTH-003", respuesta.getCodigo());
        assertEquals("El token de sesión no es válido", respuesta.getMensaje());
    }

    @Test
    void un404ConSobreDeNegocioDevuelveElSobre() {
        RespuestaBase respuesta = cliente().consultarCuenta("NOEXISTE", "t");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("CLI-001", respuesta.getCodigo());
        assertEquals("El cliente solicitado no existe", respuesta.getMensaje());
    }

    @Test
    void un409ConSobreDeNegocioDevuelveElSobre() {
        RespuestaBase respuesta = cliente().consultarCuenta("CONFLICTO", "t");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("SAL-001", respuesta.getCodigo());
        assertEquals("El saldo de la cuenta es insuficiente para la operación", respuesta.getMensaje());
    }

    @Test
    void un500ConSobreDeNegocioDevuelveElSobre() {
        RespuestaBase respuesta = cliente().consultarCuenta("ERROR500", "t");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("ERR-001", respuesta.getCodigo());
        assertEquals("Ocurrió un error interno, intente nuevamente", respuesta.getMensaje());
    }

    // ---------- direcciones mal escritas ----------

    @ParameterizedTest
    @ValueSource(strings = {
        "esto no es una url", "ftp://localhost/servicio", "http://", "localhost:8080/EUREKABANK_RESTFUL_JAVA",
        "//localhost:8080/ws", "http:///sin-equipo", "C:/carpeta/servicio", "http://local host/ws"})
    void unaDireccionMalEscritaSeRechazaAlCrearElCliente(String direccion) {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> new ClienteEUREKABANK(direccion));

        assertTrue(error.getMessage().contains(direccion));
    }

    @Test
    void seAceptanDireccionesHttpsYConDireccionIp() {
        assertEquals("https://banco.monster.edu.ec/api",
                new ClienteEUREKABANK("https://banco.monster.edu.ec/api").getUrl());
        assertEquals("http://192.168.1.50:8080/api",
                new ClienteEUREKABANK("http://192.168.1.50:8080/api").getUrl());
    }

    @Test
    void laBarraFinalDeLaDireccionNoSeRepiteEnLasRutas() {
        ClienteEUREKABANK cliente = new ClienteEUREKABANK(url + "/");

        assertEquals(url, cliente.getUrl());
        cliente.consultarCuenta("NOEXISTE", "t");

        assertEquals(BASE + "/cuentas/NOEXISTE", servidor.ultimaPeticion().getRuta());
    }

    // ---------- servidores que contestan mal ----------

    @Test
    void siElServidorTardaDemasiadoElClienteNoSeQuedaColgado() {
        ClienteEUREKABANK cliente = new ClienteEUREKABANK(url, 1_000, 300);

        assertTimeout(Duration.ofSeconds(5),
                () -> assertThrows(ExcepcionServicio.class,
                        () -> cliente.consultarCuenta("LENTO", "t")));
    }

    @Test
    void unEquipoQueNoExisteFallaConLaExcepcionDelClienteAlLlamar() {
        ClienteEUREKABANK cliente = new ClienteEUREKABANK(
                "http://equipo-que-no-existe.invalid:8080/EUREKABANK_RESTFUL_JAVA/api",
                1_000, 1_000);

        assertThrows(ExcepcionServicio.class, () -> cliente.consultarCuenta("LENTO", "t"));
    }

    @Test
    void despuesDeUnFalloElMismoClienteSigueSirviendo() {
        ClienteEUREKABANK cliente = cliente();
        assertThrows(ExcepcionServicio.class, () -> cliente.consultarCuenta("HTML", "t"));

        assertEquals("monster", cliente.autenticar("monster", "p").getMensaje());
    }

    // ---------- textos y listas difíciles ----------

    @ParameterizedTest
    @ValueSource(strings = {
        "a & b", "<script>alert(1)</script>", "]]>", "comilla\"doble", "comilla'simple", "50% + 1",
        "ñandú áéíóú ÑÁÉÍÓÚ ü", "°C → °F", "日本語", "emoji 😀", "linea1\nlinea2", "tab\tulado",
        "<![CDATA[x]]>", "&amp;", "' OR '1'='1", "a\\b/c", "{json: true}"})
    void losCaracteresEspecialesLleganIntactosEnLosDosSentidos(String texto) {
        RespuestaAutenticacion respuesta = cliente().autenticar(texto, texto);

        assertEquals(texto, respuesta.getMensaje());
        assertEquals(texto, respuesta.getToken());
    }

    @Test
    void unParametroNuloLlegaNuloAlServidor() {
        ClienteEUREKABANK cliente = cliente();

        assertEquals("<nulo>", cliente.autenticar(null, null).getMensaje());
        assertEquals("<nulo>", cliente.cerrarSesion(null).getMensaje());
    }

    @Test
    void unaListaGrandeLlegaCompletaYEnOrden() {
        List<ec.edu.monster.web.modelo.CuentaDTO> cuentas = cliente()
                .listarCuentasPorCliente("06914897", "t").getCuentas();

        assertEquals(300, cuentas.size());
        assertEquals("CUENTA-0", cuentas.get(0).getCodigo());
        assertEquals("CUENTA-299", cuentas.get(299).getCodigo());
    }

    @Test
    void unTextoMuyLargoViajaCompleto() {
        String largo = "x".repeat(50_000);

        assertEquals(largo, cliente().autenticar(largo, "p").getMensaje());
    }

    @Test
    void variosClientesALaVezRecibenCadaUnoSuRespuesta() throws Exception {
        int hilos = 4;
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<Boolean>> tareas = new ArrayList<>();
            for (int h = 0; h < hilos; h++) {
                Future<Boolean> tarea = ejecutor.submit(() -> {
                    ClienteEUREKABANK cliente = cliente();
                    boolean correcto = true;
                    for (int i = 0; i < 5; i++) {
                        RespuestaCuentas respuesta =
                                cliente.listarCuentasPorCliente("06914897", "t");
                        correcto &= respuesta.getCuentas().size() == 300;
                        correcto &= "t".equals(respuesta.getMensaje());
                    }
                    return correcto;
                });
                tareas.add(tarea);
            }
            for (Future<Boolean> tarea : tareas) {
                assertTrue(tarea.get(60, TimeUnit.SECONDS));
            }
        } finally {
            ejecutor.shutdownNow();
        }
    }

    // ---------- la cabecera del token ----------

    @Test
    void unTokenVacioNoProduceCabeceraAuthorization() {
        cliente().consultarCuenta("NOEXISTE", "");

        assertNull(servidor.ultimaPeticion().getAutorizacion());
    }

    @Test
    void unTokenNuloNoProduceCabeceraAuthorization() {
        cliente().consultarCuenta("NOEXISTE", null);

        assertNull(servidor.ultimaPeticion().getAutorizacion());
    }

    @Test
    void laCuentaConCaracteresReservadosVaCodificadaEnLaRuta() {
        cliente().consultarCuenta("00 20002", "t");

        String ruta = servidor.ultimaPeticion().getRutaCruda();
        assertTrue(ruta.contains("%20"), ruta);
        assertFalse(ruta.contains(" "), ruta);
    }

    @Test
    void elMontoViajaConDosDecimalesEnElCuerpo() {
        cliente().realizarDeposito("00200002", new BigDecimal("0.01"), "t");

        assertNotNull(servidor.ultimaPeticion().getCuerpo());
        assertTrue(servidor.ultimaPeticion().getCuerpo().contains("0.01"),
                servidor.ultimaPeticion().getCuerpo());
    }
}
