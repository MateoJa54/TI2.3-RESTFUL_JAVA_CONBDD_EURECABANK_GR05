package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Libro de movimientos de una cuenta: formulario arriba, tres cifras de
 * resumen (ingresos, salidas y cantidad) y la tabla debajo, con los estados
 * cargando, vacío, error y éxito.
 */
public class PanelMovimientos extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Resumen de una lista de movimientos. */
    public record Resumen(BigDecimal ingresos, BigDecimal salidas, int cantidad) {
    }

    private static final String TEXTO_BOTON = "Consultar";
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";
    private static final int COLUMNA_TIPO = 3;
    private static final int COLUMNA_IMPORTE = 4;
    private static final String[] COLUMNAS = {"Número", "Fecha", "Descripción", "Tipo", "Importe", "Referencia", "Empleado"};

    private final ControladorMovimientos controlador;
    private final Consumer<String> alSesionCaducada;
    private final JTextField campoCuenta = new JTextField(20);
    private final JButton botonConsultar = PanelTarjeta.boton("botonConsultar", TEXTO_BOTON);
    private final JTable tabla = PanelTarjeta.tabla("tabla", COLUMNAS, 0, COLUMNA_IMPORTE);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JLabel etiquetaCuenta = new JLabel("");
    private final JPanel resumenCifras = new JPanel(new GridLayout(1, 3, 16, 0));
    private final JPanel resultado = new JPanel(new CardLayout());
    private final JPanel vacio = PanelTarjeta.vacio("Sin resultados todavía",
            "Escriba el código de una cuenta y pulse Consultar.");
    private boolean enCurso;

    /**
     * @param controlador      controlador de movimientos, obligatorio
     * @param alSesionCaducada recibe el mensaje del servidor cuando la sesión ya no sirve; puede ser {@code null}
     */
    public PanelMovimientos(ControladorMovimientos controlador, Consumer<String> alSesionCaducada) {
        super(new BorderLayout(0, 16));
        this.controlador = Objects.requireNonNull(controlador, "El controlador es obligatorio");
        this.alSesionCaducada = alSesionCaducada;
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        campoCuenta.setName("campoCuenta");
        etiquetaCuenta.setName("etiquetaCuenta");
        etiquetaCuenta.setFont(Tema.fuenteNegrita(Tema.TAMANO_TARJETA));
        etiquetaCuenta.setForeground(Tema.TEXTO);
        etiquetaCuenta.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        resumenCifras.setOpaque(false);
        tabla.getColumnModel().getColumn(COLUMNA_TIPO).setCellRenderer(new PanelTarjeta.Insignia());
        tabla.getColumnModel().getColumn(COLUMNA_IMPORTE).setCellRenderer(new PanelTarjeta.ImporteConSigno(COLUMNA_TIPO));
        add(PanelTarjeta.cabecera("Movimientos",
                "Consulte el libro de movimientos de una cuenta por su código.",
                new PanelTarjeta(PanelTarjeta.formulario(
                        PanelTarjeta.campo(PanelTarjeta.etiqueta("Código de la cuenta"), campoCuenta),
                        botonConsultar, etiquetaEstado))), BorderLayout.NORTH);
        JPanel datos = new JPanel(new BorderLayout(0, 16));
        datos.setOpaque(false);
        datos.add(resumenCifras, BorderLayout.NORTH);
        JPanel libro = new JPanel(new BorderLayout());
        libro.setOpaque(false);
        libro.add(etiquetaCuenta, BorderLayout.NORTH);
        libro.add(PanelTarjeta.desplazable(tabla), BorderLayout.CENTER);
        datos.add(new PanelTarjeta(libro), BorderLayout.CENTER);
        resultado.setOpaque(false);
        resultado.add(new PanelTarjeta(vacio), CARTA_VACIO);
        resultado.add(new PanelTarjeta(new PanelTarjeta.Esqueleto(5)), CARTA_CARGANDO);
        resultado.add(datos, CARTA_DATOS);
        add(resultado, BorderLayout.CENTER);
        campoCuenta.addActionListener(e -> consultar());
        botonConsultar.addActionListener(e -> consultar());
    }

    /**
     * Valida el código de la cuenta y consulta sus movimientos. Con el código
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
        TareaAsincrona.lanzar(() -> controlador.consultarMovimientos(cuenta), this::alResponder, this::alFallar);
    }

    private void alResponder(RespuestaMovimientos respuesta) {
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
        llenarTabla(respuesta);
        mostrarEstado(respuesta.getMensaje(), false);
    }

    private void alFallar(Throwable fallo) {
        terminar();
        mostrarVacio();
        mostrarEstado(PanelTarjeta.mensajeDe(fallo), true);
    }

    private void mostrarVacio() {
        PanelTarjeta.textoVacio(vacio, "Sin resultados todavía", "Escriba el código de una cuenta y pulse Consultar.");
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
    }

    private void terminar() {
        enCurso = false;
        PanelTarjeta.ocupado(botonConsultar, TEXTO_BOTON, false);
    }

    /**
     * Pone los movimientos de la respuesta en la tabla y calcula el resumen.
     * Si no hay ninguno muestra el estado vacío.
     *
     * @param respuesta sobre con los movimientos de la cuenta
     */
    public void llenarTabla(RespuestaMovimientos respuesta) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        List<MovimientoDTO> movimientos = respuesta == null ? null : respuesta.getMovimientos();
        if (movimientos == null || movimientos.isEmpty()) {
            etiquetaCuenta.setText("");
            mostrarResumen(new Resumen(BigDecimal.ZERO, BigDecimal.ZERO, 0));
            PanelTarjeta.textoVacio(vacio, "La cuenta no tiene movimientos", "Pruebe con otra cuenta.");
            ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
            return;
        }
        etiquetaCuenta.setText("Cuenta " + Formato.texto(respuesta.getCuenta()));
        for (MovimientoDTO movimiento : movimientos) {
            modelo.addRow(new Object[] {
                    Formato.entero(movimiento.getNumero()),
                    Formato.texto(movimiento.getFecha()),
                    (Formato.texto(movimiento.getTipoCodigo()) + " " + Formato.texto(movimiento.getTipoDescripcion())).trim(),
                    Formato.texto(movimiento.getTipoAccion()),
                    PanelTarjeta.dinero(movimiento.getImporte()),
                    Formato.texto(movimiento.getReferencia()),
                    Formato.texto(movimiento.getEmpleado())});
        }
        mostrarResumen(resumen(movimientos));
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_DATOS);
    }

    /**
     * Calcula el resumen de una lista: la suma de los ingresos, la suma de las
     * salidas y la cantidad de movimientos. Los importes vacíos no suman.
     *
     * @param movimientos lista de movimientos de una cuenta
     * @return el resumen
     */
    public static Resumen resumen(List<MovimientoDTO> movimientos) {
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal salidas = BigDecimal.ZERO;
        int cantidad = 0;
        if (movimientos != null) {
            for (MovimientoDTO movimiento : movimientos) {
                cantidad++;
                BigDecimal importe = movimiento.getImporte();
                if (importe == null) {
                    continue;
                }
                if ("INGRESO".equalsIgnoreCase(movimiento.getTipoAccion())) {
                    ingresos = ingresos.add(importe);
                } else if ("SALIDA".equalsIgnoreCase(movimiento.getTipoAccion())) {
                    salidas = salidas.add(importe);
                }
            }
        }
        return new Resumen(ingresos, salidas, cantidad);
    }

    private void mostrarResumen(Resumen r) {
        resumenCifras.removeAll();
        resumenCifras.add(PanelTarjeta.cifra("Ingresos", PanelTarjeta.dinero(r.ingresos())));
        resumenCifras.add(PanelTarjeta.cifra("Salidas", PanelTarjeta.dinero(r.salidas())));
        resumenCifras.add(PanelTarjeta.cifra("Movimientos", Formato.entero(r.cantidad())));
        resumenCifras.revalidate();
        resumenCifras.repaint();
    }

    /**
     * Pone en el campo el código de una cuenta elegida desde otra pestaña,
     * sin consultarla.
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
}
