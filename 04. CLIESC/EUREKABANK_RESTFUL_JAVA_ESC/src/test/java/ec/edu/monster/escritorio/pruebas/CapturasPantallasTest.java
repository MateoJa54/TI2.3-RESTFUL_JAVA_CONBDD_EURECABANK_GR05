package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCostos;
import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaCostos;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelCostos;
import ec.edu.monster.escritorio.vista.PanelCuentas;
import ec.edu.monster.escritorio.vista.PanelLogin;
import ec.edu.monster.escritorio.vista.PanelMovimientos;
import ec.edu.monster.escritorio.vista.PanelOperacion;
import ec.edu.monster.escritorio.vista.Tema;
import ec.edu.monster.escritorio.vista.VentanaPrincipal;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Evidencia visual del rediseño: construye cada pantalla con datos falsos, la
 * dimensiona a 1100x720, la pinta en una imagen y guarda un PNG en
 * la carpeta ui_esc dentro de TEMP (fuera del repositorio). No abre ventanas, salvo la
 * ventana principal, que se pinta solo si hay entorno gráfico.
 */
class CapturasPantallasTest {

    private static final Dimension TAMANO = new Dimension(1100, 720);
    private static final Path CARPETA = carpetaDeSalida();

    @BeforeAll
    static void tema() throws IOException {
        Swing.enEdt(Tema::instalar);
        Files.createDirectories(CARPETA);
    }

    private static Path carpetaDeSalida() {
        String temp = System.getenv("TEMP");
        if (temp == null || temp.isBlank()) {
            temp = System.getProperty("java.io.tmpdir");
        }
        return Paths.get(temp, "ui_esc");
    }

    /** Distribuye el árbol completo: cada contenedor reparte el espacio de sus hijos. */
    private static void distribuir(Component componente) {
        if (componente instanceof java.awt.Container contenedor) {
            contenedor.doLayout();
            for (Component hijo : contenedor.getComponents()) {
                distribuir(hijo);
            }
        }
    }

    /** Dimensiona el componente, lo distribuye y lo pinta en una imagen; guarda el PNG. */
    private static void guardar(Component pantalla, String nombre) throws IOException {
        BufferedImage imagen = new BufferedImage(TAMANO.width, TAMANO.height, BufferedImage.TYPE_INT_ARGB);
        Swing.enEdt(() -> {
            pantalla.setSize(TAMANO);
            distribuir(pantalla);
            Graphics2D g = imagen.createGraphics();
            try {
                g.setColor(Tema.FONDO);
                g.fillRect(0, 0, TAMANO.width, TAMANO.height);
                pantalla.paint(g);
            } finally {
                g.dispose();
            }
        });
        File archivo = CARPETA.resolve(nombre).toFile();
        assertTrue(ImageIO.write(imagen, "png", archivo), "no se pudo escribir " + archivo);
        assertTrue(archivo.length() > 0, "PNG vacío: " + archivo);
    }

    private static SesionUsuario sesionIniciada(ClienteFalso servidor) {
        SesionUsuario sesion = new SesionUsuario();
        new ControladorAutenticacion(servidor, sesion).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
        return sesion;
    }

    private static CuentaDTO cuenta(String codigo, String moneda, String saldo, String estado, int operaciones) {
        CuentaDTO cuenta = new CuentaDTO();
        cuenta.setCodigo(codigo);
        cuenta.setDescripcionMoneda(moneda);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.setEstado(estado);
        cuenta.setCodigoSucursal("001");
        cuenta.setFechaCreacion("2020-01-01");
        cuenta.setOperacionesRealizadas(operaciones);
        return cuenta;
    }

    private static MovimientoDTO movimiento(int numero, String fecha, String tipo, String descripcion,
            String importe, String referencia) {
        MovimientoDTO m = new MovimientoDTO();
        m.setNumero(numero);
        m.setFecha(fecha);
        m.setTipoCodigo("00" + numero);
        m.setTipoDescripcion(descripcion);
        m.setTipoAccion(tipo);
        m.setImporte(new BigDecimal(importe));
        m.setReferencia(referencia);
        m.setEmpleado("cromero");
        return m;
    }

