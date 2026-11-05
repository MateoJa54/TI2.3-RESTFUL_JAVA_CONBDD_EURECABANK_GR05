package ec.edu.monster.web.modelo;

/**
 * Sesión actual del usuario: el token que entregó el servidor y los datos del
 * empleado autenticado.
 *
 * <p>No es parte del contrato del servicio: es estado local del cliente web.
 */
public class SesionUsuario {

    /** Token de sesión entregado por el servidor. */
    private String token;

    /** Código del empleado autenticado. */
    private String empleado;

    /** Nombre completo del empleado autenticado. */
    private String nombreEmpleado;

    /** Fecha y hora de expiración del token, en ISO-8601. */
    private String expiracion;

    /** Rol o módulo de acceso del empleado autenticado. */
    private String rol;

    public SesionUsuario() {
    }

    /**
     * Constructor completo.
     *
     * @param token         token de sesión
     * @param empleado      código del empleado
     * @param nombreEmpleado nombre completo del empleado
     * @param expiracion    fecha y hora de expiración
     * @param rol           rol del empleado
     */
    public SesionUsuario(String token, String empleado, String nombreEmpleado,
            String expiracion, String rol) {
        this.token = token;
        this.empleado = empleado;
        this.nombreEmpleado = nombreEmpleado;
        this.expiracion = expiracion;
        this.rol = rol;
    }

    /**
     * @return {@code true} si hay un token guardado y sirve para operar
     */
    public boolean estaVigente() {
        return token != null && !token.isBlank();
    }

    /**
     * Olvida la sesión: se usa al cerrarla o cuando el servidor ya no reconoce
     * el token.
     */
    public void limpiar() {
        this.token = null;
        this.empleado = null;
        this.nombreEmpleado = null;
        this.expiracion = null;
        this.rol = null;
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
