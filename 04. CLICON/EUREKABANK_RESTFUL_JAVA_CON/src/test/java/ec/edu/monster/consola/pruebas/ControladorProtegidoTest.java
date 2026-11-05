package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.controlador.ControladorProtegido;
import ec.edu.monster.consola.modelo.RespuestaBase;
import ec.edu.monster.consola.modelo.RespuestaCuentas;
import ec.edu.monster.consola.modelo.SesionUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El comportamiento común de los controladores que necesitan sesión.
 */
class ControladorProtegidoTest {

    /** Subclase mínima para poder probar los métodos protegidos. */
    private static final class ControladorDePrueba extends ControladorProtegido {

        private ControladorDePrueba(ClienteFalso cliente, SesionUsuario sesion) {
            super(cliente, sesion);
        }

        private RespuestaCuentas exigir(RespuestaCuentas respuesta) {
            return exigirSesion(respuesta);
        }

        private RespuestaBase revisar(RespuestaBase respuesta) {
            return revisarSesion(respuesta);
        }
    }

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private ControladorDePrueba controlador;

    @BeforeEach
    void preparar() {
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        controlador = new ControladorDePrueba(servidor, sesion);
    }

    @Test
    void sinTokenNoHaySesionActiva() {
        assertFalse(controlador.haySesionActiva());
    }

    @Test
    void conTokenHaySesionActiva() {
        sesion.setToken("TOKEN-1");

        assertTrue(controlador.haySesionActiva());
        assertEquals("TOKEN-1", controlador.obtenerToken());
    }

    @Test
    void sinSesionLaRespuestaLlevaElCodigoDeTokenInvalido() {
        RespuestaCuentas respuesta = controlador.exigir(new RespuestaCuentas());

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("AUTH-003", respuesta.getCodigo());
        assertEquals("El token de sesión no es válido", respuesta.getMensaje());
    }

    @Test
    void conSesionLaRespuestaNoSeToca() {
        sesion.setToken("TOKEN-1");
        RespuestaCuentas respuesta = new RespuestaCuentas();
        respuesta.setEstado("OK");
        respuesta.setCodigo("200");

        controlador.exigir(respuesta);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
    }

    @Test
    void unTokenInvalidoDescarteLaSesionLocal() {
        sesion.setToken("TOKEN-1");
        RespuestaBase respuesta = new RespuestaBase();
        respuesta.setCodigo("AUTH-003");

        controlador.revisar(respuesta);

        assertNull(sesion.getToken());
        assertFalse(controlador.haySesionActiva());
    }

    @Test
    void unTokenExpiradoDescarteLaSesionLocal() {
        sesion.setToken("TOKEN-1");
        RespuestaBase respuesta = new RespuestaBase();
        respuesta.setCodigo("AUTH-004");

        controlador.revisar(respuesta);

        assertNull(sesion.getToken());
    }

    @Test
    void unErrorDeNegocioNoDescarteLaSesionLocal() {
        sesion.setToken("TOKEN-1");
        RespuestaBase respuesta = new RespuestaBase();
        respuesta.setCodigo("CTA-001");

        controlador.revisar(respuesta);

        assertEquals("TOKEN-1", sesion.getToken());
        assertTrue(controlador.haySesionActiva());
    }

    @Test
    void unaRespuestaNulaNoRompeElControlador() {
        assertNull(controlador.revisar(null));
    }

    @Test
    void lasDependenciasSonObligatorias() {
        assertThrows(NullPointerException.class, () -> new ControladorDePrueba(null, sesion));
        assertThrows(NullPointerException.class, () -> new ControladorDePrueba(servidor, null));
    }
}
