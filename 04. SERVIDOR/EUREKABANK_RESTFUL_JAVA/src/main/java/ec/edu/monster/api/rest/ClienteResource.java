package ec.edu.monster.api.rest;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.seguridad.FiltroToken;
import ec.edu.monster.servicios.FabricaRespuestas;
import ec.edu.monster.utilidades.Textos;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

/** Operacion 3: cuentas de un cliente. */
@Path("/api/clientes")
@Produces(ManejadorExcepcion.TIPO_JSON)
public class ClienteResource {

    private final ServiciosEurekaBank servicios;

    public ClienteResource() {
        this(ServiciosEurekaBank.porDefecto());
    }

    public ClienteResource(ServiciosEurekaBank servicios) {
        this.servicios = Objects.requireNonNull(servicios, "Los servicios son obligatorios");
    }

    @GET
    @Path("/{cedula:[^/]*}/cuentas")
    public Response listarCuentasPorCliente(@PathParam("cedula") String cedula,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null || Textos.estaVacio(cedula)) {
            return ManejadorExcepcion.responder(
                    FabricaRespuestas.cuentasFallidas(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(
                servicios.cuentas().listarCuentasPorCliente(cedula, token));
    }
}
