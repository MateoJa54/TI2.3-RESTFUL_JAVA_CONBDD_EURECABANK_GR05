package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.modelo.RespuestaMovimiento;
import ec.edu.monster.escritorio.modelo.RespuestaTransferencia;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/**
 * Depósito, retiro y transferencia en un solo formulario. La operación se
 * elige con un control segmentado; la cuenta de destino solo aparece en la
 * transferencia. A la derecha, una tarjeta recuerda las reglas antes de
 * confirmar, y el resultado del servidor aparece en una tarjeta propia.
 */
public class PanelOperacion extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Textos del selector, en el orden de {@link #selectorModo}. */
    private static final String[] MODOS = {"Depósito", "Retiro", "Transferencia"};
    /** Nombre de la operación en el botón, en el mismo orden. */
    private static final String[] NOMBRES_ACCION = {"Depositar", "Retirar", "Transferir"};
    private static final int MODO_TRANSFERENCIA = 2;
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";
    private static final int ENTEROS_MAXIMOS = 10;
    private static final int DECIMALES_MAXIMOS = 2;
    private static final int ESPACIO = 16;
    private static final String MENSAJE_MONTO =
            "El monto debe ser mayor que cero y con máximo dos decimales";
    private static final String MENSAJE_MONTO_GRANDE =
            "El monto no puede tener más de " + ENTEROS_MAXIMOS + " dígitos enteros";

    /** Dígitos con punto o coma decimal y signo opcional: 12, -3.5, 0,25. Sin notación científica. */
    private static final Pattern NUMERO_DECIMAL = Pattern.compile("[+-]?(\\d+([.,]\\d*)?|[.,]\\d+)");

    private final ControladorMovimientos controlador;
    private final Consumer<String> alSesionCaducada;
    private final JComboBox<String> selectorModo = new JComboBox<>(MODOS);
    private final Segmento[] segmentos = new Segmento[MODOS.length];
    private final JTextField campoCuenta = new JTextField(14);
    private final JTextField campoCuentaDestino = new JTextField(14);
    private final JTextField campoMonto = new JTextField(10);
    private final JButton botonEjecutar = PanelTarjeta.boton("botonEjecutar", NOMBRES_ACCION[0]);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JLabel etiquetaCuenta = PanelTarjeta.etiqueta("Código de la cuenta");
    private final JPanel columnaDestino;
    private final JPanel resultado = new JPanel(new CardLayout());
    private final JPanel vacio = PanelTarjeta.vacio("Sin operaciones todavía",
            "Elija la operación, complete los datos y pulse el botón.");
    private final JPanel datos = new JPanel(new BorderLayout());
    private boolean enCurso;

    /**
     * @param controlador      controlador de movimientos, obligatorio
     * @param alSesionCaducada recibe el mensaje del servidor cuando la sesión ya no sirve; puede ser {@code null}
     */
    public PanelOperacion(ControladorMovimientos controlador, Consumer<String> alSesionCaducada) {
        super(new BorderLayout(0, ESPACIO));
        this.controlador = Objects.requireNonNull(controlador, "El controlador es obligatorio");
        this.alSesionCaducada = alSesionCaducada;
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        selectorModo.setName("selectorModo");
        campoCuenta.setName("campoCuenta");
        campoCuentaDestino.setName("campoCuentaDestino");
        campoMonto.setName("campoMonto");
        columnaDestino = conLineaDeAyuda(PanelTarjeta.campo(PanelTarjeta.etiqueta("Cuenta destino"), campoCuentaDestino));

        JPanel formulario = construirFormulario();
        add(PanelTarjeta.cabecera("Operaciones",
                "Deposite, retire o transfiera desde una cuenta. El costo lo calcula el servidor.",
                new PanelTarjeta(formulario)), BorderLayout.NORTH);

        datos.setOpaque(false);
        resultado.setOpaque(false);
        resultado.add(new PanelTarjeta(vacio), CARTA_VACIO);
        resultado.add(new PanelTarjeta(new PanelTarjeta.Esqueleto(4)), CARTA_CARGANDO);
        resultado.add(new PanelTarjeta(datos), CARTA_DATOS);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 3;
        c.insets = new Insets(0, 0, 0, ESPACIO);
        centro.add(resultado, c);
        c.gridx = 1;
        c.weightx = 2;
        c.insets = new Insets(0, 0, 0, 0);
        centro.add(new PanelTarjeta(construirReglas()), c);
        add(centro, BorderLayout.CENTER);

        // El selector sigue existiendo como modelo de la operación; el control
        // segmentado lo maneja. Va en un contenedor oculto, sin ocupar espacio.
        JPanel oculto = new JPanel(new BorderLayout());
        oculto.add(selectorModo, BorderLayout.CENTER);
        oculto.setVisible(false);
        add(oculto, BorderLayout.SOUTH);

        mostrarDestino(false);
        selectorModo.addActionListener(e -> alCambiarModo());
        alCambiarModo();
        botonEjecutar.addActionListener(e -> ejecutar());
        campoMonto.addActionListener(e -> ejecutar());
    }

