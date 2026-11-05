package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ec.edu.monster.api.rest.ClienteResource;
import ec.edu.monster.api.rest.ServiciosEurekaBank;
import ec.edu.monster.modelos.RespuestaBase;
import ec.edu.monster.modelos.RespuestaCuentas;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Operacion 3: listar cuentas por cliente (contrato, seccion 6.3). */
class ClienteResourceTest {

    private ServiciosEurekaBank servicios;
    private ClienteResource recurso;

    @BeforeEach
    void preparar() {
        servicios = SimuladosRest.servicios();
        recurso = new ClienteResource(servicios);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Bearer", "Bearer "})
    void cabeceraAusenteOMalFormadaDevuelveDat001SinConsultarElCliente(String cabecera) {
        Response respuesta = recurso.listarCuentasPorCliente("99999999", cabecera);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void cedulaVaciaDevuelveDat001(String cedula) {
        Response respuesta = recurso.listarCuentasPorCliente(cedula, "Bearer " + SimuladosRest.tokenValido(servicios));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void tokenInexistenteDevuelve401ConAuth003() {
        Response respuesta = recurso.listarCuentasPorCliente(SimuladosRest.CEDULA, "Bearer no-existe");

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void clienteInexistenteDevuelve404ConCli001() {
        Response respuesta = recurso.listarCuentasPorCliente("99999999", "Bearer " + SimuladosRest.tokenValido(servicios));

        assertEquals(404, respuesta.getStatus());
        assertEquals("CLI-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void caminoFelizDevuelve200ConLasCuentasDelCliente() {
        Response respuesta = recurso.listarCuentasPorCliente(SimuladosRest.CEDULA,
                "Bearer " + SimuladosRest.tokenValido(servicios));

        assertEquals(200, respuesta.getStatus());
        RespuestaCuentas cuerpo = assertInstanceOf(RespuestaCuentas.class, respuesta.getEntity());
        assertEquals("200", cuerpo.getCodigo());
        assertEquals(SimuladosRest.CEDULA, cuerpo.getCedula());
        assertEquals("Eric Gustavo Coronel Castillo", cuerpo.getNombreCliente());
        assertEquals(4, cuerpo.getCuentas().size());
    }

    @Test
    void cedulaVaciaConTokenInexistenteDevuelveDat001YNoAuth003() {
        Response respuesta = recurso.listarCuentasPorCliente("", "Bearer no-existe");

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }
}
