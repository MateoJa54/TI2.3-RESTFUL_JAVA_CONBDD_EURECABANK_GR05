package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.monster.api.rest.ServiciosEurekaBank;
import ec.edu.monster.api.rest.TransferenciaResource;
import ec.edu.monster.modelos.RespuestaBase;
import ec.edu.monster.modelos.TransferenciaRequest;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Operacion 8: transferencia (contrato, seccion 6.1). */
class TransferenciaResourceTest {

    private ServiciosEurekaBank servicios;
    private TransferenciaResource recurso;
    private String bearer;

    @BeforeEach
    void preparar() {
        servicios = SimuladosRest.servicios();
        recurso = new TransferenciaResource(servicios);
        bearer = "Bearer " + SimuladosRest.tokenValido(servicios);
    }

    private static TransferenciaRequest pedido(String origen, String destino, String monto) {
        return new TransferenciaRequest(origen, destino, monto == null ? null : new BigDecimal(monto));
    }

    private static String codigo(Response respuesta) {
        return ((RespuestaBase) respuesta.getEntity()).getCodigo();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Bearer "})
    void cabeceraAusenteOMalFormadaDevuelveDat001(String cabecera) {
        Response respuesta = recurso.realizarTransferencia(cabecera,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, "50.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void cuerpoAusenteDevuelveDat001() {
        Response respuesta = recurso.realizarTransferencia(bearer, null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void cuentaOrigenVaciaDevuelveDat001(String origen) {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(origen, SimuladosRest.DESTINO, "50.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void cuentaDestinoVaciaDevuelveDat001(String destino) {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, destino, "50.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void montoAusenteDevuelveDat001() {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, null));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void tokenInvalidoConCuerpoValidoDevuelve401ConAuth003() {
        Response respuesta = recurso.realizarTransferencia("Bearer no-existe",
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, "50.00"));

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", codigo(respuesta));
    }

    @Test
    void cuentaOrigenIgualADestinoDevuelve400ConCta003() {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.ORIGEN, "50.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("CTA-003", codigo(respuesta));
    }

    @Test
    void montoNoValidoDevuelve400ConMon001() {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, "-5.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("MON-001", codigo(respuesta));
    }

    @Test
    void monedaDistintaDevuelve409ConCta004() {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.EXTRANJERA, "50.00"));

        assertEquals(409, respuesta.getStatus());
        assertEquals("CTA-004", codigo(respuesta));
    }

    @Test
    void saldoInsuficienteDevuelve409ConSal001() {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, "999999.00"));

        assertEquals(409, respuesta.getStatus());
        assertEquals("SAL-001", codigo(respuesta));
    }

    @Test
    void caminoFelizDevuelve200() {
        Response respuesta = recurso.realizarTransferencia(bearer,
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, "50.00"));

        assertEquals(200, respuesta.getStatus());
        assertEquals("OK", ((RespuestaBase) respuesta.getEntity()).getEstado());
        assertEquals("200", codigo(respuesta));
    }

    @Test
    void faltaCuentaOrigenConTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.realizarTransferencia("Bearer no-existe",
                pedido(null, SimuladosRest.DESTINO, "50.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void faltaCuentaDestinoConTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.realizarTransferencia("Bearer no-existe",
                pedido(SimuladosRest.ORIGEN, null, "50.00"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }

    @Test
    void faltaMontoConTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.realizarTransferencia("Bearer no-existe",
                pedido(SimuladosRest.ORIGEN, SimuladosRest.DESTINO, null));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", codigo(respuesta));
    }
}
