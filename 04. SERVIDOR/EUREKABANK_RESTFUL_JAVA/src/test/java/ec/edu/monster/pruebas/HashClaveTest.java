package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.utilidades.HashClave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del hash de la clave (regla RB-13).
 */
class HashClaveTest {

    /** Hash SHA-256 de {@code Cromero2026}, el del usuario {@code cromero}. */
    private static final String HASH_CROMERO =
            "891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4";

    @Test
    void elHashDeLaClaveDelSemillaCoincideConElDeLaBaseDeDatos() {
        assertEquals(HASH_CROMERO, HashClave.sha256("Cromero2026"));
    }

    @Test
    void elHashTieneSesentaYCuatroCaracteresHexadecimales() {
        String hash = HashClave.sha256("Cromero2026");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"), hash);
    }

    @Test
    void elHashEsDeterminista() {
        assertEquals(HashClave.sha256("Cromero2026"), HashClave.sha256("Cromero2026"));
    }

    @Test
    void dosClavesDistintasDanHashesDistintos() {
        assertNotEquals(HashClave.sha256("Cromero2026"), HashClave.sha256("Cromero2027"));
    }

    @Test
    void unTextoNuloNoSePuedeHashear() {
        assertThrows(IllegalArgumentException.class, () -> HashClave.sha256(null));
    }

    @Test
    void laClaveCorrectaCoincideConElHash() {
        assertTrue(HashClave.coincide("Cromero2026", HASH_CROMERO));
    }

    @Test
    void elHashRecibidoTambienCoincideConElHashAlmacenado() {
        // Decision D7 del contrato congelado: el cliente envia el hash y el servidor
        // lo compara contra usuario.vch_emplclave.
        assertTrue(HashClave.coincide(HASH_CROMERO, HASH_CROMERO));
    }

    @Test
    void unHashDeLongitudDistintaNoSeComparaDirectamente() {
        assertFalse(HashClave.coincide(
                "32b3491336522e073489725b5daf298cd749007a",
                "32b3491336522e073489725b5daf298cd749007a"));
    }

    @Test
    void unTextoDeSesentaYCuatroCaracteresNoHexadecimalesSeHashea() {
        assertFalse(HashClave.esHashSha256("z".repeat(64)));
        assertTrue(HashClave.esHashSha256(HASH_CROMERO));
        assertFalse(HashClave.esHashSha256(HASH_CROMERO.substring(1)));
        assertFalse(HashClave.esHashSha256(null));
    }

    @Test
    void unaClaveIncorrectaNoCoincide() {
        assertFalse(HashClave.coincide("Cromero2027", HASH_CROMERO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cromero2026", "CROMERO2026", "Cromero2026 ", " Cromero2026"})
    void unaClaveConVariacionesNoCoincide(String clave) {
        assertFalse(HashClave.coincide(clave, HASH_CROMERO));
    }

    @Test
    void unHashDeLongitudDistintaNoCoincide() {
        // Los usuarios de la semilla con SHA-1 heredado (40 caracteres) no son
        // utilizables: la comparacion en tiempo constante falla.
        assertFalse(HashClave.coincide("Cromero2026",
                "32b3491336522e073489725b5daf298cd749007a"));
    }

    @Test
    void unHashNuloNoCoincideConNingunaClave() {
        assertFalse(HashClave.coincide("Cromero2026", null));
        assertFalse(HashClave.coincide(null, HASH_CROMERO));
    }

    @Test
    void laComparacionEsEnTiempoConstantePorLongitudDeBytes() {
        // MessageDigest.isEqual no da informacion por tiempo: con longitudes
        // distintas devuelve false sin comparar byte a byte.
        assertFalse(HashClave.coincide("Cromero2026", HASH_CROMERO.substring(1)));
    }

    @Test
    void elHexadecimalSeEscribeEnMinusculas() {
        assertEquals("00ff10", HashClave.aHexadecimal(new byte[] {0, (byte) 0xFF, 0x10}));
        assertEquals("", HashClave.aHexadecimal(new byte[0]));
    }
}
