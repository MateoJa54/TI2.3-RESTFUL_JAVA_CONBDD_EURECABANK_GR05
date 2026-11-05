package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.datos.ClienteDAO;
import ec.edu.monster.datos.CostoDAO;
import ec.edu.monster.datos.CuentaDAO;
import ec.edu.monster.datos.IClienteDAO;
import ec.edu.monster.datos.ILogSessionDAO;
import ec.edu.monster.datos.LogSessionDAO;
import ec.edu.monster.datos.MonedaDAO;
import ec.edu.monster.datos.MovimientoDAO;
import ec.edu.monster.datos.ParametroDAO;
import ec.edu.monster.datos.TipoMovimientoDAO;
import ec.edu.monster.datos.UsuarioDAO;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de integracion de los repositorios JDBC contra la base de datos real
 * del laboratorio.
 *
 * <p>Si no esta definida la variable de entorno {@code EUREKABANK_DB_URL} las
 * pruebas se saltan con {@code Assumptions.assumeTrue}: no fallan, simplemente
 * no se ejecutan.
 */
class DaoIntegracionTest {

    private ClienteDAO clienteDAO;
    private CuentaDAO cuentaDAO;
    private MovimientoDAO movimientoDAO;
    private UsuarioDAO usuarioDAO;
    private ParametroDAO parametroDAO;
    private TipoMovimientoDAO tipoMovimientoDAO;
    private MonedaDAO monedaDAO;
    private CostoDAO costoDAO;
    private LogSessionDAO logSessionDAO;

    @BeforeAll
    static void exigirBaseDeDatos() {
        Assumptions.assumeTrue(Integracion.hayBaseDeDatos(),
                "Se necesita la variable de entorno EUREKABANK_DB_URL para las pruebas de integracion");
    }

    @BeforeEach
    void preparar() throws Exception {
        // Cada prueba arranca desde el estado de la semilla.
        Integracion.recargarDatosDePrueba();
        clienteDAO = new ClienteDAO(Integracion.proveedor());
        cuentaDAO = new CuentaDAO(Integracion.proveedor());
        movimientoDAO = new MovimientoDAO(Integracion.proveedor());
        usuarioDAO = new UsuarioDAO(Integracion.proveedor());
        parametroDAO = new ParametroDAO(Integracion.proveedor());
        tipoMovimientoDAO = new TipoMovimientoDAO(Integracion.proveedor());
        monedaDAO = new MonedaDAO(Integracion.proveedor());
        costoDAO = new CostoDAO(Integracion.proveedor());
        logSessionDAO = new LogSessionDAO(Integracion.proveedor());
    }

    @AfterAll
    static void restaurarDatos() throws Exception {
        if (Integracion.hayBaseDeDatos()) {
            Integracion.recargarDatosDePrueba();
        }
    }

    @Test
    void elUsuarioCromeroExisteConSuHashYSuRol() {
        Optional<UsuarioDAO.Usuario> usuario = usuarioDAO.buscarPorNombre("cromero");

        assertTrue(usuario.isPresent());
        assertEquals("0001", usuario.get().codigo());
        assertEquals("891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4",
                usuario.get().hash());
        assertEquals("ACTIVO", usuario.get().estado());
        assertEquals("Carlos Alberto Romero Castillo", usuario.get().nombreEmpleado());
        assertNotNull(usuario.get().rol());
    }

    @Test
    void unUsuarioInexistenteNoSeEncuentra() {
        assertTrue(usuarioDAO.buscarPorNombre("no-existe").isEmpty());
    }

    @Test
    void unUsuarioInactivoTieneSuEstadoANulado() {
        Optional<UsuarioDAO.Usuario> usuario = usuarioDAO.buscarPorNombre("csarmiento");

        assertTrue(usuario.isPresent());
        assertEquals("ANULADO", usuario.get().estado());
    }

    @Test
    void elClienteSeBuscaPorSuDni() {
        Optional<IClienteDAO.DatosCliente> cliente = clienteDAO.buscarPorDni("06914897");

        assertTrue(cliente.isPresent());
        assertEquals("00001", cliente.get().codigo());
        assertEquals("ERIC GUSTAVO CORONEL CASTILLO", cliente.get().nombre());
    }

    @Test
    void unDniInexistenteNoDevuelveCliente() {
        assertTrue(clienteDAO.buscarPorDni("99999999").isEmpty());
    }

    @Test
    void laCuentaSeConsultaConTodosSusDatos() {
        Optional<CuentaDTO> cuenta = cuentaDAO.buscarPorCodigo("00200002");

        assertTrue(cuenta.isPresent());
        CuentaDTO datos = cuenta.get();
        assertEquals("00200002", datos.getCodigo());
        assertEquals("01", datos.getMoneda());
        assertEquals("Soles", datos.getDescripcionMoneda());
        assertEquals("ACTIVO", datos.getEstado());
        assertEquals("00001", datos.getCodigoCliente());
        assertEquals("002", datos.getCodigoSucursal());
        assertEquals("2008-01-09", datos.getFechaCreacion());
        assertEquals("123456", datos.getClave());
        assertEquals(7, datos.getOperacionesRealizadas());
        assertEquals(2, datos.getSaldo().scale());
    }

