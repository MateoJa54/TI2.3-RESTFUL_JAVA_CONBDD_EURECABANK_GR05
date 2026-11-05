package ec.edu.monster.escritorio.modelo;

/**
 * Datos comunes de toda respuesta del servicio: estado (OK / ERROR), código y
 * mensaje.
 *
 * <p>Es una clase concreta (no abstracta) porque la operación
 * {@code cerrarSesion} devuelve el sobre común tal cual, y JSON-B necesita una
 * clase instanciable para deserializarlo.
 *
 * <p>Los nombres de los atributos son exactamente los nombres de los elementos
 * del contrato congelado. No lleva anotaciones de mapeo: el orden en que JSON-B los
 * serializa es el alfabético, que es el que declara el esquema.
 */
public class RespuestaBase {

    /** Estado de la operación: {@code OK} o {@code ERROR}. */
    private String estado;

    /** Código de resultado del catálogo congelado. */
    private String codigo;

    /** Mensaje literal en español según el catálogo de códigos. */
    private String mensaje;

    public RespuestaBase() {
    }

    /**
     * Constructor completo.
     *
     * @param estado  {@code OK} o {@code ERROR}
     * @param codigo  código de resultado del catálogo
     * @param mensaje mensaje literal del catálogo
     */
    public RespuestaBase(String estado, String codigo, String mensaje) {
        this.estado = estado;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
