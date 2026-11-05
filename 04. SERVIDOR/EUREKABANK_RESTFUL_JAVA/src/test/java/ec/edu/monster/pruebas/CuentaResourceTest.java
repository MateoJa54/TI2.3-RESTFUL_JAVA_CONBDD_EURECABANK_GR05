package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ec.edu.monster.api.rest.CuentaResource;
import ec.edu.monster.api.rest.ServiciosEurekaBank;
import ec.edu.monster.modelos.MontoRequest;
import ec.edu.monster.modelos.RespuestaBase;
import ec.edu.monster.modelos.RespuestaCuenta;
import ec.edu.monster.modelos.RespuestaCostos;
import ec.edu.monster.modelos.RespuestaMovimiento;
import ec.edu.monster.modelos.RespuestaMovimientos;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;

/** Operaciones 4 a 7 y 9 sobre una cuenta (contrato, secciones 6.1 y 6.5). */
class CuentaResourceTest {

    private static final String AUSENTE = "99999999";

    private ServiciosEurekaBank servicios;
    private CuentaResource recurso;
    private String bearer;

    @BeforeEach
    void preparar() {
        servicios = SimuladosRest.servicios();
        recurso = new CuentaResource(servicios);
        bearer = "Bearer " + SimuladosRest.tokenValido(servicios);
    }

    private static MontoRequest monto(String valor) {
        return new MontoRequest(valor == null ? null : new BigDecimal(valor));
    }

    private static String codigo(Response respuesta) {
        return ((RespuestaBase) respuesta.getEntity()).getCodigo();
    }

