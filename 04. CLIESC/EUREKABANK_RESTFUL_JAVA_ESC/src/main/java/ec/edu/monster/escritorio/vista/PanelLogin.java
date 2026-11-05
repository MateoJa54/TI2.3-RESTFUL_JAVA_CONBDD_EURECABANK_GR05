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
        JLabel titulo = new JLabel("Ingreso de empleados");
        titulo.setFont(Tema.fuenteNegrita(22f));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        titulo.setBorder(BorderFactory.createEmptyBorder(6, 0, 20, 0));
        botonIngresar.setAlignmentX(Component.LEFT_ALIGNMENT);
        botonIngresar.setMaximumSize(new Dimension(Integer.MAX_VALUE, botonIngresar.getPreferredSize().height + 6));
        contenido.add(marca);
        contenido.add(titulo);
        contenido.add(PanelTarjeta.campo(PanelTarjeta.etiqueta("Usuario"), campoUsuario));
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(PanelTarjeta.campo(PanelTarjeta.etiqueta("Clave"), campoClave));
        contenido.add(Box.createVerticalStrut(20));
        contenido.add(botonIngresar);
        contenido.add(etiquetaEstado);
        PanelTarjeta tarjeta = new PanelTarjeta(contenido);
        tarjeta.setPreferredSize(new Dimension(380, 380));
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new java.awt.Insets(0, 24, 0, 24);
        centro.add(tarjeta, c);
        return centro;
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
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int ancho = getWidth();
                int alto = getHeight();
                g2.setClip(new RoundRectangle2D.Float(0, 0, ancho, alto,
                        Tema.RADIO_TARJETA * 2, Tema.RADIO_TARJETA * 2));
                g2.setColor(Tema.ACENTO);
                g2.fillRect(0, 0, ancho, alto);
                if (imagen != null) {
                    double escala = Math.max((double) ancho / imagen.getWidth(), (double) alto / imagen.getHeight());
                    int w = (int) Math.ceil(imagen.getWidth() * escala);
                    int h = (int) Math.ceil(imagen.getHeight() * escala);
                    Image ajustada = imagen;
                    g2.drawImage(ajustada, (ancho - w) / 2, (alto - h) / 2, w, h, null);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
