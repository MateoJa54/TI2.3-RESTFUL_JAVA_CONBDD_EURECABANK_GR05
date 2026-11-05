package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.RespuestaCostos;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.servicios.ServicioCostos;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la proyeccion de costos de mantenimiento (reglas RB-10, RB-11 y
 * RB-12).
 */
class ServicioCostosTest {

    private static final Instant INICIO = Instant.parse("2026-11-03T10:00:00Z");

    private Dobles.CuentaDAOFalso cuentaDAO;
    private Dobles.CostoDAOFalso costoDAO;
    private Dobles.MonedaDAOFalso monedaDAO;
    private Dobles.ParametroDAOFalso parametroDAO;
    private TokenServicio tokenServicio;
    private ServicioCostos servicio;
    private String token;

    @BeforeEach
    void preparar() {
        cuentaDAO = new Dobles.CuentaDAOFalso()
                .conCuenta(cuenta("00200002", "01", "ACTIVO", "6800.00"));
        costoDAO = new Dobles.CostoDAOFalso()
                .conMantenimiento("01", new BigDecimal("3500.00"), new BigDecimal("7.00"))
                .conCostoMovimiento("01", new BigDecimal("2.00"))
                .conInteres("01", new BigDecimal("0.70"));
        monedaDAO = new Dobles.MonedaDAOFalso().conMoneda("01", "Soles", true);
        parametroDAO = new Dobles.ParametroDAOFalso().conValor("002", new BigDecimal("15"));
        tokenServicio = new TokenServicio(Duration.ofMinutes(30),
                Clock.fixed(INICIO, ZoneOffset.UTC));
        token = tokenServicio.generarToken("0001");
        servicio = new ServicioCostos(cuentaDAO, costoDAO, monedaDAO, parametroDAO, tokenServicio);
    }

    @Test
    void sinTokenDevuelveDat001() {
        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00200002", "");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void conTokenDesconocidoDevuelveAuth003() {
        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00200002", "no-existe");

        assertEquals("AUTH-003", respuesta.getCodigo());
    }

    @Test
    void unaCuentaInexistenteDevuelveCta001() {
        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("99999999", token);

        assertEquals("CTA-001", respuesta.getCodigo());
    }

    @Test
    void unaCuentaNoActivaDevuelveCta002() {
        cuentaDAO.conCuenta(cuenta("00300001", "01", "CANCELADO", "0.00"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00300001", token);

        assertEquals("CTA-002", respuesta.getCodigo());
    }

    @Test
    void unaMonedaNoSoportadaDevuelveMon002() {
        monedaDAO.conMoneda("01", "Soles", false);

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00200002", token);

        assertEquals("MON-002", respuesta.getCodigo());
    }

    @Test
    void laProyeccionDevuelveLosSeisValores() {
        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00200002", token);

        assertEquals("OK", respuesta.getEstado());
        assertEquals("Costos de mantenimiento consultados correctamente", respuesta.getMensaje());
        assertEquals(new BigDecimal("6800.00"), respuesta.getSaldoActual());
        assertEquals(new BigDecimal("0.00"), respuesta.getCostoMantenimiento());
        assertEquals(new BigDecimal("2.00"), respuesta.getCostoMovimiento());
        assertEquals(new BigDecimal("47.60"), respuesta.getInteresProyectado());
        assertEquals(new BigDecimal("3500.00"), respuesta.getLimiteMantenimiento());
        assertEquals(15, respuesta.getOperacionesSinCosto());
    }

    @Test
    void conSaldoIgualAlUmbralSeCobraElMantenimiento() {
        cuentaDAO.conCuenta(cuenta("00700001", "01", "ACTIVO", "3500.00"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00700001", token);

        assertEquals(new BigDecimal("7.00"), respuesta.getCostoMantenimiento());
    }

    @Test
    void conSaldoPorDebajoDelUmbralSeCobraElMantenimiento() {
        cuentaDAO.conCuenta(cuenta("00700001", "01", "ACTIVO", "100.00"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00700001", token);

        assertEquals(new BigDecimal("7.00"), respuesta.getCostoMantenimiento());
        assertEquals(new BigDecimal("0.70"), respuesta.getInteresProyectado());
    }

    @Test
    void conSaldoPorEncimaDelUmbralNoSeCobraElMantenimiento() {
        cuentaDAO.conCuenta(cuenta("00700001", "01", "ACTIVO", "3500.01"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00700001", token);

        assertEquals(new BigDecimal("0.00"), respuesta.getCostoMantenimiento());
    }

    @Test
    void elInteresSeProyectaSobreElSaldoActual() {
        cuentaDAO.conCuenta(cuenta("00700001", "01", "ACTIVO", "10000.00"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00700001", token);

        assertEquals(new BigDecimal("70.00"), respuesta.getInteresProyectado());
    }

    @Test
    void elInteresSeRedondeaADosDecimales() {
        cuentaDAO.conCuenta(cuenta("00700001", "01", "ACTIVO", "1234.56"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00700001", token);

        assertEquals(new BigDecimal("8.64"), respuesta.getInteresProyectado());
    }

    @Test
    void elCobroNoSeEjecutaSoloSeProyecta() {
        // El saldo no cambia: el cobro real es un proceso batch externo (RB-12).
        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00200002", token);

        assertEquals(new BigDecimal("6800.00"),
                cuentaDAO.buscarPorCodigo("00200002").orElseThrow().getSaldo());
        assertTrue(respuesta.getCostoMantenimiento().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void elNumeroDeOperacionesSinCostoVieneDelParametro() {
        parametroDAO.conValor("002", new BigDecimal("20"));

        RespuestaCostos respuesta = servicio.consultarCostosMantenimiento("00200002", token);

        assertEquals(20, respuesta.getOperacionesSinCosto());
    }

    private static CuentaDTO cuenta(String codigo, String moneda, String estado, String saldo) {
        return new CuentaDTO(codigo, moneda, "Soles", new BigDecimal(saldo), estado,
                "00001", "Eric Gustavo Coronel Castillo", "002", "2008-01-09", "123456", 7);
    }
}
