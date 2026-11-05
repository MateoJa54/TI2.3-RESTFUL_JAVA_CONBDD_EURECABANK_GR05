package ec.edu.monster.escritorio.vista;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Aviso no modal de confirmación o de error. Aparece en la esquina inferior
 * derecha de la ventana, se cierra solo a los {@link #DURACION_MS} milisegundos
 * o con su botón de cerrar, y nunca bloquea la interfaz ni tapa los botones
 * de la barra lateral.
 */
public final class Aviso extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Tiempo que el aviso permanece visible, en milisegundos. */
    public static final int DURACION_MS = 6_000;
    private static final int ANCHO_MAXIMO = 360;
    private static final int MARGEN = 16;

    private final boolean error;
    private final Timer temporizador;
    private final transient ComponentAdapter alRedimensionar;
    private JLayeredPane capa;

    /**
     * @param mensaje texto del aviso, tal como lo envió el servidor
     * @param error   {@code true} para un error, {@code false} para una confirmación
     */
    public Aviso(String mensaje, boolean error) {
        super(new BorderLayout(12, 0));
        this.error = error;
        setName("aviso");
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 10));
        getAccessibleContext().setAccessibleName(error ? "Aviso de error" : "Aviso de confirmación");
        JLabel icono = new JLabel(Iconos.de(error ? Iconos.Tipo.ERROR : Iconos.Tipo.EXITO, 20,
                error ? Tema.ERROR : Tema.EXITO));
        icono.setVerticalAlignment(SwingConstants.TOP);
        JLabel texto = new JLabel("<html><body style='width: 240px'>" + escapar(mensaje) + "</body></html>");
        texto.setName("textoAviso");
        texto.setForeground(error ? Tema.ERROR : Tema.EXITO);
        texto.setFont(Tema.fuente(Tema.TAMANO_BASE));
        JButton cerrar = new JButton(Iconos.de(Iconos.Tipo.CERRAR, 14, Tema.TEXTO_SUAVE));
        cerrar.setName("botonCerrarAviso");
        cerrar.setToolTipText("Cerrar aviso");
        cerrar.getAccessibleContext().setAccessibleName("Cerrar aviso");
        cerrar.setContentAreaFilled(false);
        cerrar.setBorderPainted(false);
        cerrar.setFocusPainted(false);
        cerrar.setMargin(new java.awt.Insets(0, 0, 0, 0));
        cerrar.addActionListener(e -> cerrar());
        add(icono, BorderLayout.WEST);
        add(texto, BorderLayout.CENTER);
        add(cerrar, BorderLayout.EAST);
        temporizador = new Timer(DURACION_MS, e -> cerrar());
        temporizador.setRepeats(false);
        alRedimensionar = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ubicar();
            }
        };
    }

    /**
     * Muestra un aviso sobre la ventana que contiene el componente de origen.
     * Si el componente no está en una ventana no hace nada y devuelve {@code null}.
     *
     * @param origen  componente que provoca el aviso
     * @param mensaje texto del aviso
     * @param error   {@code true} para un error
     * @return el aviso mostrado, o {@code null}
     */
    public static Aviso mostrar(Component origen, String mensaje, boolean error) {
        JRootPane raiz = origen == null ? null : SwingUtilities.getRootPane(origen);
        if (raiz == null) {
            return null;
        }
        Aviso aviso = new Aviso(mensaje, error);
        aviso.adjuntar(raiz.getLayeredPane());
        return aviso;
    }

    /**
     * Añade el aviso a una capa, lo coloca abajo a la derecha y arranca el
     * temporizador.
     *
     * @param capa capa donde se dibuja el aviso
     */
    public void adjuntar(JLayeredPane capa) {
        this.capa = capa;
        capa.add(this, JLayeredPane.POPUP_LAYER);
        capa.addComponentListener(alRedimensionar);
        ubicar();
        capa.repaint();
        temporizador.start();
    }

    /** Coloca el aviso en la esquina inferior derecha de su capa. */
    private void ubicar() {
        if (capa == null) {
            return;
        }
        int ancho = Math.min(ANCHO_MAXIMO, Math.max(0, capa.getWidth() - 2 * MARGEN));
        setSize(ancho, 10);
        Dimension preferido = getPreferredSize();
        int alto = preferido.height;
        setBounds(capa.getWidth() - ancho - MARGEN, capa.getHeight() - alto - MARGEN, ancho, alto);
        revalidate();
        repaint();
    }

    /** Quita el aviso de la pantalla y detiene su temporizador. Es seguro llamarlo varias veces. */
    public void cerrar() {
        temporizador.stop();
        if (capa != null) {
            capa.removeComponentListener(alRedimensionar);
            Container padre = getParent();
            if (padre != null) {
                padre.remove(this);
                padre.repaint();
            }
            capa = null;
        }
    }

    /**
     * @return {@code true} si el aviso es de error
     */
    public boolean esError() {
        return error;
    }

    /**
     * @return el tiempo que el aviso sigue visible, en milisegundos
     */
    public int duracionMs() {
        return temporizador.getDelay();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arco = Tema.RADIO_TARJETA * 2;
            RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, arco, arco);
            g2.setColor(error ? Tema.ERROR_FONDO : Tema.EXITO_FONDO);
            g2.fill(forma);
            g2.setColor(Tema.BORDE);
            g2.draw(forma);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private static String escapar(String texto) {
        return (texto == null ? "" : texto)
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
