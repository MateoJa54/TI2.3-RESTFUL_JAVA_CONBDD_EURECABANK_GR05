package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La pantalla de movimientos: consulta, depósito, retiro y transferencia.
 */
class VistaMovimientosTest {

    @Test
    void laConsultaMuestraUnaTablaConLosMovimientos() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA);
        escenario.autenticar();

        escenario.getVistaMovimientos().consultarMovimientos();

        assertTrue(escenario.contiene("NUMERO"));
        assertTrue(escenario.contiene("FECHA"));
        assertTrue(escenario.contiene("TIPO"));
        assertTrue(escenario.contiene("ACCION"));
        assertTrue(escenario.contiene("IMPORTE"));
        assertTrue(escenario.contiene("REFERENCIA"));
        assertTrue(escenario.contiene("2026-01-05"));
        assertTrue(escenario.contiene("Apertura de Cuenta"));
        assertTrue(escenario.contiene("INGRESO"));
        assertTrue(escenario.contiene("1000.00"));
    }

    @Test
    void unaCuentaSinMovimientosSeInformaSinFallar() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA);
        escenario.autenticar();

        escenario.getVistaMovimientos().consultarMovimientos();

        assertTrue(escenario.contiene("MOVIMIENTOS DE LA CUENTA 00200002"));
    }

    @Test
    void elDepositoMuestraElSaldoNuevoElCostoYElItf() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "100.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().depositar();

        assertTrue(escenario.contiene("DEPÓSITO"));
        assertTrue(escenario.contiene("Saldo nuevo:  6900.00"));
        assertTrue(escenario.contiene("Costo:        0.00"));
        assertTrue(escenario.contiene("ITF:          0.00"));
    }

    @Test
    void elDepositoMandaElMontoTalComoSeEscribio() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "100.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().depositar();

        assertEquals("realizarDeposito(00200002,100.00,TOKEN-1)",
                escenario.getServidor().llamadas.get(0));
    }

    @Test
    void unMontoConTresDecimalesNoLlegaAlServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "100.555", "10.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().depositar();

        assertTrue(escenario.contiene("Monto no válido"));
        assertEquals("realizarDeposito(00200002,10.00,TOKEN-1)",
                escenario.getServidor().llamadas.get(0));
    }

    @Test
    void unMontoNegativoNoLlegaAlServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "-50", "10.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().depositar();

        assertTrue(escenario.contiene("Monto no válido"));
        assertEquals(1, escenario.getServidor().llamadas.size());
    }

    @Test
    void elRetiroMuestraElSaldoNuevo() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "50.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().retirar();

        assertTrue(escenario.contiene("RETIRO"));
        assertTrue(escenario.contiene("Saldo nuevo:  6750.00"));
    }

    @Test
    void unRetiroSinSaldoSuficienteAvisaConElCodigoDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "50000.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().retirar();

        assertTrue(escenario.contiene("No se pudo realizar el retiro: [SAL-001] "
                + "El saldo de la cuenta es insuficiente para la operación"));
    }

    @Test
    void laTransferenciaMuestraLosSaldosYElItf() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, "00100001", "25.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().transferir();

        assertTrue(escenario.contiene("TRANSFERENCIA"));
        assertTrue(escenario.contiene("Saldo origen:       6775.00"));
        assertTrue(escenario.contiene("Saldo destino:      7429.00"));
        assertTrue(escenario.contiene("ITF aplicado:        0.02"));
    }

    @Test
    void unaTransferenciaALaMismaCuentaAvisaConElCodigoDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(),
                ClienteFalso.CUENTA, ClienteFalso.CUENTA, "25.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().transferir();

        assertTrue(escenario.contiene("No se pudo realizar la transferencia: [CTA-003] "
                + "La cuenta origen y la cuenta destino no pueden ser iguales"));
    }

    @Test
    void unaTransferenciaAMonedaDistintaAvisaConElCodigoDelServidor() {
        Escenario escenario = Escenario.crear(new ClienteFalso(),
                ClienteFalso.CUENTA, ClienteFalso.CUENTA_DOLARES, "25.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().transferir();

        assertTrue(escenario.contiene("No se pudo realizar la transferencia: [CTA-004] "
                + "La cuenta de destino pertenece a una moneda distinta"));
    }

    @Test
    void unaCuentaVaciaCancelaLaOperacion() {
        Escenario escenario = Escenario.crear(new ClienteFalso(), "");
        escenario.autenticar();

        escenario.getVistaMovimientos().consultarMovimientos();

        assertTrue(escenario.contiene("Operación cancelada."));
        assertTrue(escenario.getServidor().llamadas.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "1.234", ""})
    void unMontoInvalidoNuncaLlegaAlServidor(String escrito) {
        Escenario escenario = Escenario.crear(new ClienteFalso(), ClienteFalso.CUENTA, escrito, "10.00");
        escenario.autenticar();

        escenario.getVistaMovimientos().retirar();

        assertTrue(escenario.contiene("Monto no válido"));
        assertEquals("realizarRetiro(00200002,10.00,TOKEN-1)",
                escenario.getServidor().llamadas.get(0));
    }
}
