package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.servlet.LogoutServlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Cierre de sesión: solo POST con CSRF, cierra en el servicio y en el navegador. */
class LogoutServletTest {

    private final ClienteFalso servidor = new ClienteFalso();
    private final LogoutServlet servlet = new LogoutServlet();

    private ServletFalso sesionAbierta(String metodo) {
        return ServletFalso.crear().cliente(servidor).metodo(metodo).ruta("/logout").conUsuarioAutenticado(servidor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void soloAceptaPost(String verbo) throws Exception {
        ServletFalso f = sesionAbierta(verbo);
        servidor.llamadas.clear();
        f.ejecutar(servlet);
        assertEquals(405, f.errorEnviado());
        assertTrue(servidor.llamadas.isEmpty(), "un enlace no puede cerrar la sesión");
        assertNotNull(f.usuario());
    }

    @Test
    void postSinCsrfDa403YLaSesionSigueAbierta() throws Exception {
        ServletFalso f = sesionAbierta("POST");
        servidor.llamadas.clear();
        f.ejecutar(servlet);
        assertEquals(403, f.estado());
        assertEquals("/WEB-INF/vistas/error.jsp", f.reenvio());
        assertTrue(servidor.llamadas.isEmpty());
        assertNotNull(f.usuario());
        assertEquals(0, f.sesionesInvalidadas());
    }

    @Test
    void postConCsrfIncorrectoDa403() throws Exception {
        ServletFalso f = sesionAbierta("POST").parametro("_csrf", "falso");
        servidor.llamadas.clear();
        f.ejecutar(servlet);
        assertEquals(403, f.estado());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void cierraEnElServicioEInvalidaLaSesionHttp() throws Exception {
        ServletFalso f = sesionAbierta("POST").conCsrfCorrecto();
        servidor.llamadas.clear();
        f.ejecutar(servlet);
        assertEquals(java.util.List.of("cerrarSesion(TOKEN-1)"), servidor.llamadas);
        assertEquals(1, f.sesionesInvalidadas());
        assertEquals("/web/login", f.redireccion());
    }

    @Test
    void despuesDelCierreHayUnaSesionNuevaSinUsuarioNiToken() throws Exception {
        ServletFalso f = sesionAbierta("POST").conCsrfCorrecto();
        f.ejecutar(servlet);
        assertNull(f.usuario());
        assertNull(f.csrf());
    }

    @Test
    void elMensajeDeCierreEsElLiteralDelServidor() throws Exception {
        ServletFalso f = sesionAbierta("POST").conCsrfCorrecto();
        f.ejecutar(servlet);
        assertEquals("Sesión cerrada correctamente", f.flash().get("texto"));
        assertEquals("exito", f.flash().get("tipo"));
    }

    @Test
    void conElServicioApagadoLaSesionLocalSeCierraIgual() throws Exception {
        ServletFalso f = sesionAbierta("POST").conCsrfCorrecto();
        servidor.servidorApagado = true;
        f.ejecutar(servlet);
        assertEquals(1, f.sesionesInvalidadas());
        assertEquals("/web/login", f.redireccion());
        assertEquals("SER-001", f.flash().get("codigo"));
        assertFalse(String.valueOf(f.flash()).contains("http://"));
    }

    @Test
    void siElServidorYaOlvidoElTokenMuestraSuMensajeYCierraLocalmente() throws Exception {
        ServletFalso f = sesionAbierta("POST").conCsrfCorrecto();
        servidor.perderSesion();
        f.ejecutar(servlet);
        assertEquals("AUTH-003", f.flash().get("codigo"));
        assertEquals("El token de sesión no es válido", f.flash().get("texto"));
        assertEquals(1, f.sesionesInvalidadas());
    }

    @Test
    void sinUsuarioSoloVuelveAlLoginSinLlamarAlServicio() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).metodo("POST").ruta("/logout").conSesionAnonima();
        ec.edu.monster.web.util.SesionWeb.tokenCsrf(f.peticion());
        f.conCsrfCorrecto();
        f.ejecutar(servlet);
        assertEquals("/web/login", f.redireccion());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void cerrarDosVecesNoRepiteLaLlamada() throws Exception {
        ServletFalso f = sesionAbierta("POST").conCsrfCorrecto();
        servidor.llamadas.clear();
        f.ejecutar(servlet);
        f.ejecutar(servlet);
        assertEquals(1, servidor.llamadas.size());
    }
}
