package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.controlador.ControladorAutenticacion;
import ec.edu.monster.web.controlador.ControladorCostos;
import ec.edu.monster.web.controlador.ControladorProtegido;
import ec.edu.monster.web.modelo.RespuestaCostos;
import ec.edu.monster.web.modelo.SesionUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La proyección de costos de mantenimiento.
 */
class ControladorCostosTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private ControladorCostos controlador;

    @BeforeEach
    void preparar() {
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        controlador = new ControladorCostos(servidor, sesion);
        new ControladorAutenticacion(servidor, sesion)
                .autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
    }

    @Test
    void consultarCostosMandaLaCuentaYElToken() {
        controlador.consultarCostos(ClienteFalso.CUENTA);

        assertEquals("consultarCostosMantenimiento(00200002,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void laProyeccionDevuelveTodosLosImportes() {
        RespuestaCostos respuesta = controlador.consultarCostos(ClienteFalso.CUENTA);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("6800.00", respuesta.getSaldoActual().toPlainString());
        assertEquals("5.00", respuesta.getCostoMantenimiento().toPlainString());
        assertEquals("0.50", respuesta.getCostoMovimiento().toPlainString());
        assertEquals("54.40", respuesta.getInteresProyectado().toPlainString());
        assertEquals("100.00", respuesta.getLimiteMantenimiento().toPlainString());
        assertEquals(15, respuesta.getOperacionesSinCosto());
    }

    @Test
    void sinSesionNoSeLlamaAlServidor() {
        ControladorCostos sinSesion = new ControladorCostos(servidor, new SesionUsuario());

        RespuestaCostos respuesta = sinSesion.consultarCostos(ClienteFalso.CUENTA);

        assertEquals("AUTH-003", respuesta.getCodigo());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void siElTokenExpiraSeDescartaLaSesionLocal() {
        servidor.vencerSesion();

        RespuestaCostos respuesta = controlador.consultarCostos(ClienteFalso.CUENTA);

        assertEquals("AUTH-004", respuesta.getCodigo());
        assertFalse(controlador.haySesionActiva());
    }

    @Test
    void lasDependenciasSonObligatorias() {
        assertThrows(NullPointerException.class, () -> new ControladorCostos(null, sesion));
        assertThrows(NullPointerException.class, () -> new ControladorCostos(servidor, null));
    }

    @Test
    void elControladorHeredaDeControladorProtegido() {
        assertTrue(ControladorProtegido.class.isAssignableFrom(ControladorCostos.class));
    }
}
