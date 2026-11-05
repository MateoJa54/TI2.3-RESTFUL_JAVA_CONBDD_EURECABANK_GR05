package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.modelo.RespuestaAutenticacion;
import ec.edu.monster.consola.util.ExcepcionServicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * La pantalla de inicio de sesión: qué se ve cuando el servidor acepta la
 * credencial y cuando la rechaza.
 */
class VistaAutenticacionTest {

    @Test
    void conCredencialCorrectaSeMuestraElTokenElEmpleadoYLaExpiracion() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        assertTrue(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.contiene("Acceso concedido."));
        assertTrue(escenario.contiene("TOKEN-1"));
        assertTrue(escenario.contiene("0001 - Carlos Romero"));
        assertTrue(escenario.contiene(ClienteFalso.EXPIRACION));
    }

    @Test
    void laContrasenaSeHasheaAntesDeEnviarse() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.USUARIO, ClienteFalso.CLAVE);

        escenario.getVistaAutenticacion().iniciar();

        assertEquals("autenticar(cromero," + ClienteFalso.HASH_CLAVE + ")",
                escenario.getServidor().llamadas.get(0));
    }

    @Test
    void conCredencialIncorrectaSeMuestraElCodigoYElMensajeDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.USUARIO, "mala", "");

        assertFalse(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.contiene("[AUTH-001] El usuario o la contraseña son incorrectos"));
    }

    @ParameterizedTest
    @CsvSource({
        "AUTH-002, El usuario se encuentra inactivo",
        "AUTH-003, El token de sesión no es válido",
        "DAT-001, Los datos de la petición están incompletos o son incorrectos",
        "SER-001, El servicio no está disponible en este momento",
        "XYZ-999, Código que el cliente no conoce"})
    void cualquierErrorSeMuestraConSuCodigoYSuMensaje(String codigo, String mensaje) {
        ClienteFalso servidor = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String usuario, String clave) {
                RespuestaAutenticacion respuesta = super.autenticar(usuario, clave);
                respuesta.setEstado("ERROR");
                respuesta.setCodigo(codigo);
                respuesta.setMensaje(mensaje);
                return respuesta;
            }
        };
        Escenario escenario = Escenario.crear(servidor, ClienteFalso.USUARIO, ClienteFalso.CLAVE, "");

        assertFalse(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.contiene("[" + codigo + "] " + mensaje));
    }

    @Test
    void unUsuarioVacioTerminaElProgramaSinLlamarAlServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "");

        assertFalse(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.getServidor().llamadas.isEmpty());
    }

    @Test
    void siElServidorEstaApagadoSeExplicaYSePuedeReintentar() {
        ClienteFalso servidor = new ClienteFalso();
        servidor.servidorApagado = true;
        Escenario escenario = Escenario.crear(servidor, ClienteFalso.USUARIO, ClienteFalso.CLAVE, "");

        assertFalse(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.contiene("No se pudo comunicar con el servidor"));
        assertTrue(escenario.contiene("Verifique que el servidor esté encendido"));
    }

    @Test
    void muchosIntentosFallidosNoBloqueanAlUsuario() {
        String[] teclado = new String[2 * 5 + 3];
        for (int i = 0; i < 5; i++) {
            teclado[2 * i] = ClienteFalso.USUARIO;
            teclado[2 * i + 1] = "mala" + i;
        }
        teclado[10] = ClienteFalso.USUARIO;
        teclado[11] = ClienteFalso.CLAVE;
        teclado[12] = "";
        Escenario escenario = Escenario.crear(new ClienteFalso(), teclado);

        assertTrue(escenario.getVistaAutenticacion().iniciar());

        assertEquals(5, escenario.veces("[AUTH-001]"));
        assertTrue(escenario.contiene("Acceso concedido."));
    }

    @Test
    void unOkSinTokenNoDejaPasarAlMenu() {
        ClienteFalso servidor = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String usuario, String clave) {
                RespuestaAutenticacion respuesta = super.autenticar(usuario, clave);
                respuesta.setToken(null);
                return respuesta;
            }
        };
        Escenario escenario = Escenario.crear(servidor, ClienteFalso.USUARIO, ClienteFalso.CLAVE, "");

        assertFalse(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.contiene("No se pudo iniciar sesión"));
    }

    @Test
    void laExcepcionDelServidorSeMuestraConSuCodigo() {
        ClienteFalso servidor = new ClienteFalso() {
            @Override
            public RespuestaAutenticacion autenticar(String usuario, String clave) {
                throw new ExcepcionServicio("No se pudo comunicar con el servidor en http://falso", null);
            }
        };
        Escenario escenario = Escenario.crear(servidor, ClienteFalso.USUARIO, ClienteFalso.CLAVE, "");

        assertFalse(escenario.getVistaAutenticacion().iniciar());

        assertTrue(escenario.contiene("[SER-001] El servicio no está disponible en este momento"));
    }
}