    // ---- Consulta de cuenta ---------------------------------------------------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc"})
    void consultarSinCabeceraValidaDevuelveDat001(String cabecera) {
        Response respuesta = recurso.consultarCuenta(SimuladosRest.ORIGEN, cabecera);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void consultarCuentaVaciaDevuelveDat001(String cuenta) {
        Response respuesta = recurso.consultarCuenta(cuenta, bearer);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void consultarConTokenInvalidoDevuelve401ConAuth003() {
        Response respuesta = recurso.consultarCuenta(SimuladosRest.ORIGEN, "Bearer no-existe");

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", codigo(respuesta));
    }

    @Test
    void consultarCuentaInexistenteDevuelve404ConCta001() {
        Response respuesta = recurso.consultarCuenta(AUSENTE, bearer);

        assertEquals(404, respuesta.getStatus());
        assertEquals("CTA-001", codigo(respuesta));
    }

    @Test
    void consultarCuentaInactivaDevuelve409ConCta002() {
        Response respuesta = recurso.consultarCuenta(SimuladosRest.INACTIVA, bearer);

        assertEquals(409, respuesta.getStatus());
        assertEquals("CTA-002", codigo(respuesta));
    }

    @Test
    void consultarCuentaCaminoFelizDevuelve200() {
        Response respuesta = recurso.consultarCuenta(SimuladosRest.ORIGEN, bearer);

        assertEquals(200, respuesta.getStatus());
        RespuestaCuenta cuerpo = assertInstanceOf(RespuestaCuenta.class, respuesta.getEntity());
        assertEquals(SimuladosRest.ORIGEN, cuerpo.getCuenta().getCodigo());
    }

    // ---- Movimientos ----------------------------------------------------------

    @Test
    void movimientosSinCabeceraDevuelveDat001() {
        Response respuesta = recurso.consultarMovimientos(SimuladosRest.ORIGEN, null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void movimientosConCuentaVaciaDevuelveDat001() {
        Response respuesta = recurso.consultarMovimientos("", bearer);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void movimientosConTokenInvalidoDevuelve401() {
        Response respuesta = recurso.consultarMovimientos(SimuladosRest.ORIGEN, "Bearer no-existe");

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", codigo(respuesta));
    }

    @Test
    void movimientosCaminoFelizDevuelve200() {
        Response respuesta = recurso.consultarMovimientos(SimuladosRest.ORIGEN, bearer);

        assertEquals(200, respuesta.getStatus());
        RespuestaMovimientos cuerpo = assertInstanceOf(RespuestaMovimientos.class, respuesta.getEntity());
        assertEquals(SimuladosRest.ORIGEN, cuerpo.getCuenta());
    }

    // ---- Depósitos ------------------------------------------------------------

    @Test
    void depositoSinMontoConTokenInvalidoDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.realizarDeposito("00200002", "Bearer token-invalido", monto(null));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void depositoSinCuerpoDevuelveDat001() {
        Response respuesta = recurso.realizarDeposito(SimuladosRest.ORIGEN, bearer, null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void depositoSinCabeceraDevuelveDat001() {
        Response respuesta = recurso.realizarDeposito(SimuladosRest.ORIGEN, null, monto("100.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void depositoConCuentaVaciaDevuelveDat001() {
        Response respuesta = recurso.realizarDeposito(" ", bearer, monto("100.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void depositoConTokenInvalidoYMontoValidoDevuelve401ConAuth003() {
        Response respuesta = recurso.realizarDeposito(SimuladosRest.ORIGEN, "Bearer no-existe", monto("100.00"));

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", codigo(respuesta));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00", "10.001"})
    void depositoConMontoNoValidoDevuelve400ConMon001(String valor) {
        Response respuesta = recurso.realizarDeposito(SimuladosRest.ORIGEN, bearer, monto(valor));

        assertEquals(400, respuesta.getStatus());
        assertEquals("MON-001", codigo(respuesta));
    }

    @Test
    void depositoEnCuentaInactivaDevuelve409ConCta002() {
        Response respuesta = recurso.realizarDeposito(SimuladosRest.INACTIVA, bearer, monto("100.00"));

        assertEquals(409, respuesta.getStatus());
        assertEquals("CTA-002", codigo(respuesta));
    }

    @Test
    void depositoCaminoFelizDevuelve200ConElSaldoNuevo() {
        Response respuesta = recurso.realizarDeposito(SimuladosRest.ORIGEN, bearer, monto("100.00"));

        assertEquals(200, respuesta.getStatus());
        RespuestaMovimiento cuerpo = assertInstanceOf(RespuestaMovimiento.class, respuesta.getEntity());
        assertEquals(0, new BigDecimal("6900.00").compareTo(cuerpo.getSaldoNuevo()));
    }

    // ---- Retiros --------------------------------------------------------------

    @Test
    void retiroSinCuerpoDevuelveDat001() {
        Response respuesta = recurso.realizarRetiro(SimuladosRest.ORIGEN, bearer, null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void retiroConTokenInvalidoDevuelve401ConAuth003() {
        Response respuesta = recurso.realizarRetiro(SimuladosRest.ORIGEN, "Bearer no-existe", monto("25.00"));

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", codigo(respuesta));
    }

    @Test
    void retiroMayorQueElSaldoDevuelve409ConSal001() {
        Response respuesta = recurso.realizarRetiro(SimuladosRest.ORIGEN, bearer, monto("999999.00"));

        assertEquals(409, respuesta.getStatus());
        assertEquals("SAL-001", codigo(respuesta));
    }

    @Test
    void retiroCaminoFelizDevuelve200() {
        Response respuesta = recurso.realizarRetiro(SimuladosRest.ORIGEN, bearer, monto("25.00"));

        assertEquals(200, respuesta.getStatus());
        assertEquals("200", codigo(respuesta));
    }

    // ---- Costos ---------------------------------------------------------------

    @Test
    void costosSinCabeceraDevuelveDat001() {
        Response respuesta = recurso.consultarCostosMantenimiento(SimuladosRest.ORIGEN, null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void costosDeCuentaInexistenteDevuelve404ConCta001() {
        Response respuesta = recurso.consultarCostosMantenimiento(AUSENTE, bearer);

        assertEquals(404, respuesta.getStatus());
        assertEquals("CTA-001", codigo(respuesta));
    }

    @Test
    void costosCaminoFelizDevuelve200() {
        Response respuesta = recurso.consultarCostosMantenimiento(SimuladosRest.ORIGEN, bearer);

        assertEquals(200, respuesta.getStatus());
        RespuestaCostos cuerpo = assertInstanceOf(RespuestaCostos.class, respuesta.getEntity());
        assertEquals("200", cuerpo.getCodigo());
    }

    @Test
    void consultarCuentaVaciaConTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.consultarCuenta("", "Bearer no-existe");

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void movimientosConCuentaVaciaYTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.consultarMovimientos("", "Bearer no-existe");

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void retiroSinMontoConTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.realizarRetiro(SimuladosRest.ORIGEN, "Bearer no-existe", monto(null));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void costosConCuentaVaciaYTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.consultarCostosMantenimiento("", "Bearer no-existe");

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void tokenVencidoDevuelve401ConAuth004() {
        RelojManual reloj = new RelojManual(SimuladosRest.INICIO);
        ServiciosEurekaBank conReloj = SimuladosRest.servicios(reloj);
        String token = SimuladosRest.tokenValido(conReloj);
        reloj.avanzar(Duration.ofMinutes(30));

        Response respuesta = new CuentaResource(conReloj).consultarCuenta(SimuladosRest.ORIGEN, "Bearer " + token);

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-004", codigo(respuesta));
    }
}
