package ec.edu.monster.api.rest;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.RespuestaBase;
import jakarta.json.JsonException;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Único punto donde se decide el HTTP status de una respuesta (contrato, seccion 10.3).
 */
@Provider
public class ManejadorExcepcion implements ExceptionMapper<Throwable> {

    /** Tipo de contenido de todas las respuestas. */
    public static final String TIPO_JSON = "application/json; charset=utf-8";

    private static final Logger LOGGER = Logger.getLogger(ManejadorExcepcion.class.getName());
    private static final int PROFUNDIDAD_MAXIMA = 20;

    /**
     * @param codigo codigo de resultado del catalogo, por ejemplo {@code AUTH-003}
     * @return el HTTP status que le corresponde; 500 si el codigo no esta en el catalogo
     */
    public static int estadoHttp(String codigo) {
        if (codigo == null) {
            return 500;
        }
        return switch (codigo) {
            case "200" -> 200;
            case "DAT-001", "CTA-003", "MON-001" -> 400;
            case "AUTH-001", "AUTH-003", "AUTH-004" -> 401;
            case "AUTH-002" -> 403;
            case "CTA-001", "CLI-001" -> 404;
            case "CTA-002", "CTA-004", "SAL-001", "MON-002", "LIM-001" -> 409;
            case "SER-001" -> 503;
            default -> 500;
        };
    }

    /**
     * @param sobre sobre de respuesta ya construido por la fabrica
     * @return la respuesta JAX-RS con el status del codigo y el sobre como cuerpo JSON
     */
    public static Response responder(RespuestaBase sobre) {
        return Response.status(estadoHttp(sobre.getCodigo()))
                .type(TIPO_JSON)
                .entity(sobre)
                .build();
    }

    @Override
    public Response toResponse(Throwable error) {
        if (esCuerpoMalFormado(error)) {
            return responder(sobre(CodigoRespuesta.DAT_001));
        }
        if (error instanceof WebApplicationException web && web.getResponse().getStatus() < 500) {
            return web.getResponse();
        }
        if (causadoPorSql(error)) {
            LOGGER.log(Level.WARNING, "Base de datos no disponible", error);
            return responder(sobre(CodigoRespuesta.SER_001));
        }
        LOGGER.log(Level.SEVERE, "Error no previsto en la API REST", error);
        return responder(sobre(CodigoRespuesta.ERR_001));
    }

    private static RespuestaBase sobre(CodigoRespuesta codigo) {
        return new RespuestaBase(codigo.getEstado(), codigo.getCodigo(), codigo.getMensaje());
    }

    private static boolean esCuerpoMalFormado(Throwable error) {
        return cadena(error).anyMatch(t -> t instanceof JsonbException
                || t instanceof JsonException
                || t instanceof BadRequestException
                || t instanceof NotSupportedException);
    }

    private static boolean causadoPorSql(Throwable error) {
        return cadena(error).anyMatch(t -> t instanceof SQLException);
    }

    private static Stream<Throwable> cadena(Throwable error) {
        List<Throwable> causas = new ArrayList<>();
        Throwable actual = error;
        while (actual != null && causas.size() < PROFUNDIDAD_MAXIMA) {
            causas.add(actual);
            actual = actual.getCause();
        }
        return causas.stream();
    }
}
