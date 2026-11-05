package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.modelo.RespuestaMovimiento;
import ec.edu.monster.web.servlet.OperacionServlet;
import ec.edu.monster.web.util.SesionWeb;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de {@link OperacionServlet}: depósito, retiro y transferencia con CSRF, Post/Redirect/Get,
 * envío único, errores de negocio y de formato, y sesión caducada.
 */
class OperacionServletTest {

    private static final String RUTA = "/operacion";
    private static final String VISTA = "/WEB-INF/vistas/operacion.jsp";
    private static final String VISTA_ERROR = "/WEB-INF/vistas/error.jsp";
    private static final String LOGIN = "/web/login";
    private static final String PRG = "/web/operacion";
    private static final String CUENTA = ClienteFalso.CUENTA;
    private static final String DESTINO = "00300003";
    private static final String HOSTIL = "<script>alert(1)</script>";

    private static final String MONTO_FORMATO =
            "El monto debe ser un número con punto decimal, por ejemplo 150.00";
    private static final String MONTO_COMA =
            "Use punto como separador decimal y no escriba separador de miles, por ejemplo 1500.50";
    private static final String MONTO_MON001 = "El monto debe ser mayor que cero y con máximo dos decimales";
    private static final String CUENTA_FORMATO = "El código de cuenta ingresado no tiene un formato válido";

    private final ClienteFalso servidor = new ClienteFalso();
    private final OperacionServlet servlet = new OperacionServlet();

    // ------------------------------------------------------------------ ayudas

