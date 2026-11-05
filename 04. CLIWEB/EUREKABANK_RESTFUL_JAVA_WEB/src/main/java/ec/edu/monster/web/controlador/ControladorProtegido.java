package ec.edu.monster.web.controlador;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.SesionUsuario;
import java.util.Objects;

/**
 * Base de los controladores que necesitan una sesión iniciada.
 *
 * <p>Valida que haya sesión antes de dejar operar: sin token no se llama al
 * servidor y se devuelve el sobre con el código {@code AUTH-003}, que es el
 * que corresponde a una petición sin sesión válida.
 *
 * <p>También vigila la sesión: si el servidor contesta que el token ya no es
 * válido o que expiró, se descarta la sesión local y hay que autenticarse de
 * nuevo.
 */
public abstract class ControladorProtegido {

    /** Código del catálogo para un token que el servidor no reconoce. */
    public static final String CODIGO_TOKEN_INVALIDO = "AUTH-003";

    /** Mensaje literal del catálogo para un token que el servidor no reconoce. */
    public static final String MENSAJE_TOKEN_INVALIDO = "El token de sesión no es válido";

    /** Código del catálogo para un token que ya expiró. */
    public static final String CODIGO_TOKEN_EXPIRADO = "AUTH-004";

    /** Mensaje literal del catálogo para un token que ya expiró. */
    public static final String MENSAJE_TOKEN_EXPIRADO =
            "El token de sesión ha expirado, inicie sesión nuevamente";

    protected final IClienteEUREKABANK cliente;
    protected final SesionUsuario sesion;

    /**
     * @param cliente cliente del servicio, obligatorio
     * @param sesion  sesión local del usuario, obligatoria
     */
    protected ControladorProtegido(IClienteEUREKABANK cliente, SesionUsuario sesion) {
        this.cliente = Objects.requireNonNull(cliente, "El cliente del servicio es obligatorio");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria");
    }

    /**
     * @return {@code true} si hay un token guardado y se puede operar
     */
    public boolean haySesionActiva() {
        return sesion.estaVigente();
    }

    /**
     * @return el token de la sesión local
     */
    public String obtenerToken() {
        return sesion.getToken();
    }

    /**
     * Devuelve la respuesta tal cual si hay sesión; si no, la devuelve con el
     * código {@code AUTH-003} y sin llamar al servidor.
     *
     * @param <R>     tipo de respuesta
     * @param respuesta sobre que se devuelve al usuario
     * @return el mismo sobre, corregido si no hay sesión
     */
    protected <R extends RespuestaBase> R exigirSesion(R respuesta) {
        if (!haySesionActiva()) {
            respuesta.setEstado("ERROR");
            respuesta.setCodigo(CODIGO_TOKEN_INVALIDO);
            respuesta.setMensaje(MENSAJE_TOKEN_INVALIDO);
        }
        return respuesta;
    }

    /**
     * Descarta la sesión local si el servidor ya no reconoce el token.
     *
     * @param <R>      tipo de respuesta
     * @param respuesta sobre recibido del servidor
     * @return el mismo sobre
     */
    protected <R extends RespuestaBase> R revisarSesion(R respuesta) {
        if (respuesta != null && (CODIGO_TOKEN_INVALIDO.equals(respuesta.getCodigo())
                || CODIGO_TOKEN_EXPIRADO.equals(respuesta.getCodigo()))) {
            sesion.limpiar();
        }
        return respuesta;
    }
}
