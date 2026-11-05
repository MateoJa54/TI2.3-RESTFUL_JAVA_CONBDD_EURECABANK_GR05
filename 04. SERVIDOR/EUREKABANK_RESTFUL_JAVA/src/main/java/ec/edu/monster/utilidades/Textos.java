package ec.edu.monster.utilidades;

/**
 * Utilidades de texto.
 *
 * <p>Mismo criterio que el proyecto del docente {@code TI.1.1}: una cadena
 * "vacia" es {@code null}, vacia o solo con espacios. Se usan espacios Unicode
 * (\p{Z}) y no solo los de la tabla ASCII, porque al copiar y pegar desde una
 * pagina web llega tambien el espacio duro (U+00A0).
 */
public final class Textos {

    private Textos() {
    }

    /**
     * Indica si un texto esta vacio.
     *
     * @param texto texto a comprobar
     * @return {@code true} si es {@code null}, vacio o solo tiene espacios
     */
    public static boolean estaVacio(String texto) {
        if (texto == null || texto.isEmpty()) {
            return true;
        }
        return texto.chars().allMatch(c -> Character.isWhitespace(c) || c == '\u00A0');
    }

    /**
     * Recorta los espacios exteriores de un texto.
     *
     * @param texto texto a recortar
     * @return el texto sin espacios exteriores, o {@code null} si la entrada fue {@code null}
     */
    public static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.strip();
    }
}
