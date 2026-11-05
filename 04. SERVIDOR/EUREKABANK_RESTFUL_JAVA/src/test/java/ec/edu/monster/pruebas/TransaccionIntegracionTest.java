package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.datos.CostoDAO;
import ec.edu.monster.datos.CuentaDAO;
import ec.edu.monster.datos.MonedaDAO;
import ec.edu.monster.datos.MovimientoDAO;
import ec.edu.monster.datos.ParametroDAO;
import ec.edu.monster.datos.ProveedorConexion;
import ec.edu.monster.datos.TipoMovimientoDAO;
import ec.edu.monster.datos.UnidadTrabajoJdbc;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import ec.edu.monster.modelos.RespuestaMovimiento;
import ec.edu.monster.modelos.RespuestaTransferencia;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.servicios.ServicioMovimientos;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de integracion de la transaccion de movimientos contra la base de
 * datos real (reglas RB-02, RB-03 y RB-04).
 *
 * <p>Si no esta definida la variable de entorno {@code EUREKABANK_DB_URL} las
 * pruebas se saltan con {@code Assumptions.assumeTrue}.
 */
class TransaccionIntegracionTest {

    private static final String ORIGEN = "00200002";
    private static final String DESTINO = "00200001";

    private CuentaDAO cuentaDAO;
    private MovimientoDAO movimientoDAO;
    private TokenServicio tokenServicio;
    private ServicioMovimientos servicio;
    private String token;

    @BeforeAll
    static void exigirBaseDeDatos() {
        Assumptions.assumeTrue(Integracion.hayBaseDeDatos(),
                "Se necesita la variable de entorno EUREKABANK_DB_URL para las pruebas de integracion");
    }

    @BeforeEach
    void preparar() throws Exception {
        // Cada prueba arranca desde el estado de la semilla.
        Integracion.recargarDatosDePrueba();
        ProveedorConexion proveedor = Integracion.proveedor();
        cuentaDAO = new CuentaDAO(proveedor);
        movimientoDAO = new MovimientoDAO(proveedor);
        tokenServicio = new TokenServicio(Duration.ofMinutes(30), Clock.systemUTC());
        token = tokenServicio.generarToken("0001");
        servicio = new ServicioMovimientos(cuentaDAO, movimientoDAO,
                new TipoMovimientoDAO(proveedor), new ParametroDAO(proveedor),
                new CostoDAO(proveedor), new MonedaDAO(proveedor),
                new UnidadTrabajoJdbc(proveedor), tokenServicio);
    }

    @AfterAll
    static void restaurarDatos() throws Exception {
        if (Integracion.hayBaseDeDatos()) {
            Integracion.recargarDatosDePrueba();
        }
    }

    @Test
    void unDepositoSumaElSaldoYRegistraElMovimiento() throws Exception {
        BigDecimal saldoInicial = Integracion.saldoDe(ORIGEN);
        int movimientosIniciales = Integracion.contar(
                "SELECT COUNT(*) FROM movimiento WHERE chr_cuencodigo = ?", ORIGEN);

        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal("100.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(saldoInicial.add(new BigDecimal("100.00")),
                Integracion.saldoDe(ORIGEN));
        assertEquals(movimientosIniciales + 1,
                Integracion.contar("SELECT COUNT(*) FROM movimiento WHERE chr_cuencodigo = ?",
                        ORIGEN));
    }

    @Test
    void elNumeroDelMovimientoEsElContadorAnteriorMasUno() throws Exception {
        int contador = cuentaDAO.buscarPorCodigo(ORIGEN).orElseThrow()
                .getOperacionesRealizadas();

        RespuestaMovimiento respuesta =
                servicio.realizarDeposito(ORIGEN, new BigDecimal("10.00"), token);

        assertEquals(contador + 1, respuesta.getMovimiento().getNumero());
    }

    @Test
    void unRetiroSinSaldoDevuelveSal001YNoCambiaElSaldo() throws Exception {
        BigDecimal saldoInicial = Integracion.saldoDe(ORIGEN);

        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("999999.00"), token);