    @Test
    void elSaldoDeLaCuentaEsElDeLaSemilla() {
        assertEquals(new BigDecimal("6800.00"), cuentaDAO.buscarPorCodigo("00200002")
                .orElseThrow().getSaldo());
    }

    @Test
    void lasCuentasDeUnClienteSeListanConSuEstado() {
        List<CuentaDTO> cuentas = cuentaDAO.listarPorCliente("00010");

        assertEquals(1, cuentas.size());
        assertEquals("00300001", cuentas.get(0).getCodigo());
        assertEquals("CANCELADO", cuentas.get(0).getEstado());
    }

    @Test
    void unClienteSinCuentasDevuelveUnaListaVacia() {
        assertTrue(cuentaDAO.listarPorCliente("99999").isEmpty());
    }

    @Test
    void losMovimientosDeUnaCuentaVienenOrdenados() {
        List<MovimientoDTO> movimientos = movimientoDAO.listarPorCuenta("00200002");

        assertEquals(7, movimientos.size());
        assertEquals(1, movimientos.get(0).getNumero());
        assertEquals("001", movimientos.get(0).getTipoCodigo());
        assertEquals("INGRESO", movimientos.get(0).getTipoAccion());
        assertEquals(7, movimientos.get(6).getNumero());
    }

    @Test
    void losMovimientosTraenLaDescripcionDelTipo() {
        List<MovimientoDTO> movimientos = movimientoDAO.listarPorCuenta("00200002");

        assertEquals("Apertura de Cuenta", movimientos.get(0).getTipoDescripcion());
        assertEquals("Deposito", movimientos.get(1).getTipoDescripcion());
        assertEquals("0001", movimientos.get(0).getEmpleado());
    }

    @Test
    void unaCuentaSinMovimientosDevuelveUnaListaVacia() {
        assertTrue(movimientoDAO.listarPorCuenta("99999999").isEmpty());
    }

    @Test
    void elParametroDelItfEsElOchoPorCien() {
        assertEquals(new BigDecimal("0.08"), parametroDAO.valor("001").orElseThrow());
    }

    @Test
    void elParametroDeOperacionesSinCostoEsQuince() {
        assertEquals(new BigDecimal("15"), parametroDAO.valor("002").orElseThrow());
    }

    @Test
    void unParametroInexistenteNoDevuelveValor() {
        assertTrue(parametroDAO.valor("999").isEmpty());
    }

    @Test
    void elCatalogoDeTiposDeMovimientoEstaCompleto() {
        assertEquals("Deposito", tipoMovimientoDAO.buscarPorCodigo("003").orElseThrow().descripcion());
        assertEquals("INGRESO", tipoMovimientoDAO.buscarPorCodigo("003").orElseThrow().accion());
        assertEquals("SALIDA", tipoMovimientoDAO.buscarPorCodigo("009").orElseThrow().accion());
        assertEquals("Transferencia", tipoMovimientoDAO.buscarPorCodigo("008").orElseThrow().descripcion());
        assertTrue(tipoMovimientoDAO.buscarPorCodigo("999").isEmpty());
    }

    @Test
    void lasMonedasDelCatalogoSeConsultanPorCodigo() {
        assertEquals("Soles", monedaDAO.descripcion("01").orElseThrow());
        assertEquals("Dolares", monedaDAO.descripcion("02").orElseThrow());
        assertTrue(monedaDAO.descripcion("99").isEmpty());
    }

    @Test
    void lasMonedasDeLaSemillaEstanSoportadas() {
        assertTrue(monedaDAO.estaSoportada("01"));
        assertTrue(monedaDAO.estaSoportada("02"));
        assertFalse(monedaDAO.estaSoportada("99"));
    }

    @Test
    void elCargoDeMantenimientoSeConsultaPorMoneda() {
        CostoDAO.CostoMantenimiento mantenimiento =
                costoDAO.costoMantenimiento("01").orElseThrow();

        assertEquals(new BigDecimal("3500.00"), mantenimiento.montoMaximo());
        assertEquals(new BigDecimal("7.00"), mantenimiento.importe());
    }

    @Test
    void elCostoPorMovimientoYElInteresSeConsultanPorMoneda() {
        assertEquals(new BigDecimal("2.00"), costoDAO.costoMovimiento("01").orElseThrow());
        assertEquals(new BigDecimal("0.70"), costoDAO.interesMensual("01").orElseThrow());
        assertTrue(costoDAO.costoMovimiento("99").isEmpty());
    }

    @Test
    void laSesionSeRegistraYSeCierraEnLaAuditoria() {
        Integer id = logSessionDAO.registrarIngreso("0001", "127.0.0.1", "laboratorio");

        assertNotNull(id);
        assertTrue(logSessionDAO.registrarSalida(id));
    }

    @Test
    void laBaseDeDatosNoTieneCuentasConSaldoNegativo() throws Exception {
        Number negativas = (Number) Integracion.valorUnico(
                "SELECT COUNT(*) FROM cuenta WHERE dec_cuensaldo < 0");

        assertNotNull(negativas);
        assertEquals(0, negativas.intValue());
    }

    @Test
    void laBaseDeDatosTieneLasDiecisieteTablasDelContrato() throws Exception {
        Number tablas = (Number) Integracion.valorUnico(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()");

        assertNotNull(tablas);
        assertEquals(17, tablas.intValue());
    }
}
