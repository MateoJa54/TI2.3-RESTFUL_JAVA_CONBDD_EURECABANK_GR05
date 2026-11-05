package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import ec.edu.monster.servicios.FabricaRespuestas;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la fabrica de respuestas.
 */
class FabricaRespuestasTest {

    @Test
    void laAutenticacionExitosaLlevaElMensajeDelContrato() {
        var respuesta = FabricaRespuestas.autenticacionExitosa("token", "0001", "Nombre",
                "2026-11-03T10:30:00", "Procesos");

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Autenticación exitosa", respuesta.getMensaje());
        assertEquals("token", respuesta.getToken());
        assertEquals("0001", respuesta.getEmpleado());
        assertEquals("Nombre", respuesta.getNombreEmpleado());
        assertEquals("2026-11-03T10:30:00", respuesta.getExpiracion());
        assertEquals("Procesos", respuesta.getRol());
    }

    @Test
    void laAutenticacionFallidaNoDevuelveToken() {
        var respuesta = FabricaRespuestas.autenticacionFallida(CodigoRespuesta.AUTH_001);

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("AUTH-001", respuesta.getCodigo());
        assertNull(respuesta.getToken());
        assertNull(respuesta.getEmpleado());
    }

    @Test
    void elCierreDeSesionExitosoLlevaSuMensaje() {
        var respuesta = FabricaRespuestas.cierreSesionExitoso();

        assertEquals("Sesión cerrada correctamente", respuesta.getMensaje());
        assertEquals("200", respuesta.getCodigo());
    }

    @Test
    void lasCuentasExitosasLlevanLaListaDelCliente() {
        CuentaDTO cuenta = cuenta("00200002");

        var respuesta = FabricaRespuestas.cuentasExitosas("06914897", "Eric Gustavo", List.of(cuenta));

        assertEquals("Cuentas consultadas correctamente", respuesta.getMensaje());
        assertEquals("06914897", respuesta.getCedula());
        assertEquals("Eric Gustavo", respuesta.getNombreCliente());
        assertEquals(1, respuesta.getCuentas().size());
    }

    @Test
    void lasCuentasFallidasDevuelvenLaListaVacia() {
        var respuesta = FabricaRespuestas.cuentasFallidas(CodigoRespuesta.CLI_001);

        assertEquals("CLI-001", respuesta.getCodigo());
        assertNotNull(respuesta.getCuentas());
        assertTrue(respuesta.getCuentas().isEmpty());
    }

    @Test
    void laCuentaFallidaNoDevuelveCuenta() {
        var respuesta = FabricaRespuestas.cuentaFallida(CodigoRespuesta.CTA_001);

        assertEquals("CTA-001", respuesta.getCodigo());
        assertNull(respuesta.getCuenta());
    }

    @Test
    void losMovimientosFallidosDevuelvenLaListaVacia() {
        var respuesta = FabricaRespuestas.movimientosFallidos(CodigoRespuesta.CTA_002);

        assertEquals("CTA-002", respuesta.getCodigo());
        assertTrue(respuesta.getMovimientos().isEmpty());
    }

