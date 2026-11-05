package ec.edu.monster.escritorio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formatos de presentación del cliente de escritorio.
 *
 * <p>Los importes se muestran siempre con dos decimales, que es la escala de la
 * base de datos (regla RB-01 del contrato).
 */
public final class Formato {

    /** Escala de los importes: dos decimales. */
    public static final int ESCALA = 2;

    private Formato() {
    }

    /**
     * Da formato a un importe con dos decimales.
     *
     * @param valor importe a formatear
     * @return el importe con dos decimales; {@code 0.00} si no hay importe
     */
    public static String monto(BigDecimal valor) {
        if (valor == null) {
            return "0.00";
        }
        return valor.setScale(ESCALA, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Da formato a un número entero del sobre.
     *
     * @param valor entero a formatear
     * @return el entero en texto; {@code 0} si no hay valor
     */
    public static String entero(Integer valor) {
        return valor == null ? "0" : String.valueOf(valor);
    }

    /**
     * Da formato a un texto del sobre.
     *
     * @param valor texto a formatear
     * @return el texto; cadena vacía si no hay valor
     */
    public static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /**
     * Presenta el resultado del servidor tal como lo entrega: el código y el
     * mensaje literal, sin reescribirlos.
     *
     * @param codigo  código de resultado del catálogo
     * @param mensaje mensaje literal del catálogo
     * @return el resultado con el formato {@code [CODIGO] mensaje}
     */
    public static String resultado(String codigo, String mensaje) {
        return "[" + texto(codigo) + "] " + texto(mensaje);
    }
}
