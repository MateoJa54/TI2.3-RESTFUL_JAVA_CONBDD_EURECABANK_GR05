package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import ec.edu.monster.modelos.RespuestaMovimiento;
import ec.edu.monster.modelos.RespuestaMovimientos;
import ec.edu.monster.modelos.RespuestaTransferencia;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.servicios.ServicioMovimientos;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del servicio de movimientos (reglas RB-01 a RB-09 y orden de
 * validacion 6.1).
 */
class ServicioMovimientosTest {

    private static final Instant INICIO = Instant.parse("2026-11-03T10:00:00Z");
    private static final String ORIGEN = "00200002";
    private static final String DESTINO = "00200001";

    private Dobles.CuentaDAOFalso cuentaDAO;
    private Dobles.MovimientoDAOFalso movimientoDAO;
    private Dobles.ParametroDAOFalso parametroDAO;
    private Dobles.CostoDAOFalso costoDAO;
    private Dobles.MonedaDAOFalso monedaDAO;
    private TokenServicio tokenServicio;
    private ServicioMovimientos servicio;
    private String token;

    @BeforeEach
    void preparar() {
        cuentaDAO = new Dobles.CuentaDAOFalso()
                .conCuenta(cuenta(ORIGEN, "01", "ACTIVO", "6800.00", 7))
                .conCuenta(cuenta(DESTINO, "01", "ACTIVO", "7000.00", 20));
        movimientoDAO = new Dobles.MovimientoDAOFalso();
        parametroDAO = new Dobles.ParametroDAOFalso()
                .conValor("001", new BigDecimal("0.08"))
                .conValor("002", new BigDecimal("15"));
        costoDAO = new Dobles.CostoDAOFalso()
                .conMantenimiento("01", new BigDecimal("3500.00"), new BigDecimal("7.00"))
                .conCostoMovimiento("01", new BigDecimal("2.00"))
                .conInteres("01", new BigDecimal("0.70"));
        monedaDAO = new Dobles.MonedaDAOFalso().conMoneda("01", "Soles", true);
        tokenServicio = new TokenServicio(Duration.ofMinutes(30),
                Clock.fixed(INICIO, ZoneOffset.UTC));
        token = tokenServicio.generarToken("0001");
        servicio = new ServicioMovimientos(cuentaDAO, movimientoDAO,
                new Dobles.TipoMovimientoDAOFalso(), parametroDAO, costoDAO, monedaDAO,
                new Dobles.UnidadTrabajoFalsa(), tokenServicio);
    }

    // ------------------------------------------------------------------
    // Consulta de movimientos
    // ------------------------------------------------------------------

