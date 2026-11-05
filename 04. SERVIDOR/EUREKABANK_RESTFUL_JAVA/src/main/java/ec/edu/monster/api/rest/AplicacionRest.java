package ec.edu.monster.api.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Raiz JAX-RS: las rutas quedan bajo el contexto del WAR. */
@ApplicationPath("/")
public class AplicacionRest extends Application {
}
