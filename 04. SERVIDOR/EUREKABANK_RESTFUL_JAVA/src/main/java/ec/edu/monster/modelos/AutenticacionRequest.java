package ec.edu.monster.modelos;

/**
 * Cuerpo de la peticion de autenticacion ({@code POST /api/auth/login}).
 *
 * <p>POJO plano: solo atributos, constructor vacio, constructor completo,
 * getters y setters. El mapeo a JSON lo hace JSON-B sin anotaciones.
 */
public class AutenticacionRequest {

    /** Nombre de acceso del empleado. */
    private String usuario;

    /** Clave del empleado en hexadecimal SHA-256. */
    private String clave;

    public AutenticacionRequest() {
    }

    /**
     * Constructor completo.
     *
     * @param usuario nombre de acceso del empleado
     * @param clave   clave del empleado en hexadecimal SHA-256
     */
    public AutenticacionRequest(String usuario, String clave) {
        this.usuario = usuario;
        this.clave = clave;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
