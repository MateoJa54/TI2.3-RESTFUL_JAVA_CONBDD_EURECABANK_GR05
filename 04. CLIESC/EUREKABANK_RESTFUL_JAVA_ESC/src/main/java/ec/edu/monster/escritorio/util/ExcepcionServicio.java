package ec.edu.monster.escritorio.util;

/**
 * Fallo de comunicación con el servicio.
 *
 * <p>El cliente convierte cualquier error de red o de protocolo en esta excepción, con el
 * código y el mensaje literal de {@code SER-001} del catálogo congelado. Así
 * el resto del programa no depende de las excepciones de red.
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
     * @param causa    excepción original
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