    @Test
    void capturaElLogin() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = new SesionUsuario();
        AtomicReference<PanelLogin> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelLogin(new ControladorAutenticacion(servidor, sesion))));
        PanelLogin panel = ref.get();
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoUsuario", JTextField.class).setText("cromero");
            Swing.buscar(panel, "campoClave", JPasswordField.class).setText("Cromero2026");
            panel.mostrarEstado("El usuario o la contraseña son incorrectos", true);
        });
        guardar(panel, "login.png");
    }

    @Test
    void capturaCuentasConDatos() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelCuentas> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelCuentas(new ControladorCuentas(servidor, sesion), null)));
        PanelCuentas panel = ref.get();
        RespuestaCuentas respuesta = new RespuestaCuentas("OK", "OK", "Cuentas consultadas correctamente");
        respuesta.setNombreCliente("Carlos Romero Mena");
        respuesta.setCuentas(List.of(
                cuenta("00200002", "Soles", "6800", "ACTIVO", 7),
                cuenta("00100002", "Dólares", "1250.40", "ACTIVO", 12),
                cuenta("00200009", "Soles", "35.00", "BLOQUEADO", 0)));
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoCedula", JTextField.class).setText("06914897");
            panel.llenarTabla(respuesta);
            panel.mostrarEstado(respuesta.getMensaje(), false);
        });
        guardar(panel, "cuentas.png");
    }

    @Test
    void capturaCuentasVacio() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        PanelCuentas panel = new PanelCuentas(new ControladorCuentas(servidor, sesion), null);
        guardar(panel, "cuentas_vacio.png");
    }

    @Test
    void capturaCuentasConError() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        PanelCuentas panel = new PanelCuentas(new ControladorCuentas(servidor, sesion), null);
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoCedula", JTextField.class).setText("06914897");
            panel.mostrarEstado("El servicio no está disponible en este momento", true);
        });
        guardar(panel, "cuentas_error.png");
    }

    @Test
    void capturaMovimientos() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelMovimientos> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null)));
        PanelMovimientos panel = ref.get();
        RespuestaMovimientos respuesta = new RespuestaMovimientos("OK", "OK", "Movimientos consultados correctamente");
        respuesta.setCuenta("00200002");
        respuesta.setMovimientos(List.of(
                movimiento(1, "2026-01-05", "INGRESO", "Apertura de Cuenta", "1000.00", ""),
                movimiento(2, "2026-01-06", "SALIDA", "Retiro en ventanilla", "50.50", "00100001"),
                movimiento(3, "2026-01-09", "INGRESO", "Depósito en efectivo", "320.00", ""),
                movimiento(4, "2026-01-12", "SALIDA", "Costo de mantenimiento", "2.50", "")));
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoCuenta", JTextField.class).setText("00200002");
            panel.llenarTabla(respuesta);
            panel.mostrarEstado(respuesta.getMensaje(), false);
        });
        guardar(panel, "movimientos.png");
    }

    @Test
    void capturaOperacionesConResultado() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelOperacion> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelOperacion(new ControladorMovimientos(servidor, sesion), null)));
        PanelOperacion panel = ref.get();
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoCuenta", JTextField.class).setText(ClienteFalso.CUENTA);
            Swing.buscar(panel, "campoMonto", JTextField.class).setText("10");
            Swing.buscar(panel, "botonEjecutar", JButton.class).doClick();
        });
        Swing.esperar(() -> Swing.buscar(panel, "botonEjecutar", JButton.class).isEnabled());
        Swing.enEdt(() -> {
            Swing.buscar(panel, "modoRetiro", javax.swing.JToggleButton.class).doClick();
            Swing.buscar(panel, "campoCuenta", JTextField.class).setText(ClienteFalso.CUENTA);
            Swing.buscar(panel, "campoMonto", JTextField.class).setText("25");
        });
        guardar(panel, "operaciones.png");
    }

    @Test
    void capturaOperacionesConError() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        servidor.servidorApagado = true;
        AtomicReference<PanelOperacion> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelOperacion(new ControladorMovimientos(servidor, sesion), null)));
        PanelOperacion panel = ref.get();
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoCuenta", JTextField.class).setText(ClienteFalso.CUENTA);
            Swing.buscar(panel, "campoMonto", JTextField.class).setText("10");
            Swing.buscar(panel, "botonEjecutar", JButton.class).doClick();
        });
        Swing.esperar(() -> Swing.buscar(panel, "botonEjecutar", JButton.class).isEnabled());
        guardar(panel, "operaciones_error.png");
    }

    @Test
    void capturaCostos() throws IOException {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelCostos> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelCostos(new ControladorCostos(servidor, sesion), null)));
        PanelCostos panel = ref.get();
        RespuestaCostos respuesta = new RespuestaCostos("OK", "OK", "Costos consultados correctamente");
        respuesta.setSaldoActual(new BigDecimal("6800.00"));
        respuesta.setCostoMantenimiento(new BigDecimal("2.50"));
        respuesta.setCostoMovimiento(new BigDecimal("0.30"));
        respuesta.setInteresProyectado(new BigDecimal("14.20"));
        respuesta.setLimiteMantenimiento(new BigDecimal("500.00"));
        respuesta.setOperacionesSinCosto(4);
        Swing.enEdt(() -> {
            Swing.buscar(panel, "campoCuenta", JTextField.class).setText("00200002");
            panel.mostrarDetalle(respuesta);
            panel.mostrarEstado(respuesta.getMensaje(), false);
        });
        guardar(panel, "costos.png");
    }

    @Test
    void capturaLaVentanaPrincipal() throws IOException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "la ventana principal necesita entorno gráfico");
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<VentanaPrincipal> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new VentanaPrincipal(sesion, null,
                new PanelCuentas(new ControladorCuentas(servidor, sesion), null),
                new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null),
                new PanelOperacion(new ControladorMovimientos(servidor, sesion), null),
                new PanelCostos(new ControladorCostos(servidor, sesion), null))));
        VentanaPrincipal ventana = ref.get();
        try {
            guardar(ventana.getContentPane(), "ventana.png");
        } finally {
            Swing.enEdt(ventana::dispose);
        }
    }

}