    @Test
    void consultarMovimientosSinTokenDevuelveDat001() {
        RespuestaMovimientos respuesta = servicio.consultarMovimientos(ORIGEN, "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void consultarMovimientosConTokenDesconocidoDevuelveAuth003() {
        RespuestaMovimientos respuesta = servicio.consultarMovimientos(ORIGEN, "no-existe");

        assertEquals("AUTH-003", respuesta.getCodigo());
    }

    @Test
    void consultarMovimientosDeUnaCuentaInexistenteDevuelveCta001() {
        RespuestaMovimientos respuesta = servicio.consultarMovimientos("99999999", token);

        assertEquals("CTA-001", respuesta.getCodigo());
    }

    @Test
    void consultarMovimientosDeUnaCuentaAnuladaDevuelveCta002() {
        cuentaDAO.conCuenta(cuenta("00300001", "01", "CANCELADO", "0.00", 3));

        RespuestaMovimientos respuesta = servicio.consultarMovimientos("00300001", token);

        assertEquals("CTA-002", respuesta.getCodigo());
    }

    @Test
    void consultarMovimientosDevuelveElCodigoDeLaCuenta() {
        RespuestaMovimientos respuesta = servicio.consultarMovimientos(ORIGEN, token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("Movimientos consultados correctamente", respuesta.getMensaje());
        assertEquals(ORIGEN, respuesta.getCuenta());
    }

    // ------------------------------------------------------------------
    // Deposito
    // ------------------------------------------------------------------

    @Test
    void unDepositoSinTokenDevuelveDat001() {
        RespuestaMovimiento respuesta = servicio.realizarDeposito(ORIGEN, new BigDecimal("100.00"), "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unDepositoSinCuentaDevuelveDat001() {
        RespuestaMovimiento respuesta = servicio.realizarDeposito("", new BigDecimal("100.00"), token);

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unDepositoSinMontoDevuelveDat001() {
        RespuestaMovimiento respuesta = servicio.realizarDeposito(ORIGEN, null, token);

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-10.00", "1.001", "-0.01"})
    void unMontoInvalidoDevuelveMon001(String monto) {
        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal(monto), token);

        assertEquals("MON-001", respuesta.getCodigo());
        assertEquals("El monto debe ser mayor que cero y con máximo dos decimales",
                respuesta.getMensaje());
    }

    @Test
    void unDepositoEnUnaCuentaInexistenteDevuelveCta001() {
        RespuestaMovimiento respuesta =
                servicio.realizarDeposito("99999999", new BigDecimal("100.00"), token);

        assertEquals("CTA-001", respuesta.getCodigo());
    }

    @Test
    void unDepositoEnUnaCuentaNoActivaDevuelveCta002() {
        cuentaDAO.conCuenta(cuenta("00300001", "01", "CANCELADO", "0.00", 3));

        RespuestaMovimiento respuesta =
                servicio.realizarDeposito("00300001", new BigDecimal("100.00"), token);

        assertEquals("CTA-002", respuesta.getCodigo());
    }

    @Test
    void unDepositoEnUnaMonedaNoSoportadaDevuelveMon002() {
        monedaDAO.conMoneda("01", "Soles", false);

        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal("100.00"), token);

        assertEquals("MON-002", respuesta.getCodigo());
    }

    @Test
    void unDepositoSumaElSaldoYRegistraElMovimiento() {
        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal("100.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("Depósito realizado correctamente", respuesta.getMensaje());
        assertEquals(new BigDecimal("6900.00"), respuesta.getSaldoNuevo());
        assertNotNull(respuesta.getMovimiento());
        assertEquals(8, respuesta.getMovimiento().getNumero());
        assertEquals("003", respuesta.getMovimiento().getTipoCodigo());
        assertEquals("INGRESO", respuesta.getMovimiento().getTipoAccion());
        assertEquals(new BigDecimal("100.00"), respuesta.getMovimiento().getImporte());
        assertEquals(new BigDecimal("0.00"), respuesta.getCostoAplicado());
        assertEquals(new BigDecimal("0.00"), respuesta.getItfAplicado());
    }

    @Test
    void unDepositoActualizaElSaldoEnLaCuenta() {
        servicio.realizarDeposito(ORIGEN, new BigDecimal("100.00"), token);

        assertEquals(new BigDecimal("6900.00"),
                cuentaDAO.buscarPorCodigo(ORIGEN).orElseThrow().getSaldo());
        assertEquals(8, cuentaDAO.buscarPorCodigo(ORIGEN).orElseThrow().getOperacionesRealizadas());
    }

    @Test
    void unDepositoRegistraElMovimientoEnElLibroDeLaCuenta() {
        servicio.realizarDeposito(ORIGEN, new BigDecimal("100.00"), token);

        assertEquals(1, movimientoDAO.getMovimientosDe(ORIGEN).size());
        assertEquals("003", movimientoDAO.getMovimientosDe(ORIGEN).get(0).getTipoCodigo());
    }

    @Test
    void unDepositoNoCobraItf() {
        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal("1000.00"), token);

        assertEquals(new BigDecimal("0.00"), respuesta.getItfAplicado());
    }

    @Test
    void unDepositoEnElMovimientoQuinceNoCobraCosto() {
        cuentaDAO.conCuenta(cuenta("00500001", "01", "ACTIVO", "100.00", 14));

        RespuestaMovimiento respuesta =
                servicio.realizarDeposito("00500001", new BigDecimal("100.00"), token);

        assertEquals(15, respuesta.getMovimiento().getNumero());
        assertEquals(new BigDecimal("0.00"), respuesta.getCostoAplicado());
        assertEquals(1, movimientoDAO.getMovimientosDe("00500001").size());
    }

    @Test
    void unDepositoEnElMovimientoDieciseisCobraCostoPorMovimiento() {
        cuentaDAO.conCuenta(cuenta("00500001", "01", "ACTIVO", "100.00", 15));

        RespuestaMovimiento respuesta =
                servicio.realizarDeposito("00500001", new BigDecimal("100.00"), token);

        assertEquals(16, respuesta.getMovimiento().getNumero());
        assertEquals(new BigDecimal("2.00"), respuesta.getCostoAplicado());
        assertEquals(new BigDecimal("198.00"), respuesta.getSaldoNuevo());
        assertEquals(2, movimientoDAO.getMovimientosDe("00500001").size());
        assertEquals("010", movimientoDAO.getMovimientosDe("00500001").get(1).getTipoCodigo());
        assertEquals("SALIDA", movimientoDAO.getMovimientosDe("00500001").get(1).getTipoAccion());
    }

    // ------------------------------------------------------------------
    // Retiro
    // ------------------------------------------------------------------

    @Test
    void unRetiroSinTokenDevuelveDat001() {
        RespuestaMovimiento respuesta = servicio.realizarRetiro(ORIGEN, new BigDecimal("10.00"), "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unRetiroConSaldoInsuficienteDevuelveSal001() {
        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("999999.00"), token);

        assertEquals("SAL-001", respuesta.getCodigo());
        assertEquals("El saldo de la cuenta es insuficiente para la operación",
                respuesta.getMensaje());
    }

    @Test
    void unRetiroConSaldoInsuficienteNoCambiaElSaldo() {
        servicio.realizarRetiro(ORIGEN, new BigDecimal("999999.00"), token);

        assertEquals(new BigDecimal("6800.00"),
                cuentaDAO.buscarPorCodigo(ORIGEN).orElseThrow().getSaldo());
        assertTrue(movimientoDAO.getMovimientosDe(ORIGEN).isEmpty());
    }

    @Test
    void unRetiroRestaElSaldoYRegistraElMovimiento() {
        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("1200.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("Retiro realizado correctamente", respuesta.getMensaje());
        assertEquals(new BigDecimal("5600.00"), respuesta.getSaldoNuevo());
        assertEquals("004", respuesta.getMovimiento().getTipoCodigo());
        assertEquals("SALIDA", respuesta.getMovimiento().getTipoAccion());
        assertEquals(new BigDecimal("1200.00"), respuesta.getMovimiento().getImporte());
    }

    @Test
    void unRetiroJustoDelSaldoDejaElSaldoACero() {
        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("6800.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(new BigDecimal("0.00"), respuesta.getSaldoNuevo());
    }

    @Test
    void unRetiroDeUnCentimoSeAcepta() {
        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("0.01"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(new BigDecimal("6799.99"), respuesta.getSaldoNuevo());
    }

    // ------------------------------------------------------------------
    // Transferencia
    // ------------------------------------------------------------------

    @Test
    void unaTransferenciaSinTokenDevuelveDat001() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unaTransferenciaSinCuentaDestinoDevuelveDat001() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, "", new BigDecimal("50.00"), token);

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unaTransferenciaEntreLaMismaCuentaDevuelveCta003() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, ORIGEN, new BigDecimal("50.00"), token);

        assertEquals("CTA-003", respuesta.getCodigo());
        assertEquals("La cuenta origen y la cuenta destino no pueden ser iguales",
                respuesta.getMensaje());
    }

    @Test
    void unaTransferenciaAMonedaDistintaDevuelveCta004() {
        cuentaDAO.conCuenta(cuenta("00200003", "02", "ACTIVO", "6000.00", 8));

        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, "00200003", new BigDecimal("50.00"), token);

        assertEquals("CTA-004", respuesta.getCodigo());
        assertEquals("La cuenta de destino pertenece a una moneda distinta",
                respuesta.getMensaje());
    }

    @Test
    void unaTransferenciaDesdeUnaCuentaInexistenteDevuelveCta001() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia("99999999", DESTINO, new BigDecimal("50.00"), token);

        assertEquals("CTA-001", respuesta.getCodigo());
    }

    @Test
    void unaTransferenciaHaciaUnaCuentaInexistenteDevuelveCta001() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, "99999999", new BigDecimal("50.00"), token);

