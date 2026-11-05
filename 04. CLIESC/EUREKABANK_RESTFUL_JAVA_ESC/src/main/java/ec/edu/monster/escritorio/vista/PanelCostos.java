package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorCostos;
import ec.edu.monster.escritorio.modelo.RespuestaCostos;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Proyección de costos de mantenimiento de una cuenta: formulario compacto
 * arriba y el detalle debajo, con los estados cargando, vacío, error y éxito.
 */
public class PanelCostos extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TEXTO_BOTON = "Consultar";
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";

    private final ControladorCostos controlador;
    private final Consumer<String> alSesionCaducada;
    private final JTextField campoCuenta = new JTextField(20);
    private final JButton botonConsultar = PanelTarjeta.boton("botonConsultar", TEXTO_BOTON);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JPanel resultado = new JPanel(new CardLayout());
    private final JPanel vacio = PanelTarjeta.vacio("Sin resultados todavía",
            "Escriba el código de una cuenta y pulse Consultar.");
    private final JPanel datos = new JPanel(new BorderLayout());
    private boolean enCurso;

    /**
     * @param controlador      controlador de costos, obligatorio
     * @param alSesionCaducada recibe el mensaje del servidor cuando la sesión ya no sirve; puede ser {@code null}
     */
    public PanelCostos(ControladorCostos controlador, Consumer<String> alSesionCaducada) {
        super(new BorderLayout(0, 16));
        this.controlador = Objects.requireNonNull(controlador, "El controlador es obligatorio");
        this.alSesionCaducada = alSesionCaducada;
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        campoCuenta.setName("campoCuenta");
        add(new PanelTarjeta(PanelTarjeta.formulario(
                PanelTarjeta.campo(PanelTarjeta.etiqueta("Código de la cuenta"), campoCuenta),
                botonConsultar, etiquetaEstado)), BorderLayout.NORTH);
        datos.setOpaque(false);
        resultado.add(vacio, CARTA_VACIO);
        resultado.add(new PanelTarjeta.Esqueleto(4), CARTA_CARGANDO);
        resultado.add(datos, CARTA_DATOS);
        add(new PanelTarjeta(resultado), BorderLayout.CENTER);
        campoCuenta.addActionListener(e -> consultar());
        botonConsultar.addActionListener(e -> consultar());
    }

    /**
     * Valida el código de la cuenta y consulta la proyección. Con el código
     * vacío, con espacios o de más de 20 caracteres muestra el aviso y no
     * llama al servicio.
     */
    public void consultar() {
        if (enCurso) {
            return;
        }
        String error = PanelTarjeta.errorDeCodigo(campoCuenta.getText(), "el código de la cuenta");
        if (error != null) {
            mostrarEstado(error, true);
            return;
        }
        String cuenta = campoCuenta.getText().trim();
        enCurso = true;
        mostrarEstado("", false);
        PanelTarjeta.ocupado(botonConsultar, TEXTO_BOTON, true);
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_CARGANDO);
        TareaAsincrona.lanzar(() -> controlador.consultarCostos(cuenta), this::alResponder, this::alFallar);
    }

    private void alResponder(RespuestaCostos respuesta) {
        terminar();
        if (PanelTarjeta.sesionCaducada(respuesta)) {
            mostrarVacio();
            if (alSesionCaducada != null) {
                alSesionCaducada.accept(PanelTarjeta.mensajeDe(respuesta));
            }
            return;
        }
        if (!PanelTarjeta.esExito(respuesta)) {
            mostrarVacio();
            mostrarEstado(PanelTarjeta.mensajeDe(respuesta), true);
            return;
        }
        mostrarDetalle(respuesta);
        mostrarEstado(respuesta.getMensaje(), false);
    }

    private void alFallar(Throwable fallo) {
        terminar();
        mostrarVacio();
        mostrarEstado(PanelTarjeta.mensajeDe(fallo), true);
    }

    private void mostrarVacio() {
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
    }

    private void terminar() {
        enCurso = false;
        PanelTarjeta.ocupado(botonConsultar, TEXTO_BOTON, false);
    }

    /**
     * Muestra la proyección de costos de la respuesta.
     *
     * @param respuesta sobre con los costos de la cuenta
     */
    public void mostrarDetalle(RespuestaCostos respuesta) {
        datos.removeAll();
        datos.add(PanelTarjeta.filasDetalle(new String[][] {
                {"Saldo actual", PanelTarjeta.dinero(respuesta.getSaldoActual())},
                {"Costo de mantenimiento", PanelTarjeta.dinero(respuesta.getCostoMantenimiento())},
                {"Costo por movimiento", PanelTarjeta.dinero(respuesta.getCostoMovimiento())},
                {"Interés proyectado", PanelTarjeta.dinero(respuesta.getInteresProyectado())},
                {"Límite de mantenimiento", PanelTarjeta.dinero(respuesta.getLimiteMantenimiento())},
                {"Operaciones sin costo", Formato.entero(respuesta.getOperacionesSinCosto())}}),
                BorderLayout.NORTH);
        datos.revalidate();
        datos.repaint();
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_DATOS);
    }

    /**
     * Muestra un mensaje en línea junto a la acción.
     *
     * @param mensaje texto a mostrar; vacío lo limpia
     * @param error   {@code true} para el color de error, {@code false} para el de éxito
     */
    public void mostrarEstado(String mensaje, boolean error) {
        PanelTarjeta.mostrar(etiquetaEstado, mensaje, error);
    }
}
