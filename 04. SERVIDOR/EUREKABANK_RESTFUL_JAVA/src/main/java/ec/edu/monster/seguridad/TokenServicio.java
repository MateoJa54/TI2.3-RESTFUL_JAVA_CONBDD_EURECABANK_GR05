package ec.edu.monster.seguridad;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.utilidades.Textos;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de tokens de sesion.
 *
 * <p>Regla RB-14 del contrato congelado: el token es un UUID aleatorio con una
 * vigencia de 30 minutos y vive en un {@code ConcurrentHashMap} en memoria, de
 * modo que varios clientes pueden usar el servicio a la vez.
 *
 * <p>El reloj es inyectable: las pruebas pueden simular el paso del tiempo
 * sin esperar 30 minutos reales.
 */
public class TokenServicio {

    /** Vigencia por defecto del token de sesion. */
    public static final Duration VIGENCIA_POR_DEFECTO = Duration.ofMinutes(30);

    private final Duration vigencia;
    private final Clock reloj;
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    /**
     * Constructor por defecto: vigencia de 30 minutos y reloj del sistema.
     */
    public TokenServicio() {
        this(VIGENCIA_POR_DEFECTO, Clock.systemUTC());
    }

    /**
     * Constructor con vigencia y reloj explicitos, usado por las pruebas.
     *
     * @param vigencia tiempo de vida del token
     * @param reloj    reloj con el que se mide la vigencia
     */
    public TokenServicio(Duration vigencia, Clock reloj) {
        Objects.requireNonNull(vigencia, "La vigencia es obligatoria");
        if (vigencia.isNegative() || vigencia.isZero()) {
            throw new IllegalArgumentException("La vigencia debe ser positiva: " + vigencia);
        }
        this.vigencia = vigencia;
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio");
    }

    /**
     * Genera un token nuevo para un empleado.
     *
     * @param empleado codigo del empleado que abre la sesion
     * @return el token (un UUID)
     */
    public String generarToken(String empleado) {
        limpiarMuertas();
        String token = UUID.randomUUID().toString();
        Instant expiracion = reloj.instant().plus(vigencia);
        sesiones.put(token, new Sesion(token, empleado, null, expiracion));
        return token;
    }

    /**
     * Asocia a un token la fila de auditoria de la sesion abierta.
     *
     * @param token      token de sesion
     * @param idSesionLog identificador de la fila de {@code logsession}
     */
    public void asociarLogSession(String token, Integer idSesionLog) {
        sesiones.computeIfPresent(token, (clave, sesion) ->
                new Sesion(sesion.getToken(), sesion.getEmpleado(), idSesionLog, sesion.getExpiracion()));
    }

    /**
     * Momento en que expira un token.
     *
     * @param token token de sesion
     * @return la expiracion, o vacio si el token no existe
     */
    public Optional<Instant> obtenerExpiracion(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sesiones.get(token)).map(Sesion::getExpiracion);
    }

    /**
     * Datos de la sesion asociada a un token.
     *
     * @param token token de sesion
     * @return la sesion, o vacio si el token no existe
     */
    public Optional<Sesion> obtenerSesion(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sesiones.get(token));
    }

    /**
     * Valida un token de sesion.
     *
     * <p>Un token que ya vencio se conserva en el registro para poder cerrar
     * la sesion en {@code logsession} (contrato, seccion 6.4); por eso responde
     * {@code AUTH-004} y no {@code AUTH-003}.
     *
     * @param token token de sesion recibido
     * @return {@code OK_200} si el token es valido, {@code DAT_001} si no se
     *         recibio token, {@code AUTH_003} si no existe y {@code AUTH_004} si
     *         ha expirado
     */
    public CodigoRespuesta validarToken(String token) {
        if (Textos.estaVacio(token)) {
            return CodigoRespuesta.DAT_001;
        }
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            return CodigoRespuesta.AUTH_003;
        }
        if (!reloj.instant().isBefore(sesion.getExpiracion())) {
            return CodigoRespuesta.AUTH_004;
        }
        return CodigoRespuesta.OK_200;
    }

    /**
     * Descarta un token del registro.
     *
     * @param token token de sesion
     */
    public void cerrar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    /**
     * @return el numero de tokens que hay en el registro
     */
    public int cantidadSesiones() {
        return sesiones.size();
    }

    /**
     * @return la vigencia configurada del token
     */
    public Duration getVigencia() {
        return vigencia;
    }

    /**
     * Descarta los tokens que llevan una vigencia entera caducados, para que
     * el registro no crezca sin limite. Los tokens que acaban de caducar se
     * conservan: todavia sirven para cerrar la sesion en {@code logsession}.
     */
    private void limpiarMuertas() {
        Instant limite = reloj.instant().minus(vigencia);
        sesiones.values().removeIf(sesion -> sesion.getExpiracion().isBefore(limite));
    }

    /**
     * Sesion abierta por un empleado.
     *
     * @param token       token de sesion
     * @param empleado    codigo del empleado
     * @param idSesionLog identificador de la fila de {@code logsession}
     * @param expiracion  momento de expiracion del token
     */
    public static final class Sesion {

        private final String token;
        private final String empleado;
        private final Integer idSesionLog;
        private final Instant expiracion;

        Sesion(String token, String empleado, Integer idSesionLog, Instant expiracion) {
            this.token = token;
            this.empleado = empleado;
            this.idSesionLog = idSesionLog;
            this.expiracion = expiracion;
        }

        public String getToken() {
            return token;
        }

        public String getEmpleado() {
            return empleado;
        }

        public Integer getIdSesionLog() {
            return idSesionLog;
        }

        public Instant getExpiracion() {
            return expiracion;
        }
    }
}
