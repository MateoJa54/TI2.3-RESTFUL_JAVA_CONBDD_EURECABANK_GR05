package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * El menú principal: las ocho opciones y el cierre de sesión al salir.
 */
class VistaMenuTest {

    @Test
    void elMenuMuestraLasOchoOpciones() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "0");
        escenario.autenticar();

        escenario.getVistaMenu().ejecutar();

        assertTrue(escenario.contiene("1. Listar cuentas por cédula"));
        assertTrue(escenario.contiene("2. Consultar una cuenta"));
        assertTrue(escenario.contiene("3. Consultar movimientos de una cuenta"));
        assertTrue(escenario.contiene("4. Realizar depósito"));
        assertTrue(escenario.contiene("5. Realizar retiro"));
        assertTrue(escenario.contiene("6. Realizar transferencia"));
        assertTrue(escenario.contiene("7. Consultar costos de mantenimiento"));
        assertTrue(escenario.contiene("0. Salir"));
    }

    @Test
    void alSalirSeCierraLaSesionEnElServidorConElToken() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "0");
        escenario.autenticar();

        assertTrue(escenario.getVistaMenu().ejecutar());

        assertEquals("cerrarSesion(TOKEN-1)", escenario.getServidor().llamadas.get(0));
        assertTrue(escenario.contiene("Sesión cerrada: [200] Sesión cerrada correctamente"));
    }

    @Test
    void alSalirLaSesionLocalSeOlvida() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "0");
        escenario.autenticar();

        escenario.getVistaMenu().ejecutar();

        assertFalse(escenario.getAutenticacion().haySesionActiva());
    }

    @Test
    void unaOpcionInvalidaSeVuelveAPedir() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "9", "x", "0");
        escenario.autenticar();

        escenario.getVistaMenu().ejecutar();

        assertEquals(2, escenario.veces("Opción no válida"));
    }

    @Test
    void elMenuRepiteLaOpcionElegidaHastaQueSeElijaSalir() {
        Escenario escenario = Escenario.crear(new ClienteFalso(),
                "1", ClienteFalso.CEDULA, "3", ClienteFalso.CUENTA, "0");
        escenario.autenticar();

        escenario.getVistaMenu().ejecutar();

        assertEquals(3, escenario.veces("EURECABANK - CLIENTE DE CONSOLA"));
        assertEquals("listarCuentasPorCliente(06914897,TOKEN-1)",
                escenario.getServidor().llamadas.get(0));
        assertEquals("consultarMovimientos(00200002,TOKEN-1)",
                escenario.getServidor().llamadas.get(1));
    }

    @Test
    void sinSesionElMenuNoSeMuestra() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "0");

        assertFalse(escenario.getVistaMenu().ejecutar());

        assertFalse(escenario.contiene("EURECABANK - CLIENTE DE CONSOLA"));
    }

    @Test
    void siElServidorCaeEnUnaOperacionElMenuSigueAbierto() {
        ClienteFalso servidor = new ClienteFalso() {
            @Override
            public ec.edu.monster.consola.modelo.RespuestaCuentas listarCuentasPorCliente(
                    String cedula, String token) {
                throw new ec.edu.monster.consola.util.ExcepcionServicio(
                        "No se pudo comunicar con el servidor en http://falso", null);
            }
        };
        Escenario escenario = Escenario.crear(servidor, "1", ClienteFalso.CEDULA, "0");
        escenario.autenticar();

        escenario.getVistaMenu().ejecutar();

        assertTrue(escenario.contiene("No se pudo comunicar con el servidor"));
        assertTrue(escenario.contiene("Verifique que el servidor esté encendido"));
        // El menú se siguió mostrando después del fallo: aparece dos veces.
        assertEquals(2, escenario.veces("EURECABANK - CLIENTE DE CONSOLA"));
    }
}
