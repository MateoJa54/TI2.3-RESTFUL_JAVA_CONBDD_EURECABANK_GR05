package ec.edu.monster.pruebas;

import ec.edu.monster.util.HashClave;
import org.junit.Test;
import static org.junit.Assert.*;

public class HashClaveTest {
    @Test public void calculaSha256HexadecimalDelContrato() {
        assertEquals("2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b", HashClave.sha256("secret"));
    }

    @Test public void rechazaContrasenaNula() {
        try { HashClave.sha256(null); fail("Debió rechazar null"); }
        catch (IllegalArgumentException esperado) { assertTrue(esperado.getMessage().contains("contraseña")); }
    }
}
