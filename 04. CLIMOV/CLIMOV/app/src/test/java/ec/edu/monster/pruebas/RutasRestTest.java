package ec.edu.monster.pruebas;

import ec.edu.monster.controlador.ControladorBanco;
import ec.edu.monster.ws.ClienteEurekaBank;
import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

/** Verifica las rutas de las nueve operaciones, la URL configurable y los montos. */
public class RutasRestTest {
    @Test public void rutasCubrenLasNueveOperacionesDelClienteConsola() {
        assertEquals("/auth/login", ControladorBanco.RUTA_LOGIN);
        assertEquals("/auth/logout", ControladorBanco.RUTA_LOGOUT);
        assertEquals("/clientes/06914897/cuentas", ControladorBanco.rutaCuentas("06914897"));
        assertEquals("/cuentas/00200002", ControladorBanco.rutaCuenta("00200002"));
        assertEquals("/cuentas/00200002/movimientos", ControladorBanco.rutaMovimientos("00200002"));
        assertEquals("/cuentas/00200002/depositos", ControladorBanco.rutaDeposito("00200002"));
        assertEquals("/cuentas/00200002/retiros", ControladorBanco.rutaRetiro("00200002"));
        assertEquals("/transferencias", ControladorBanco.RUTA_TRANSFERENCIA);
        assertEquals("/cuentas/00200002/costos", ControladorBanco.rutaCostos("00200002"));
    }

    @Test public void usaDireccionQuePuedeCambiarsePorIpDeRed() {
        assertEquals("http://192.168.1.25:8080/EUREKABANK_RESTFUL_JAVA/api/clientes/06914897/cuentas",
                ClienteEurekaBank.construirUrl("http://192.168.1.25:8080/EUREKABANK_RESTFUL_JAVA/api/", ControladorBanco.rutaCuentas("06914897")));
        assertEquals("00200002", ClienteEurekaBank.codificarSegmento("00200002"));
    }

    @Test public void montoUsaBigDecimalEscalaDosYRedondeoHalfUp() {
        assertEquals(new BigDecimal("10.13"), ControladorBanco.convertirImporte("10,125"));
        assertEquals(new BigDecimal("0.00"), ControladorBanco.convertirImporte("0"));
    }

    @Test public void rechazaTextoQueNoSeaNumero() {
        try { ControladorBanco.convertirImporte("diez"); fail("Debió rechazar el monto"); }
        catch (IllegalArgumentException esperado) { assertTrue(esperado.getMessage().contains("numérico")); }
    }
}
