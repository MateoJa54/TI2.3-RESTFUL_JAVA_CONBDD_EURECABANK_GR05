package ec.edu.monster.modelos;

/**
 * Respuesta de la operacion {@code autenticar}: extiende {@link RespuestaBase}
 * con los datos de la sesion iniciada.
 */
public class RespuestaAutenticacion extends RespuestaBase {

    /** Token de sesion entregado al cliente para las operaciones autenticadas. */
    private String token;

    /** Codigo del empleado autenticado. */
    private String empleado;

    /** Nombre completo del empleado autenticado. */
    private String nombreEmpleado;

    /** Fecha y hora de expiracion del token, en ISO-8601. */
    private String expiracion;

    /** Rol o modulo de acceso del empleado autenticado. */
    private String rol;

    public RespuestaAutenticacion() {
    }

    /**
     * Constructor completo.
     *
     * @param estado        {@code OK} o {@code ERROR}
     * @param codigo        codigo de resultado del catalogo
     * @param mensaje       mensaje literal del catalogo
     * @param token         token de sesion
     * @param empleado      codigo del empleado
     * @param nombreEmpleado nombre completo del empleado
     * @param expiracion    expiracion del token en ISO-8601
     * @param rol           rol o modulo de acceso
     */
    public RespuestaAutenticacion(String estado, String codigo, String mensaje,
            String token, String empleado, String nombreEmpleado, String expiracion, String rol) {
        super(estado, codigo, mensaje);
        this.token = token;
        this.empleado = empleado;
        this.nombreEmpleado = nombreEmpleado;
        this.expiracion = expiracion;
        this.rol = rol;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmpleado() {
        return empleado;
    }

    public void setEmpleado(String empleado) {
        this.empleado = empleado;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }

    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }

    public String getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(String expiracion) {
        this.expiracion = expiracion;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
