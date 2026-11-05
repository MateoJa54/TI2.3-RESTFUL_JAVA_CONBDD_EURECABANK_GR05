package ec.edu.monster.escritorio.modelo;

/**
 * Respuesta de la operación {@code autenticar}: extiende {@link RespuestaBase}
 * con los datos de la sesión iniciada.
 */
public class RespuestaAutenticacion extends RespuestaBase {

    /** Token de sesión entregado al cliente. */
    private String token;

    /** Código del empleado autenticado. */
    private String empleado;

    /** Nombre completo del empleado autenticado. */
    private String nombreEmpleado;

    /** Fecha y hora de expiración del token, en ISO-8601. */
    private String expiracion;

    /** Rol o módulo de acceso del empleado autenticado. */
    private String rol;

    public RespuestaAutenticacion() {
    }

    public RespuestaAutenticacion(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
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
