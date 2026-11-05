package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.filtro.FiltroSesion;
import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El filtro exige sesión en todo salvo el login y los recursos estáticos, y pone las cabeceras
 * de seguridad en cada respuesta.
 */
class FiltroSesionTest {

    private final FiltroSesion filtro = new FiltroSesion();

    private void filtrar(ServletFalso f) throws Exception {
        filtro.doFilter(f.peticion(), f.respuesta(), f.cadena());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cuentas", "/movimientos", "/operacion", "/costos", "/logout", "/otra", "/login/x",
        "/WEB-INF/vistas/cuentas.jsp", "/cuentas.jsp"})
    void sinSesionLasRutasProtegidasVuelvenAlLogin(String ruta) throws Exception {
        ServletFalso f = ServletFalso.crear().ruta(ruta);
        filtrar(f);
        assertEquals("/web/login", f.redireccion());
        assertFalse(f.cadenaEjecutada(), "no debe llegar al servlet");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/css/app.css", "/img/Monster.png", "/img/marca.svg", "/js/app.js"})
    void sinSesionLasRutasPublicasPasan(String ruta) throws Exception {
        ServletFalso f = ServletFalso.crear().ruta(ruta);
        filtrar(f);
        assertTrue(f.cadenaEjecutada());
        assertNull(f.redireccion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cuentas", "/movimientos", "/operacion", "/costos", "/logout", "/login"})
    void conSesionTodoPasa(String ruta) throws Exception {
        ServletFalso f = ServletFalso.crear().ruta(ruta).conUsuarioAutenticado(new ClienteFalso());
        filtrar(f);
        assertTrue(f.cadenaEjecutada());
        assertNull(f.redireccion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/css", "/css/", "/img", "/imgx/a.png", "/login/", "/Login", "/js", "/cssx/app.css",
        "/css/../cuentas", "/img/../WEB-INF/web.xml", "/css/./app.css", "/css/%2e%2e/cuentas", "/img/..%2fcuentas",
        "/css\\..\\cuentas"})
    void rutasQueParecenPublicasPeroNoLoSonQuedanProtegidas(String ruta) {
        assertFalse(filtro.esRutaPublica(ruta), ruta);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rutaNulaOVaciaNoEsPublica(String ruta) {
        assertFalse(filtro.esRutaPublica(ruta));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/css/app.css", "/img/Monster.png", "/js/app.js", "/css/sub/x.css"})
    void rutasPublicasReconocidas(String ruta) {
        assertTrue(filtro.esRutaPublica(ruta), ruta);
    }

    @Test
    void elIdentificadorDeSesionEnLaRutaNoCambiaLaDecision() throws Exception {
        ServletFalso f = ServletFalso.crear().uriCruda("/web/login;jsessionid=ABC123");
        filtrar(f);
        assertTrue(f.cadenaEjecutada());
        ServletFalso g = ServletFalso.crear().uriCruda("/web/cuentas;jsessionid=ABC123");
        filtrar(g);
        assertEquals("/web/login", g.redireccion());
    }

    @Test
    void laRaizSinSesionVaAlLogin() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/");
        filtrar(f);
        assertEquals("/web/login", f.redireccion());
        assertFalse(f.cadenaEjecutada());
    }

    @Test
    void laRaizConSesionVaACuentas() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/").conUsuarioAutenticado(new ClienteFalso());
        filtrar(f);
        assertEquals("/web/cuentas", f.redireccion());
    }

    @Test
    void laRaizSinBarraTambienRedirige() throws Exception {
        ServletFalso f = ServletFalso.crear().uriCruda("/web");
        filtrar(f);
        assertEquals("/web/login", f.redireccion());
    }

    @Test
    void unaSesionAnonimaNoAbrePaginasProtegidas() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/cuentas").conSesionAnonima();
        filtrar(f);
        assertEquals("/web/login", f.redireccion());
        assertFalse(f.cadenaEjecutada());
    }

    @Test
    void unaSesionSinTokenNoAbrePaginasProtegidas() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/cuentas").conUsuarioAutenticado(new ClienteFalso());
        f.usuario().limpiar();
        filtrar(f);
        assertEquals("/web/login", f.redireccion());
        assertFalse(f.cadenaEjecutada());
    }

    @Test
    void unaSesionCaducadaVuelveAlLoginConElMensajeDelServidor() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/cuentas").conSesionCaducada();
        filtrar(f);
        assertEquals("/web/login", f.redireccion());
        assertNotNull(f.flash());
        assertEquals("AUTH-004", f.flash().get("codigo"));
        assertEquals("El token de sesión ha expirado, inicie sesión nuevamente", f.flash().get("texto"));
        assertEquals("error", f.flash().get("tipo"));
    }

    @Test
    void sinCookieNoSeInventaUnMensajeDeCaducidad() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/cuentas");
        filtrar(f);
        assertFalse(f.haySesion(), "no debe crear sesión si no hay nada que avisar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/cuentas", "/css/app.css", "/", "/img/marca.svg"})
    void todaRespuestaLlevaLasCabecerasDeSeguridad(String ruta) throws Exception {
        ServletFalso f = ServletFalso.crear().ruta(ruta);
        filtrar(f);
        assertEquals("nosniff", f.cabecera("X-Content-Type-Options"));
        assertEquals("DENY", f.cabecera("X-Frame-Options"));
        assertEquals("no-referrer", f.cabecera("Referrer-Policy"));
        assertEquals("default-src 'self'; img-src 'self' data:; style-src 'self'; frame-ancestors 'none'"
                + "; base-uri 'self'; form-action 'self'", f.cabecera("Content-Security-Policy"));
    }

    @Test
    void laPoliticaDeContenidoNoPermiteScriptsNiEstilosEnLinea() {
        String politica = FiltroSesion.POLITICA_CONTENIDO;
        assertFalse(politica.contains("unsafe-inline"));
        assertFalse(politica.contains("unsafe-eval"));
        assertFalse(politica.contains("http"));
        assertFalse(politica.contains("*"));
        assertTrue(politica.contains("frame-ancestors 'none'"));
        assertTrue(politica.contains("base-uri 'self'"));
        assertTrue(politica.contains("form-action 'self'"));
    }

    // ------------------------------------------------------------------ SameSite de JSESSIONID

    private static int veces(String texto, String fragmento) {
        return (texto.length() - texto.replace(fragmento, "").length()) / fragmento.length();
    }

    @Test
    void laCookieDeSesionLlevaSameSiteStrictUnaSolaVezAntesDeRedirigir() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/login");
        FilterChain cadena = (peticion, respuesta) -> {
            HttpServletResponse http = (HttpServletResponse) respuesta;
            http.addHeader("Set-Cookie", "JSESSIONID=abc123; Path=/web; HttpOnly");
            http.sendRedirect("/web/cuentas");
        };
        filtro.doFilter(f.peticion(), f.respuesta(), cadena);
        List<String> cookies = f.cabeceras("Set-Cookie");
        assertEquals(1, cookies.size());
        assertEquals(1, veces(cookies.get(0), "SameSite"));
        assertEquals("JSESSIONID=abc123; Path=/web; HttpOnly; SameSite=Strict", cookies.get(0));
        assertEquals("/web/cuentas", f.redireccion());
    }

    @Test
    void unSameSiteYaPuestoSeSustituyePorStrictSinDuplicar() {
        ServletFalso f = ServletFalso.crear();
        f.respuesta().addHeader("Set-Cookie", "JSESSIONID=abc; Path=/web; HttpOnly; SameSite=Lax");
        FiltroSesion.reforzarSameSite(f.respuesta());
        assertEquals(List.of("JSESSIONID=abc; Path=/web; HttpOnly; SameSite=Strict"), f.cabeceras("Set-Cookie"));
    }

    @Test
    void reforzarDosVecesNoDuplicaElAtributo() {
        ServletFalso f = ServletFalso.crear();
        f.respuesta().addHeader("Set-Cookie", "JSESSIONID=abc; Path=/web; HttpOnly");
        FiltroSesion.reforzarSameSite(f.respuesta());
        FiltroSesion.reforzarSameSite(f.respuesta());
        String cookie = f.cabecera("Set-Cookie");
        assertEquals(1, veces(cookie, "SameSite"));
        assertEquals("JSESSIONID=abc; Path=/web; HttpOnly; SameSite=Strict", cookie);
    }

    @Test
    void secureYHttpOnlyDeLaCookieSeConservan() {
        ServletFalso f = ServletFalso.crear();
        f.respuesta().addHeader("Set-Cookie", "JSESSIONID=abc; Path=/web; Secure; HttpOnly");
        FiltroSesion.reforzarSameSite(f.respuesta());
        String cookie = f.cabecera("Set-Cookie");
        assertTrue(cookie.contains("; Secure"));
        assertTrue(cookie.contains("; HttpOnly"));
        assertTrue(cookie.contains("; Path=/web"));
        assertEquals(1, veces(cookie, "SameSite=Strict"));
    }

    @Test
    void lasOtrasCookiesNoSeTocan() {
        ServletFalso f = ServletFalso.crear();
        f.respuesta().addHeader("Set-Cookie", "preferencia=1; Path=/");
        f.respuesta().addHeader("Set-Cookie", "JSESSIONID=abc; HttpOnly");
        FiltroSesion.reforzarSameSite(f.respuesta());
        assertEquals(List.of("preferencia=1; Path=/", "JSESSIONID=abc; HttpOnly; SameSite=Strict"),
                f.cabeceras("Set-Cookie"));
    }

    @Test
    void sinCookieDeSesionNoSeCreaNinguna() {
        ServletFalso f = ServletFalso.crear();
        FiltroSesion.reforzarSameSite(f.respuesta());
        assertNull(f.cabecera("Set-Cookie"));
    }

    @Test
    void unaRespuestaYaEnviadaNoSeModifica() throws Exception {
        ServletFalso f = ServletFalso.crear();
        f.respuesta().addHeader("Set-Cookie", "JSESSIONID=abc; HttpOnly");
        f.respuesta().sendError(500);
        FiltroSesion.reforzarSameSite(f.respuesta());
        assertEquals("JSESSIONID=abc; HttpOnly", f.cabecera("Set-Cookie"));
    }

    // ------------------------------------------------------------------ métodos no permitidos

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE", "PATCH", "OPTIONS", "TRACE"})
    void unMetodoNoPermitidoResponde405ConLaPaginaDeErrorYSinLlamarAlServlet(String verbo) throws Exception {
        ServletFalso f = ServletFalso.crear().metodo(verbo).ruta("/operacion")
                .conUsuarioAutenticado(new ClienteFalso());
        filtrar(f);
        assertEquals(405, f.estado());
        assertEquals("/WEB-INF/vistas/error.jsp", f.reenvio());
        assertFalse(f.cadenaEjecutada(), "no debe llegar al servlet");
        assertNull(f.redireccion());
        assertEquals("nosniff", f.cabecera("X-Content-Type-Options"));
        assertEquals(FiltroSesion.POLITICA_CONTENIDO, f.cabecera("Content-Security-Policy"));
    }

    @Test
    void unPutSinSesionResponde405NoLaRedireccionAlLogin() throws Exception {
        ServletFalso f = ServletFalso.crear().metodo("PUT").ruta("/operacion");
        filtrar(f);
        assertEquals(405, f.estado());
        assertNull(f.redireccion());
        assertEquals("/WEB-INF/vistas/error.jsp", f.reenvio());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST"})
    void losMetodosQueSeAtiendenPasanAlServlet(String verbo) throws Exception {
        ServletFalso f = ServletFalso.crear().metodo(verbo).ruta("/login");
        filtrar(f);
        assertTrue(f.cadenaEjecutada());
        assertFalse(f.reenvios().contains("/WEB-INF/vistas/error.jsp"));
    }

    @Test
    void lasPaginasDinamicasNoSeGuardanEnCache() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/cuentas").conUsuarioAutenticado(new ClienteFalso());
        filtrar(f);
        assertEquals("no-store", f.cabecera("Cache-Control"));
        ServletFalso login = ServletFalso.crear().ruta("/login");
        filtrar(login);
        assertEquals("no-store", login.cabecera("Cache-Control"));
    }

    @Test
    void losRecursosEstaticosSePuedenCachear() throws Exception {
        ServletFalso f = ServletFalso.crear().ruta("/css/app.css");
        filtrar(f);
        assertNull(f.cabecera("Cache-Control"));
    }

    @Test
    void unaPeticionQueNoEsHttpPasaSinTocarse() throws Exception {
        ServletRequest peticion = (ServletRequest) Proxy.newProxyInstance(
                ServletRequest.class.getClassLoader(), new Class<?>[] {ServletRequest.class}, (p, m, a) -> null);
        ServletResponse respuesta = (ServletResponse) Proxy.newProxyInstance(
                ServletResponse.class.getClassLoader(), new Class<?>[] {ServletResponse.class}, (p, m, a) -> null);
        boolean[] paso = {false};
        filtro.doFilter(peticion, respuesta, (rq, rs) -> paso[0] = true);
        assertTrue(paso[0]);
    }

    @Test
    void laVariableDeCaducidadNoDependeDeLaSesionDelUsuario() {
        ServletFalso f = ServletFalso.crear().conSesionCaducada();
        assertTrue(SesionWeb.esSesionCaducada(f.peticion()));
    }
}
