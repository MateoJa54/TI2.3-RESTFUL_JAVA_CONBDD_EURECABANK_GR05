package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.vista.Aviso;
import ec.edu.monster.escritorio.vista.Tema;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Avisos no modales: texto, color, escape del mensaje, ubicación y cierre. */
class AvisoTest {

    @BeforeAll
    static void tema() {
        Swing.enEdt(Tema::instalar);
    }

    @Test
    void elAvisoDuraSeisSegundos() {
        Aviso aviso = new Aviso("Operación registrada", false);
        assertEquals(6000, aviso.duracionMs());
        assertEquals(Aviso.DURACION_MS, aviso.duracionMs());
    }

    @Test
    void muestraElMensajeDelServidorConSuColor() {
        Aviso exito = new Aviso("Depósito registrado correctamente", false);
        JLabel texto = Swing.buscar(exito, "textoAviso", JLabel.class);
        assertTrue(texto.getText().contains("Depósito registrado correctamente"));
        assertEquals(Tema.EXITO, texto.getForeground());
        assertFalse(exito.esError());

        Aviso error = new Aviso("El servicio no está disponible en este momento", true);
        JLabel textoError = Swing.buscar(error, "textoAviso", JLabel.class);
        assertTrue(textoError.getText().contains("El servicio no está disponible en este momento"));
        assertEquals(Tema.ERROR, textoError.getForeground());
        assertTrue(error.esError());
    }

    @Test
    void elMensajeNoSeInterpretaComoHtml() {
        Aviso aviso = new Aviso("<b>negrita</b> & más", true);
        String texto = Swing.buscar(aviso, "textoAviso", JLabel.class).getText();
        assertTrue(texto.contains("&lt;b&gt;negrita&lt;/b&gt; &amp; más"), texto);
    }

    @Test
    void tieneBotonParaCerrarYRolAccesible() {
        Aviso aviso = new Aviso("ok", false);
        JButton cerrar = Swing.buscar(aviso, "botonCerrarAviso", JButton.class);
        assertEquals("Cerrar aviso", cerrar.getAccessibleContext().getAccessibleName());
        assertEquals("aviso", aviso.getName());
    }

    @Test
    void sinVentanaNoMuestraNada() {
        assertNull(Aviso.mostrar(new JPanel(), "Sin ventana", false));
        assertNull(Aviso.mostrar(null, "Nulo", true));
    }

    @Test
    void seUbicaEnLaEsquinaInferiorDerecha() {
        Swing.enEdt(() -> {
            JLayeredPane capa = new JLayeredPane();
            capa.setSize(800, 600);
            Aviso aviso = new Aviso("Ubicación", false);
            aviso.adjuntar(capa);
            assertSame(capa, aviso.getParent());
            assertEquals(360, aviso.getWidth());
            assertEquals(800 - 16 - 360, aviso.getX());
            assertEquals(600 - 16, aviso.getY() + aviso.getHeight());
            aviso.cerrar();
        });
    }

    @Test
    void cerrarQuitaElAvisoYSePuedeRepetir() {
        Swing.enEdt(() -> {
            JLayeredPane capa = new JLayeredPane();
            capa.setSize(800, 600);
            Aviso aviso = new Aviso("Cierre", true);
            aviso.adjuntar(capa);
            aviso.cerrar();
            assertNull(aviso.getParent());
            aviso.cerrar();
            assertNull(aviso.getParent());
        });
    }

    @Test
    void elBotonDeCerrarQuitaElAviso() {
        Swing.enEdt(() -> {
            JLayeredPane capa = new JLayeredPane();
            capa.setSize(800, 600);
            Aviso aviso = new Aviso("Con botón", false);
            aviso.adjuntar(capa);
            Swing.buscar(aviso, "botonCerrarAviso", JButton.class).doClick();
            assertNull(aviso.getParent());
        });
    }

    @Test
    void siHayVentanaSeMuestraYSePuedeCerrar() {
        exigirEntornoGrafico();
        Swing.enEdt(() -> {
            javax.swing.JFrame marco = new javax.swing.JFrame();
            try {
                marco.setSize(900, 600);
                marco.getContentPane().add(new JPanel());
                Aviso aviso = Aviso.mostrar(marco.getContentPane(), "En ventana", false);
                assertNotNull(aviso);
                assertSame(marco.getLayeredPane(), aviso.getParent());
                aviso.cerrar();
            } finally {
                marco.dispose();
            }
        });
    }

    private static void exigirEntornoGrafico() {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "hace falta un JFrame y un entorno gráfico");
    }
}
