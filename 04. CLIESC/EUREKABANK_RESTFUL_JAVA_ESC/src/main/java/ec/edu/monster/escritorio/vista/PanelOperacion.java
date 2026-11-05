package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.modelo.RespuestaMovimiento;
import ec.edu.monster.escritorio.modelo.RespuestaTransferencia;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Depósito, retiro y transferencia en un solo formulario. El modo se elige en
 * el selector; la cuenta de destino solo aparece en la transferencia.
 */
public class PanelOperacion extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Textos del selector, en el orden de {@link #selectorModo}. */
    private static final String[] MODOS = {"Depósito", "Retiro", "Transferencia"};
    private static final int MODO_TRANSFERENCIA = 2;
    private static final String TEXTO_BOTON = "Ejecutar";
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";
    private static final int ENTEROS_MAXIMOS = 10;
    private static final int DECIMALES_MAXIMOS = 2;
    private static final String MENSAJE_MONTO =
            "El monto debe ser mayor que cero y con máximo dos decimales";
    private static final String MENSAJE_MONTO_GRANDE =
            "El monto no puede tener más de " + ENTEROS_MAXIMOS + " dígitos enteros";

    /** Dígitos con punto o coma decimal y signo opcional: 12, -3.5, 0,25. Sin notación científica. */
    private static final Pattern NUMERO_DECIMAL = Pattern.compile("[+-]?(\\d+([.,]\\d*)?|[.,]\\d+)");

    private final ControladorMovimientos controlador;
    private final Consumer<String> alSesionCaducada;
    private final JComboBox<String> selectorModo = new JComboBox<>(MODOS);
    private final JTextField campoCuenta = new JTextField(14);
    private final JTextField campoCuentaDestino = new JTextField(14);
    private final JTextField campoMonto = new JTextField(10);
    private final JButton botonEjecutar = PanelTarjeta.boton("botonEjecutar", TEXTO_BOTON);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JLabel etiquetaCuenta = PanelTarjeta.etiqueta("Código de la cuenta");
    private final JPanel columnaDestino;
    private final JPanel resultado = new JPanel(new CardLayout());
    private final JPanel vacio = PanelTarjeta.vacio("Sin operaciones todavía",
            "Elija la operación, complete los datos y pulse Ejecutar.");
    private final JPanel datos = new JPanel(new BorderLayout());
    private boolean enCurso;

    /**
     * @param controlador      controlador de movimientos, obligatorio
     * @param alSesionCaducada recibe el mensaje del servidor cuando la sesión ya no sirve; puede ser {@code null}
     */
    public PanelOperacion(ControladorMovimientos controlador, Consumer<String> alSesionCaducada) {
        super(new BorderLayout(0, 16));
        this.controlador = Objects.requireNonNull(controlador, "El controlador es obligatorio");
        this.alSesionCaducada = alSesionCaducada;
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        selectorModo.setName("selectorModo");
        campoCuenta.setName("campoCuenta");
        campoCuentaDestino.setName("campoCuentaDestino");
        campoMonto.setName("campoMonto");
        columnaDestino = PanelTarjeta.campo(PanelTarjeta.etiqueta("Cuenta destino"), campoCuentaDestino);
        add(new PanelTarjeta(construirFormulario()), BorderLayout.NORTH);
        datos.setOpaque(false);
        resultado.add(vacio, CARTA_VACIO);
        resultado.add(new PanelTarjeta.Esqueleto(4), CARTA_CARGANDO);
        resultado.add(datos, CARTA_DATOS);
        add(new PanelTarjeta(resultado), BorderLayout.CENTER);
        mostrarDestino(false);
        selectorModo.addActionListener(e -> mostrarDestino(selectorModo.getSelectedIndex() == MODO_TRANSFERENCIA));
        botonEjecutar.addActionListener(e -> ejecutar());
        campoMonto.addActionListener(e -> ejecutar());
    }

