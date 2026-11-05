package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.modelo.RespuestaAutenticacion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/**
 * Inicio de sesión dividido: a la izquierda la ilustración de Monster y a la
 * derecha una tarjeta con el formulario. La contraseña nunca se muestra ni se
 * conserva: se limpia del campo en cuanto se lee.
 */
public class PanelLogin extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TEXTO_BOTON = "Ingresar";
    private static final String RECURSO_ILUSTRACION = "/imagenes/Monster.png";

    private final ControladorAutenticacion controlador;
    private final JTextField campoUsuario = new JTextField(22);
    private final JPasswordField campoClave = new JPasswordField(22);
    private final JButton botonIngresar = PanelTarjeta.boton("botonIngresar", TEXTO_BOTON);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JToggleButton botonMostrarClave = new JToggleButton(Iconos.de(Iconos.Tipo.MOSTRAR, Tema.TEXTO_SUAVE));
    private final char ecoPorDefecto;
    private Runnable alAutenticar;
    private boolean enCurso;

    /**
     * @param controlador controlador de autenticación, obligatorio
     */
    public PanelLogin(ControladorAutenticacion controlador) {
        super(new GridLayout(1, 2, 24, 0));
        this.controlador = java.util.Objects.requireNonNull(controlador, "El controlador es obligatorio");
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        campoUsuario.setName("campoUsuario");
        campoClave.setName("campoClave");
        ecoPorDefecto = campoClave.getEchoChar();
        botonMostrarClave.setName("botonMostrarClave");
        botonMostrarClave.setToolTipText("Mostrar clave");
        botonMostrarClave.getAccessibleContext().setAccessibleName("Mostrar clave");
        botonMostrarClave.setFocusPainted(false);
        botonMostrarClave.addActionListener(e -> mostrarClave(botonMostrarClave.isSelected()));
        add(new Ilustracion(cargarIlustracion()));
        add(construirLado());
        campoUsuario.addActionListener(e -> campoClave.requestFocusInWindow());
        campoClave.addActionListener(e -> ingresar());
        botonIngresar.addActionListener(e -> ingresar());
    }

    private JComponent construirLado() {
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        JLabel marca = new JLabel("EUREKABANK");
        marca.setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
        marca.setForeground(Tema.ACENTO);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel encabezado = PanelTarjeta.encabezado("Ingreso de empleados",
                "Ingrese con su usuario y su clave de empleado.");
        encabezado.setBorder(BorderFactory.createEmptyBorder(6, 0, 20, 0));
        botonIngresar.setAlignmentX(Component.LEFT_ALIGNMENT);
        botonIngresar.setPreferredSize(new Dimension(botonIngresar.getPreferredSize().width, Tema.ALTO_CAMPO));
        botonIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTO_CAMPO));
        contenido.add(marca);
        contenido.add(encabezado);
        contenido.add(PanelTarjeta.campo(PanelTarjeta.etiqueta("Usuario"), campoUsuario));
        contenido.add(Box.createVerticalStrut(14));
        JPanel filaClave = new JPanel(new BorderLayout(8, 0));
        filaClave.setOpaque(false);
        filaClave.add(campoClave, BorderLayout.CENTER);
        filaClave.add(botonMostrarClave, BorderLayout.EAST);
        contenido.add(PanelTarjeta.campo(PanelTarjeta.etiqueta("Clave"), filaClave));
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(botonIngresar);
        contenido.add(etiquetaEstado);
        PanelTarjeta tarjeta = new PanelTarjeta(contenido);
        tarjeta.setPreferredSize(new Dimension(420, 380));
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new java.awt.Insets(0, 24, 0, 24);
        centro.add(tarjeta, c);
        return centro;
    }

    /**
     * Muestra u oculta la clave escrita. Solo cambia cómo se dibuja: el
     * contenido sigue igual y no se envía nada al servicio.
     *
     * @param mostrar {@code true} para ver la clave en texto claro
     */
    public void mostrarClave(boolean mostrar) {
        campoClave.setEchoChar(mostrar ? (char) 0 : ecoPorDefecto);
        botonMostrarClave.setSelected(mostrar);
        botonMostrarClave.setToolTipText(mostrar ? "Ocultar clave" : "Mostrar clave");
        botonMostrarClave.getAccessibleContext().setAccessibleName(mostrar ? "Ocultar clave" : "Mostrar clave");
    }

    private static BufferedImage cargarIlustracion() {
        try (InputStream entrada = PanelLogin.class.getResourceAsStream(RECURSO_ILUSTRACION)) {
            return entrada == null ? null : ImageIO.read(entrada);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @param accion lo que se hace cuando el servidor acepta las credenciales
     */
    public void setAlAutenticar(Runnable accion) {
        this.alAutenticar = accion;
    }

    /**
     * Valida los campos y autentica contra el servicio. Sin usuario o sin
     * clave no llama al servidor. Mientras la llamada está en curso el botón
     * queda deshabilitado, así un doble clic envía una sola petición.
     */
    public void ingresar() {
        if (enCurso) {
            return;
        }
        String usuario = campoUsuario.getText().trim();
        char[] clave = campoClave.getPassword();
        if (usuario.isEmpty() || clave.length == 0) {
            Arrays.fill(clave, '\0');
            mostrarEstado("Escriba el usuario y la clave.", true);
            return;
        }
        String claveEscrita = new String(clave);
        Arrays.fill(clave, '\0');
        campoClave.setText("");
        enCurso = true;
        mostrarEstado("", false);
        PanelTarjeta.ocupado(botonIngresar, TEXTO_BOTON, true);
        TareaAsincrona.lanzar(() -> controlador.autenticar(usuario, claveEscrita),
                this::alResponder, this::alFallar);
    }

    private void alResponder(RespuestaAutenticacion respuesta) {
        terminar();
        if (PanelTarjeta.esExito(respuesta) && controlador.haySesionActiva()) {
            mostrarEstado("", false);
            if (alAutenticar != null) {
                alAutenticar.run();
            }
        } else {
            mostrarEstado(PanelTarjeta.mensajeDe(respuesta), true);
        }
    }

    private void alFallar(Throwable fallo) {
        terminar();
        mostrarEstado(PanelTarjeta.mensajeDe(fallo), true);
    }

    private void terminar() {
        enCurso = false;
        PanelTarjeta.ocupado(botonIngresar, TEXTO_BOTON, false);
    }

    /**
     * Muestra un mensaje en línea bajo el botón.
     *
     * @param mensaje texto a mostrar; vacío lo limpia
     * @param error   {@code true} para el color de error, {@code false} para el de éxito
     */
    public void mostrarEstado(String mensaje, boolean error) {
        PanelTarjeta.mostrar(etiquetaEstado, mensaje, error);
    }

    /** Ilustración a sangre con esquinas de 12 px, recortada para cubrir su área. */
    private static final class Ilustracion extends JPanel {

        private static final long serialVersionUID = 1L;
        private final transient BufferedImage imagen;
        private transient BufferedImage escalada;
        private int anchoEscalada = -1;
        private int altoEscalada = -1;

        Ilustracion(BufferedImage imagen) {
            this.imagen = imagen;
            setOpaque(false);
            setPreferredSize(new Dimension(360, 520));
            getAccessibleContext().setAccessibleName("Ilustración de Monster");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int ancho = getWidth();
                int alto = getHeight();
                g2.setClip(new RoundRectangle2D.Float(0, 0, ancho, alto,
                        Tema.RADIO_TARJETA * 2, Tema.RADIO_TARJETA * 2));
                g2.setColor(Tema.ACENTO);
                g2.fillRect(0, 0, ancho, alto);
                if (imagen != null && ancho > 0 && alto > 0) {
                    double escala = Math.max((double) ancho / imagen.getWidth(), (double) alto / imagen.getHeight());
                    int w = (int) Math.ceil(imagen.getWidth() * escala);
                    int h = (int) Math.ceil(imagen.getHeight() * escala);
                    g2.drawImage(escalada(w, h), (ancho - w) / 2, (alto - h) / 2, null);
                }
            } finally {
                g2.dispose();
            }
        }

        /** Reduce o amplía la ilustración por pasos (la mitad cada vez) y guarda el resultado mientras el tamaño no cambie. */
        private BufferedImage escalada(int w, int h) {
            if (escalada != null && anchoEscalada == w && altoEscalada == h) {
                return escalada;
            }
            BufferedImage actual = imagen;
            int aw = imagen.getWidth();
            int ah = imagen.getHeight();
            while (aw / 2 >= w && ah / 2 >= h) {
                aw /= 2;
                ah /= 2;
                actual = pasar(actual, aw, ah, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            escalada = pasar(actual, w, h, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            anchoEscalada = w;
            altoEscalada = h;
            return escalada;
        }

        private static BufferedImage pasar(BufferedImage origen, int w, int h, Object interpolacion) {
            BufferedImage destino = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = destino.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacion);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(origen, 0, 0, w, h, null);
            } finally {
                g.dispose();
            }
            return destino;
        }
    }
}
