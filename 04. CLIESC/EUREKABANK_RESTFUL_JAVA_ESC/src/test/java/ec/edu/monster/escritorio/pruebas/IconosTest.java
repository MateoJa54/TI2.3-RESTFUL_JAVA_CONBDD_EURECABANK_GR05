package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.vista.Iconos;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Iconos Java2D: tamaño, trazo y que cada figura realmente pinta píxeles. */
class IconosTest {

    private static BufferedImage pintar(Icon icono) {
        BufferedImage imagen = new BufferedImage(icono.getIconWidth() + 4, icono.getIconHeight() + 4,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        try {
            icono.paintIcon(null, g, 2, 2);
        } finally {
            g.dispose();
        }
        return imagen;
    }

    private static int pixelesPintados(BufferedImage imagen) {
        int cuenta = 0;
        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) {
                if ((imagen.getRGB(x, y) >>> 24) != 0) {
                    cuenta++;
                }
            }
        }
        return cuenta;
    }

    @Test
    void elTamanoNormalEs20YElTrazoEs175() {
        Icon icono = Iconos.de(Iconos.Tipo.CUENTAS, Color.BLACK);
        assertEquals(20, icono.getIconWidth());
        assertEquals(20, icono.getIconHeight());
        assertEquals(20, Iconos.TAMANO);
        assertEquals(1.75f, Iconos.TRAZO, 0.0001f);
    }

    @Test
    void elTamanoSePuedeCambiar() {
        Icon grande = Iconos.de(Iconos.Tipo.MOVIMIENTOS, 40, Color.BLACK);
        assertEquals(40, grande.getIconWidth());
        assertEquals(40, grande.getIconHeight());
    }

    @ParameterizedTest
    @EnumSource(Iconos.Tipo.class)
    void cadaFiguraPintaPixeles(Iconos.Tipo tipo) {
        int pixeles = pixelesPintados(pintar(Iconos.de(tipo, 20, Color.BLACK)));
        assertTrue(pixeles > 20, tipo + " pinta " + pixeles + " píxeles");
    }

    @Test
    void lasFigurasDeCadaDestinoSonDistintas() {
        BufferedImage cuentas = pintar(Iconos.de(Iconos.Tipo.CUENTAS, 20, Color.BLACK));
        BufferedImage movimientos = pintar(Iconos.de(Iconos.Tipo.MOVIMIENTOS, 20, Color.BLACK));
        BufferedImage costos = pintar(Iconos.de(Iconos.Tipo.COSTOS, 20, Color.BLACK));
        assertFalse(sonIguales(cuentas, movimientos));
        assertFalse(sonIguales(movimientos, costos));
    }

    @Test
    void elColorDelIconoSeAplica() {
        BufferedImage imagen = pintar(Iconos.de(Iconos.Tipo.ERROR, 20, new Color(0xB42318)));
        boolean encontrado = false;
        for (int y = 0; y < imagen.getHeight() && !encontrado; y++) {
            for (int x = 0; x < imagen.getWidth() && !encontrado; x++) {
                int argb = imagen.getRGB(x, y);
                encontrado = (argb >>> 24) == 255 && (argb & 0xFFFFFF) == 0xB42318;
            }
        }
        assertTrue(encontrado, "el trazo debe tener el color pedido");
    }

    @Test
    void noAceptaTipoNiTamanoInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Iconos.de(null, Color.BLACK));
        assertThrows(IllegalArgumentException.class, () -> Iconos.de(Iconos.Tipo.CUENTAS, 0, Color.BLACK));
    }

    private static boolean sonIguales(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void elIconoDeOjoCambiaAlTachado() {
        assertFalse(sonIguales(pintar(Iconos.de(Iconos.Tipo.MOSTRAR, Color.BLACK)),
                pintar(Iconos.de(Iconos.Tipo.OCULTAR, Color.BLACK))));
    }
}
