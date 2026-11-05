package ec.edu.monster.consola.util;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * Salida de la consola: todo lo que el programa escribe pasa por aquí.
 *
 * <p>Recibe el {@code PrintStream} por constructor para que las vistas se
 * puedan probar con texto preparado, sin depender de la consola real.
 */
public class Consola {

    private final PrintStream salida;

    /**
     * @param salida flujo donde se escribe
     */
    public Consola(PrintStream salida) {
        this.salida = Objects.requireNonNull(salida, "La salida es obligatoria");
    }

    /**
     * Escribe una línea de texto.
     *
     * @param texto contenido de la línea
     */
    public void linea(String texto) {
        salida.println(texto);
    }

    /**
     * Escribe una línea vacía.
     */
    public void vacio() {
        salida.println();
    }

    /**
     * Escribe un título enmarcado entre dos líneas de signos iguales.
     *
     * @param texto título del bloque
     */
    public void titulo(String texto) {
        salida.println();
        salida.println("=== " + texto + " ===");
    }

    /**
     * Escribe una tabla con cabecera y filas.
     *
     * @param cabecera títulos de las columnas
     * @param filas     contenido de cada fila
     */
    public void tabla(String[] cabecera, List<String[]> filas) {
        int[] anchos = new int[cabecera.length];
        for (int columna = 0; columna < cabecera.length; columna++) {
            anchos[columna] = cabecera[columna].length();
        }
        for (String[] fila : filas) {
            for (int columna = 0; columna < Math.min(anchos.length, fila.length); columna++) {
                anchos[columna] = Math.max(anchos[columna], fila[columna].length());
            }
        }
        StringBuilder separador = new StringBuilder();
        for (int columna = 0; columna < cabecera.length; columna++) {
            separador.append("-".repeat(anchos[columna] + 2));
        }
        salida.println(separador);
        escribirFila(cabecera, anchos);
        salida.println(separador);
        for (String[] fila : filas) {
            escribirFila(fila, anchos);
        }
        salida.println(separador);
    }

    private void escribirFila(String[] celdas, int[] anchos) {
        StringBuilder linea = new StringBuilder();
        for (int columna = 0; columna < anchos.length; columna++) {
            String celda = columna < celdas.length ? celdas[columna] : "";
            linea.append(' ').append(celda);
            if (columna < anchos.length - 1) {
                linea.append(" ".repeat(anchos[columna] - celda.length() + 1));
            }
        }
        salida.println(linea.toString());
    }
}
