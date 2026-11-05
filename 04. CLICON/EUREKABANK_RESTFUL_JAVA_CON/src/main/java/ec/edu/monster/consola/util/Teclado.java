package ec.edu.monster.consola.util;

import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Lee y valida lo que el usuario escribe en la consola.
 *
 * <p>Recibe los flujos de entrada y salida por constructor para que las
 * vistas se puedan probar con texto preparado, sin teclado.
 */
public class Teclado {

    /** Dígitos con punto o coma decimal y signo opcional: 12 | -3.5 | 0,25 */
    private static final Pattern NUMERO_DECIMAL =
            Pattern.compile("[+-]?(\\d+([.,]\\d*)?|[.,]\\d+)");
    private static final Pattern NUMERO_ENTERO = Pattern.compile("\\d{1,9}");

    /** Escalas de un importe del contrato: dos decimales como máximo. */
    private static final int ESCALA_MAXIMA = 2;

    private final Scanner lector;
    private final PrintStream salida;
    private final Console consola;

    /**
     * Constructor para pruebas: nunca usa la consola real.
     *
     * @param entrada flujo de entrada
     * @param salida  flujo de salida
     */
    public Teclado(InputStream entrada, PrintStream salida) {
        this(entrada, salida, StandardCharsets.UTF_8, null);
    }

    /**
     * @param entrada     flujo de entrada
     * @param salida      flujo de salida
     * @param codificacion codificación en que llega lo que se escribe
     * @param consola     consola real para leer la contraseña sin mostrarla;
     *                    {@code null} si no hay
     */
    public Teclado(InputStream entrada, PrintStream salida, Charset codificacion, Console consola) {
        this.lector = new Scanner(entrada, codificacion);
        this.salida = Objects.requireNonNull(salida, "La salida es obligatoria");
        this.consola = consola;
    }

    /**
     * @param mensaje texto que se muestra antes de leer
     * @return la línea escrita, sin espacios al principio ni al final
     * @throws NoSuchElementException si la entrada se cerró
     */
    public String texto(String mensaje) {
        return leerLinea(mensaje).trim();
    }

    /**
     * Lee la contraseña sin mostrarla cuando hay consola real. No se le quitan
     * los espacios: en una contraseña son caracteres válidos.
     *
     * @param mensaje texto que se muestra antes de leer
     * @return la contraseña escrita
     * @throws NoSuchElementException si la entrada se cerró
     */
    public String secreto(String mensaje) {
        if (consola == null) {
            return leerLinea(mensaje);
        }
        char[] escrito = consola.readPassword("%s: ", mensaje);
        if (escrito == null) {
            throw new NoSuchElementException("La entrada se cerró");
        }
        return new String(escrito);
    }

    /**
     * Repite la pregunta hasta recibir un número entero dentro del rango.
     *
     * @param mensaje texto que se muestra antes de leer
     * @param minimo  valor mínimo aceptado
     * @param maximo  valor máximo aceptado
     * @return el número escrito
     */
    public int opcion(String mensaje, int minimo, int maximo) {
        while (true) {
            String escrito = texto(mensaje);
            if (NUMERO_ENTERO.matcher(escrito).matches()) {
                int opcion = Integer.parseInt(escrito);
                if (opcion >= minimo && opcion <= maximo) {
                    return opcion;
                }
            }
            aviso("  Opción no válida. Escriba un número del " + minimo + " al " + maximo);
        }
    }

    /**
     * Repite la pregunta hasta recibir un importe válido: mayor que cero y con
     * dos decimales como máximo, que es lo que exige el contrato (MON-001).
     *
     * @param mensaje texto que se muestra antes de leer
     * @return el importe escrito
     */
    public BigDecimal monto(String mensaje) {
        while (true) {
            String escrito = texto(mensaje);
            BigDecimal importe = interpretarMonto(escrito);
            if (importe != null) {
                return importe;
            }
            aviso("  Monto no válido. Debe ser mayor que cero y con máximo dos decimales.");
        }
    }

    /**
     * Escribe un aviso de la consola.
     *
     * @param mensaje texto del aviso
     */
    public void aviso(String mensaje) {
        salida.println(mensaje);
    }

    /**
     * Convierte un texto en un importe del contrato.
     *
     * @param texto texto escrito por el usuario
     * @return el importe, o {@code null} si el texto no es un importe válido
     */
    public static BigDecimal interpretarMonto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty() || !NUMERO_DECIMAL.matcher(limpio).matches()) {
            return null;
        }
        BigDecimal importe;
        try {
            importe = new BigDecimal(limpio.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
        if (importe.signum() <= 0) {
            return null;
        }
        if (importe.stripTrailingZeros().scale() > ESCALA_MAXIMA) {
            return null;
        }
        return importe.setScale(ESCALA_MAXIMA, java.math.RoundingMode.HALF_UP);
    }

    private String leerLinea(String mensaje) {
        salida.print(mensaje + ": ");
        salida.flush();
        if (!lector.hasNextLine()) {
            throw new NoSuchElementException("La entrada se cerró");
        }
        return lector.nextLine();
    }
}