    private JPanel construirFormulario() {
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setOpaque(false);

        JPanel segmentado = new JPanel(new GridLayout(1, MODOS.length, 4, 0));
        segmentado.setName("segmentado");
        segmentado.setBackground(Tema.NEUTRO_FONDO);
        segmentado.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        segmentado.setAlignmentX(Component.LEFT_ALIGNMENT);
        segmentado.setMaximumSize(new Dimension(420, 48));
        ButtonGroup grupo = new ButtonGroup();
        String[] nombres = {"modoDeposito", "modoRetiro", "modoTransferencia"};
        for (int i = 0; i < MODOS.length; i++) {
            final int modo = i;
            segmentos[i] = new Segmento(MODOS[i], nombres[i]);
            grupo.add(segmentos[i]);
            segmentado.add(segmentos[i]);
            segmentos[i].addActionListener(e -> selectorModo.setSelectedIndex(modo));
        }
        JLabel etiquetaSegmentado = PanelTarjeta.etiqueta("Operación");
        contenido.add(etiquetaSegmentado);
        contenido.add(Box.createVerticalStrut(6));
        contenido.add(segmentado);
        contenido.add(Box.createVerticalStrut(ESPACIO));

        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 12);
        c.gridx = 0;
        c.weightx = 0.3;
        fila.add(conLineaDeAyuda(PanelTarjeta.campo(etiquetaCuenta, campoCuenta)), c);
        c.gridx = 1;
        c.weightx = 0.3;
        fila.add(columnaDestino, c);
        c.gridx = 2;
        c.weightx = 0.2;
        fila.add(columnaMonto(), c);
        c.gridx = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        fila.add(botonEjecutar, c);
        contenido.add(fila);
        contenido.add(etiquetaEstado);
        return contenido;
    }

    /** Añade una línea vacía con la altura de la ayuda del monto, para que los campos queden alineados. */
    private static JPanel conLineaDeAyuda(JPanel columna) {
        JLabel linea = new JLabel(" ");
        linea.setFont(Tema.fuente(Tema.TAMANO_AYUDA));
        linea.setAlignmentX(Component.LEFT_ALIGNMENT);
        linea.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        columna.add(linea);
        return columna;
    }
    private JPanel columnaMonto() {
        JLabel prefijo = new JLabel("$");
        prefijo.setName("prefijoMonto");
        prefijo.setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
        prefijo.setForeground(Tema.TEXTO_SUAVE);
        prefijo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        JPanel conPrefijo = new JPanel(new BorderLayout());
        conPrefijo.setOpaque(false);
        conPrefijo.add(prefijo, BorderLayout.WEST);
        conPrefijo.add(campoMonto, BorderLayout.CENTER);
        JPanel columna = PanelTarjeta.campo(PanelTarjeta.etiqueta("Monto"), conPrefijo);
        JLabel ayuda = new JLabel("Máximo dos decimales");
        ayuda.setName("ayudaMonto");
        ayuda.setFont(Tema.fuente(Tema.TAMANO_AYUDA));
        ayuda.setForeground(Tema.TEXTO_SUAVE);
        ayuda.setAlignmentX(Component.LEFT_ALIGNMENT);
        ayuda.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        columna.add(ayuda);
        return columna;
    }

    private JPanel construirReglas() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Antes de confirmar");
        titulo.setFont(Tema.fuenteNegrita(Tema.TAMANO_TARJETA));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        panel.add(titulo);
        String[] reglas = {
            "El monto debe ser mayor que cero.",
            "Use como máximo dos decimales.",
            "El costo por movimiento lo calcula el servidor y aparece en el resultado."
        };
        for (String regla : reglas) {
            JLabel linea = new JLabel("<html><body style='width: 260px'>" + regla + "</body></html>");
            linea.setForeground(Tema.TEXTO_SUAVE);
            linea.setFont(Tema.fuente(Tema.TAMANO_BASE));
            linea.setAlignmentX(Component.LEFT_ALIGNMENT);
            linea.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            panel.add(linea);
        }
        panel.setName("reglasConfirmacion");
        return panel;
    }

    /** Sincroniza el control segmentado, la cuenta de destino y el texto del botón con la operación elegida. */
    private void alCambiarModo() {
        int modo = Math.max(selectorModo.getSelectedIndex(), 0);
        segmentos[modo].setSelected(true);
        mostrarDestino(modo == MODO_TRANSFERENCIA);
        botonEjecutar.setText(NOMBRES_ACCION[modo]);
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
        PanelTarjeta.ocupado(botonEjecutar, NOMBRES_ACCION[modo], true);
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
                Aviso.mostrar(this, respuesta.getMensaje(), false);
                campoMonto.setText("");
                campoCuentaDestino.setText("");
            } else {
                mostrarVacio();
                mostrarEstado(PanelTarjeta.mensajeDe(respuesta), true);
                Aviso.mostrar(this, PanelTarjeta.mensajeDe(respuesta), true);
            }
        }, fallo -> {
            terminar();
            mostrarVacio();
            mostrarEstado(PanelTarjeta.mensajeDe(fallo), true);
            Aviso.mostrar(this, PanelTarjeta.mensajeDe(fallo), true);
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
        PanelTarjeta.ocupado(botonEjecutar, NOMBRES_ACCION[Math.max(selectorModo.getSelectedIndex(), 0)], false);
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
     * Pone en el campo el código de una cuenta elegida desde otra pestaña,
     * sin ejecutar nada.
     *
     * @param codigo código de la cuenta
     */
    public void cargarCuenta(String codigo) {
        campoCuenta.setText(codigo == null ? "" : codigo.trim());
        mostrarEstado("", false);
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
    static BigDecimal interpretarMonto(String texto) {
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

    /** Opción del control segmentado: píldora de 8 px que se marca al elegirla. */
    private static final class Segmento extends JToggleButton {

        private static final long serialVersionUID = 1L;

        Segmento(String texto, String nombre) {
            super(texto);
            setName(nombre);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
            setForeground(Tema.TEXTO_SUAVE);
            setMargin(new Insets(6, 12, 6, 12));
            getAccessibleContext().setAccessibleName(texto);
            addItemListener(e -> setForeground(isSelected() ? Tema.ACENTO : Tema.TEXTO_SUAVE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected()) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Tema.SUPERFICIE);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), Tema.RADIO * 2, Tema.RADIO * 2));
                    g2.setColor(Tema.BORDE);
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                            Tema.RADIO * 2, Tema.RADIO * 2));
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }
}
