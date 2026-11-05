package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * La pantalla de costos de mantenimiento.
 */
class VistaCostosTest {

    @Test
    void laProyeccionMuestraTodosLosValores() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA);
        escenario.autenticar();

        escenario.getVistaCostos().consultarCostos();

        assertTrue(escenario.contiene("COSTOS DE MANTENIMIENTO DE 00200002"));
        assertTrue(escenario.contiene("Saldo actual: 6800.00"));
        assertTrue(escenario.contiene("Costo de mantenimiento: 5.00"));
        assertTrue(escenario.contiene("Costo por movimiento: 0.50"));
        assertTrue(escenario.contiene("Interés proyectado: 54.40"));
        assertTrue(escenario.contiene("Límite de mantenimiento: 100.00"));
        assertTrue(escenario.contiene("Operaciones sin costo: 15"));
    }

    @Test
    void unaCuentaQueNoExisteSeMuestraConElCodigoDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "99999999");
        escenario.autenticar();

        escenario.getVistaCostos().consultarCostos();

        assertTrue(escenario.contiene("No se pudieron consultar los costos: "
                + "[CTA-001] La cuenta solicitada no existe"));
    }

    @Test
    void unaCuentaVaciaCancelaLaOperacion() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "");
        escenario.autenticar();

        escenario.getVistaCostos().consultarCostos();

        assertTrue(escenario.contiene("Operación cancelada."));
        assertTrue(escenario.getServidor().llamadas.isEmpty());
    }

    @Test
    void sinSesionSeMuestraElCodigoDeTokenInvalido() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA);

        escenario.getVistaCostos().consultarCostos();

        assertTrue(escenario.contiene("[AUTH-003] El token de sesión no es válido"));
    }
}
