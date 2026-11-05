package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCostos;
import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelCostos;
import ec.edu.monster.escritorio.vista.PanelCuentas;
import ec.edu.monster.escritorio.vista.PanelMovimientos;
import ec.edu.monster.escritorio.vista.PanelOperacion;
import ec.edu.monster.escritorio.vista.Tema;
import ec.edu.monster.escritorio.vista.VentanaPrincipal;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La ventana de trabajo necesita entorno gráfico: en un equipo sin pantalla se omite. */
class VentanaPrincipalTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private VentanaPrincipal ventana;
    private AtomicInteger cierres;

    @BeforeAll
    static void tema() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Swing.enEdt(Tema::instalar);
    }

    @BeforeEach
    void preparar() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        new ControladorAutenticacion(servidor, sesion).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        cierres = new AtomicInteger();
        Swing.enEdt(() -> ventana = new VentanaPrincipal(sesion, cierres::incrementAndGet,
                new PanelCuentas(new ControladorCuentas(servidor, sesion), null),
                new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null),
                new PanelOperacion(new ControladorMovimientos(servidor, sesion), null),
                new PanelCostos(new ControladorCostos(servidor, sesion), null)));
    }

    @AfterEach
    void cerrar() {
        if (ventana != null) {
            Swing.enEdt(ventana::dispose);
        }
    }

    @Test
    void tienePestanasCuentasMovimientosOperacionesYCostos() {
        var pestanas = ventana.getPestanas();
        assertEquals(4, pestanas.getTabCount());
        assertEquals("Cuentas", pestanas.getTitleAt(0));
        assertEquals("Movimientos", pestanas.getTitleAt(1));
        assertEquals("Operaciones", pestanas.getTitleAt(2));
        assertEquals("Costos", pestanas.getTitleAt(3));
    }

    @Test
    void cadaPestanaContieneSuPanel() {
        var pestanas = ventana.getPestanas();
        assertInstanceOf(PanelCuentas.class, pestanas.getComponentAt(0));
        assertInstanceOf(PanelMovimientos.class, pestanas.getComponentAt(1));
        assertInstanceOf(PanelOperacion.class, pestanas.getComponentAt(2));
        assertInstanceOf(PanelCostos.class, pestanas.getComponentAt(3));
    }

    @Test
    void laBarraMuestraElNombreYElRolDelEmpleado() {
        JLabel empleado = Swing.buscar(ventana.getRootPane(), "etiquetaEmpleado", JLabel.class);
        assertEquals("Carlos Romero (Consultas)", empleado.getText());
    }

    @Test
    void laBarraSuperiorMideComoMaximo64() {
        assertTrue(VentanaPrincipal.ALTO_BARRA <= 64);
        JLabel empleado = Swing.buscar(ventana.getRootPane(), "etiquetaEmpleado", JLabel.class);
        var barra = empleado.getParent().getParent().getParent();
        assertTrue(barra.getPreferredSize().height <= 64);
    }

    @Test
    void elBotonDiceCerrarSesion() {
        JButton boton = Swing.buscar(ventana.getRootPane(), "botonCerrarSesion", JButton.class);
        assertEquals("Cerrar sesión", boton.getText());
        assertTrue(boton.isEnabled());
    }

    @Test
    void cerrarSesionAvisaUnaVezYDeshabilitaElBoton() {
        JButton boton = Swing.buscar(ventana.getRootPane(), "botonCerrarSesion", JButton.class);
        Swing.enEdt(ventana::cerrarSesion);
        assertEquals(1, cierres.get());
        assertFalse(boton.isEnabled());
        assertEquals("Procesando", boton.getText());
    }

    @Test
    void cerrarSesionRepetidoCierraUnaSolaVez() {
        Swing.enEdt(() -> {
            ventana.cerrarSesion();
            ventana.cerrarSesion();
            ventana.cerrarSesion();
        });
        assertEquals(1, cierres.get());
    }

    @Test
    void pulsarElBotonDosVecesCierraUnaSolaVez() {
        JButton boton = Swing.buscar(ventana.getRootPane(), "botonCerrarSesion", JButton.class);
        Swing.enEdt(() -> {
            boton.doClick();
            boton.doClick();
        });
        assertEquals(1, cierres.get());
    }

    @Test
    void sinAccionDeCierreNoFalla() {
        Swing.enEdt(() -> {
            VentanaPrincipal otra = new VentanaPrincipal(sesion, null,
                    new PanelCuentas(new ControladorCuentas(servidor, sesion), null),
                    new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null),
                    new PanelOperacion(new ControladorMovimientos(servidor, sesion), null),
                    new PanelCostos(new ControladorCostos(servidor, sesion), null));
            otra.cerrarSesion();
            otra.dispose();
        });
    }

    @Test
    void unaSesionSinNombreMuestraSoloElRol() {
        SesionUsuario sola = new SesionUsuario(null, null, null, null, "Cajero");
        Swing.enEdt(() -> {
            VentanaPrincipal otra = new VentanaPrincipal(sola, null,
                    new PanelCuentas(new ControladorCuentas(servidor, sola), null),
                    new PanelMovimientos(new ControladorMovimientos(servidor, sola), null),
                    new PanelOperacion(new ControladorMovimientos(servidor, sola), null),
                    new PanelCostos(new ControladorCostos(servidor, sola), null));
            JLabel etiqueta = Swing.buscar(otra.getRootPane(), "etiquetaEmpleado", JLabel.class);
            assertEquals("Cajero", etiqueta.getText());
            otra.dispose();
        });
    }

    @Test
    void elTituloDeLaVentanaEsLaMarca() {
        assertEquals("EUREKABANK", ventana.getTitle());
        assertNotNull(ventana.getRootPane());
    }

    @Test
    void laVentanaNoSeCierraSolaAlPulsarLaX() {
        assertEquals(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE, ventana.getDefaultCloseOperation());
    }
}
