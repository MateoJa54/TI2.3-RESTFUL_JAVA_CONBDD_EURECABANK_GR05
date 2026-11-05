package ec.edu.monster.escritorio;

import ec.edu.monster.escritorio.cliente.ClienteEUREKABANK;
import ec.edu.monster.escritorio.cliente.IClienteEUREKABANK;
import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCostos;
import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelCostos;
import ec.edu.monster.escritorio.vista.PanelCuentas;
import ec.edu.monster.escritorio.vista.PanelLogin;
import ec.edu.monster.escritorio.vista.PanelMovimientos;
import ec.edu.monster.escritorio.vista.PanelOperacion;
import ec.edu.monster.escritorio.vista.TareaAsincrona;
import ec.edu.monster.escritorio.vista.Tema;
import ec.edu.monster.escritorio.vista.VentanaPrincipal;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada del cliente de escritorio de EURECABANK. Es el único lugar
 * que conoce las clases concretas: arma el cliente, los controladores y las
 * vistas, y las entrega a quien las necesita.
 *
 * <p>La dirección del servicio se toma, en este orden, del primer argumento
 * del programa, de la propiedad {@code -Dservicio.url}, de la variable de
 * entorno {@code SERVICIO_URL} o, si no hay ninguna, del servidor local.
 *
 * <p>La aplicación arranca aunque el servidor esté apagado: el fallo aparece,
 * controlado, en la primera llamada.
 */
public final class Main {

    private final String url;
    private final IClienteEUREKABANK cliente;
    private final SesionUsuario sesion = new SesionUsuario();
    private final ControladorAutenticacion autenticacion;
    private JFrame ventana;
    private String avisoPendiente;

    /**
     * @param url     dirección del servicio, solo para mostrarla en los avisos
     * @param cliente cliente del servicio, obligatorio
     */
    public Main(String url, IClienteEUREKABANK cliente) {
        this.url = url;
        this.cliente = Objects.requireNonNull(cliente, "El cliente del servicio es obligatorio");
        this.autenticacion = new ControladorAutenticacion(cliente, sesion);
    }

    /**
     * @param argumentos dirección del servicio, opcional
     */
    public static void main(String[] argumentos) {
        String url = resolverUrl(argumentos);
        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            try {
                new Main(url, new ClienteEUREKABANK(url)).abrirLogin();
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage() + "\nEjemplo: " + ClienteEUREKABANK.URL_POR_DEFECTO,
                        "EUREKABANK", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * @param argumentos argumentos del programa
     * @return la dirección del servicio resuelta
     */
    public static String resolverUrl(String[] argumentos) {
        if (argumentos != null && argumentos.length > 0 && argumentos[0] != null && !argumentos[0].isBlank()) {
            return argumentos[0].trim();
        }
        String url = System.getProperty("servicio.url");
        if (url == null || url.isBlank()) {
            url = System.getenv("SERVICIO_URL");
        }
        return (url == null || url.isBlank()) ? ClienteEUREKABANK.URL_POR_DEFECTO : url.trim();
    }

    /**
     * Muestra la pantalla de inicio de sesión. Si quedó un aviso pendiente
     * (por ejemplo, una sesión que expiró) lo muestra bajo el botón.
     */
    public void abrirLogin() {
        sesion.limpiar();
        cerrarVentana();
        PanelLogin panel = new PanelLogin(autenticacion);
        panel.setAlAutenticar(this::abrirPrincipal);
        if (avisoPendiente != null) {
            panel.mostrarEstado(avisoPendiente, true);
            avisoPendiente = null;
        }
        JFrame marco = new JFrame("EUREKABANK");
        marco.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        marco.setContentPane(panel);
        marco.setMinimumSize(Tema.VENTANA_MINIMA);
        marco.setSize(Tema.VENTANA_INICIAL);
        marco.setLocationRelativeTo(null);
        marco.setVisible(true);
        ventana = marco;
    }

    /**
     * Muestra la ventana de trabajo con la sesión ya iniciada.
     */
    public void abrirPrincipal() {
        cerrarVentana();
        ControladorCuentas cuentas = new ControladorCuentas(cliente, sesion);
        ControladorMovimientos movimientos = new ControladorMovimientos(cliente, sesion);
        ControladorCostos costos = new ControladorCostos(cliente, sesion);
        VentanaPrincipal principal = new VentanaPrincipal(sesion, this::cerrarSesionEnServidor,
                new PanelCuentas(cuentas, this::sesionCaducada),
                new PanelMovimientos(movimientos, this::sesionCaducada),
                new PanelOperacion(movimientos, this::sesionCaducada),
                new PanelCostos(costos, this::sesionCaducada));
        principal.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                principal.setVisible(false);
                TareaAsincrona.lanzar(autenticacion::cerrarSesion, r -> cerrarVentana(), f -> cerrarVentana());
            }
        });
        principal.setVisible(true);
        ventana = principal;
    }

    /** Cierra la sesión en el servidor sin bloquear la interfaz y vuelve al inicio. */
    private void cerrarSesionEnServidor() {
        TareaAsincrona.lanzar(autenticacion::cerrarSesion, r -> abrirLogin(), f -> abrirLogin());
    }

    /** El servidor dijo que la sesión ya no sirve: vuelve al inicio con su mensaje. */
    private void sesionCaducada(String mensaje) {
        avisoPendiente = mensaje;
        abrirLogin();
    }

    private void cerrarVentana() {
        if (ventana != null) {
            ventana.dispose();
            ventana = null;
        }
    }

    /**
     * @return la dirección del servicio con la que se armó la aplicación
     */
    public String getUrl() {
        return url;
    }
}
