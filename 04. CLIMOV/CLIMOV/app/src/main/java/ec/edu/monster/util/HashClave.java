package ec.edu.monster.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Calcula el SHA-256 hexadecimal exigido por el contrato al iniciar sesión. */
public final class HashClave {
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private HashClave() { }

    public static String sha256(String texto) {
        if (texto == null) throw new IllegalArgumentException("La contraseña es obligatoria.");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder salida = new StringBuilder(hash.length * 2);
            for (byte valor : hash) {
                salida.append(HEX[(valor >>> 4) & 0x0f]);
                salida.append(HEX[valor & 0x0f]);
            }
            return salida.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en este dispositivo.", e);
        }
    }
}
