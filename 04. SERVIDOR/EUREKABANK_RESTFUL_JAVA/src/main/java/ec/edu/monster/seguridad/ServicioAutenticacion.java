package ec.edu.monster.seguridad;

import ec.edu.monster.datos.ILogSessionDAO;
import ec.edu.monster.datos.IUsuarioDAO;
import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.RespuestaAutenticacion;
import ec.edu.monster.modelos.RespuestaBase;
import ec.edu.monster.servicios.FabricaRespuestas;
import ec.edu.monster.utilidades.HashClave;
import ec.edu.monster.utilidades.Textos;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de autenticacion de los empleados del banco.
 *
 * <p>Orden de validacion del contrato congelado (seccion 6.2):
 * <ol>
 * <li>{@code DAT-001} si el usuario o la clave llegan vacios;</li>
 * <li>{@code AUTH-001} si el usuario no existe o el hash no coincide;</li>
 * <li>{@code AUTH-002} si el usuario esta inactivo.</li>
 * </ol>
 *
 * <p>Regla RB-13: la clave se compara con {@code MessageDigest.isEqual}, en
 * tiempo constante, y el error no revela si fallo el usuario o la clave.
 */
public class ServicioAutenticacion {

    /** Direccion IP que se registra cuando el servidor no la conoce. */
    public static final String IP_DESCONOCIDA = "NONE";

    /** Nombre de host que se registra cuando el servidor no lo conoce. */
    public static final String HOST_DESCONOCIDO = "NONE";

    private static final DateTimeFormatter FORMATO_EXPIRACION =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);

    private final IUsuarioDAO usuarioDAO;
    private final ILogSessionDAO logSessionDAO;
    private final TokenServicio tokenServicio;

    /**
     * Constructor con las dependencias explicitas.
     *
     * @param usuarioDAO    repositorio de usuarios
     * @param logSessionDAO repositorio de auditoria de sesiones
     * @param tokenServicio registro de tokens
     */
    public ServicioAutenticacion(IUsuarioDAO usuarioDAO, ILogSessionDAO logSessionDAO,
            TokenServicio tokenServicio) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "El repositorio de usuarios es obligatorio");
        this.logSessionDAO = Objects.requireNonNull(logSessionDAO,
                "El repositorio de sesiones es obligatorio");
        this.tokenServicio = Objects.requireNonNull(tokenServicio,
                "El registro de tokens es obligatorio");
    }

    /**
     * Autentica a un empleado.
     *
     * @param usuario usuario del empleado
     * @param clave   clave del empleado
     * @return el sobre de respuesta: con token si la credencial es valida, o con
     *         el codigo de error del catalogo
     */
    public RespuestaAutenticacion autenticar(String usuario, String clave) {
        if (Textos.estaVacio(usuario) || Textos.estaVacio(clave)) {
            return FabricaRespuestas.autenticacionFallida(CodigoRespuesta.DAT_001);
        }
        Optional<IUsuarioDAO.Usuario> encontrado = usuarioDAO.buscarPorNombre(usuario.trim());
        if (encontrado.isEmpty()) {
            return FabricaRespuestas.autenticacionFallida(CodigoRespuesta.AUTH_001);
        }
        IUsuarioDAO.Usuario encontradoUsuario = encontrado.get();
        if (!HashClave.coincide(clave, encontradoUsuario.hash())) {
            return FabricaRespuestas.autenticacionFallida(CodigoRespuesta.AUTH_001);
        }
        if (!"ACTIVO".equals(encontradoUsuario.estado())) {
            return FabricaRespuestas.autenticacionFallida(CodigoRespuesta.AUTH_002);
        }
        String token = tokenServicio.generarToken(encontradoUsuario.codigo());
        Integer idSesion = logSessionDAO.registrarIngreso(encontradoUsuario.codigo(),
                IP_DESCONOCIDA, HOST_DESCONOCIDO);
        tokenServicio.asociarLogSession(token, idSesion);
        Instant expiracion = tokenServicio.obtenerExpiracion(token)
                .orElseThrow(() -> new IllegalStateException("El token recien generado no existe"));
        return FabricaRespuestas.autenticacionExitosa(token, encontradoUsuario.codigo(),
                encontradoUsuario.nombreEmpleado(), FORMATO_EXPIRACION.format(expiracion),
                encontradoUsuario.rol());
    }

    /**
     * Operacion 2: cierra la sesion abierta con el token recibido.
     *
     * <p>Orden del contrato congelado (seccion 6.4): {@code DAT-001} si no
     * llega token, {@code AUTH-003} si el token no existe, {@code AUTH-004} si
     * el token ya expiro y, en los dos ultimos casos, la sesion se cierra
     * igualmente en {@code logsession}.
     *
     * @param token token de sesion que se cierra
     * @return el sobre de respuesta
     */
    public RespuestaBase cerrarSesion(String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso == CodigoRespuesta.DAT_001) {
            return FabricaRespuestas.cierreSesionFallido(CodigoRespuesta.DAT_001);
        }
        if (acceso == CodigoRespuesta.AUTH_003) {
            return FabricaRespuestas.cierreSesionFallido(CodigoRespuesta.AUTH_003);
        }
        tokenServicio.obtenerSesion(token).ifPresent(sesion ->
                logSessionDAO.registrarSalida(sesion.getIdSesionLog()));
        tokenServicio.cerrar(token);
        if (acceso == CodigoRespuesta.AUTH_004) {
            return FabricaRespuestas.cierreSesionFallido(CodigoRespuesta.AUTH_004);
        }
        return FabricaRespuestas.cierreSesionExitoso();
    }
}