    private JPanel construirFormulario() {
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 12);
        c.gridx = 0;
        c.weightx = 0.2;
        fila.add(PanelTarjeta.campo(PanelTarjeta.etiqueta("Operación"), selectorModo), c);
        c.gridx = 1;
        c.weightx = 0.3;
        fila.add(PanelTarjeta.campo(etiquetaCuenta, campoCuenta), c);
        c.gridx = 2;
        fila.add(columnaDestino, c);
        c.gridx = 3;
        c.weightx = 0.2;
        fila.add(PanelTarjeta.campo(PanelTarjeta.etiqueta("Monto"), campoMonto), c);
        c.gridx = 4;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        fila.add(botonEjecutar, c);
        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.add(fila);
        formulario.add(etiquetaEstado);
        return formulario;
    }

    /**
     * Valida los datos y ejecuta la operación elegida. Con una cuenta vacía,
     * con espacios o de más de 20 caracteres, o con un monto que no sea mayor
     * que cero con máximo dos decimales, muestra el aviso y no llama al
     * servicio.
     */
    public void ejecutar() {
        if (enCurso) {
            return;
        }
        int modo = Math.max(selectorModo.getSelectedIndex(), 0);
        boolean transferencia = modo == MODO_TRANSFERENCIA;
        String error = PanelTarjeta.errorDeCodigo(campoCuenta.getText(),
                transferencia ? "la cuenta de origen" : "el código de la cuenta");
        if (error == null && transferencia) {
            error = PanelTarjeta.errorDeCodigo(campoCuentaDestino.getText(), "la cuenta de destino");
        }
        if (error != null) {
            mostrarEstado(error, true);
            return;
        }
        BigDecimal monto = interpretarMonto(campoMonto.getText());
        if (monto == null) {
            mostrarEstado(MENSAJE_MONTO, true);
            return;
        }
        if (monto.precision() - monto.scale() > ENTEROS_MAXIMOS) {
            mostrarEstado(MENSAJE_MONTO_GRANDE, true);
            return;
        }
        String cuenta = campoCuenta.getText().trim();
        String destino = campoCuentaDestino.getText().trim();
        enCurso = true;
        mostrarEstado("", false);
        PanelTarjeta.ocupado(botonEjecutar, TEXTO_BOTON, true);
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_CARGANDO);
        switch (modo) {
            case 0 -> lanzar(() -> controlador.depositar(cuenta, monto), this::mostrarMovimiento);
            case 1 -> lanzar(() -> controlador.retirar(cuenta, monto), this::mostrarMovimiento);
            default -> lanzar(() -> controlador.transferir(cuenta, destino, monto), this::mostrarTransferencia);
        }
    }

    private <R extends RespuestaBase> void lanzar(Supplier<R> trabajo, Consumer<R> alExito) {
        TareaAsincrona.lanzar(trabajo, respuesta -> {
            terminar();
            if (PanelTarjeta.sesionCaducada(respuesta)) {
                mostrarVacio();
                if (alSesionCaducada != null) {
                    alSesionCaducada.accept(PanelTarjeta.mensajeDe(respuesta));
                }
            } else if (PanelTarjeta.esExito(respuesta)) {
                alExito.accept(respuesta);
                mostrarEstado(respuesta.getMensaje(), false);
            } else {
                mostrarVacio();
                mostrarEstado(PanelTarjeta.mensajeDe(respuesta), true);
            }
        }, fallo -> {
            terminar();
            mostrarVacio();
            mostrarEstado(PanelTarjeta.mensajeDe(fallo), true);
        });
    }

    private void mostrarMovimiento(RespuestaMovimiento respuesta) {
        MovimientoDTO movimiento = respuesta.getMovimiento();
        String numero = movimiento == null ? "" : Formato.entero(movimiento.getNumero());
        String fecha = movimiento == null ? "" : Formato.texto(movimiento.getFecha());
        BigDecimal importe = movimiento == null ? null : movimiento.getImporte();
        mostrarFilas(new String[][] {
                {"Movimiento", numero},
                {"Fecha", fecha},
                {"Importe", PanelTarjeta.dinero(importe)},
                {"Saldo nuevo", PanelTarjeta.dinero(respuesta.getSaldoNuevo())},
                {"Costo aplicado", PanelTarjeta.dinero(respuesta.getCostoAplicado())},
                {"ITF aplicado", PanelTarjeta.dinero(respuesta.getItfAplicado())}});
    }

    private void mostrarTransferencia(RespuestaTransferencia respuesta) {
        mostrarFilas(new String[][] {
                {"Movimiento de origen", Formato.entero(respuesta.getNumeroOrigen())},
                {"Movimiento de destino", Formato.entero(respuesta.getNumeroDestino())},
                {"Saldo de origen", PanelTarjeta.dinero(respuesta.getSaldoOrigen())},
                {"Saldo de destino", PanelTarjeta.dinero(respuesta.getSaldoDestino())},
                {"ITF aplicado", PanelTarjeta.dinero(respuesta.getItfAplicado())}});
    }

    private void mostrarFilas(String[][] filas) {
        datos.removeAll();
        datos.add(PanelTarjeta.filasDetalle(filas), BorderLayout.NORTH);
        datos.revalidate();
        datos.repaint();
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_DATOS);
    }

    private void mostrarVacio() {
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
    }

    private void terminar() {
        enCurso = false;
        PanelTarjeta.ocupado(botonEjecutar, TEXTO_BOTON, false);
    }

    /**
     * Muestra u oculta la cuenta de destino y ajusta la etiqueta de la cuenta
     * principal según sea transferencia o no.
     *
     * @param visible {@code true} en la transferencia
     */
    public void mostrarDestino(boolean visible) {
        columnaDestino.setVisible(visible);
        etiquetaCuenta.setText(visible ? "Cuenta de origen" : "Código de la cuenta");
        revalidate();
        repaint();
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

    /**
     * Convierte lo escrito en un importe: mayor que cero, con punto o coma
     * decimal y máximo dos decimales. Rechaza notación científica, texto,
     * {@code NaN}, negativos y cero.
     */
    private static BigDecimal interpretarMonto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty() || !NUMERO_DECIMAL.matcher(limpio).matches()) {
            return null;
        }
        BigDecimal importe;
        try {
            importe = new BigDecimal(limpio.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
        if (importe.signum() <= 0 || importe.stripTrailingZeros().scale() > DECIMALES_MAXIMOS) {
            return null;
        }
        return importe.setScale(DECIMALES_MAXIMOS, RoundingMode.HALF_UP);
    }
}
