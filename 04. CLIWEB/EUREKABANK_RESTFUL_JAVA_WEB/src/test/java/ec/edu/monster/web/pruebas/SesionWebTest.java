package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.SesionUsuario;
import ec.edu.monster.web.util.SesionWeb;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** La sesión web guarda el token solo en la HttpSession y protege contra fijación y CSRF. */
class SesionWebTest {

    private static SesionUsuario usuario() {
        return new SesionUsuario("TOKEN-X", "0001", "Carlos Romero", "2026-11-03T12:15:00Z", "Consultas");
    }

    @Test
    void sinSesionHttpNoHayUsuarioNiToken() {
        ServletFalso f = ServletFalso.crear();
        assertNull(SesionWeb.sesionDe(f.peticion()));
        assertNull(SesionWeb.tokenDe(f.peticion()));
        assertFalse(f.haySesion(), "consultar no debe crear la sesión");
    }

    @Test
    void iniciarGuardaLaSesionYCambiaElIdentificador() {
        ServletFalso f = ServletFalso.crear().conSesionAnonima();
        String antes = f.idDeSesion();
        SesionUsuario u = usuario();
        SesionWeb.iniciar(f.peticion(), u);
        assertEquals(1, f.cambiosDeId());
        assertNotEquals(antes, f.idDeSesion());
        assertSame(u, SesionWeb.sesionDe(f.peticion()));
        assertEquals("TOKEN-X", SesionWeb.tokenDe(f.peticion()));
    }

    @Test
    void iniciarSinSesionPreviaLaCreaYCambiaElIdentificador() {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.iniciar(f.peticion(), usuario());
        assertTrue(f.haySesion());
        assertEquals(1, f.cambiosDeId());
    }

    @Test
    void iniciarRenuevaElTokenCsrf() {
        ServletFalso f = ServletFalso.crear().conSesionAnonima();
        String anonimo = SesionWeb.tokenCsrf(f.peticion());
        SesionWeb.iniciar(f.peticion(), usuario());
        assertNotEquals(anonimo, f.csrf());
        assertFalse(f.csrf().isEmpty());
    }

    @Test
    void iniciarExigeUsuario() {
        ServletFalso f = ServletFalso.crear();
        assertThrows(NullPointerException.class, () -> SesionWeb.iniciar(f.peticion(), null));
    }

    @Test
    void elTokenDelServicioSoloVivePorDentroDeLaSesionHttp() {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.iniciar(f.peticion(), usuario());
        assertNull(f.atributo("token"));
        assertNull(f.atributo("usuario"));
        assertNull(f.cabecera("Set-Cookie"));
        assertEquals("TOKEN-X", f.usuario().getToken());
    }

    @Test
    void unUsuarioSinTokenNoCuentaComoSesion() {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.iniciar(f.peticion(), usuario());
        f.usuario().limpiar();
        assertNull(SesionWeb.tokenDe(f.peticion()));
    }

    @Test
    void cerrarInvalidaLaSesionHttp() {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.iniciar(f.peticion(), usuario());
        SesionWeb.cerrar(f.peticion());
        assertEquals(1, f.sesionesInvalidadas());
        assertNull(SesionWeb.tokenDe(f.peticion()));
    }

    @Test
    void cerrarSinSesionNoHaceNada() {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.cerrar(f.peticion());
        assertEquals(0, f.sesionesCreadas());
    }

    @Test
    void esSesionCaducadaSoloSiHabiaCookieDeUnaSesionPerdida() {
        assertFalse(SesionWeb.esSesionCaducada(ServletFalso.crear().peticion()));
        assertTrue(SesionWeb.esSesionCaducada(ServletFalso.crear().conSesionCaducada().peticion()));
        assertFalse(SesionWeb.esSesionCaducada(ServletFalso.crear().conSesionAnonima().peticion()));
    }

    // ------------------------------------------------------------------ CSRF

    @Test
    void elTokenCsrfEsAleatorioYSeMantieneEnLaSesion() {
        ServletFalso f = ServletFalso.crear();
        String primero = SesionWeb.tokenCsrf(f.peticion());
        assertTrue(primero.length() >= 40, "debe tener al menos 256 bits de entropía");
        assertEquals(primero, SesionWeb.tokenCsrf(f.peticion()));
        assertTrue(primero.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void cadaSesionTieneSuPropioTokenCsrf() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            vistos.add(SesionWeb.tokenCsrf(ServletFalso.crear().peticion()));
        }
        assertEquals(50, vistos.size());
    }

