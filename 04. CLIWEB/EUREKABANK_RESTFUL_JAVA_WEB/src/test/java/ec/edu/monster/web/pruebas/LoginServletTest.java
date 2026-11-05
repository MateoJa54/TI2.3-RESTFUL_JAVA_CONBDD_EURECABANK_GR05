package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.modelo.RespuestaAutenticacion;
import ec.edu.monster.web.servlet.LoginServlet;
import ec.edu.monster.web.util.SesionWeb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Inicio de sesión: formulario, CSRF, fijación de sesión y mensajes literales del servidor. */
class LoginServletTest {

    private final ClienteFalso servidor = new ClienteFalso();
    private final LoginServlet servlet = new LoginServlet();

    private ServletFalso nuevo(String metodo) {
        return ServletFalso.crear().cliente(servidor).metodo(metodo).ruta("/login");
    }

    /** POST /login con una sesión anónima y el token CSRF correcto. */
    private ServletFalso publicar(String usuario, String clave) {
        ServletFalso f = nuevo("POST").conSesionAnonima();
        SesionWeb.tokenCsrf(f.peticion());
        f.conCsrfCorrecto();
        if (usuario != null) {
            f.parametro("usuario", usuario);
        }
        if (clave != null) {
            f.parametro("clave", clave);
        }
        return f;
    }

    // ------------------------------------------------------------------ GET

