package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.http.HttpServlet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Casos comunes de los tres módulos de consulta (cuentas, movimientos y costos): mismo flujo,
 * mismos bordes. Cada módulo concreto dice su ruta, su parámetro y su llamada al servicio.
 */
abstract class ConsultaServletBase {

    final ClienteFalso servidor = new ClienteFalso();

    abstract HttpServlet servlet();

    abstract String ruta();

    abstract String parametro();

    abstract String modulo();

    abstract String vista();

    abstract String valorExistente();

    abstract String codigoInexistente();

    abstract String mensajeInexistente();

    /** Nombre del método del cliente falso que atiende este módulo. */
    abstract String metodoDelServicio();

    String llamadaEsperada(String valor, String token) {
        return metodoDelServicio() + "(" + valor + "," + token + ")";
    }

    ServletFalso peticion(String verbo) {
        return ServletFalso.crear().cliente(servidor).metodo(verbo).ruta(ruta()).conUsuarioAutenticado(servidor);
    }

    ServletFalso consulta(String valor) {
        return peticion("GET").parametro(parametro(), valor);
    }

    int llamadasAlServicioDeConsulta() {
        int total = 0;
        for (String llamada : servidor.llamadas) {
            if (llamada.startsWith(metodoDelServicio() + "(")) {
                total++;
            }
        }
        return total;
    }

    @Test
    void sinParametroSoloMuestraElFormulario() throws Exception {
        ServletFalso f = peticion("GET");
        f.ejecutar(servlet());
        assertEquals(vista(), f.reenvio());
        assertNull(f.atributo("resultado"));
        assertNull(f.atributo("mensaje"));
        assertNull(f.atributo("consulta"));
        assertEquals(0, llamadasAlServicioDeConsulta());
    }

    @Test
    void preparaLaVistaConModuloUsuarioYCsrf() throws Exception {
        ServletFalso f = peticion("GET");
        f.ejecutar(servlet());
        assertEquals(modulo(), f.atributo("modulo"));
        assertNotNull(f.atributo("usuario"));
        assertEquals(f.csrf(), f.atributo("csrf"));
    }

    @Test
    void unaConsultaCorrectaDejaElResultadoEnLaPeticion() throws Exception {
        ServletFalso f = consulta(valorExistente());
        f.ejecutar(servlet());
        assertEquals(vista(), f.reenvio());
        assertNotNull(f.atributo("resultado"));
        assertNull(f.atributo("mensaje"));
        assertEquals(valorExistente(), f.atributo("consulta"));
        assertTrue(servidor.llamadas.contains(llamadaEsperada(valorExistente(), "TOKEN-1")));
    }

    @Test
    void elTokenViajaAlServicioPeroNuncaVuelveALaPeticion() throws Exception {
        ServletFalso f = consulta(valorExistente());
        f.ejecutar(servlet());
        assertNull(f.atributo("token"));
        assertFalse(String.valueOf(f.atributo("usuario")).contains("TOKEN-1") && f.atributo("usuario") instanceof String);
        assertNull(f.redireccion());
    }

    @Test
    void elValorConEspaciosEnLosExtremosSeRecorta() throws Exception {
        ServletFalso f = consulta("  " + valorExistente() + "\t ");
        f.ejecutar(servlet());
        assertTrue(servidor.llamadas.contains(llamadaEsperada(valorExistente(), "TOKEN-1")));
        assertNotNull(f.atributo("resultado"));
    }

