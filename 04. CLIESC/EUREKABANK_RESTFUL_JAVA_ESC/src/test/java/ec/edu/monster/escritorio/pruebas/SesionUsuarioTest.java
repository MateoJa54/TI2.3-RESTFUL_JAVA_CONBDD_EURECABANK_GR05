package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.modelo.SesionUsuario;
import org.junit.jupiter.api.Test;

/**
 * La sesión local del usuario: el token y los datos del empleado.
 */
class SesionUsuarioTest {

    @Test
    void unaSesionNuevaNoEstaVigente() {
        assertFalse(new SesionUsuario().estaVigente());
    }

    @Test
    void conTokenLaSesionEstaVigente() {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setToken("TOKEN-1");

        assertTrue(sesion.estaVigente());
    }

    @Test
    void unTokenVacioNoActivaLaSesion() {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setToken("   ");

        assertFalse(sesion.estaVigente());
    }

    @Test
    void limpiarOlvidaElTokenYLosDatosDelEmpleado() {
        SesionUsuario sesion = new SesionUsuario("TOKEN-1", "0001", "Carlos Romero",
                "2026-11-03T12:15:00Z", "Consultas");

        sesion.limpiar();

        assertFalse(sesion.estaVigente());
        assertNull(sesion.getToken());
        assertNull(sesion.getEmpleado());
        assertNull(sesion.getNombreEmpleado());
        assertNull(sesion.getExpiracion());
        assertNull(sesion.getRol());
    }

    @Test
    void elConstructorCompletoDejaLosDatosPuestos() {
        SesionUsuario sesion = new SesionUsuario("TOKEN-1", "0001", "Carlos Romero",
                "2026-11-03T12:15:00Z", "Consultas");

        assertEquals("TOKEN-1", sesion.getToken());
        assertEquals("0001", sesion.getEmpleado());
        assertEquals("Carlos Romero", sesion.getNombreEmpleado());
        assertEquals("2026-11-03T12:15:00Z", sesion.getExpiracion());
        assertEquals("Consultas", sesion.getRol());
    }

    @Test
    void losAtributosSePuedenCambiarUnoPorUno() {
        SesionUsuario sesion = new SesionUsuario();

        sesion.setToken("TOKEN-2");
        sesion.setEmpleado("0002");
        sesion.setNombreEmpleado("Luis Castro");
        sesion.setExpiracion("2026-11-03T13:00:00Z");
        sesion.setRol("Procesos");

        assertEquals("TOKEN-2", sesion.getToken());
        assertEquals("0002", sesion.getEmpleado());
        assertEquals("Luis Castro", sesion.getNombreEmpleado());
        assertEquals("2026-11-03T13:00:00Z", sesion.getExpiracion());
        assertEquals("Procesos", sesion.getRol());
    }
}