    @Test
    void getMuestraElFormularioConElTokenCsrf() throws Exception {
        ServletFalso f = nuevo("GET");
        f.ejecutar(servlet);
        assertEquals("/WEB-INF/vistas/login.jsp", f.reenvio());
        assertEquals(200, f.estado());
        String csrf = (String) f.atributo("csrf");
        assertNotNull(csrf);
        assertEquals(csrf, f.csrf());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void getNoPonePosibleTokenDelServicioEnLaPeticion() throws Exception {
        ServletFalso f = nuevo("GET");
        f.ejecutar(servlet);
        assertNull(f.atributo("token"));
        assertNull(f.atributo("usuario"));
    }

    @Test
    void getConSesionIniciadaVaACuentas() throws Exception {
        ServletFalso f = nuevo("GET").conUsuarioAutenticado(servidor);
        f.ejecutar(servlet);
        assertEquals("/web/cuentas", f.redireccion());
        assertNull(f.reenvio());
    }

    @Test
    void getMuestraYConsumeElMensajePendiente() throws Exception {
        ServletFalso f = nuevo("GET").conSesionAnonima();
        SesionWeb.guardarMensaje(f.peticion(), SesionWeb.mensajeError("AUTH-001", "El usuario o la contraseña son incorrectos"));
        f.ejecutar(servlet);
        assertNotNull(f.atributo("mensaje"));
        assertNull(f.flash(), "el mensaje se muestra una sola vez");
    }

    @Test
    void getDosVecesDaElMismoTokenCsrf() throws Exception {
        ServletFalso f = nuevo("GET");
        f.ejecutar(servlet);
        String primero = (String) f.atributo("csrf");
        f.ejecutar(servlet);
        assertEquals(primero, f.atributo("csrf"));
    }

    // ------------------------------------------------------------------ CSRF

    @Test
    void postSinTokenCsrfDa403YNoLlamaAlServicio() throws Exception {
        ServletFalso f = nuevo("POST").conSesionAnonima().parametro("usuario", "cromero").parametro("clave", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals(403, f.estado());
        assertEquals("/WEB-INF/vistas/error.jsp", f.reenvio());
        assertTrue(servidor.llamadas.isEmpty());
        assertNull(f.redireccion());
    }

    @Test
    void postConTokenCsrfIncorrectoDa403YNoLlamaAlServicio() throws Exception {
        ServletFalso f = nuevo("POST").conSesionAnonima().parametro("_csrf", "falso")
                .parametro("usuario", "cromero").parametro("clave", "Cromero2026");
        SesionWeb.tokenCsrf(f.peticion());
        f.ejecutar(servlet);
        assertEquals(403, f.estado());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void postSinSesionDa403() throws Exception {
        ServletFalso f = nuevo("POST").parametro("_csrf", "x").parametro("usuario", "cromero").parametro("clave", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals(403, f.estado());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void elRechazoCsrfNoRefleja_laClave() throws Exception {
        ServletFalso f = nuevo("POST").conSesionAnonima().parametro("usuario", "cromero").parametro("clave", "Cromero2026");
        f.ejecutar(servlet);
        assertFalse(todoLoQueQuedoEnLaPeticion(f).contains("Cromero2026"));
    }

    // ------------------------------------------------------------------ autenticación correcta

    @Test
    void loginCorrectoRedirigeACuentas() throws Exception {
        ServletFalso f = publicar("cromero", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("/web/cuentas", f.redireccion());
        assertNull(f.reenvio());
    }

    @Test
    void loginCorrectoCambiaElIdentificadorDeSesion() throws Exception {
        ServletFalso f = publicar("cromero", "Cromero2026");
        String antes = f.idDeSesion();
        f.ejecutar(servlet);
        assertEquals(1, f.cambiosDeId());
        assertNotEquals(antes, f.idDeSesion());
    }

    @Test
    void loginCorrectoRenuevaElTokenCsrf() throws Exception {
        ServletFalso f = publicar("cromero", "Cromero2026");
        String antes = f.csrf();
        f.ejecutar(servlet);
        assertNotEquals(antes, f.csrf());
    }

    @Test
    void loginCorrectoGuardaElTokenSoloEnLaSesionHttp() throws Exception {
        ServletFalso f = publicar("cromero", "Cromero2026");
        f.ejecutar(servlet);
        assertNotNull(f.usuario());
        assertEquals("TOKEN-1", f.usuario().getToken());
        assertEquals("Carlos Romero", f.usuario().getNombreEmpleado());
        assertEquals("Consultas", f.usuario().getRol());
        assertNull(f.atributo("token"));
        assertNull(f.atributo("usuario"));
        assertFalse(f.redireccion().contains("TOKEN"), "el token no va en la URL");
        assertNull(f.cabecera("Set-Cookie"), "el token no va en una cookie propia");
        assertFalse(todoLoQueQuedoEnLaPeticion(f).contains("TOKEN-1"));
    }

    @Test
    void elServicioRecibeElHashYNuncaLaClaveEnClaro() throws Exception {
        ServletFalso f = publicar("cromero", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals(1, servidor.llamadas.size());
        String llamada = servidor.llamadas.get(0);
        assertTrue(llamada.contains(ClienteFalso.HASH_CLAVE));
        assertFalse(llamada.contains("Cromero2026"));
    }

    @Test
    void elUsuarioConEspaciosEnLosExtremosSeRecorta() throws Exception {
        ServletFalso f = publicar("  cromero \t", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("/web/cuentas", f.redireccion());
        assertTrue(servidor.llamadas.get(0).startsWith("autenticar(cromero,"));
    }

    @Test
    void laClaveNoSeRecorta() throws Exception {
        ServletFalso f = publicar("cromero", " Cromero2026 ");
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("AUTH-001", f.flash().get("codigo"));
    }

    // ------------------------------------------------------------------ autenticación fallida

    @Test
    void claveIncorrectaMuestraElMensajeLiteralDelServidor() throws Exception {
        ServletFalso f = publicar("cromero", "incorrecta");
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("AUTH-001", f.flash().get("codigo"));
        assertEquals("El usuario o la contraseña son incorrectos", f.flash().get("texto"));
        assertEquals("error", f.flash().get("tipo"));
        assertNull(f.usuario());
        assertEquals(0, f.cambiosDeId(), "sin éxito no se abre sesión");
    }

    @Test
    void elMensajeDeErrorConservaElUsuarioPeroNuncaLaClave() throws Exception {
        ServletFalso f = publicar("cromero", "incorrecta");
        f.ejecutar(servlet);
        assertEquals("cromero", f.flash().get("usuario"));
        assertFalse(String.valueOf(f.flash()).contains("incorrecta"));
        assertFalse(f.redireccion().contains("incorrecta"));
    }

    @Test
    void unUsuarioInexistenteDaElMismoMensajeQueUnaClaveIncorrecta() throws Exception {
        ServletFalso a = publicar("nadie", "Cromero2026");
        a.ejecutar(servlet);
        ServletFalso b = publicar("cromero", "mala");
        b.ejecutar(servlet);
        assertEquals(a.flash().get("texto"), b.flash().get("texto"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"<script>alert(1)</script>", "\"><img src=x onerror=alert(1)>", "' OR '1'='1", "../../etc/passwd"})
    void unUsuarioMalicioso_noSeRefleja_sinEscapar_yElServidorLoRechaza(String usuario) throws Exception {
        ServletFalso f = publicar(usuario, "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("AUTH-001", f.flash().get("codigo"));
        assertNull(f.usuario());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void usuarioVacioNoLlamaAlServicio(String usuario) throws Exception {
        ServletFalso f = publicar(usuario, "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("Ingrese el usuario y la clave", f.flash().get("texto"));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void claveVaciaNoLlamaAlServicio() throws Exception {
        ServletFalso f = publicar("cromero", "");
        f.ejecutar(servlet);
        assertEquals("Ingrese el usuario y la clave", f.flash().get("texto"));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void cuerpoSinCamposNoLlamaAlServicio() throws Exception {
        ServletFalso f = publicar(null, null);
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("Ingrese el usuario y la clave", f.flash().get("texto"));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cro\u0000mero", "cro\nmero", "cro\u001bmero"})
    void usuarioConCaracteresDeControlNoLlamaAlServicio(String usuario) throws Exception {
        ServletFalso f = publicar(usuario, "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("El usuario o la clave no tienen un formato válido", f.flash().get("texto"));
        assertTrue(servidor.llamadas.isEmpty());
        assertEquals("", f.flash().get("usuario"), "no se repite lo que escribió");
    }

    @Test
    void usuarioMuyLargoNoLlamaAlServicio() throws Exception {
        ServletFalso f = publicar("u".repeat(65), "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("El usuario o la clave no tienen un formato válido", f.flash().get("texto"));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void claveMuyLargaNoLlamaAlServicio() throws Exception {
        ServletFalso f = publicar("cromero", "c".repeat(129));
        f.ejecutar(servlet);
        assertEquals("El usuario o la clave no tienen un formato válido", f.flash().get("texto"));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void claveDeLongitudMaximaSiSeEnvia() throws Exception {
        ServletFalso f = publicar("cromero", "c".repeat(128));
        f.ejecutar(servlet);
        assertEquals(1, servidor.llamadas.size());
    }

    @Test
    void servidorApagadoMuestraSer001SinTraza() throws Exception {
        servidor.servidorApagado = true;
        ServletFalso f = publicar("cromero", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("SER-001", f.flash().get("codigo"));
        assertEquals("El servicio no está disponible en este momento", f.flash().get("texto"));
        String todo = String.valueOf(f.flash());
        assertFalse(todo.contains("Exception"));
        assertFalse(todo.contains("http://"));
        assertFalse(todo.contains("\tat "));
    }

    @Test
    void unaRespuestaOkSinTokenNoAbreSesion() throws Exception {
        ClienteFalso sinToken = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String usuario, String clave) {
                return new RespuestaAutenticacion("OK", "200", "Autenticación exitosa");
            }
        };
        ServletFalso f = ServletFalso.crear().cliente(sinToken).metodo("POST").ruta("/login").conSesionAnonima();
        SesionWeb.tokenCsrf(f.peticion());
        f.conCsrfCorrecto().parametro("usuario", "cromero").parametro("clave", "Cromero2026");
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertEquals("error", f.flash().get("tipo"));
        assertNull(f.usuario());
    }

    @Test
    void unLoginCorrectoLlamaUnaSolaVezAlServicio() throws Exception {
        ServletFalso f = publicar("cromero", "Cromero2026");
        f.ejecutar(servlet);
        String primero = f.usuario().getToken();
        assertEquals("TOKEN-1", primero);
        assertEquals(1, servidor.llamadas.size());
    }

    // ------------------------------------------------------------------ otros métodos

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void otrosVerbosDan405(String verbo) throws Exception {
        ServletFalso f = nuevo(verbo);
        f.ejecutar(servlet);
        assertEquals(405, f.errorEnviado());
        assertTrue(servidor.llamadas.isEmpty());
    }

    private static String todoLoQueQuedoEnLaPeticion(ServletFalso f) {
        StringBuilder sb = new StringBuilder();
        for (String nombre : new String[] {"csrf", "mensaje", "token", "usuario", "estado", "titulo", "detalle"}) {
            sb.append(f.atributo(nombre));
        }
        sb.append(f.redireccion());
        return sb.toString();
    }
}
