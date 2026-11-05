package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.vista.Tema;
import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Paleta, radios, tipografía y contraste del tema Monster. */
class TemaTest {

    @BeforeAll
    static void instalarTema() {
        Swing.enEdt(Tema::instalar);
    }

    /** Luminancia relativa según WCAG 2.1. */
    private static double luminancia(Color c) {
        double[] canales = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) {
            canales[i] = canales[i] <= 0.03928 ? canales[i] / 12.92 : Math.pow((canales[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * canales[0] + 0.7152 * canales[1] + 0.0722 * canales[2];
    }

    private static double contraste(Color a, Color b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    @Test
    void elAcentoEsElTealDeSulley() {
        assertEquals(new Color(0x0B7A8A), Tema.ACENTO);
        assertEquals(new Color(0x096574), Tema.ACENTO_HOVER);
    }

    @Test
    void lasSuperficiesYElTextoSonLosDelDiseno() {
        assertEquals(new Color(0xF4F7F9), Tema.FONDO);
        assertEquals(new Color(0xFFFFFF), Tema.SUPERFICIE);
        assertEquals(new Color(0xDDE5EA), Tema.BORDE);
        assertEquals(new Color(0x12202B), Tema.TEXTO);
        assertEquals(new Color(0x55636F), Tema.TEXTO_SUAVE);
        assertEquals(new Color(0xB42318), Tema.ERROR);
        assertEquals(new Color(0x1E7A4F), Tema.EXITO);
    }

    @Test
    void elContrasteDelAcentoConBlancoEsAlMenos4_5() {
        assertTrue(contraste(Tema.ACENTO, Color.WHITE) >= 4.5, "contraste " + contraste(Tema.ACENTO, Color.WHITE));
    }

    @Test
    void elContrasteDelAcentoAlPasarElPunteroConBlancoEsAlMenos4_5() {
        assertTrue(contraste(Tema.ACENTO_HOVER, Color.WHITE) >= 4.5);
    }

    @Test
    void elTextoSuaveSobreBlancoTieneContrasteAA() {
        assertTrue(contraste(Tema.TEXTO_SUAVE, Tema.SUPERFICIE) >= 4.5);
    }

    @Test
    void elTextoPrincipalSobreElFondoTieneContrasteAA() {
        assertTrue(contraste(Tema.TEXTO, Tema.FONDO) >= 4.5);
        assertTrue(contraste(Tema.TEXTO, Tema.SUPERFICIE) >= 4.5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ERROR", "EXITO", "TEXTO_SUAVE", "ACENTO"})
    void losColoresDeMensajeSeLeenSobreLaTarjetaYElFondo(String nombre) throws ReflectiveOperationException {
        Color color = (Color) Tema.class.getField(nombre).get(null);
        assertTrue(contraste(color, Tema.SUPERFICIE) >= 4.5, nombre + " sobre la tarjeta");
        assertTrue(contraste(color, Tema.FONDO) >= 4.5, nombre + " sobre el fondo");
    }

    @Test
    void elContrasteSeCalculaBienConUnCasoConocido() {
        assertEquals(21.0, contraste(Color.BLACK, Color.WHITE), 0.001);
        assertEquals(1.0, contraste(Color.WHITE, Color.WHITE), 0.001);
    }

    @Test
    void noHayNegroPuroNiVioleta() {
        for (Color c : new Color[] {Tema.TEXTO, Tema.FONDO, Tema.BORDE, Tema.ERROR, Tema.EXITO, Tema.ACENTO, Tema.TEXTO_SUAVE}) {
            assertNotEquals(Color.BLACK, c);
            // Violeta: azul y rojo altos con verde bajo.
            assertFalse(c.getRed() > 120 && c.getBlue() > 120 && c.getGreen() < c.getRed() - 40 && c.getGreen() < c.getBlue() - 40,
                    "color violeta: " + c);
        }
    }

    @Test
    void losRadiosSonSoloOchoYDoce() {
        assertEquals(8, Tema.RADIO);
        assertEquals(12, Tema.RADIO_TARJETA);
    }

    @Test
    void lasFilasDeLasTablasMiden40() {
        assertEquals(40, Tema.ALTO_FILA);
        assertEquals(40, UIManager.getInt("Table.rowHeight"));
    }

    @ParameterizedTest
    @ValueSource(floats = {11f, 12f, 13f, 15f, 22f})
    void laFuenteTieneElTamanoPedido(float tamano) {
        Font fuente = Tema.fuente(tamano);
        assertEquals(tamano, fuente.getSize2D(), 0.001f);
        assertFalse(fuente.isBold());
    }

    @ParameterizedTest
    @ValueSource(floats = {12f, 13f, 15f, 22f})
    void laFuenteNegritaTieneElTamanoPedido(float tamano) {
        Font fuente = Tema.fuenteNegrita(tamano);
        assertEquals(tamano, fuente.getSize2D(), 0.001f);
        assertTrue(fuente.isBold());
    }

    @Test
    void laFamiliaEsSegoeUiOSuRespaldoSansSerif() {
        String familia = Tema.fuente(13f).getFamily();
        assertTrue(familia.equals("Segoe UI") || familia.equals(Font.SANS_SERIF) || familia.equals(Font.DIALOG),
                "familia " + familia);
    }

    @Test
    void laFuenteBaseEsDe13Puntos() {
        assertEquals(13f, Tema.TAMANO_BASE, 0.001f);
        assertEquals(13f, UIManager.getFont("defaultFont").getSize2D(), 0.001f);
    }

    @Test
    void instalarAplicaElAcentoALosAnillosDeFoco() {
        assertEquals(Tema.ACENTO, UIManager.getColor("Component.focusColor"));
        assertEquals(Tema.ACENTO, UIManager.getColor("Component.focusedBorderColor"));
        assertEquals(2, UIManager.getInt("Component.focusWidth"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Button.arc", "Component.arc", "TextComponent.arc", "CheckBox.arc", "ComboBox.arc"})
    void instalarAplicaElRadioDeLosControles(String clave) {
        assertEquals(8, UIManager.getInt(clave), clave);
    }

    @Test
    void instalarPintaElBotonPrincipalConElAcento() {
        assertEquals(Tema.ACENTO, UIManager.getColor("Button.background"));
        assertEquals(Color.WHITE, UIManager.getColor("Button.foreground"));
        assertEquals(Tema.ACENTO_HOVER, UIManager.getColor("Button.hoverBackground"));
    }

    @Test
    void instalarPonePanelesYTablasConLaPaleta() {
        assertEquals(Tema.FONDO, UIManager.getColor("Panel.background"));
        assertEquals(Tema.SUPERFICIE, UIManager.getColor("Table.background"));
        assertEquals(Tema.BORDE, UIManager.getColor("Table.gridColor"));
        assertEquals(Tema.TEXTO, UIManager.getColor("Table.foreground"));
    }

    @Test
    void laBaseEsFlatLafClaro() {
        assertEquals("FlatLaf Light", UIManager.getLookAndFeel().getName());
    }

    @Test
    void instalarDosVecesNoFalla() {
        Swing.enEdt(Tema::instalar);
        assertEquals(Tema.ACENTO, UIManager.getColor("Component.focusColor"));
    }
}
