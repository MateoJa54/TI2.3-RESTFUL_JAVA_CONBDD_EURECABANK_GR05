package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
 * Libro de movimientos de una cuenta: formulario compacto arriba y la tabla
 * debajo, con los estados cargando, vacío, error y éxito.
 */
public class PanelMovimientos extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TEXTO_BOTON = "Consultar";
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";
    private static final String[] COLUMNAS = {"Número", "Fecha", "Tipo", "Acción", "Importe", "Referencia"};

    private final ControladorMovimientos controlador;
    private final Consumer<String> alSesionCaducada;
    private final JTextField campoCuenta = new JTextField(20);
    private final JButton botonConsultar = PanelTarjeta.boton("botonConsultar", TEXTO_BOTON);
    private final JTable tabla = PanelTarjeta.tabla("tabla", COLUMNAS, 0, 4);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JLabel etiquetaCuenta = new JLabel("");
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
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        campoCuenta.setName("campoCuenta");
        etiquetaCuenta.setFont(Tema.fuenteNegrita(15f));
        etiquetaCuenta.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(new PanelTarjeta(PanelTarjeta.formulario(
                PanelTarjeta.campo(PanelTarjeta.etiqueta("Código de la cuenta"), campoCuenta),
                botonConsultar, etiquetaEstado)), BorderLayout.NORTH);
        JPanel datos = new JPanel(new BorderLayout());
        datos.add(etiquetaCuenta, BorderLayout.NORTH);
        datos.add(PanelTarjeta.desplazable(tabla), BorderLayout.CENTER);
        resultado.add(vacio, CARTA_VACIO);
        resultado.add(new PanelTarjeta.Esqueleto(5), CARTA_CARGANDO);
        resultado.add(datos, CARTA_DATOS);
        add(new PanelTarjeta(resultado), BorderLayout.CENTER);
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
     * Pone los movimientos de la respuesta en la tabla. Si no hay ninguno
     * muestra el estado vacío.
     *
     * @param respuesta sobre con los movimientos de la cuenta
     */
    public void llenarTabla(RespuestaMovimientos respuesta) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        List<MovimientoDTO> movimientos = respuesta == null ? null : respuesta.getMovimientos();
        if (movimientos == null || movimientos.isEmpty()) {
            etiquetaCuenta.setText("");
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
                    Formato.texto(movimiento.getReferencia())});
        }
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
