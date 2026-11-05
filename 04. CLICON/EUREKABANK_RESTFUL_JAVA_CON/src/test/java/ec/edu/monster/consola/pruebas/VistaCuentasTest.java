package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.modelo.RespuestaCuentas;
import org.junit.jupiter.api.Test;

/**
 * La pantalla de cuentas: el listado por cédula y el detalle de una cuenta.
 */
class VistaCuentasTest {

    @Test
    void elListadoMuestraUnaTablaConLosDatosDeCadaCuenta() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CEDULA);
        escenario.autenticar();

        escenario.getVistaCuentas().listarCuentas();

        assertTrue(escenario.contiene("CODIGO"));
        assertTrue(escenario.contiene("MONEDA"));
        assertTrue(escenario.contiene("SALDO"));
        assertTrue(escenario.contiene("ESTADO"));
        assertTrue(escenario.contiene("CLIENTE"));
        assertTrue(escenario.contiene("OPERACIONES"));
        assertTrue(escenario.contiene("00200002"));
        assertTrue(escenario.contiene("Soles"));
        assertTrue(escenario.contiene("6800.00"));
        assertTrue(escenario.contiene("ACTIVO"));
        assertTrue(escenario.contiene("Cliente de Prueba"));
    }

    @Test
    void elListadoEnviaLaCedulaYElToken() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CEDULA);
        escenario.autenticar();

        escenario.getVistaCuentas().listarCuentas();

        assertEquals("listarCuentasPorCliente(06914897,TOKEN-1)",
                escenario.getServidor().llamadas.get(0));
    }

    @Test
    void unaCedulaQueNoExisteSeMuestraConElCodigoDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "00000000");
        escenario.autenticar();

        escenario.getVistaCuentas().listarCuentas();

        assertTrue(escenario.contiene("No se pudieron listar las cuentas: "
                + "[CLI-001] El cliente solicitado no existe"));
    }

    @Test
    void unaCedulaVaciaCancelaLaOperacion() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "");
        escenario.autenticar();

        escenario.getVistaCuentas().listarCuentas();

        assertTrue(escenario.contiene("Operación cancelada."));
        assertTrue(escenario.getServidor().llamadas.isEmpty());
    }

    @Test
    void unClienteSinCuentasSeInformaSinFallar() {
        ClienteFalso servidor = new ClienteFalso() {
            @Override
            public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
                RespuestaCuentas respuesta = super.listarCuentasPorCliente(cedula, token);
                respuesta.setCuentas(java.util.List.of());
                return respuesta;
            }
        };
        Escenario escenario = Escenario.crear(servidor, ClienteFalso.CEDULA);
        escenario.autenticar();

        escenario.getVistaCuentas().listarCuentas();

        assertTrue(escenario.contiene("El cliente no tiene cuentas registradas."));
    }

    @Test
    void elDetalleMuestraTodosLosDatosDeLaCuenta() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA);
        escenario.autenticar();

        escenario.getVistaCuentas().consultarCuenta();

        assertTrue(escenario.contiene("CUENTA 00200002"));
        assertTrue(escenario.contiene("Cliente de Prueba"));
        assertTrue(escenario.contiene("6800.00"));
        assertTrue(escenario.contiene("ACTIVO"));
        assertTrue(escenario.contiene("2020-01-01"));
    }

    @Test
    void unaCuentaQueNoExisteSeMuestraConElCodigoDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "99999999");
        escenario.autenticar();

        escenario.getVistaCuentas().consultarCuenta();

        assertTrue(escenario.contiene("No se pudo consultar la cuenta: "
                + "[CTA-001] La cuenta solicitada no existe"));
    }

    @Test
    void unCodigoDeCuentaVacioCancelaLaOperacion() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "");
        escenario.autenticar();

        escenario.getVistaCuentas().consultarCuenta();

        assertTrue(escenario.contiene("Operación cancelada."));
        assertTrue(escenario.getServidor().llamadas.isEmpty());
    }

    @Test
    void sinSesionElServidorNoSeLlamaYSeMuestraElCodigo() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CEDULA);

        escenario.getVistaCuentas().listarCuentas();

        assertTrue(escenario.contiene("[AUTH-003] El token de sesión no es válido"));
        assertTrue(escenario.getServidor().llamadas.isEmpty());
    }
}