    @Test
    void elDepositoExitosoLlevaElMensajeDelDeposito() {
        MovimientoDTO movimiento = movimiento(8, "003", new BigDecimal("100.00"));

        var respuesta = FabricaRespuestas.depositoExitoso(movimiento, new BigDecimal("6900.00"),
                BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals("Depósito realizado correctamente", respuesta.getMensaje());
        assertEquals(new BigDecimal("6900.00"), respuesta.getSaldoNuevo());
        assertEquals(2, respuesta.getSaldoNuevo().scale());
    }

    @Test
    void elRetiroExitosoLlevaElMensajeDelRetiro() {
        MovimientoDTO movimiento = movimiento(9, "004", new BigDecimal("50.00"));

        var respuesta = FabricaRespuestas.retiroExitoso(movimiento, new BigDecimal("6750.00"),
                new BigDecimal("2.00"), BigDecimal.ZERO);

        assertEquals("Retiro realizado correctamente", respuesta.getMensaje());
        assertEquals(new BigDecimal("2.00"), respuesta.getCostoAplicado());
    }

    @Test
    void elMovimientoFallidoPoneLosImportesACero() {
        var respuesta = FabricaRespuestas.movimientoFallido(CodigoRespuesta.MON_001);

        assertEquals("MON-001", respuesta.getCodigo());
        assertEquals(new BigDecimal("0.00"), respuesta.getSaldoNuevo());
        assertEquals(new BigDecimal("0.00"), respuesta.getCostoAplicado());
        assertEquals(new BigDecimal("0.00"), respuesta.getItfAplicado());
    }

    @Test
    void laTransferenciaExitosaLlevaLosSeisDatos() {
        var respuesta = FabricaRespuestas.transferenciaExitosa(8, 9,
                new BigDecimal("6700.00"), new BigDecimal("5100.00"), new BigDecimal("0.04"));

        assertEquals("Transferencia realizada correctamente", respuesta.getMensaje());
        assertEquals(8, respuesta.getNumeroOrigen());
        assertEquals(9, respuesta.getNumeroDestino());
        assertEquals(new BigDecimal("6700.00"), respuesta.getSaldoOrigen());
        assertEquals(new BigDecimal("5100.00"), respuesta.getSaldoDestino());
        assertEquals(new BigDecimal("0.04"), respuesta.getItfAplicado());
    }

    @Test
    void laTransferenciaFallidaPoneLosNumerosACero() {
        var respuesta = FabricaRespuestas.transferenciaFallida(CodigoRespuesta.CTA_004);

        assertEquals("CTA-004", respuesta.getCodigo());
        assertEquals(0, respuesta.getNumeroOrigen());
        assertEquals(0, respuesta.getNumeroDestino());
    }

    @Test
    void losCostosExitososLlevanLosSeisValores() {
        var respuesta = FabricaRespuestas.costosExitosos(new BigDecimal("6800.00"),
                new BigDecimal("7.00"), new BigDecimal("2.00"), new BigDecimal("47.60"),
                new BigDecimal("3500.00"), 15);

        assertEquals("Costos de mantenimiento consultados correctamente", respuesta.getMensaje());
        assertEquals(new BigDecimal("6800.00"), respuesta.getSaldoActual());
        assertEquals(new BigDecimal("7.00"), respuesta.getCostoMantenimiento());
        assertEquals(new BigDecimal("2.00"), respuesta.getCostoMovimiento());
        assertEquals(new BigDecimal("47.60"), respuesta.getInteresProyectado());
        assertEquals(new BigDecimal("3500.00"), respuesta.getLimiteMantenimiento());
        assertEquals(15, respuesta.getOperacionesSinCosto());
    }

    @Test
    void losCostosFallidosPonenLosValoresACero() {
        var respuesta = FabricaRespuestas.costosFallidos(CodigoRespuesta.CTA_001);

        assertEquals("CTA-001", respuesta.getCodigo());
        assertEquals(new BigDecimal("0.00"), respuesta.getSaldoActual());
        assertEquals(0, respuesta.getOperacionesSinCosto());
    }

    @Test
    void unCodigoDeExitoNoSePuedeUsarComoError() {
        assertThrows(IllegalArgumentException.class,
                () -> FabricaRespuestas.cuentasFallidas(CodigoRespuesta.OK_200));
        assertThrows(NullPointerException.class,
                () -> FabricaRespuestas.cuentasFallidas(null));
    }

    private static CuentaDTO cuenta(String codigo) {
        return new CuentaDTO(codigo, "01", "Soles", new BigDecimal("6800.00"), "ACTIVO",
                "00001", "Eric Gustavo", "002", "2008-01-09", "123456", 7);
    }

    private static MovimientoDTO movimiento(int numero, String tipo, BigDecimal importe) {
        return new MovimientoDTO(numero, "2026-11-03", tipo, "Deposito", "INGRESO",
                importe, null, "0001");
    }
}
