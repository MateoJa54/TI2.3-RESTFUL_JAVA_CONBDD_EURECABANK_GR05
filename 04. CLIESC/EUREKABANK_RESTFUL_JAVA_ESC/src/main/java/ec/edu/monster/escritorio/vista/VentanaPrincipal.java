package ec.edu.monster.escritorio.vista;

import com.formdev.flatlaf.FlatClientProperties;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Ventana de trabajo: barra superior de una línea (marca, nombre y rol del
 * empleado, botón "Cerrar sesión") y las pestañas Cuentas, Movimientos,
 * Operaciones y Costos.
 */
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Alto máximo de la barra superior, en píxeles. */
    public static final int ALTO_BARRA = 56;

    private static final String TEXTO_CERRAR = "Cerrar sesión";

    private final SesionUsuario sesion;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final Runnable alCerrarSesion;
    private final JButton botonCerrarSesion = new JButton(TEXTO_CERRAR);
    private boolean cerrando;

    /**
     * @param sesion         sesión del empleado, para mostrar su nombre y su rol
     * @param alCerrarSesion lo que se hace al pulsar "Cerrar sesión" (cierra en el servidor y vuelve al inicio)
     * @param cuentas        pestaña de cuentas
     * @param movimientos    pestaña de movimientos
     * @param operacion      pestaña de operaciones
     * @param costos         pestaña de costos
     */
    public VentanaPrincipal(SesionUsuario sesion, Runnable alCerrarSesion, PanelCuentas cuentas,
            PanelMovimientos movimientos, PanelOperacion operacion, PanelCostos costos) {
        super("EUREKABANK");
        this.sesion = sesion;
        this.alCerrarSesion = alCerrarSesion;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout());
        add(construirBarra(), BorderLayout.NORTH);
        pestanas.addTab("Cuentas", cuentas);
        pestanas.addTab("Movimientos", movimientos);
        pestanas.addTab("Operaciones", operacion);
        pestanas.addTab("Costos", costos);
        pestanas.setName("pestanas");
        pestanas.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        add(pestanas, BorderLayout.CENTER);
        setMinimumSize(new Dimension(900, 560));
        setSize(1080, 680);
        setLocationRelativeTo(null);
    }

    private JPanel construirBarra() {
        JPanel barra = new JPanel(new BorderLayout(16, 0));
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(0, 20, 0, 20)));
        barra.setPreferredSize(new Dimension(10, ALTO_BARRA));
        barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_BARRA));
        JLabel marca = new JLabel("EUREKABANK");
        marca.setFont(Tema.fuenteNegrita(15f));
        marca.setForeground(Tema.ACENTO);
        JLabel empleado = new JLabel(descripcionDelEmpleado());
        empleado.setName("etiquetaEmpleado");
        empleado.setForeground(Tema.TEXTO_SUAVE);
        empleado.setHorizontalAlignment(JLabel.RIGHT);
        botonCerrarSesion.setName("botonCerrarSesion");
        botonCerrarSesion.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; foreground: #12202B; borderWidth: 1; borderColor: #DDE5EA;"
                + "hoverBackground: #F4F7F9; pressedBackground: #E9EEF1; focusedBackground: #FFFFFF");
        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        JPanel derecha = new JPanel(new BorderLayout(16, 0));
        derecha.setOpaque(false);
        derecha.add(empleado, BorderLayout.CENTER);
        derecha.add(botonCerrarSesion, BorderLayout.EAST);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(derecha, BorderLayout.CENTER);
        barra.add(marca, BorderLayout.WEST);
        barra.add(contenedor, BorderLayout.CENTER);
        return barra;
    }

    private String descripcionDelEmpleado() {
        String nombre = Formato.texto(sesion == null ? null : sesion.getNombreEmpleado());
        String rol = Formato.texto(sesion == null ? null : sesion.getRol());
        if (nombre.isEmpty()) {
            return rol;
        }
        return rol.isEmpty() ? nombre : nombre + " (" + rol + ")";
    }

    /**
     * Cierra la sesión: deshabilita el botón con el texto "Procesando" y
     * delega en la acción recibida. Si ya hay un cierre en curso no hace nada,
     * así pulsar dos veces cierra una sola vez.
     */
    public void cerrarSesion() {
        if (cerrando) {
            return;
        }
        cerrando = true;
        PanelTarjeta.ocupado(botonCerrarSesion, TEXTO_CERRAR, true);
        if (alCerrarSesion != null) {
            alCerrarSesion.run();
        }
    }

    /**
     * @return las pestañas de la ventana
     */
    public JTabbedPane getPestanas() {
        return pestanas;
    }
}
