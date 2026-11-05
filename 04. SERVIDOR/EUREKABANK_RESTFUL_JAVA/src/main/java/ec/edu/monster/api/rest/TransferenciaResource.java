package ec.edu.monster.api.rest;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.TransferenciaRequest;
import ec.edu.monster.seguridad.FiltroToken;
import ec.edu.monster.servicios.FabricaRespuestas;
import ec.edu.monster.utilidades.Textos;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

/** Operacion 8: transferencia entre cuentas. */
@Path("/api/transferencias")
@Produces(ManejadorExcepcion.TIPO_JSON)
public class TransferenciaResource {

    private final ServiciosEurekaBank servicios;

    public TransferenciaResource() {
        this(ServiciosEurekaBank.porDefecto());
    }

    public TransferenciaResource(ServiciosEurekaBank servicios) {
        this.servicios = Objects.requireNonNull(servicios, "Los servicios son obligatorios");
    }

    @POST
    @Consumes("application/json")
    public Response realizarTransferencia(@HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion,
            TransferenciaRequest cuerpo) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || cuerpo == null || Textos.estaVacio(cuerpo.getCuentaOrigen())
                || Textos.estaVacio(cuerpo.getCuentaDestino()) || cuerpo.getMonto() == null) {
            return ManejadorExcepcion.responder(
                    FabricaRespuestas.transferenciaFallida(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(servicios.movimientos().realizarTransferencia(
                cuerpo.getCuentaOrigen(), cuerpo.getCuentaDestino(), cuerpo.getMonto(), token));
    }
}
