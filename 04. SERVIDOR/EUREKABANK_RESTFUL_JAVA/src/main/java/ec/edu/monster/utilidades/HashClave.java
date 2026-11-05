package ec.edu.monster.utilidades;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidades de hash de la clave del usuario.
 *
 * <p>Regla RB-13 del contrato congelado: la clave se compara con
 * {@code MessageDigest.isEqual}, que no filtra informacion por tiempo, y el
 * valor almacenado en {@code usuario.vch_emplclave} es el SHA-256 en
 * hexadecimal de la clave real.
 *
 * <p>La decision D7 del contrato congelado ({@code 01. UML/00. CONTRATO/}) fija que <b>el cliente envia
 * el hash</b> y que el servidor compara directamente contra
 * {@code usuario.vch_emplclave}. Para no romper a un cliente que envie la
 * clave en claro, {@link #coincide(String, String)} acepta las dos formas:
 * si el valor recibido ya es un SHA-256 hexadecimal de 64 caracteres se
 * compara tal cual; si no, se calcula su SHA-256 y se compara ese.
 */
public final class HashClave {

    /** Longitud en caracteres de un hash SHA-256 hexadecimal. */
    public static final int LONGITUD_HASH = 64;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HashClave() {
    }

    /**
     * Calcula el SHA-256 en hexadecimal de un texto.
     *
     * @param texto texto a hashear
     * @return el hash en minusculas, con 64 caracteres
     */
    public static String sha256(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El texto a hashear es obligatorio");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return aHexadecimal(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El algoritmo SHA-256 no esta disponible en esta JVM", e);
        }
    }

    /**
     * Compara la clave recibida con el hash almacenado en tiempo constante.
     *
     * <p>Si el valor recibido ya es un SHA-256 hexadecimal (64 caracteres) se
     * compara directamente; en caso contrario se compara su SHA-256.
     *
     * @param claveRecibida clave que llega del cliente (hash o clave en claro)
     * @param hashEsperado  hash SHA-256 hexadecimal guardado en {@code usuario}
     * @return {@code true} si la clave corresponde al hash almacenado
     */
    public static boolean coincide(String claveRecibida, String hashEsperado) {
        if (claveRecibida == null || hashEsperado == null) {
            return false;
        }
        String recibido = claveRecibida.trim();
        // El hash se tolera con espacios exteriores (viene de copiar y pegar);
        // la clave en claro se usa tal cual: un espacio final es otra clave.
        String candidato = esHashSha256(recibido) ? recibido : sha256(claveRecibida);
        return MessageDigest.isEqual(
                candidato.getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Indica si un texto es un hash SHA-256 hexadecimal.
     *
     * @param texto texto a comprobar
     * @return {@code true} si tiene 64 caracteres hexadecimales
     */
    public static boolean esHashSha256(String texto) {
        if (texto == null || texto.length() != LONGITUD_HASH) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            boolean hexadecimal = (caracter >= '0' && caracter <= '9')
                    || (caracter >= 'a' && caracter <= 'f')
                    || (caracter >= 'A' && caracter <= 'F');
            if (!hexadecimal) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convierte un arreglo de bytes en su representacion hexadecimal.
     *
     * @param bytes bytes a convertir
     * @return la cadena hexadecimal en minusculas
     */
    public static String aHexadecimal(byte[] bytes) {
        StringBuilder texto = new StringBuilder(bytes.length * 2);
        for (byte valor : bytes) {
            texto.append(HEX[(valor >> 4) & 0x0F]);
            texto.append(HEX[valor & 0x0F]);
        }
        return texto.toString();
    }
}
