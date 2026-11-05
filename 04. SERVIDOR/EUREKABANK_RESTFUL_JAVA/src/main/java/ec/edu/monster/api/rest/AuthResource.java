package ec.edu.monster.api.rest;

import ec.edu.monster.modelos.AutenticacionRequest;
import ec.edu.monster.modelos.CodigoRespuesta;
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

/** Operaciones 1 y 2: inicio y cierre de sesion. */
@Path("/api/auth")
@Produces(ManejadorExcepcion.TIPO_JSON)
public class AuthResource {

    private final ServiciosEurekaBank servicios;

    public AuthResource() {
        this(ServiciosEurekaBank.porDefecto());
    }

    public AuthResource(ServiciosEurekaBank servicios) {
        this.servicios = Objects.requireNonNull(servicios, "Los servicios son obligatorios");
    }

    @POST
    @Path("/login")
    @Consumes("application/json")
    public Response autenticar(AutenticacionRequest cuerpo) {
        if (cuerpo == null || Textos.estaVacio(cuerpo.getUsuario()) || Textos.estaVacio(cuerpo.getClave())) {
            return ManejadorExcepcion.responder(
                    FabricaRespuestas.autenticacionFallida(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(
                servicios.autenticacion().autenticar(cuerpo.getUsuario(), cuerpo.getClave()));
    }

    @POST
    @Path("/logout")
    public Response cerrarSesion(@HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion) {
        String token = FiltroToken.extraerToken(autorizacion);
        if (token == null) {
            return ManejadorExcepcion.responder(
                    FabricaRespuestas.cierreSesionFallido(CodigoRespuesta.DAT_001));
        }
        return ManejadorExcepcion.responder(servicios.autenticacion().cerrarSesion(token));
    }
}
