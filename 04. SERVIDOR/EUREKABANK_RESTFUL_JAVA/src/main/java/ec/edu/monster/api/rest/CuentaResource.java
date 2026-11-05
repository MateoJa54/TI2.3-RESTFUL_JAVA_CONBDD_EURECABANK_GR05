package ec.edu.monster.api.rest;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.MontoRequest;
import ec.edu.monster.seguridad.FiltroToken;
import ec.edu.monster.servicios.FabricaRespuestas;
import ec.edu.monster.utilidades.Textos;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

/**
 * Operaciones 4 a 7 y 9: consulta, movimientos, depositos, retiros y costos de una cuenta.
 *
 * <p>Las plantillas usan {@code [^/]*} para que un segmento vacio llegue al recurso
 * y responda DAT-001, y no un 404 del enrutador. Con {@code [^/]*} la plantilla
 * {@code /{cuenta}} no puede tragarse {@code /{cuenta}/movimientos}.
 */
@Path("/api/cuentas")
@Produces(ManejadorExcepcion.TIPO_JSON)
public class CuentaResource {

    private final ServiciosEurekaBank servicios;

    public CuentaResource() {
        this(ServiciosEurekaBank.porDefecto());
    }

    public CuentaResource(ServiciosEurekaBank servicios) {
        this.servicios = Objects.requireNonNull(servicios, "Los servicios son obligatorios");
    }

    @GET
    @Path("/{cuenta:[^/]*}")
    public Response consultarCuenta(@PathParam("cuenta") String cuenta,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || Textos.estaVacio(cuenta)) {
            return ManejadorExcepcion.responder(FabricaRespuestas.cuentaFallida(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(servicios.cuentas().consultarCuenta(cuenta, token));
    }

    @GET
    @Path("/{cuenta:[^/]*}/movimientos")
    public Response consultarMovimientos(@PathParam("cuenta") String cuenta,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || Textos.estaVacio(cuenta)) {
            return ManejadorExcepcion.responder(
                    FabricaRespuestas.movimientosFallidos(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(servicios.movimientos().consultarMovimientos(cuenta, token));
    }

    @POST
    @Path("/{cuenta:[^/]*}/depositos")
    @Consumes("application/json")
    public Response realizarDeposito(@PathParam("cuenta") String cuenta,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion, MontoRequest cuerpo) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || Textos.estaVacio(cuenta) || cuerpo == null || cuerpo.getMonto() == null) {
            return ManejadorExcepcion.responder(FabricaRespuestas.movimientoFallido(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(
                servicios.movimientos().realizarDeposito(cuenta, cuerpo.getMonto(), token));
    }

    @POST
    @Path("/{cuenta:[^/]*}/retiros")
    @Consumes("application/json")
    public Response realizarRetiro(@PathParam("cuenta") String cuenta,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion, MontoRequest cuerpo) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || Textos.estaVacio(cuenta) || cuerpo == null || cuerpo.getMonto() == null) {
            return ManejadorExcepcion.responder(FabricaRespuestas.movimientoFallido(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(
                servicios.movimientos().realizarRetiro(cuenta, cuerpo.getMonto(), token));
    }

    @GET
    @Path("/{cuenta:[^/]*}/costos")
    public Response consultarCostosMantenimiento(@PathParam("cuenta") String cuenta,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || Textos.estaVacio(cuenta)) {
            return ManejadorExcepcion.responder(FabricaRespuestas.costosFallidos(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(servicios.costos().consultarCostosMantenimiento(cuenta, token));
    }
}