        assertEquals("SAL-001", respuesta.getCodigo());
        assertEquals(saldoInicial, Integracion.saldoDe(ORIGEN));
    }

    @Test
    void unRetiroValidoRestaElSaldo() throws Exception {
        BigDecimal saldoInicial = Integracion.saldoDe(ORIGEN);

        RespuestaMovimiento respuesta =
                servicio.realizarRetiro(ORIGEN, new BigDecimal("25.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(saldoInicial.subtract(new BigDecimal("25.00")), Integracion.saldoDe(ORIGEN));
    }

    @Test
    void elSaldoNuncaQuedaNegativo() throws Exception {
        servicio.realizarRetiro(ORIGEN, new BigDecimal("999999.00"), token);

        BigDecimal saldo = Integracion.saldoDe(ORIGEN);
        assertTrue(saldo.compareTo(BigDecimal.ZERO) >= 0, "El saldo quedo negativo: " + saldo);
    }

    @Test
    void laTransferenciaMueveElDineroEnUnaSolaTransaccion() throws Exception {
        BigDecimal saldoOrigenInicial = Integracion.saldoDe(ORIGEN);
        BigDecimal saldoDestinoInicial = Integracion.saldoDe(DESTINO);
        int numeroOrigen = cuentaDAO.buscarPorCodigo(ORIGEN).orElseThrow()
                .getOperacionesRealizadas() + 1;
        int numeroDestino = cuentaDAO.buscarPorCodigo(DESTINO).orElseThrow()
                .getOperacionesRealizadas() + 1;
        BigDecimal costoOrigen = numeroOrigen > 15 ? new BigDecimal("2.00") : BigDecimal.ZERO;
        BigDecimal costoDestino = numeroDestino > 15 ? new BigDecimal("2.00") : BigDecimal.ZERO;

        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(saldoOrigenInicial.subtract(new BigDecimal("50.00"))
                .subtract(respuesta.getItfAplicado()).subtract(costoOrigen),
                Integracion.saldoDe(ORIGEN));
        assertEquals(saldoDestinoInicial.add(new BigDecimal("50.00")).subtract(costoDestino),
                Integracion.saldoDe(DESTINO));
    }

    @Test
    void laTransferenciaRegistraLaSalidaYLaEntradaConReferenciaCruzada() throws Exception {
        servicio.realizarTransferencia(ORIGEN, DESTINO, new BigDecimal("50.00"), token);

        Number salidas = (Number) Integracion.valorUnico(
                "SELECT COUNT(*) FROM movimiento WHERE chr_cuencodigo = ? AND chr_tipocodigo = '009' "
                + "AND chr_cuenreferencia = ?", ORIGEN, DESTINO);
        Number entradas = (Number) Integracion.valorUnico(
                "SELECT COUNT(*) FROM movimiento WHERE chr_cuencodigo = ? AND chr_tipocodigo = '008' "
                + "AND chr_cuenreferencia = ?", DESTINO, ORIGEN);

        assertNotNull(salidas);
        assertNotNull(entradas);
        assertTrue(salidas.intValue() >= 1, "No se registro la salida de la transferencia");
        assertTrue(entradas.intValue() >= 1, "No se registro la entrada de la transferencia");
    }

    @Test
    void siElMovimientoNoSeRegistraSeDeshaceTodoElCambio() throws Exception {
        BigDecimal saldoInicial = Integracion.saldoDe(ORIGEN);
        MovimientoDAO movimientoQueFalla = new MovimientoDAO(Integracion.proveedor()) {
            @Override
            public void insertar(Connection conexion, String codigoCuenta, MovimientoDTO movimiento) {
                throw new IllegalStateException("Fallo simulado al registrar el movimiento");
            }
        };
        ServicioMovimientos servicioConFallo = new ServicioMovimientos(cuentaDAO,
                movimientoQueFalla, new TipoMovimientoDAO(Integracion.proveedor()),
                new ParametroDAO(Integracion.proveedor()), new CostoDAO(Integracion.proveedor()),
                new MonedaDAO(Integracion.proveedor()), new UnidadTrabajoJdbc(Integracion.proveedor()),
                tokenServicio);

        assertThrows(IllegalStateException.class,
                () -> servicioConFallo.realizarDeposito(ORIGEN, new BigDecimal("100.00"), token));

        assertEquals(saldoInicial, Integracion.saldoDe(ORIGEN),
                "La transaccion no se deshizo y el saldo quedo cambiado");
    }

    @Test
    void elMovimientoRegistradoLlevaElEmpleadoDeLaSesion() throws Exception {
        servicio.realizarDeposito(ORIGEN, new BigDecimal("5.00"), token);

        List<MovimientoDTO> movimientos = movimientoDAO.listarPorCuenta(ORIGEN);
        MovimientoDTO ultimoDeposito = null;
        for (MovimientoDTO movimiento : movimientos) {
            if ("003".equals(movimiento.getTipoCodigo())) {
                ultimoDeposito = movimiento;
            }
        }

        assertNotNull(ultimoDeposito, "No se registro ningun deposito");
        assertEquals("0001", ultimoDeposito.getEmpleado());
        assertEquals("003", ultimoDeposito.getTipoCodigo());
    }

    @Test
    void elSaldoYElContadorQuedanCoherentesConLosMovimientos() throws Exception {
        servicio.realizarDeposito(ORIGEN, new BigDecimal("15.00"), token);

        BigDecimal saldoCalculado = (BigDecimal) Integracion.valorUnico(
                "SELECT COALESCE(SUM(CASE WHEN t.vch_tipoaccion = 'INGRESO' "
                + "THEN m.dec_moviimporte ELSE -m.dec_moviimporte END), 0) "
                + "FROM movimiento m JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo "
                + "WHERE m.chr_cuencodigo = ?", ORIGEN);

        assertEquals(saldoCalculado, Integracion.saldoDe(ORIGEN));
    }

    @Test
    void laCuentaBloqueadaDevuelveElSaldoActual() throws Exception {
        BigDecimal saldoInicial = Integracion.saldoDe(ORIGEN);

        CuentaDTO bloqueada;
        try (Connection conexion = Integracion.proveedor().obtenerConexion()) {
            bloqueada = cuentaDAO.bloquearPorCodigo(conexion, ORIGEN).orElseThrow();
        }

        assertEquals(saldoInicial, bloqueada.getSaldo());
    }

    @Test
    void unaMonedaDistintaNoDejaTransferir() throws Exception {
        RespuestaTransferencia respuesta =
                servicio.realizarTransferencia(ORIGEN, "00200003", new BigDecimal("50.00"), token);

        assertEquals("CTA-004", respuesta.getCodigo());
        assertFalse(respuesta.getItfAplicado().compareTo(BigDecimal.ZERO) > 0);
    }
}
