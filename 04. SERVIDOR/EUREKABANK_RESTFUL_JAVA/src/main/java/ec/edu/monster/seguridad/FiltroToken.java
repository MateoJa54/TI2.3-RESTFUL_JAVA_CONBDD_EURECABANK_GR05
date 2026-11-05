package ec.edu.monster.seguridad;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/**
 * Extrae el token de la cabecera {@code Authorization: Bearer <token>}.
 *
 * <p>Solo extrae y deja el token en la propiedad {@link #PROPIEDAD_TOKEN}. No
 * consulta el registro de sesiones y nunca aborta la peticion: {@code AUTH-003}
 * y {@code AUTH-004} los produce el servicio (contrato, seccion 6.5).
 */
@Provider
public class FiltroToken implements ContainerRequestFilter {

    /** Nombre de la propiedad del request donde queda el token extraido. */
    public static final String PROPIEDAD_TOKEN = "ec.edu.monster.token";

    private static final String PREFIJO = "Bearer ";

    @Override
    public void filter(ContainerRequestContext peticion) {
        String token = extraerToken(peticion.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (token != null) {
            peticion.setProperty(PROPIEDAD_TOKEN, token);
        }
    }

    /**
     * Devuelve el token de una cabecera {@code Authorization}.
     *
     * @param autorizacion valor de la cabecera, puede ser {@code null}
     * @return el token, o {@code null} si la cabecera falta, no empieza por
     *         {@code "Bearer "} (con un solo espacio), el token queda vacio o
     *         contiene espacios en blanco
     */
    public static String extraerToken(String autorizacion) {
        if (autorizacion == null || !autorizacion.startsWith(PREFIJO)) {
            return null;
        }
        String token = autorizacion.substring(PREFIJO.length());
        if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        return token;
    }
}