    @Test
    void csrfValidoConElTokenDeLaSesion() {
        ServletFalso f = ServletFalso.crear().conSesionAnonima();
        SesionWeb.tokenCsrf(f.peticion());
        f.conCsrfCorrecto();
        assertTrue(SesionWeb.csrfValido(f.peticion()));
    }

    @Test
    void csrfInvalidoSinSesion() {
        ServletFalso f = ServletFalso.crear().parametro("_csrf", "cualquiera");
        assertFalse(SesionWeb.csrfValido(f.peticion()));
    }

    @Test
    void csrfInvalidoSinElCampo() {
        ServletFalso f = ServletFalso.crear().conSesionAnonima();
        SesionWeb.tokenCsrf(f.peticion());
        assertFalse(SesionWeb.csrfValido(f.peticion()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "otro", "AAAA", "<script>", "%00"})
    void csrfInvalidoConUnValorIncorrecto(String valor) {
        ServletFalso f = ServletFalso.crear().conSesionAnonima();
        SesionWeb.tokenCsrf(f.peticion());
        if (valor != null) {
            f.parametro("_csrf", valor);
        }
        assertFalse(SesionWeb.csrfValido(f.peticion()));
    }

    @Test
    void csrfInvalidoSiSoloCoincideElPrefijo() {
        ServletFalso f = ServletFalso.crear().conSesionAnonima();
        String token = SesionWeb.tokenCsrf(f.peticion());
        f.parametro("_csrf", token.substring(0, token.length() - 1));
        assertFalse(SesionWeb.csrfValido(f.peticion()));
        ServletFalso g = ServletFalso.crear().conSesionAnonima();
        String t2 = SesionWeb.tokenCsrf(g.peticion());
        g.parametro("_csrf", t2 + "x");
        assertFalse(SesionWeb.csrfValido(g.peticion()));
    }

    @Test
    void elTokenDeOtraSesionNoSirve() {
        ServletFalso a = ServletFalso.crear().conSesionAnonima();
        String ajeno = SesionWeb.tokenCsrf(a.peticion());
        ServletFalso b = ServletFalso.crear().conSesionAnonima();
        SesionWeb.tokenCsrf(b.peticion());
        b.parametro("_csrf", ajeno);
        assertFalse(SesionWeb.csrfValido(b.peticion()));
    }

    @Test
    void denegarResponde403SinLlamarAlServicio() throws Exception {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.denegar(f.peticion(), f.respuesta());
        assertEquals(403, f.estado());
        assertEquals("/WEB-INF/vistas/error.jsp", f.reenvio());
        assertNotNull(f.atributo("detalle"));
    }

    // ------------------------------------------------------------------ mensaje de una sola vez

    @Test
    void elMensajeSeMuestraUnaSolaVez() {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.guardarMensaje(f.peticion(), SesionWeb.mensajeExito("200", "Listo"));
        Map<String, Object> primero = SesionWeb.tomarMensaje(f.peticion());
        assertEquals("Listo", primero.get("texto"));
        assertNull(SesionWeb.tomarMensaje(f.peticion()));
    }

    @Test
    void sinSesionNoHayMensaje() {
        ServletFalso f = ServletFalso.crear();
        assertNull(SesionWeb.tomarMensaje(f.peticion()));
        assertFalse(f.haySesion());
    }

    @Test
    void mensajeDeUnSobreConservaElTextoLiteral() {
        RespuestaBase sobre = new RespuestaBase("ERROR", "SAL-001", "El saldo de la cuenta es insuficiente para la operación");
        Map<String, Object> m = SesionWeb.mensajeDe(sobre);
        assertEquals("error", m.get("tipo"));
        assertEquals("SAL-001", m.get("codigo"));
        assertEquals("El saldo de la cuenta es insuficiente para la operación", m.get("texto"));
    }

    @Test
    void mensajeDeUnSobreOkEsDeExito() {
        Map<String, Object> m = SesionWeb.mensajeDe(new RespuestaBase("OK", "200", "Depósito realizado correctamente"));
        assertEquals("exito", m.get("tipo"));
    }

    @Test
    void mensajeDeNuloEsUnErrorVacio() {
        Map<String, Object> m = SesionWeb.mensajeDe(null);
        assertEquals("error", m.get("tipo"));
        assertEquals("", m.get("codigo"));
        assertEquals("", m.get("texto"));
    }

    @Test
    void elMensajeDeServicioCaidoNoRevelaDetallesInternos() {
        Map<String, Object> m = SesionWeb.mensajeServicioCaido();
        assertEquals("SER-001", m.get("codigo"));
        assertEquals("El servicio no está disponible en este momento", m.get("texto"));
        assertFalse(String.valueOf(m).contains("http"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUTH-003", "AUTH-004"})
    void losCodigosDeTokenSonRechazoDeSesion(String codigo) {
        assertTrue(SesionWeb.esRechazoDeSesion(new RespuestaBase("ERROR", codigo, "x")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUTH-001", "AUTH-002", "CTA-001", "SAL-001", "SER-001", "200", ""})
    void otrosCodigosNoSonRechazoDeSesion(String codigo) {
        assertFalse(SesionWeb.esRechazoDeSesion(new RespuestaBase("ERROR", codigo, "x")));
    }

    @Test
    void nuloNoEsRechazoDeSesion() {
        assertFalse(SesionWeb.esRechazoDeSesion(null));
    }

    // ------------------------------------------------------------------ envíos únicos

    @Test
    void unIdentificadorDeEnvioSirveUnaSolaVez() {
        ServletFalso f = ServletFalso.crear();
        String id = SesionWeb.nuevoEnvio(f.peticion());
        assertTrue(SesionWeb.consumirEnvio(f.peticion(), id));
        assertFalse(SesionWeb.consumirEnvio(f.peticion(), id));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"inventado", " "})
    void unIdentificadorDesconocidoNoSirve(String id) {
        ServletFalso f = ServletFalso.crear();
        SesionWeb.nuevoEnvio(f.peticion());
        assertFalse(SesionWeb.consumirEnvio(f.peticion(), id));
    }

    @Test
    void sinSesionNingunEnvioSirve() {
        assertFalse(SesionWeb.consumirEnvio(ServletFalso.crear().peticion(), "x"));
    }

    @Test
    void varioFormulariosAbiertosConvivenPeroConLimite() {
        ServletFalso f = ServletFalso.crear();
        String primero = SesionWeb.nuevoEnvio(f.peticion());
        String segundo = SesionWeb.nuevoEnvio(f.peticion());
        assertTrue(SesionWeb.consumirEnvio(f.peticion(), segundo));
        assertTrue(SesionWeb.consumirEnvio(f.peticion(), primero));
        String viejo = SesionWeb.nuevoEnvio(f.peticion());
        for (int i = 0; i < SesionWeb.ENVIOS_PENDIENTES_MAXIMOS; i++) {
            SesionWeb.nuevoEnvio(f.peticion());
        }
        assertFalse(SesionWeb.consumirEnvio(f.peticion(), viejo), "el más antiguo se descarta");
    }

    // ------------------------------------------------------------------ entradas

    @Test
    void normalizarQuitaEspaciosYAceptaNulos() {
        assertEquals("", SesionWeb.normalizar(null));
        assertEquals("", SesionWeb.normalizar("   "));
        assertEquals("abc", SesionWeb.normalizar("  abc \t"));
        assertEquals("a b", SesionWeb.normalizar(" a b "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"06914897", "00200002", "a b", "../../x", "<script>alert(1)</script>", "ñandú"})
    void entradasSegurasAceptadas(String valor) {
        assertTrue(SesionWeb.esEntradaSegura(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a\u0000b", "a\nb", "a\rb", "a\tb", "\u001b[31m", "a\u007fb", "a\u0085b"})
    void entradasConCaracteresDeControlRechazadas(String valor) {
        assertFalse(SesionWeb.esEntradaSegura(valor));
    }

    @Test
    void laLongitudMaximaSeRespeta() {
        assertTrue(SesionWeb.esEntradaSegura("1".repeat(SesionWeb.LONGITUD_MAXIMA)));
        assertFalse(SesionWeb.esEntradaSegura("1".repeat(SesionWeb.LONGITUD_MAXIMA + 1)));
        assertFalse(SesionWeb.esEntradaSegura(null));
    }

    @Test
    void exigirRedirigeAlLoginSinUsuario() throws Exception {
        ServletFalso f = ServletFalso.crear();
        assertNull(SesionWeb.exigir(f.peticion(), f.respuesta()));
        assertEquals("/web/login", f.redireccion());
    }

    @Test
    void exigirDevuelveElUsuarioVigente() throws Exception {
        ServletFalso f = ServletFalso.crear().conUsuarioAutenticado(new ClienteFalso());
        assertNotNull(SesionWeb.exigir(f.peticion(), f.respuesta()));
        assertNull(f.redireccion());
    }

    @Test
    void prepararVistaDejaModuloUsuarioYCsrf() {
        ServletFalso f = ServletFalso.crear().conUsuarioAutenticado(new ClienteFalso());
        SesionWeb.prepararVista(f.peticion(), "cuentas");
        assertEquals("cuentas", f.atributo("modulo"));
        assertNotNull(f.atributo("usuario"));
        assertEquals(f.csrf(), f.atributo("csrf"));
    }
}
