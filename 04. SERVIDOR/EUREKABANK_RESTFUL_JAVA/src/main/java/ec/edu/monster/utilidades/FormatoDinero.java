package ec.edu.monster.utilidades;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilidades de formato de importes.
 *
 * <p>Regla RB-01 del contrato congelado: todo importe se maneja con
 * {@code BigDecimal} de escala 2 y redondeo {@code HALF_UP}. Nunca con
 * {@code double} ni con {@code float}, porque esos tipos no representan
 * exactamente los centimos del banco.
 */
public final class FormatoDinero {

    /** Escala de todos los importes y saldos del banco. */
    public static final int ESCALA = 2;

    /** Redondeo usado en todo el sistema. */
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /** Cero con la escala del banco. */
    public static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA, REDONDEO);

    private FormatoDinero() {
    }

    /**
     * Normaliza un importe a dos decimales con redondeo {@code HALF_UP}.
     *
     * @param valor importe de entrada; puede ser {@code null}
     * @return el importe con escala 2, o {@code null} si la entrada fue {@code null}
     */
    public static BigDecimal aDosDecimales(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return valor.setScale(ESCALA, REDONDEO);
    }

    /**
     * Suma dos importes y normaliza el resultado a dos decimales.
     *
     * @param valor1 primer sumando
     * @param valor2 segundo sumando
     * @return la suma con escala 2
     */
    public static BigDecimal sumar(BigDecimal valor1, BigDecimal valor2) {
        return aDosDecimales(valor1.add(valor2));
    }

    /**
     * Resta dos importes y normaliza el resultado a dos decimales.
     *
     * @param valor1 minuendo
     * @param valor2 sustraendo
     * @return la resta con escala 2
     */
    public static BigDecimal restar(BigDecimal valor1, BigDecimal valor2) {
        return aDosDecimales(valor1.subtract(valor2));
    }

    /**
     * Calcula un porcentaje sobre un importe y normaliza a dos decimales.
     *
     * <p>Se usa para el ITF ({@code monto x parametro / 100}) y para el interes
     * mensual proyectado ({@code saldo x interes / 100}).
     *
     * @param base     importe base
     * @param porcentaje porcentaje a aplicar
     * @return el resultado con escala 2
     */
    public static BigDecimal porcentaje(BigDecimal base, BigDecimal porcentaje) {
        return aDosDecimales(base.multiply(porcentaje).divide(BigDecimal.valueOf(100)));
    }

    /**
     * Indica si el importe es utilizable en una operacion: mayor que cero y con
     * como maximo dos decimales.
     *
     * @param valor importe de entrada
     * @return {@code true} si cumple las dos condiciones
     */
    public static boolean esMontoValido(BigDecimal valor) {
        if (valor == null) {
            return false;
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return valor.stripTrailingZeros().scale() <= ESCALA;
    }
}