        assertEquals("CTA-001", respuesta.getCodigo());
    }

    @Test
    void unaTransferenciaConSaldoInsuficienteDevuelveSal001() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("999999.00"), token);

        assertEquals("SAL-001", respuesta.getCodigo());
    }

    @Test
    void unaTransferenciaConSaldoInsuficienteNoMueveDinero() {
        servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("999999.00"), token);

        assertEquals(new BigDecimal("6800.00"),
                cuentaDAO.buscarPorCodigo(ORIGEN).orElseThrow().getSaldo());
        assertEquals(new BigDecimal("7000.00"),
                cuentaDAO.buscarPorCodigo(DESTINO).orElseThrow().getSaldo());
        assertTrue(movimientoDAO.getMovimientos().isEmpty());
    }

    @Test
    void unaTransferenciaMueveElDineroEnUnaSolaOperacion() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("Transferencia realizada correctamente", respuesta.getMensaje());
        assertEquals(8, respuesta.getNumeroOrigen());
        assertEquals(21, respuesta.getNumeroDestino());
        // El ITF del 0,08 % (parametro 001) sobre 50,00 es 0,04 y el cargo por
        // movimiento (numero 21, mas de 15) es 2,00.
        assertEquals(new BigDecimal("0.04"), respuesta.getItfAplicado());
        assertEquals(new BigDecimal("6749.96"), respuesta.getSaldoOrigen());
        assertEquals(new BigDecimal("7048.00"), respuesta.getSaldoDestino());
    }

    @Test
    void unaTransferenciaRegistraLaSalidaYLaEntradaConReferenciaCruzada() {
        servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        MovimientoDTO salida = movimientoDAO.getMovimientosDe(ORIGEN).get(0);
        MovimientoDTO entrada = movimientoDAO.getMovimientosDe(DESTINO).get(0);
        assertEquals("009", salida.getTipoCodigo());
        assertEquals("SALIDA", salida.getTipoAccion());
        assertEquals(DESTINO, salida.getReferencia());
        assertEquals(new BigDecimal("50.00"), salida.getImporte());
        assertEquals("008", entrada.getTipoCodigo());
        assertEquals("INGRESO", entrada.getTipoAccion());
        assertEquals(ORIGEN, entrada.getReferencia());
        assertEquals(new BigDecimal("50.00"), entrada.getImporte());
    }

    @Test
    void unaTransferenciaRegistraElItfYElCargoPorMovimientoEnLasCuentas() {
        servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        // En el origen: salida 009, ITF 007 y cargo por movimiento 010 no hay
        // (el numero 8 no supera las 15 operaciones sin costo).
        assertEquals(2, movimientoDAO.getMovimientosDe(ORIGEN).size());
        assertEquals("009", movimientoDAO.getMovimientosDe(ORIGEN).get(0).getTipoCodigo());
        assertEquals("007", movimientoDAO.getMovimientosDe(ORIGEN).get(1).getTipoCodigo());
        // En el destino: entrada 008 y cargo por movimiento 010 (numero 21).
        assertEquals(2, movimientoDAO.getMovimientosDe(DESTINO).size());
        assertEquals("008", movimientoDAO.getMovimientosDe(DESTINO).get(0).getTipoCodigo());
        assertEquals("010", movimientoDAO.getMovimientosDe(DESTINO).get(1).getTipoCodigo());
    }

    @Test
    void unaTransferenciaCobraElItfSobreElMonto() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("1000.00"), token);

        assertEquals(new BigDecimal("0.80"), respuesta.getItfAplicado());
        assertEquals(new BigDecimal("5799.20"), respuesta.getSaldoOrigen());
        assertEquals(new BigDecimal("7998.00"), respuesta.getSaldoDestino());
    }

    @Test
    void unaTransferenciaConItfRegistraElMovimientoDeImpuesto() {
        servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("1000.00"), token);

        List<MovimientoDTO> origen = movimientoDAO.getMovimientosDe(ORIGEN);
        assertEquals(2, origen.size());
        assertEquals("007", origen.get(1).getTipoCodigo());
        assertEquals(new BigDecimal("0.80"), origen.get(1).getImporte());
    }

    @Test
    void unaTransferenciaCobraElCostoPorMovimientoEnLaCuentaOrigen() {
        // La cuenta origen tiene 15 movimientos: el nuevo es el 16 y se cobra.
        cuentaDAO.conCuenta(cuenta(ORIGEN, "01", "ACTIVO", "6800.00", 15));

        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        assertEquals(16, respuesta.getNumeroOrigen());
        assertEquals(new BigDecimal("6747.96"), respuesta.getSaldoOrigen());
        assertEquals(3, movimientoDAO.getMovimientosDe(ORIGEN).size());
        assertEquals("009", movimientoDAO.getMovimientosDe(ORIGEN).get(0).getTipoCodigo());
        assertEquals("007", movimientoDAO.getMovimientosDe(ORIGEN).get(1).getTipoCodigo());
        assertEquals("010", movimientoDAO.getMovimientosDe(ORIGEN).get(2).getTipoCodigo());
    }

    @Test
    void unaTransferenciaCobraElCostoPorMovimientoEnLaCuentaDestino() {
        // La cuenta destino tiene 20 movimientos: el nuevo es el 21 y se cobra.
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        assertEquals(21, respuesta.getNumeroDestino());
        assertEquals(new BigDecimal("7048.00"), respuesta.getSaldoDestino());
        assertEquals(2, movimientoDAO.getMovimientosDe(DESTINO).size());
    }

    @Test
    void unaTransferenciaConMontoInvalidoDevuelveMon001() {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("0"), token);

        assertEquals("MON-001", respuesta.getCodigo());
    }

    @Test
    void elItfSeCalculaConElParametroDeLaBaseDeDatos() {
        assertEquals(new BigDecimal("0.80"), servicio.calcularItf(new BigDecimal("1000.00")));
        assertEquals(new BigDecimal("0.04"), servicio.calcularItf(new BigDecimal("50.00")));
        assertEquals(new BigDecimal("8.00"), servicio.calcularItf(new BigDecimal("10000.00")));
    }

    @Test
    void elCostoPorMovimientoSeCobraSoloPasadoElUmbral() {
        CuentaDTO cuenta = cuenta("00600001", "01", "ACTIVO", "100.00", 0);

        assertEquals(new BigDecimal("0.00"), servicio.costoPorMovimiento(cuenta, 1));
        assertEquals(new BigDecimal("0.00"), servicio.costoPorMovimiento(cuenta, 15));
        assertEquals(new BigDecimal("2.00"), servicio.costoPorMovimiento(cuenta, 16));
        assertEquals(new BigDecimal("2.00"), servicio.costoPorMovimiento(cuenta, 99));
    }

    // ------------------------------------------------------------------
    // Tope del monto (decimal(12,2)) en deposito, retiro y transferencia
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"1e999", "12345678901234567890", "10000000000.00", "1e-999"})
    void unDepositoFueraDeRangoDevuelveMon001(String monto) {
        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal(monto), token);

        assertEquals("MON-001", respuesta.getCodigo());
        assertEquals("El monto debe ser mayor que cero y con máximo dos decimales",
                respuesta.getMensaje());
        assertEquals(0, movimientoDAO.getMovimientosDe(ORIGEN).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e999", "12345678901234567890", "10000000000.00", "1e-999"})
    void unRetiroFueraDeRangoDevuelveMon001(String monto) {
        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal(monto), token);

        assertEquals("MON-001", respuesta.getCodigo());
        assertEquals(0, movimientoDAO.getMovimientosDe(ORIGEN).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e999", "12345678901234567890", "10000000000.00", "1e-999"})
    void unaTransferenciaFueraDeRangoDevuelveMon001(String monto) {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal(monto), token);

        assertEquals("MON-001", respuesta.getCodigo());
        assertEquals(0, movimientoDAO.getMovimientosDe(ORIGEN).size());
    }

    @Test
    void elMontoMaximoDeLaColumnaSeAceptaSinMon001() {
        RespuestaMovimiento deposito =
                servicio.realizarDeposito(ORIGEN, new BigDecimal("9999999999.99"), token);
        RespuestaMovimiento retiro =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("9999999999.99"), token);
        RespuestaTransferencia transferencia =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("9999999999.99"), token);

        assertNotEquals("MON-001", deposito.getCodigo());
        assertNotEquals("MON-001", retiro.getCodigo());
        assertNotEquals("MON-001", transferencia.getCodigo());
    }

    @Test
    void unCentimoEnDepositoRetiroYTransferenciaSeAcepta() {
        assertEquals("OK", servicio.realizarDeposito(ORIGEN, new BigDecimal("0.01"), token)
                .getEstado());
        assertEquals("OK", servicio.realizarRetiro(ORIGEN, new BigDecimal("0.01"), token)
                .getEstado());
        assertEquals("OK", servicio.realizarTransferencia(ORIGEN, DESTINO,
                new BigDecimal("0.01"), token).getEstado());
    }

    @Test
    void unTokenInvalidoGanaAMon001EnTodasLasOperaciones() {
        BigDecimal fueraDeRango = new BigDecimal("1e999");

        assertEquals("AUTH-003", servicio.realizarDeposito(ORIGEN, fueraDeRango, "no-existe")
                .getCodigo());
        assertEquals("AUTH-003", servicio.realizarRetiro(ORIGEN, fueraDeRango, "no-existe")
                .getCodigo());
        assertEquals("AUTH-003", servicio.realizarTransferencia(ORIGEN, DESTINO, fueraDeRango,
                "no-existe").getCodigo());
    }

    private static CuentaDTO cuenta(String codigo, String moneda, String estado,
            String saldo, int movimientos) {
        return new CuentaDTO(codigo, moneda, "Soles", new BigDecimal(saldo), estado,
                "00001", "Eric Gustavo Coronel Castillo", "002", "2008-01-09", "123456",
                movimientos);
    }
}
