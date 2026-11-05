package ec.edu.monster.modelos;

/**
 * Sobre comun de toda respuesta del servicio.
 *
 * <p>Es la clase base de las siete respuestas concretas del contrato
 * congelado (seccion 3). Los tres campos heredados (estado, codigo y mensaje)
 * se propagan al XML porque las subclases viven en el mismo contexto JAXB.
 *
 * <p>Clase concreta y no abstracta a proposito: la operacion {@code cerrarSesion}
 * devuelve el tipo {@code RespuestaBase} segun el XSD congelado
 * ({@code cerrarSesionResponse/return} es de tipo {@code tns:respuestaBase} y el
 * tipo {@code respuestaBase} del esquema no esta declarado abstracto), por lo que
 * el servidor necesita poder instanciarlo.
 *
 * <p>Regla del docente: aqui solo hay atributos, constructores, getters y setters.
 */
public class RespuestaBase {

    /** Resultado de la operacion: {@code OK} o {@code ERROR}. */
    private String estado;

    /** Codigo de resultado del catalogo congelado (17 codigos). */
    private String codigo;

    /** Mensaje literal en espanol segun el catalogo de codigos. */
    private String mensaje;

    /** Constructor vacio: lo exige JAXB para deserializar. */
    public RespuestaBase() {
    }

    /**
     * Constructor del sobre comun.
     *
     * @param estado  {@code OK} o {@code ERROR}
     * @param codigo  codigo de resultado del catalogo
     * @param mensaje mensaje literal del catalogo
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
