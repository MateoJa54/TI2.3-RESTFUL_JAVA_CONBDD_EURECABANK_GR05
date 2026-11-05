package ec.edu.monster.consola.controlador;

import ec.edu.monster.consola.cliente.IClienteEUREKABANK;
import ec.edu.monster.consola.modelo.RespuestaAutenticacion;
import ec.edu.monster.consola.modelo.RespuestaBase;
import ec.edu.monster.consola.modelo.SesionUsuario;
import ec.edu.monster.consola.util.HashClave;
import java.util.Objects;

/**
 * Controlador del inicio y el cierre de sesión (CU-01).
 *
 * <p>La contraseña se hashea con SHA-256 antes de enviarse: por la red solo
 * viaja el hash, nunca la contraseña en claro (regla RB-13 del contrato).
 */
public class ControladorAutenticacion {

    private final IClienteEUREKABANK cliente;
    private final SesionUsuario sesion;

    /**
     * @param cliente cliente del servicio, obligatorio
     * @param sesion  sesión local del usuario, obligatoria
     */
    public ControladorAutenticacion(IClienteEUREKABANK cliente, SesionUsuario sesion) {
        this.cliente = Objects.requireNonNull(cliente, "El cliente del servicio es obligatorio");
        this.sesion = Objects.requireNonNull(sesion, "La sesión es obligatoria");
    }

    /**
     * Autentica al empleado y, si el servidor lo acepta, guarda la sesión.
     *
     * @param usuario usuario del empleado
     * @param clave   contraseña en claro, que se hashea antes de enviarse
     * @return el sobre del servidor
     */
    public RespuestaAutenticacion autenticar(String usuario, String clave) {
        String hash = HashClave.sha256(clave);
        RespuestaAutenticacion respuesta = cliente.autenticar(usuario, hash);
        if (esExitosa(respuesta) && respuesta.getToken() != null && !respuesta.getToken().isBlank()) {
            sesion.setToken(respuesta.getToken());
            sesion.setEmpleado(respuesta.getEmpleado());
            sesion.setNombreEmpleado(respuesta.getNombreEmpleado());
            sesion.setExpiracion(respuesta.getExpiracion());
            sesion.setRol(respuesta.getRol());
        } else {
            sesion.limpiar();
        }
        return respuesta;
    }

    /**
     * Cierra la sesión en el servidor y descarta la sesión local.
     *
     * @return el sobre del servidor
     */
    public RespuestaBase cerrarSesion() {
        String token = sesion.getToken();
        try {
            if (token == null || token.isBlank()) {
                sesion.limpiar();
                return sobreVacio();
            }
            return cliente.cerrarSesion(token);
        } finally {
            sesion.limpiar();
        }
    }

    /**
     * @return {@code true} si hay sesión iniciada
     */
    public boolean haySesionActiva() {
        return sesion.estaVigente();
    }

    /**
     * @return la sesión local del usuario
     */
    public SesionUsuario obtenerSesion() {
        return sesion;
    }

    private static boolean esExitosa(RespuestaBase respuesta) {
        return respuesta != null && "OK".equals(respuesta.getEstado());
    }

    private static RespuestaBase sobreVacio() {
        RespuestaBase sobre = new RespuestaBase();
        sobre.setEstado("ERROR");
        sobre.setCodigo(ControladorProtegido.CODIGO_TOKEN_INVALIDO);
        sobre.setMensaje(ControladorProtegido.MENSAJE_TOKEN_INVALIDO);
        return sobre;
    }
}
