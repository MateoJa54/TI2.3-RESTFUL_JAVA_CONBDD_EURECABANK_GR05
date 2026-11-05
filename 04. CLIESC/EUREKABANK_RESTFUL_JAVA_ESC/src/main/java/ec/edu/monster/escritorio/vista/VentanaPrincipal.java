package ec.edu.monster.escritorio.vista;

import com.formdev.flatlaf.FlatClientProperties;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Ventana de trabajo: barra lateral de 220 px con la marca, los cuatro destinos
 * (Cuentas, Movimientos, Operaciones y Costos) y la tarjeta del empleado con
 * su botón de cerrar sesión. A la derecha están las páginas. Las pestañas del
 * {@link JTabbedPane} no se ven: la barra lateral las controla.
 */
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String TEXTO_CERRAR = "Cerrar sesión";
    private static final String[] TITULOS = {"Cuentas", "Movimientos", "Operaciones", "Costos"};
    private static final Iconos.Tipo[] ICONOS = {
        Iconos.Tipo.CUENTAS, Iconos.Tipo.MOVIMIENTOS, Iconos.Tipo.OPERACIONES, Iconos.Tipo.COSTOS};
    private static final int ALTO_DESTINO = 44;

    private final SesionUsuario sesion;
    private final JTabbedPane pestanas = pestanasSinBarra();
    private final Runnable alCerrarSesion;
    private final JButton botonCerrarSesion = new JButton(TEXTO_CERRAR);
    private final JLabel etiquetaEmpleado = new JLabel("");
    private final JLabel etiquetaRol = new JLabel("");
    private final Avatar avatar = new Avatar();
    private final Destino[] destinos = new Destino[TITULOS.length];
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
        add(construirBarraLateral(), BorderLayout.WEST);
        pestanas.addTab(TITULOS[0], cuentas);
        pestanas.addTab(TITULOS[1], movimientos);
        pestanas.addTab(TITULOS[2], operacion);
        pestanas.addTab(TITULOS[3], costos);
        pestanas.setName("pestanas");
        pestanas.setBackground(Tema.FONDO);
        pestanas.addChangeListener(e -> sincronizarDestinos());
        cuentas.setAlNavegar((accion, codigo) -> navegar(accion, codigo, movimientos, operacion, costos));
        add(pestanas, BorderLayout.CENTER);
        setMinimumSize(Tema.VENTANA_MINIMA);
        setSize(Tema.VENTANA_INICIAL);
        setLocationRelativeTo(null);
        sincronizarDestinos();
    }

    /** Lleva a la pestaña indicada por una tarjeta de cuenta y rellena el código de la cuenta. */
    private void navegar(TarjetaCuenta.Accion accion, String codigo, PanelMovimientos movimientos,
            PanelOperacion operacion, PanelCostos costos) {
        switch (accion) {
            case MOVIMIENTOS -> {
                movimientos.cargarCuenta(codigo);
                pestanas.setSelectedComponent(movimientos);
            }
            case COSTOS -> {
                costos.cargarCuenta(codigo);
                pestanas.setSelectedComponent(costos);
            }
            case OPERAR -> {
                operacion.cargarCuenta(codigo);
                pestanas.setSelectedComponent(operacion);
            }
            default -> throw new IllegalStateException("Acción sin destino: " + accion);
        }
    }

    private JPanel construirBarraLateral() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setName("barraLateral");
        barra.setBackground(Tema.SUPERFICIE);
        barra.setPreferredSize(new Dimension(Tema.ANCHO_BARRA, 0));
        barra.setMinimumSize(new Dimension(Tema.ANCHO_BARRA, 0));
        barra.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE));
        barra.add(construirMarca(), BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        for (int i = 0; i < TITULOS.length; i++) {
            final int indice = i;
            destinos[i] = new Destino(TITULOS[i], ICONOS[i], "nav" + TITULOS[i]);
            destinos[i].addActionListener(e -> pestanas.setSelectedIndex(indice));
            lista.add(destinos[i]);
        }
        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(lista, BorderLayout.NORTH);
        barra.add(norte, BorderLayout.CENTER);
        barra.add(construirTarjetaEmpleado(), BorderLayout.SOUTH);
        return barra;
    }

    private static JPanel construirMarca() {
        JLabel marca = new JLabel("EUREKABANK");
        marca.setName("marca");
        marca.setFont(Tema.fuenteNegrita(15f));
        marca.setForeground(Tema.ACENTO);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));
        panel.add(marca, BorderLayout.WEST);
        return panel;
    }

    private JPanel construirTarjetaEmpleado() {
        etiquetaEmpleado.setName("etiquetaEmpleado");
        etiquetaEmpleado.setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
        etiquetaEmpleado.setForeground(Tema.TEXTO);
        etiquetaRol.setName("etiquetaRol");
        etiquetaRol.setFont(Tema.fuente(12f));
        etiquetaRol.setForeground(Tema.TEXTO_SUAVE);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(etiquetaEmpleado);
        textos.add(etiquetaRol);
        JPanel cabecera = new JPanel(new BorderLayout(12, 0));
        cabecera.setOpaque(false);
        cabecera.add(avatar, BorderLayout.WEST);
        cabecera.add(textos, BorderLayout.CENTER);

        botonCerrarSesion.setName("botonCerrarSesion");
        botonCerrarSesion.setIcon(Iconos.de(Iconos.Tipo.SALIR, 18, Tema.TEXTO));
        botonCerrarSesion.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        botonCerrarSesion.setMargin(new Insets(8, 12, 8, 12));
        botonCerrarSesion.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; foreground: #12202B; borderWidth: 1; borderColor: #DDE5EA;"
                + "hoverBackground: #F4F7F9; pressedBackground: #E9EEF1; focusedBackground: #FFFFFF");
        botonCerrarSesion.addActionListener(e -> cerrarSesion());

        JPanel tarjeta = new JPanel(new BorderLayout(0, 14));
        tarjeta.setBackground(Tema.SUPERFICIE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(16, 20, 20, 20)));
        tarjeta.add(cabecera, BorderLayout.NORTH);
        tarjeta.add(botonCerrarSesion, BorderLayout.SOUTH);
        Dimension tope = new Dimension(Tema.ANCHO_BARRA, tarjeta.getPreferredSize().height);
        tarjeta.setPreferredSize(tope);
        textoDelEmpleado();
        return tarjeta;
    }

    /** Pone el nombre, el rol y las iniciales del empleado en la tarjeta. */
    private void textoDelEmpleado() {
        String nombre = Formato.texto(sesion == null ? null : sesion.getNombreEmpleado());
        String rol = Formato.texto(sesion == null ? null : sesion.getRol());
        etiquetaEmpleado.setText(nombre.isEmpty() ? rol : nombre);
        etiquetaRol.setText(nombre.isEmpty() ? "" : rol);
        avatar.setIniciales(iniciales(nombre.isEmpty() ? rol : nombre));
    }

    /**
     * Iniciales de un nombre: la primera letra de las dos primeras palabras.
     *
     * @param nombre nombre completo
     * @return las iniciales en mayúsculas, o {@code "?"} si no hay letras
     */
    public static String iniciales(String nombre) {
        String[] palabras = Formato.texto(nombre).trim().split("\\s+");
        StringBuilder letras = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty() && letras.length() < 2) {
                letras.append(Character.toUpperCase(palabra.charAt(0)));
            }
        }
        return letras.length() == 0 ? "?" : letras.toString();
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

    private void sincronizarDestinos() {
        int seleccionada = pestanas.getSelectedIndex();
        for (int i = 0; i < destinos.length; i++) {
            destinos[i].marcar(i == seleccionada);
        }
    }

    /**
     * @return las pestañas de la ventana
     */
    public JTabbedPane getPestanas() {
        return pestanas;
    }

    /** Pestañas sin barra visible: la navegación la hace la barra lateral. */
    private static JTabbedPane pestanasSinBarra() {
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int horizRun, int maxTabHeight) {
                return 0;
            }

            @Override
            protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                // Sin barra de pestañas.
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Sin marco alrededor del contenido.
            }
        });
        return pestanas;
    }

    /** Elemento de la barra lateral: icono, texto y, si está activo, fondo tenue con barra de 3 px. */
    private static final class Destino extends JToggleButton {

        private static final long serialVersionUID = 1L;
        private static final int BARRA = 3;
        private final Iconos.Tipo tipo;

        Destino(String texto, Iconos.Tipo tipo, String nombre) {
            super(texto);
            this.tipo = tipo;
            setName(nombre);
            setHorizontalAlignment(LEFT);
            setIconTextGap(12);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Tema.fuente(Tema.TAMANO_BASE));
            setMargin(new Insets(0, 22, 0, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_DESTINO));
            setPreferredSize(new Dimension(Tema.ANCHO_BARRA, ALTO_DESTINO));
            getAccessibleContext().setAccessibleName(texto);
            marcar(false);
        }

        /** Pone el destino como activo o inactivo: color del texto y del icono. */
        void marcar(boolean activo) {
            setSelected(activo);
            Color color = activo ? Tema.ACENTO : Tema.TEXTO_SUAVE;
            setForeground(Tema.TEXTO);
            setFont(activo ? Tema.fuenteNegrita(Tema.TAMANO_BASE) : Tema.fuente(Tema.TAMANO_BASE));
            setIcon(Iconos.de(tipo, 20, color));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected()) {
                    g2.setColor(Tema.TINTE);
                    g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
                    g2.setColor(Tema.ACENTO);
                    g2.fill(new Rectangle2D.Double(0, 0, BARRA, getHeight()));
                } else if (getModel().isRollover()) {
                    g2.setColor(Tema.FONDO);
                    g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Círculo teal con las iniciales del empleado. */
    private static final class Avatar extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final int LADO = 36;
        private String iniciales = "?";

        Avatar() {
            setPreferredSize(new Dimension(LADO, LADO));
            setMinimumSize(new Dimension(LADO, LADO));
            setMaximumSize(new Dimension(LADO, LADO));
        }

        void setIniciales(String texto) {
            this.iniciales = texto == null || texto.isEmpty() ? "?" : texto;
            repaint();
        }

        String getIniciales() {
            return iniciales;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.ACENTO);
                g2.fill(new Ellipse2D.Double(0, 0, LADO, LADO));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Tema.fuenteNegrita(13f).getFamily(), Font.BOLD, 13));
                java.awt.FontMetrics metricas = g2.getFontMetrics();
                int x = (LADO - metricas.stringWidth(iniciales)) / 2;
                int y = (LADO - metricas.getHeight()) / 2 + metricas.getAscent();
                g2.drawString(iniciales, x, y);
            } finally {
                g2.dispose();
            }
        }
    }
}
