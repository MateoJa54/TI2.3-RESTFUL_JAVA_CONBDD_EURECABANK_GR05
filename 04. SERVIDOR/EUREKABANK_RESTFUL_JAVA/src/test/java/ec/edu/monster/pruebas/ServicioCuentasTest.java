package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.RespuestaCuenta;
import ec.edu.monster.modelos.RespuestaCuentas;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.servicios.ServicioCuentas;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del servicio de consulta de cuentas (orden de validacion 6.1 y 6.3).
 */
class ServicioCuentasTest {

    private static final Instant INICIO = Instant.parse("2026-11-03T10:00:00Z");

    private Dobles.ClienteDAOFalso clienteDAO;
    private Dobles.CuentaDAOFalso cuentaDAO;
    private TokenServicio tokenServicio;
    private ServicioCuentas servicio;
    private String token;

    @BeforeEach
    void preparar() {
        clienteDAO = new Dobles.ClienteDAOFalso()
                .conCliente("06914897", "00001", "Eric Gustavo Coronel Castillo");
        cuentaDAO = new Dobles.CuentaDAOFalso()
                .conCuenta(cuenta("00200002", "00001", "ACTIVO", "6800.00", 7))
                .conCuenta(cuenta("00300001", "00010", "CANCELADO", "0.00", 3));
        tokenServicio = new TokenServicio(Duration.ofMinutes(30),
                Clock.fixed(INICIO, ZoneOffset.UTC));
        token = tokenServicio.generarToken("0001");
        servicio = new ServicioCuentas(clienteDAO, cuentaDAO, tokenServicio);
    }

    @Test
    void sinTokenDevuelveDat001() {
        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente("06914897", "");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void conUnTokenDesconocidoDevuelveAuth003() {
        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente("06914897", "no-existe");

        assertEquals("AUTH-003", respuesta.getCodigo());
    }

    @Test
    void conUnTokenVencidoDevuelveAuth004() {
        RelojManual reloj = new RelojManual(INICIO);
        TokenServicio conReloj = new TokenServicio(Duration.ofMinutes(30), reloj);
        ServicioCuentas servicioConReloj = new ServicioCuentas(clienteDAO, cuentaDAO, conReloj);
        String tokenVencido = conReloj.generarToken("0001");
        reloj.avanzar(Duration.ofMinutes(31));

        RespuestaCuentas respuesta = servicioConReloj.listarCuentasPorCliente("06914897", tokenVencido);

        assertEquals("AUTH-004", respuesta.getCodigo());
    }

    @Test
    void elTokenSeRevisaAntesQueLosDatos() {
        // Sin token y con cedula vacia: el error es el del token.
        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente("", "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unaCedulaVaciaDevuelveDat001(String cedula) {
        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente(cedula, token);

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unDniInexistenteDevuelveCli001() {
        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente("99999999", token);

        assertEquals("CLI-001", respuesta.getCodigo());
        assertEquals("El cliente solicitado no existe", respuesta.getMensaje());
        assertTrue(respuesta.getCuentas().isEmpty());
    }

    @Test
    void unClienteExistenteDevuelveSusCuentas() {
        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente("06914897", token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Cuentas consultadas correctamente", respuesta.getMensaje());
        assertEquals("06914897", respuesta.getCedula());
        assertEquals("Eric Gustavo Coronel Castillo", respuesta.getNombreCliente());
        assertEquals(1, respuesta.getCuentas().size());
        assertEquals("00200002", respuesta.getCuentas().get(0).getCodigo());
    }

    @Test
    void elListadoIncluyeLasCuentasQueNoEstanActivas() {
        clienteDAO.conCliente("10192376", "00010", "Gabriel Alejandro Gonzales Garcia");

        RespuestaCuentas respuesta = servicio.listarCuentasPorCliente("10192376", token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(1, respuesta.getCuentas().size());
        assertEquals("CANCELADO", respuesta.getCuentas().get(0).getEstado());
    }

    @Test
    void laCuentaConsultadaDevuelveTodosSusDatos() {
        RespuestaCuenta respuesta = servicio.consultarCuenta("00200002", token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("Cuenta consultada correctamente", respuesta.getMensaje());
        assertNotNull(respuesta.getCuenta());
        assertEquals("00200002", respuesta.getCuenta().getCodigo());
        assertEquals("01", respuesta.getCuenta().getMoneda());
        assertEquals("Soles", respuesta.getCuenta().getDescripcionMoneda());
        assertEquals(new BigDecimal("6800.00"), respuesta.getCuenta().getSaldo());
        assertEquals("00001", respuesta.getCuenta().getCodigoCliente());
        assertEquals("002", respuesta.getCuenta().getCodigoSucursal());
        assertEquals("2008-01-09", respuesta.getCuenta().getFechaCreacion());
        assertEquals("123456", respuesta.getCuenta().getClave());
        assertEquals(7, respuesta.getCuenta().getOperacionesRealizadas());
    }

    @Test
    void unaCuentaInexistenteDevuelveCta001() {
        RespuestaCuenta respuesta = servicio.consultarCuenta("99999999", token);

        assertEquals("CTA-001", respuesta.getCodigo());
        assertEquals("La cuenta solicitada no existe", respuesta.getMensaje());
    }

    @Test
    void unaCuentaAnuladaDevuelveCta002() {
        RespuestaCuenta respuesta = servicio.consultarCuenta("00300001", token);

        assertEquals("CTA-002", respuesta.getCodigo());
        assertEquals("La cuenta no se encuentra en estado ACTIVO", respuesta.getMensaje());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unaCuentaVaciaDevuelveDat001(String cuenta) {
        RespuestaCuenta respuesta = servicio.consultarCuenta(cuenta, token);

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void consultarUnaCuentaSinTokenDevuelveDat001() {
        RespuestaCuenta respuesta = servicio.consultarCuenta("00200002", "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    private static CuentaDTO cuenta(String codigo, String cliente, String estado,
            String saldo, int movimientos) {
        return new CuentaDTO(codigo, "01", "Soles", new BigDecimal(saldo), estado,
                cliente, "Eric Gustavo Coronel Castillo", "002", "2008-01-09", "123456",
                movimientos);
    }
}
