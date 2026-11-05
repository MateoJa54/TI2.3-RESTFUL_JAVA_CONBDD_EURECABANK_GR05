package ec.edu.monster.consola.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash SHA-256 de la clave del usuario.
 *
 * <p>Regla RB-13 del contrato congelado: la contraseña nunca viaja en claro
 * por la red. El cliente calcula el SHA-256 hexadecimal de la contraseña y lo
 * envía; el servidor compara ese valor con {@code usuario.vch_emplclave} en
 * tiempo constante.
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
     * @return el hash en minúsculas, con 64 caracteres
     */
    public static String sha256(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El texto a hashear es obligatorio");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return aHexadecimal(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible en esta JVM", e);
        }
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
     * Convierte un arreglo de bytes en su representación hexadecimal.
     *
     * @param bytes bytes a convertir
     * @return la cadena hexadecimal en minúsculas
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