    @Test
    void unValorInexistenteMuestraElMensajeLiteralDelServidor() throws Exception {
        ServletFalso f = consulta("99999999");
        f.ejecutar(servlet());
        assertNull(f.atributo("resultado"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertNotNull(mensaje);
        assertEquals(codigoInexistente(), mensaje.get("codigo"));
        assertEquals(mensajeInexistente(), mensaje.get("texto"));
        assertEquals("error", mensaje.get("tipo"));
        assertEquals(vista(), f.reenvio());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " \t\n "})
    void unValorVacioNoLlamaAlServicio(String valor) throws Exception {
        ServletFalso f = consulta(valor);
        f.ejecutar(servlet());
        assertEquals(0, llamadasAlServicioDeConsulta());
        assertNull(f.atributo("resultado"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertNotNull(mensaje);
        assertEquals("error", mensaje.get("tipo"));
        assertEquals(vista(), f.reenvio());
    }

    /** Los caracteres de control se quitan: el servicio y el formulario reciben el valor limpio. */
    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "\t", "\r", "\n", "\r\n", "\u007f", "\u001b", "\u0001"})
    void unCaracterDeControlSeQuitaAntesDelServicioYDelFormulario(String control) throws Exception {
        String valor = valorExistente().substring(0, 2) + control + valorExistente().substring(2);
        ServletFalso f = consulta(valor);
        f.ejecutar(servlet());
        assertTrue(servidor.llamadas.contains(llamadaEsperada(valorExistente(), "TOKEN-1")));
        assertEquals(valorExistente(), f.atributo("consulta"));
        assertNotNull(f.atributo("resultado"));
        assertNull(f.atributo("mensaje"));
    }

    /** Si tras quitar los controles no queda nada, es el mensaje de campo vacío. */
    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "\t", "\r\n", " \u0000\t ", "\u007f\u0001"})
    void unValorDeSoloControlesQuedaVacioYNoLlamaAlServicio(String valor) throws Exception {
        ServletFalso f = consulta(valor);
        f.ejecutar(servlet());
        assertEquals(0, llamadasAlServicioDeConsulta());
        assertNull(f.atributo("resultado"));
        assertEquals("", f.atributo("consulta"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertNotNull(mensaje);
        assertEquals("error", mensaje.get("tipo"));
        assertEquals("", mensaje.get("codigo"));
        assertEquals(vista(), f.reenvio());
    }

    @Test
    void unValorMuyLargoNoLlamaAlServicio() throws Exception {
        ServletFalso f = consulta("9".repeat(65));
        f.ejecutar(servlet());
        assertEquals(0, llamadasAlServicioDeConsulta());
        assertNotNull(f.atributo("mensaje"));
        assertNull(f.atributo("resultado"));
    }

    @Test
    void unValorDeLaLongitudMaximaSiSeConsulta() throws Exception {
        ServletFalso f = consulta("9".repeat(64));
        f.ejecutar(servlet());
        assertEquals(1, llamadasAlServicioDeConsulta());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../etc/passwd", "..", "../", "%2e%2e%2f", "a/b", "a?b=c", "a#b", "1;2", "a&b=c", "con espacios dentro"})
    void losValoresRarosVanAlServicioSinInterpretarseYSinRomperNada(String valor) throws Exception {
        ServletFalso f = consulta(valor);
        f.ejecutar(servlet());
        assertEquals(vista(), f.reenvio());
        assertEquals(1, llamadasAlServicioDeConsulta());
        assertTrue(servidor.llamadas.contains(llamadaEsperada(valor, "TOKEN-1")));
        assertNull(f.atributo("resultado"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals(codigoInexistente(), mensaje.get("codigo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<script>alert(1)</script>", "\"><img src=x onerror=alert(1)>", "'\"</td><script>x()</script>"})
    void unValorConHtmlSeDejaIntactoParaQueLaVistaLoEscape(String valor) throws Exception {
        ServletFalso f = consulta(valor);
        f.ejecutar(servlet());
        assertEquals(valor, f.atributo("consulta"), "el servlet no altera el texto: la vista lo escapa con c:out");
        assertNull(f.reenvio() == null ? "sin reenvio" : null);
    }

    @Test
    void elMensajeDelServidorConHtmlSeDejaIntactoParaQueLaVistaLoEscape() throws Exception {
        ClienteFalso hostil = new ClienteFalso() {
            @Override
            public ec.edu.monster.web.modelo.RespuestaCuentas listarCuentasPorCliente(String c, String t) {
                ec.edu.monster.web.modelo.RespuestaCuentas r = new ec.edu.monster.web.modelo.RespuestaCuentas(
                        "ERROR", "CLI-001", "<script>alert('x')</script>");
                return r;
            }

            @Override
            public ec.edu.monster.web.modelo.RespuestaMovimientos consultarMovimientos(String c, String t) {
                return new ec.edu.monster.web.modelo.RespuestaMovimientos("ERROR", "CTA-001", "<script>alert('x')</script>");
            }

            @Override
            public ec.edu.monster.web.modelo.RespuestaCostos consultarCostosMantenimiento(String c, String t) {
                return new ec.edu.monster.web.modelo.RespuestaCostos("ERROR", "CTA-001", "<script>alert('x')</script>");
            }
        };
        ServletFalso f = ServletFalso.crear().cliente(hostil).metodo("GET").ruta(ruta())
                .conUsuarioConToken("TOKEN-X").parametro(parametro(), "1");
        f.ejecutar(servlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("<script>alert('x')</script>", mensaje.get("texto"));
        assertEquals(vista(), f.reenvio());
    }

    @Test
    void auth003InvalidaLaSesionYVuelveAlLoginConElMensajeDelServidor() throws Exception {
        ServletFalso f = consulta(valorExistente());
        servidor.perderSesion();
        f.ejecutar(servlet());
        assertEquals("/web/login", f.redireccion());
        assertNull(f.reenvio());
        assertEquals(1, f.sesionesInvalidadas());
        assertNull(f.usuario());
        assertEquals("AUTH-003", f.flash().get("codigo"));
        assertEquals("El token de sesión no es válido", f.flash().get("texto"));
    }

    @Test
    void auth004InvalidaLaSesionYVuelveAlLoginConElMensajeDelServidor() throws Exception {
        ServletFalso f = consulta(valorExistente());
        servidor.vencerSesion();
        f.ejecutar(servlet());
        assertEquals("/web/login", f.redireccion());
        assertEquals(1, f.sesionesInvalidadas());
        assertEquals("AUTH-004", f.flash().get("codigo"));
        assertEquals("El token de sesión ha expirado, inicie sesión nuevamente", f.flash().get("texto"));
    }

    @Test
    void despuesDeUnRechazoLaSesionNoSirveParaNada() throws Exception {
        ServletFalso f = consulta(valorExistente());
        servidor.perderSesion();
        f.ejecutar(servlet());
        assertNull(SesionWeb.tokenDe(f.peticion()));
    }

    @Test
    void conElServidorApagadoSeMuestraSer001Y_nuncaUnaTraza() throws Exception {
        ServletFalso f = consulta(valorExistente());
        servidor.servidorApagado = true;
        f.ejecutar(servlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("SER-001", mensaje.get("codigo"));
        assertEquals("El servicio no está disponible en este momento", mensaje.get("texto"));
        assertFalse(String.valueOf(mensaje).contains("http"));
        assertFalse(String.valueOf(mensaje).contains("Exception"));
        assertEquals(vista(), f.reenvio());
        assertEquals(200, f.estado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void losMetodosQueNoSonGetDan405(String verbo) throws Exception {
        ServletFalso f = peticion(verbo).parametro(parametro(), valorExistente());
        f.ejecutar(servlet());
        assertEquals(405, f.errorEnviado());
        assertEquals(0, llamadasAlServicioDeConsulta());
    }

    @Test
    void sinSesionVuelveAlLoginSinLlamarAlServicio() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).metodo("GET").ruta(ruta()).parametro(parametro(), valorExistente());
        f.ejecutar(servlet());
        assertEquals("/web/login", f.redireccion());
        assertNull(f.reenvio());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void conUnaSesionSinTokenVuelveAlLogin() throws Exception {
        ServletFalso f = consulta(valorExistente());
        f.usuario().limpiar();
        servidor.llamadas.clear();
        f.ejecutar(servlet());
        assertEquals("/web/login", f.redireccion());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void unMensajePendienteDeLaSesionSeMuestraUnaVez() throws Exception {
        ServletFalso f = peticion("GET");
        SesionWeb.guardarMensaje(f.peticion(), SesionWeb.mensajeExito("200", "Listo"));
        f.ejecutar(servlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("Listo", mensaje.get("texto"));
        assertNull(f.flash());
    }

    @Test
    void dosConsultasSeguidasLlamanDosVeces() throws Exception {
        ServletFalso f = consulta(valorExistente());
        f.ejecutar(servlet());
        f.ejecutar(servlet());
        assertEquals(2, llamadasAlServicioDeConsulta());
    }

    @Test
    void laConsultaNoCambiaElIdentificadorDeSesionNiElCsrf() throws Exception {
        ServletFalso f = consulta(valorExistente());
        String csrf = f.csrf();
        int cambios = f.cambiosDeId();
        f.ejecutar(servlet());
        assertEquals(csrf, f.csrf());
        assertEquals(cambios, f.cambiosDeId());
    }
}
