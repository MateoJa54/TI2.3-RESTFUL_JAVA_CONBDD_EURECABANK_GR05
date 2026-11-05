package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.util.HashClave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El hash de la clave es el mismo que calcula el servidor: si no, el usuario
 * nunca podría autenticarse.
 */
class HashClaveTest {

    @Test
    void elHashDeLaClaveDeLaSemillaEsElDelContrato() {
        assertEquals(ClienteFalso.HASH_CLAVE, HashClave.sha256(ClienteFalso.CLAVE));
    }

    @Test
    void elHashDelTextoVacioEsElConocido() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HashClave.sha256(""));
    }

    @Test
    void elHashTiene64CaracteresHexadecimalesEnMinusculas() {
        String hash = HashClave.sha256("Cromero2026");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void elHashEsDeterminista() {
        assertEquals(HashClave.sha256("clave"), HashClave.sha256("clave"));
    }

    @Test
    void dosClavesDistintasDanHashesDistintos() {
        assertNotEquals(HashClave.sha256("clave"), HashClave.sha256("clave "));
    }

    @Test
    void elTextoNuloSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> HashClave.sha256(null));
    }

    @Test
    void elHashDeUnTextoConAcentosUsaUtf8() {
        assertEquals("024bb90888ca89a15a19e9bdd8c712bfb070465fce1ef25e43c170ea44fc5e5f",
                HashClave.sha256("ñ"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "ab", "abc", "abcd", "abcde", "abcdef", "abcdefg", "abcdefgh",
            "abcdefghi", "abcdefghij", "abcdefghijk", "abcdefghijkl", "abcdefghijklm",
            "abcdefghijklmn", "abcdefghijklmno", "abcdefghijklmnop", "abcdefghijklmnopq",
            "abcdefghijklmnopqr", "abcdefghijklmnopqrs", "abcdefghijklmnopqrst",
            "abcdefghijklmnopqrstu", "abcdefghijklmnopqrstuv", "abcdefghijklmnopqrstuvw"})
    void elHashDeCualquierTextoSiempreEsUnHashDe64Caracteres(String texto) {
        assertTrue(HashClave.esHashSha256(HashClave.sha256(texto)));
        assertFalse(HashClave.esHashSha256(texto));
    }

    @Test
    void unHashEnMinusculasYEnMayusculasSeReconocen() {
        assertTrue(HashClave.esHashSha256(ClienteFalso.HASH_CLAVE));
        assertTrue(HashClave.esHashSha256(ClienteFalso.HASH_CLAVE.toUpperCase()));
    }

    @Test
    void unTextoQueNoEsHashNoSeReconoce() {
        assertFalse(HashClave.esHashSha256(null));
        assertFalse(HashClave.esHashSha256("Cromero2026"));
        assertFalse(HashClave.esHashSha256(ClienteFalso.HASH_CLAVE + "a"));
        assertFalse(HashClave.esHashSha256(ClienteFalso.HASH_CLAVE.substring(1)));
    }

    @Test
    void unHashConLetraNoHexadecimalNoSeReconoce() {
        StringBuilder conLetra = new StringBuilder(ClienteFalso.HASH_CLAVE);
        conLetra.setCharAt(0, 'z');

        assertFalse(HashClave.esHashSha256(conLetra.toString()));
    }

    @Test
    void losBytesSeConviertenEnHexadecimalDeDosCaracteresCadaUno() {
        assertEquals("00010f", HashClave.aHexadecimal(new byte[] {0x00, 0x01, 0x0F}));
    }

    @Test
    void unArregloVacioDaUnaCadenaVacia() {
        assertEquals("", HashClave.aHexadecimal(new byte[0]));
    }
}
