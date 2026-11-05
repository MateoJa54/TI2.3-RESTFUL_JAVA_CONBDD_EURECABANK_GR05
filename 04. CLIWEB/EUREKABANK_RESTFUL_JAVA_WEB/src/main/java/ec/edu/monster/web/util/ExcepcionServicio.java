package ec.edu.monster.web.util;

/**
 * Fallo de comunicación con el servicio REST.
 *
 * <p>El cliente convierte cualquier error de JAX-WS en esta excepción, con el
 * código y el mensaje literal de {@code SER-001} del catálogo congelado. Así
 * el resto del programa no depende de las excepciones de JAX-WS.
 */
public class ExcepcionServicio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Código del catálogo congelado para el servicio no disponible. */
    public static final String CODIGO = "SER-001";

    /** Mensaje literal del catálogo congelado para el servicio no disponible. */
    public static final String MENSAJE = "El servicio no está disponible en este momento";

    /** Detalle técnico de lo que pasó, para el diagnóstico. */
    private final String detalle;

    /**
     * @param detalle explicación de lo ocurrido, por ejemplo la dirección
     * @param causa    excepción original de JAX-WS
     */
    public ExcepcionServicio(String detalle, Throwable causa) {
        super("[" + CODIGO + "] " + MENSAJE + (detalle == null || detalle.isBlank() ? "" : " (" + detalle + ")"), causa);
        this.detalle = detalle;
    }

    /**
     * @return el código del catálogo congelado
     */
    public String getCodigo() {
        return CODIGO;
    }

    /**
     * @return el detalle técnico del fallo
     */
    public String getDetalle() {
        return detalle;
    }
}
