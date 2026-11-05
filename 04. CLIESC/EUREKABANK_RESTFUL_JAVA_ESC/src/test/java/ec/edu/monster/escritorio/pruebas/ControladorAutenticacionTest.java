package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.modelo.RespuestaAutenticacion;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.util.ExcepcionServicio;
import ec.edu.monster.escritorio.util.HashClave;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El inicio y el cierre de sesión: la contraseña se hashea antes de enviarse
 * y la sesión solo se guarda si el servidor la acepta.
 */
class ControladorAutenticacionTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private ControladorAutenticacion controlador;

    @BeforeEach
    void preparar() {
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        controlador = new ControladorAutenticacion(servidor, sesion);
    }

    @Test
    void laContrasenaSeEnviaHasheadaYNoEnClaro() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertEquals("autenticar(cromero," + ClienteFalso.HASH_CLAVE + ")", servidor.llamadas.get(0));
    }

    @Test
    void laContrasenaEnClaroNuncaViajaPorLaRed() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertTrue(servidor.llamadas.stream().noneMatch(l -> l.contains(ClienteFalso.CLAVE)));
    }

    @Test
    void elHashEnviadoEsElMismoQueCalculaElUtilitario() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertTrue(servidor.llamadas.get(0).contains(HashClave.sha256(ClienteFalso.CLAVE)));
    }

    @Test
    void alAutenticarseSeGuardaElTokenYLosDatosDelEmpleado() {
        RespuestaAutenticacion respuesta = controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertEquals("OK", respuesta.getEstado());
        assertTrue(controlador.haySesionActiva());
        assertEquals("TOKEN-1", sesion.getToken());
        assertEquals("0001", sesion.getEmpleado());
        assertEquals("Carlos Romero", sesion.getNombreEmpleado());
        assertEquals(ClienteFalso.EXPIRACION, sesion.getExpiracion());
        assertEquals("Consultas", sesion.getRol());
    }

    @Test
    void conCredencialIncorrectaNoHaySesion() {
        RespuestaAutenticacion respuesta = controlador.autenticar(ClienteFalso.USUARIO, "mala");

        assertEquals("AUTH-001", respuesta.getCodigo());
        assertFalse(controlador.haySesionActiva());
        assertNull(sesion.getToken());
    }

    @Test
    void unIntentoFallidoDescartaLaSesionAnterior() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        controlador.autenticar(ClienteFalso.USUARIO, "mala");

        assertFalse(controlador.haySesionActiva());
    }

    @Test
    void unaRespuestaOkSinTokenNoAbreSesion() {
        ClienteFalso sinToken = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String usuario, String clave) {
                RespuestaAutenticacion respuesta = super.autenticar(usuario, clave);
                respuesta.setToken(null);
                return respuesta;
            }
        };
        ControladorAutenticacion otro = new ControladorAutenticacion(sinToken, sesion);

        otro.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertFalse(otro.haySesionActiva());
    }

    @Test
    void autenticarseDeNuevoReemplazaElToken() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertEquals("TOKEN-2", sesion.getToken());
    }

    @Test
    void cerrarSesionLlamaAlServidorConElToken() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        RespuestaBase respuesta = controlador.cerrarSesion();

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Sesión cerrada correctamente", respuesta.getMensaje());
        assertEquals(List.of("autenticar(cromero," + ClienteFalso.HASH_CLAVE + ")",
                "cerrarSesion(TOKEN-1)"), servidor.llamadas);
    }

    @Test
    void cerrarSesionOlvidaElTokenLocal() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        controlador.cerrarSesion();

        assertFalse(controlador.haySesionActiva());
        assertNull(sesion.getToken());
    }

    @Test
    void cerrarSesionSinSesionNoLlamaAlServidor() {
        RespuestaBase respuesta = controlador.cerrarSesion();

        assertEquals("AUTH-003", respuesta.getCodigo());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void siElServidorEstaApagadoLaSesionSeCierraIgual() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.servidorApagado = true;

        assertThrows(ExcepcionServicio.class, () -> controlador.cerrarSesion());

        assertFalse(controlador.haySesionActiva());
    }

    @Test
    void laSesionSePuedeConsultar() {
        controlador.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertNotNull(controlador.obtenerSesion());
        assertEquals("TOKEN-1", controlador.obtenerSesion().getToken());
    }

    @Test
    void lasDependenciasSonObligatorias() {
        assertThrows(NullPointerException.class, () -> new ControladorAutenticacion(null, sesion));
        assertThrows(NullPointerException.class, () -> new ControladorAutenticacion(servidor, null));
    }

    @Test
    void dosClavesDistintasDanHashesDistintos() {
        assertNotEquals(HashClave.sha256("clave1"), HashClave.sha256("clave2"));
    }
}
