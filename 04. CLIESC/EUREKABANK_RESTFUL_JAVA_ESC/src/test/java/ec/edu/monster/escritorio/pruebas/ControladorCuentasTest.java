package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.controlador.ControladorProtegido;
import ec.edu.monster.escritorio.modelo.RespuestaCuenta;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Las consultas de cuentas: la lista por cédula y el detalle de una cuenta.
 */
class ControladorCuentasTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private ControladorCuentas controlador;

    @BeforeEach
    void preparar() {
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        controlador = new ControladorCuentas(servidor, sesion);
        // Se autentica para tener sesión y se borra el rastro de esa llamada.
        new ControladorAutenticacion(servidor, sesion)
                .autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
    }

    @Test
    void listarCuentasMandaLaCedulaYElToken() {
        controlador.listarCuentas(ClienteFalso.CEDULA);

        assertEquals("listarCuentasPorCliente(06914897,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void listarCuentasDevuelveLasCuentasDelServidor() {
        RespuestaCuentas respuesta = controlador.listarCuentas(ClienteFalso.CEDULA);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(1, respuesta.getCuentas().size());
        assertEquals("00200002", respuesta.getCuentas().get(0).getCodigo());
    }

    @Test
    void sinSesionNoSeLlamaAlServidor() {
        ControladorCuentas sinSesion = new ControladorCuentas(servidor, new SesionUsuario());

        RespuestaCuentas respuesta = sinSesion.listarCuentas(ClienteFalso.CEDULA);

        assertEquals("AUTH-003", respuesta.getCodigo());
        assertEquals("El token de sesión no es válido", respuesta.getMensaje());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void unaCedulaQueNoExisteDevuelveElErrorDelServidor() {
        RespuestaCuentas respuesta = controlador.listarCuentas("00000000");

        assertEquals("CLI-001", respuesta.getCodigo());
        assertEquals("El cliente solicitado no existe", respuesta.getMensaje());
    }

    @Test
    void consultarCuentaMandaLaCuentaYElToken() {
        controlador.consultarCuenta(ClienteFalso.CUENTA);

        assertEquals("consultarCuenta(00200002,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void consultarCuentaDevuelveElDetalle() {
        RespuestaCuenta respuesta = controlador.consultarCuenta(ClienteFalso.CUENTA);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("00200002", respuesta.getCuenta().getCodigo());
        assertEquals("6800.00", respuesta.getCuenta().getSaldo().toPlainString());
    }

    @Test
    void unaCuentaQueNoExisteDevuelveElErrorDelServidor() {
        RespuestaCuenta respuesta = controlador.consultarCuenta("99999999");

        assertEquals("CTA-001", respuesta.getCodigo());
        assertEquals("La cuenta solicitada no existe", respuesta.getMensaje());
    }

    @Test
    void siElTokenExpiraSeDescartaLaSesionLocal() {
        servidor.vencerSesion();

        RespuestaCuentas respuesta = controlador.listarCuentas(ClienteFalso.CEDULA);

        assertEquals("AUTH-004", respuesta.getCodigo());
        assertFalse(controlador.haySesionActiva());
        assertNull(sesion.getToken());
    }

    @Test
    void siElTokenNoExisteSeDescartaLaSesionLocal() {
        servidor.perderSesion();

        RespuestaCuenta respuesta = controlador.consultarCuenta(ClienteFalso.CUENTA);

        assertEquals("AUTH-003", respuesta.getCodigo());
        assertFalse(controlador.haySesionActiva());
    }

    @Test
    void unErrorDeNegocioNoCierraLaSesion() {
        controlador.listarCuentas("00000000");

        assertTrue(controlador.haySesionActiva());
    }

    @Test
    void lasDependenciasSonObligatorias() {
        assertThrows(NullPointerException.class, () -> new ControladorCuentas(null, sesion));
        assertThrows(NullPointerException.class, () -> new ControladorCuentas(servidor, null));
    }

    @Test
    void elControladorHeredaDeControladorProtegido() {
        assertTrue(ControladorProtegido.class.isAssignableFrom(ControladorCuentas.class));
    }
}
