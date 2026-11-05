package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import ec.edu.monster.api.rest.ManejadorExcepcion;
import ec.edu.monster.modelos.RespuestaBase;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/** Traduccion de excepciones a sobres y status (contrato, seccion 10.3). */
class ManejadorExcepcionTest {

    private static final String MENSAJE_DAT = "Los datos de la petición están incompletos o son incorrectos";
    private static final String MENSAJE_ERR = "Ocurrió un error interno, intente nuevamente";
    private static final String MENSAJE_SER = "El servicio no está disponible en este momento";

    private final ManejadorExcepcion manejador = new ManejadorExcepcion();

    @Test
    void jsonMalFormadoDevuelve400ConDat001() {
        Response respuesta = manejador.toResponse(new JsonbException("cuerpo roto"));

        assertEquals(400, respuesta.getStatus());
        RespuestaBase sobre = (RespuestaBase) respuesta.getEntity();
        assertEquals("ERROR", sobre.getEstado());
        assertEquals("DAT-001", sobre.getCodigo());
        assertEquals(MENSAJE_DAT, sobre.getMensaje());
    }

    @Test
    void unJsonMalFormadoEnvueltoEnOtraExcepcionTambienDevuelveDat001() {
        Response respuesta = manejador.toResponse(new ProcessingException(new JsonbException("x")));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void unaPeticionSinTipoSoportadoDevuelve400ConDat001() {
        Response respuesta = manejador.toResponse(new NotSupportedException("text/plain"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void unaPeticionMalFormadaDevuelve400ConDat001() {
        Response respuesta = manejador.toResponse(new BadRequestException("x"));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void excepcionGenericaDevuelve500ConErr001YSuMensajeLiteral() {
        Response respuesta = manejador.toResponse(new IllegalStateException("algo inesperado"));

        assertEquals(500, respuesta.getStatus());
        RespuestaBase sobre = (RespuestaBase) respuesta.getEntity();
        assertEquals("ERROR", sobre.getEstado());
        assertEquals("ERR-001", sobre.getCodigo());
        assertEquals(MENSAJE_ERR, sobre.getMensaje());
    }

    @Test
    void errorDeServidorHttpDevuelve500ConErr001() {
        Response respuesta = manejador.toResponse(new InternalServerErrorException("x"));

        assertEquals(500, respuesta.getStatus());
        assertEquals("ERR-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void baseDeDatosNoDisponibleDevuelve503ConSer001() {
        IllegalStateException conexion = new IllegalStateException(
                "No se pudo abrir la conexion con la base de datos del banco",
                new SQLException("Communications link failure"));

        Response respuesta = manejador.toResponse(conexion);

        assertEquals(503, respuesta.getStatus());
        RespuestaBase sobre = (RespuestaBase) respuesta.getEntity();
        assertEquals("SER-001", sobre.getCodigo());
        assertEquals(MENSAJE_SER, sobre.getMensaje());
    }

    @Test
    void unaRutaInexistenteConservaSuStatusSinSobre() {
        Response respuesta = manejador.toResponse(new NotFoundException("no existe"));

        assertEquals(404, respuesta.getStatus());
        assertNull(respuesta.getEntity());
    }

    @Test
    void responderUsaElStatusDelCodigoYEscribeElSobreCompleto() {
        RespuestaBase sobre = new RespuestaBase("ERROR", "AUTH-003", "El token de sesión no es válido");

        Response respuesta = ManejadorExcepcion.responder(sobre);

        assertEquals(401, respuesta.getStatus());
        assertEquals("application", respuesta.getMediaType().getType());
        assertEquals("json", respuesta.getMediaType().getSubtype());
        RespuestaBase cuerpo = assertInstanceOf(RespuestaBase.class, respuesta.getEntity());
        assertEquals("ERROR", cuerpo.getEstado());
        assertEquals("AUTH-003", cuerpo.getCodigo());
        assertEquals("El token de sesión no es válido", cuerpo.getMensaje());
    }
}