    /** Usuario con sesión vigente que ya pidió el formulario (GET). Deja el envío y el CSRF listos. */
    private ServletFalso formulario(ClienteFalso cliente) throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(cliente).conUsuarioAutenticado(cliente)
                .metodo("GET").ruta(RUTA);
        f.ejecutar(servlet);
        cliente.llamadas.clear();
        return f;
    }

    private ServletFalso formulario() throws Exception {
        return formulario(servidor);
    }

    /** Envía el formulario como lo haría el navegador. Un CSRF null significa que el campo no viaja. */
    private ServletFalso enviar(ServletFalso f, String csrf, String modo, String cuenta, String destino,
            String monto) throws Exception {
        f.metodo("POST").ruta(RUTA);
        f.parametro("modo", modo);
        f.parametro("cuenta", cuenta);
        f.parametro("destino", destino);
        f.parametro("monto", monto);
        f.parametro("envio", (String) f.atributo("envio"));
        if (csrf != null) {
            f.parametro(SesionWeb.CAMPO_CSRF, csrf);
        }
        f.ejecutar(servlet);
        return f;
    }

    private ServletFalso enviarBien(ServletFalso f, String modo, String cuenta, String destino, String monto)
            throws Exception {
        return enviar(f, f.csrf(), modo, cuenta, destino, monto);
    }

    private static long llamadasDeOperacion(ClienteFalso cliente) {
        return cliente.llamadas.stream().filter(llamada -> llamada.startsWith("realizar")).count();
    }

    private static RespuestaMovimiento movimientoConError(String codigo, String mensaje) {
        return new RespuestaMovimiento("ERROR", codigo, mensaje);
    }

    // ------------------------------------------------------------------ acceso y formulario

    @Test
    void sinSesionElFormularioRedirigeAlLogin() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).metodo("GET").ruta(RUTA);
        f.ejecutar(servlet);
        assertEquals(LOGIN, f.redireccion());
        assertNull(f.reenvio());
    }

    @Test
    void sinSesionUnPostNoLlamaAlServicio() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).metodo("POST").ruta(RUTA)
                .parametro("modo", "deposito").parametro("cuenta", CUENTA).parametro("monto", "10.00")
                .parametro("envio", "ENVIO-X").parametro(SesionWeb.CAMPO_CSRF, "CSRF-X");
        f.ejecutar(servlet);
        assertEquals(LOGIN, f.redireccion());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void elFormularioSeMuestraConCsrfYUnEnvioNuevo() throws Exception {
        ServletFalso f = formulario();
        assertEquals(VISTA, f.reenvio());
        assertEquals("operacion", f.atributo("modulo"));
        assertNotNull(f.atributo("envio"));
        assertEquals(f.csrf(), f.atributo("csrf"));
        assertNull(f.atributo("mensaje"));
    }

    @Test
    void cadaCargaDelFormularioTraeUnEnvioDistinto() throws Exception {
        ServletFalso f = formulario();
        Object primero = f.atributo("envio");
        f.ejecutar(servlet);
        assertNotEquals(primero, f.atributo("envio"));
    }

    // ------------------------------------------------------------------ CSRF

    @Test
    void sinCampoCsrfResponde403YNoLlamaAlServicio() throws Exception {
        ServletFalso f = enviar(formulario(), null, "deposito", CUENTA, "", "150.00");
        assertEquals(403, f.estado());
        assertEquals(VISTA_ERROR, f.reenvio());
        assertNull(f.redireccion());
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "CSRF-FALSO", "x"})
    void unCsrfIncorrectoResponde403YNoLlamaAlServicio(String csrf) throws Exception {
        ServletFalso f = enviar(formulario(), csrf, "retiro", CUENTA, "", "150.00");
        assertEquals(403, f.estado());
        assertEquals(VISTA_ERROR, f.reenvio());
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void elCsrfDeOtraSesionNoSirve() throws Exception {
        ServletFalso f = formulario();
        ServletFalso otra = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("GET").ruta(RUTA);
        otra.ejecutar(servlet);
        servidor.llamadas.clear();
        ServletFalso enviado = enviar(f, otra.csrf(), "deposito", CUENTA, "", "150.00");
        assertEquals(403, enviado.estado());
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void unCsrfErroneoNoGastaElEnvio() throws Exception {
        ServletFalso f = formulario();
        enviar(f, "MAL", "deposito", CUENTA, "", "150.00");
        enviarBien(f, "deposito", CUENTA, "", "150.00");
        assertEquals(PRG, f.redireccion());
        assertEquals(1, llamadasDeOperacion(servidor));
    }

    @Test
    void unPostSinCamposConCsrfValidoDaErrorDeOperacionSinLlamarAlServicio() throws Exception {
        ServletFalso f = enviarBien(formulario(), null, null, null, null);
        assertEquals(PRG, f.redireccion());
        assertEquals("error", f.flash().get("tipo"));
        assertEquals("Elija una operación: depósito, retiro o transferencia", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    // ------------------------------------------------------------------ operaciones válidas (PRG)

    @Test
    void unDepositoValidoSeRegistraYVuelveAlFormularioConPRG() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", "150.00");
        assertEquals(PRG, f.redireccion());
        assertEquals(1, llamadasDeOperacion(servidor));
        assertTrue(servidor.llamadas.contains("realizarDeposito(" + CUENTA + ",150.00,TOKEN-1)"));
        Map<String, Object> mensaje = f.flash();
        assertEquals("exito", mensaje.get("tipo"));
        assertEquals("200", mensaje.get("codigo"));
        assertEquals("Depósito realizado correctamente", mensaje.get("texto"));
        assertEquals(new BigDecimal("6900.00"), mensaje.get("saldoNuevo"));
    }

    @Test
    void unRetiroValidoSeRegistraYMuestraElSaldoDelServidor() throws Exception {
        ServletFalso f = enviarBien(formulario(), "retiro", CUENTA, "", "250.5");
        assertEquals(PRG, f.redireccion());
        assertTrue(servidor.llamadas.contains("realizarRetiro(" + CUENTA + ",250.5,TOKEN-1)"));
        assertEquals("Retiro realizado correctamente", f.flash().get("texto"));
        assertEquals(new BigDecimal("6750.00"), f.flash().get("saldoNuevo"));
    }

    @Test
    void unaTransferenciaValidaMuestraLosDosSaldos() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, DESTINO, "100.00");
        assertEquals(PRG, f.redireccion());
        assertTrue(servidor.llamadas.contains("realizarTransferencia(" + CUENTA + "," + DESTINO + ",100.00,TOKEN-1)"));
        Map<String, Object> mensaje = f.flash();
        assertEquals("exito", mensaje.get("tipo"));
        assertEquals("Transferencia realizada correctamente", mensaje.get("texto"));
        assertEquals(new BigDecimal("6775.00"), mensaje.get("saldoOrigen"));
        assertEquals(new BigDecimal("7429.00"), mensaje.get("saldoDestino"));
        assertEquals(new BigDecimal("0.02"), mensaje.get("itfAplicado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "150", "150.5", "1500.50"})
    void losMontosDecimalesPlanosValidosLlegan(String monto) throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", monto);
        assertEquals(1, llamadasDeOperacion(servidor));
        assertEquals("exito", f.flash().get("tipo"));
    }

    @Test
    void laCuentaSeRecortaAntesDeLlamarAlServicio() throws Exception {
        enviarBien(formulario(), "deposito", "  " + CUENTA + "\t ", "", "150.00");
        assertEquals(1, llamadasDeOperacion(servidor));
        assertTrue(servidor.llamadas.contains("realizarDeposito(" + CUENTA + ",150.00,TOKEN-1)"));
    }

    @Test
    void recargarLaPaginaTrasUnaOperacionNoLaRepite() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", "150.00");
        f.metodo("GET").ruta(RUTA);
        f.ejecutar(servlet);
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("exito", mensaje.get("tipo"));
        f.ejecutar(servlet);
        assertNull(f.atributo("mensaje"));
        assertEquals(1, llamadasDeOperacion(servidor));
    }

    @Test
    void reenviarElMismoFormularioNoRepiteLaOperacion() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", "150.00");
        f.ejecutar(servlet);
        assertEquals(1, llamadasDeOperacion(servidor));
        assertEquals(PRG, f.redireccion());
        assertEquals("error", f.flash().get("tipo"));
        assertTrue(String.valueOf(f.flash().get("texto")).startsWith("Esta operación ya se envió"));
    }

    // ------------------------------------------------------------------ montos

    @ParameterizedTest
    @ValueSource(strings = {"1e3", "1E3", "NaN", "Infinity", "1e999", "abc", "15.5.5", "+5", ".5", "12 000", "--5"})
    void notacionCientificaNaNInfinitosYTextoNoLlegan(String monto) throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", monto);
        assertEquals(0, llamadasDeOperacion(servidor));
        assertEquals("error", f.flash().get("tipo"));
        assertEquals("", f.flash().get("codigo"));
        assertEquals(MONTO_FORMATO, f.flash().get("texto"));
        assertEquals(monto, f.flash().get("monto"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"150,00", "1,000.00", "1500,5"})
    void unMontoConComaSeRechazaSinLlamarAlServicio(String monto) throws Exception {
        ServletFalso f = enviarBien(formulario(), "retiro", CUENTA, "", monto);
        assertEquals(0, llamadasDeOperacion(servidor));
        assertEquals(MONTO_COMA, f.flash().get("texto"));
        assertEquals(monto, f.flash().get("monto"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void unMontoVacioPideElMonto(String monto) throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", monto);
        assertEquals(0, llamadasDeOperacion(servidor));
        assertEquals("Ingrese el monto", f.flash().get("texto"));
    }

    @Test
    void unMontoAusentePideElMonto() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", null);
        assertEquals("Ingrese el monto", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5", "-0.01", "0", "0.00", "10.125"})
    void losMontosQueRechazaElServicioLlegan_conElMensajeLiteral(String monto) throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", monto);
        assertEquals(1, llamadasDeOperacion(servidor));
        assertEquals("error", f.flash().get("tipo"));
        assertEquals("MON-001", f.flash().get("codigo"));
        assertEquals(MONTO_MON001, f.flash().get("texto"));
        assertEquals(monto, f.flash().get("monto"));
    }

    // ------------------------------------------------------------------ cuentas

    @Test
    void unaCuentaVaciaSePideAntesDeLlamarAlServicio() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", "  ", "", "150.00");
        assertEquals("Ingrese el código de la cuenta", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void enUnaTransferenciaLaCuentaVaciaEsLaDeOrigen() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", "", DESTINO, "10.00");
        assertEquals("Ingrese la cuenta de origen", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void unaCuentaMuyLargaNoLlega() throws Exception {
        ServletFalso largo = enviarBien(formulario(), "deposito", "9".repeat(65), "", "10.00");
        assertEquals(CUENTA_FORMATO, largo.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    /** Un control dentro de la cuenta se quita: el servicio recibe la cuenta limpia. */
    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "\t", "\r", "\n"})
    void unControlEnLaCuentaSeQuitaAntesDelServicio(String control) throws Exception {
        enviarBien(formulario(), "deposito", CUENTA.substring(0, 4) + control + CUENTA.substring(4), "", "10.00");
        assertEquals(1, llamadasDeOperacion(servidor));
        assertTrue(servidor.llamadas.contains("realizarDeposito(" + CUENTA + ",10.00,TOKEN-1)"));
    }

    /** Si la cuenta queda vacía tras quitar los controles, pide el código como cualquier campo vacío. */
    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "\t", "\r\n", " \u0000 "})
    void unaCuentaDeSoloControlesPideElCodigoYSeDevuelveLimpia(String valor) throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", valor, "", "10.00");
        assertEquals("Ingrese el código de la cuenta", f.flash().get("texto"));
        assertEquals("", f.flash().get("cuenta"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void unControlEnElMontoSeQuitaYElMontoDevueltoVaLimpio() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", CUENTA, "", "abc\u0000");
        assertEquals("abc", f.flash().get("monto"));
        assertEquals(0, llamadasDeOperacion(servidor));
        ServletFalso bien = enviarBien(formulario(), "deposito", CUENTA, "", "10\t.00");
        assertEquals(1, llamadasDeOperacion(servidor));
        assertTrue(servidor.llamadas.stream().anyMatch(l -> l.startsWith("realizarDeposito(" + CUENTA + ",10.00,")));
        assertNotNull(bien.flash());
    }

    @Test
    void unControlEnElDestinoSeDevuelveLimpioAlFormulario() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, "\u0000", "10.00");
        assertEquals("Ingrese la cuenta de destino", f.flash().get("texto"));
        assertEquals("", f.flash().get("destino"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void laLongitudMaximaDeCuentaSiLlega() throws Exception {
        enviarBien(formulario(), "deposito", "9".repeat(64), "", "10.00");
        assertEquals(1, llamadasDeOperacion(servidor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../etc/passwd", "a/b", "a?b=c", "con espacios dentro"})
    void losValoresRarosVanAlServicioSinInterpretarse(String cuenta) throws Exception {
        enviarBien(formulario(), "deposito", cuenta, "", "10.00");
        assertTrue(servidor.llamadas.contains("realizarDeposito(" + cuenta + ",10.00,TOKEN-1)"));
    }

    @Test
    void unaCuentaConHtmlLlegaIntactaParaQueLaVistaLaEscape() throws Exception {
        ServletFalso f = enviarBien(formulario(), "deposito", HOSTIL, "", "abc");
        assertEquals(HOSTIL, f.flash().get("cuenta"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    // ------------------------------------------------------------------ transferencias

    @Test
    void laTransferenciaSinDestinoPideLaCuentaDeDestino() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, "", "10.00");
        assertEquals("Ingrese la cuenta de destino", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void elDestinoConFormatoInvalidoNoLlega() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, "9".repeat(65), "10.00");
        assertEquals("El código de la cuenta de destino no tiene un formato válido", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void elDestinoConHtmlLlegaIntactoParaQueLaVistaLoEscape() throws Exception {
        String hostil = "\"><img src=x onerror=alert(1)>";
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, hostil, "abc");
        assertEquals(hostil, f.flash().get("destino"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    @Test
    void unModoDesconocidoNoLlamaAlServicio() throws Exception {
        ServletFalso f = enviarBien(formulario(), "pago", CUENTA, "", "10.00");
        assertEquals("Elija una operación: depósito, retiro o transferencia", f.flash().get("texto"));
        assertEquals(0, llamadasDeOperacion(servidor));
    }

    // ------------------------------------------------------------------ errores de negocio

    @Test
    void unRetiroMayorAlSaldoMuestraSal001Literal() throws Exception {
        ServletFalso f = enviarBien(formulario(), "retiro", CUENTA, "", "10000.01");
        assertEquals("SAL-001", f.flash().get("codigo"));
        assertEquals("El saldo de la cuenta es insuficiente para la operación", f.flash().get("texto"));
        assertEquals("10000.01", f.flash().get("monto"));
        assertEquals("error", f.flash().get("tipo"));
    }

    @Test
    void unaTransferenciaAUnaMismaCuentaMuestraCta003Literal() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, CUENTA, "10.00");
        assertEquals("CTA-003", f.flash().get("codigo"));
        assertEquals("La cuenta origen y la cuenta destino no pueden ser iguales", f.flash().get("texto"));
        assertEquals("transferencia", f.flash().get("modo"));
        assertEquals(CUENTA, f.flash().get("destino"));
    }

    @Test
    void unaTransferenciaAOtraMonedaMuestraCta004Literal() throws Exception {
        ServletFalso f = enviarBien(formulario(), "transferencia", CUENTA, ClienteFalso.CUENTA_DOLARES, "10.00");
        assertEquals("CTA-004", f.flash().get("codigo"));
        assertEquals("La cuenta de destino pertenece a una moneda distinta", f.flash().get("texto"));
    }

    @Test
    void unaCuentaNoActivaMuestraCta002LiteralDelServidor() throws Exception {
        ClienteFalso inactiva = new ClienteFalso() {
            @Override
            public RespuestaMovimiento realizarDeposito(String cuenta, BigDecimal monto, String token) {
                llamadas.add("realizarDeposito(" + cuenta + "," + monto + "," + token + ")");
                return movimientoConError("CTA-002", "La cuenta no se encuentra en estado ACTIVO");
            }
        };
        ServletFalso f = enviarBien(formulario(inactiva), "deposito", CUENTA, "", "150.00");
        assertEquals("CTA-002", f.flash().get("codigo"));
        assertEquals("La cuenta no se encuentra en estado ACTIVO", f.flash().get("texto"));
        assertEquals(1, llamadasDeOperacion(inactiva));
    }

    @Test
    void unMensajeHostilDelServidorLlegaIntactoParaQueLaVistaLoEscape() throws Exception {
        ClienteFalso hostil = new ClienteFalso() {
            @Override
            public RespuestaMovimiento realizarRetiro(String cuenta, BigDecimal monto, String token) {
                llamadas.add("realizarRetiro(" + cuenta + "," + monto + "," + token + ")");
                return movimientoConError("CTA-002", HOSTIL);
            }
        };
        ServletFalso f = enviarBien(formulario(hostil), "retiro", CUENTA, "", "5.00");
        assertEquals(HOSTIL, f.flash().get("texto"));
        assertEquals(PRG, f.redireccion());
    }

    // ------------------------------------------------------------------ sesión y servicio caído

    @Test
    void auth003InvalidaLaSesionYVuelveAlLoginConElMensajeDelServidor() throws Exception {
        ServletFalso f = formulario();
        servidor.perderSesion();
        enviarBien(f, "deposito", CUENTA, "", "150.00");
        assertEquals(LOGIN, f.redireccion());
        assertEquals(1, f.sesionesInvalidadas());
        assertNull(f.usuario());
        assertEquals("AUTH-003", f.flash().get("codigo"));
        assertEquals("El token de sesión no es válido", f.flash().get("texto"));
    }

    @Test
    void auth004InvalidaLaSesionYVuelveAlLoginConElMensajeDelServidor() throws Exception {
        ServletFalso f = formulario();
        servidor.vencerSesion();
        enviarBien(f, "retiro", CUENTA, "", "150.00");
        assertEquals(LOGIN, f.redireccion());
        assertEquals(1, f.sesionesInvalidadas());
        assertEquals("AUTH-004", f.flash().get("codigo"));
        assertEquals("El token de sesión ha expirado, inicie sesión nuevamente", f.flash().get("texto"));
    }

    @Test
    void conElServidorApagadoSeMuestraSer001SinTraza() throws Exception {
        ServletFalso f = formulario();
        servidor.servidorApagado = true;
        enviarBien(f, "transferencia", CUENTA, DESTINO, "10.00");
        assertEquals(PRG, f.redireccion());
        assertEquals("SER-001", f.flash().get("codigo"));
        assertEquals("El servicio no está disponible en este momento", f.flash().get("texto"));
        assertFalse(String.valueOf(f.flash()).contains("Exception"));
        assertFalse(String.valueOf(f.flash()).contains("http"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void losMetodosQueNoSonGetNiPostDan405(String verbo) throws Exception {
        ServletFalso f = formulario();
        f.metodo(verbo);
        f.ejecutar(servlet);
        assertEquals(405, f.errorEnviado());
        assertEquals(0, llamadasDeOperacion(servidor));
    }
}
