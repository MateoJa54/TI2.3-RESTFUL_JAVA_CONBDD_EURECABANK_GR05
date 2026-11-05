package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.util.Formato;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.Scrollable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Cuentas de un cliente por cédula. El resultado se muestra como una rejilla
 * de tarjetas de cuenta, con un resumen de cuántas cuentas hay y su saldo por
 * moneda. La tabla de abajo es el modelo de los datos y no se muestra.
 */
public class PanelCuentas extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Cédula de ejemplo del contrato, para probar la pantalla. */
    public static final String CEDULA_EJEMPLO = "06914897";

    private static final String TEXTO_BOTON = "Buscar";
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";
    private static final int ESPACIO = 16;
    private static final String[] COLUMNAS = {"Cuenta", "Moneda", "Saldo", "Estado", "Sucursal", "Creada", "Operaciones"};

    private final ControladorCuentas controlador;
    private final Consumer<String> alSesionCaducada;
    private final JTextField campoCedula = new JTextField(20);
    private final JButton botonBuscar = PanelTarjeta.boton("botonBuscar", TEXTO_BOTON);
    private final JTable tabla = PanelTarjeta.tabla("tabla", COLUMNAS, 2, 6);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JLabel etiquetaCliente = new JLabel("");
    private final JLabel resumen = new JLabel("");
    private final JPanel grilla = new Rejilla();
    private final JScrollPane desplazable = new JScrollPane(grilla);
    private final JPanel resultado = new JPanel(new CardLayout());
    private final JPanel vacio = PanelTarjeta.vacio("Sin resultados todavía",
            "Escriba la cédula de un cliente y pulse Buscar.");
    private BiConsumer<TarjetaCuenta.Accion, String> alNavegar;
    private boolean enCurso;

    /**
     * @param controlador      controlador de cuentas, obligatorio
     * @param alSesionCaducada recibe el mensaje del servidor cuando la sesión ya no sirve; puede ser {@code null}
     */
    public PanelCuentas(ControladorCuentas controlador, Consumer<String> alSesionCaducada) {
        super(new BorderLayout(0, ESPACIO));
        this.controlador = Objects.requireNonNull(controlador, "El controlador es obligatorio");
        this.alSesionCaducada = alSesionCaducada;
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        campoCedula.setName("campoCedula");
        etiquetaCliente.setName("etiquetaCliente");
        etiquetaCliente.setFont(Tema.fuenteNegrita(Tema.TAMANO_TARJETA));
        etiquetaCliente.setForeground(Tema.TEXTO);
        resumen.setName("resumen");
        resumen.setFont(Tema.fuente(Tema.TAMANO_AYUDA));
        resumen.setForeground(Tema.TEXTO_SUAVE);
        grilla.setOpaque(false);
        desplazable.setBorder(BorderFactory.createEmptyBorder());
        desplazable.setOpaque(false);
        desplazable.getViewport().setOpaque(false);
        JPanel formulario = new PanelTarjeta(PanelTarjeta.formulario(
                PanelTarjeta.campo(PanelTarjeta.etiqueta("Cédula del cliente"), campoCedula),
                botonBuscar, etiquetaEstado));
        add(PanelTarjeta.cabecera("Cuentas",
                "Busque un cliente por su cédula para ver sus cuentas y su saldo.", formulario), BorderLayout.NORTH);

        JPanel vacioConSugerencia = new JPanel(new BorderLayout(0, 12));
        vacioConSugerencia.setOpaque(false);
        vacioConSugerencia.add(new PanelTarjeta(vacio), BorderLayout.CENTER);
        JButton ejemplo = new JButton("Usar la cédula de ejemplo " + CEDULA_EJEMPLO);
        ejemplo.setName("botonCedulaEjemplo");
        ejemplo.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        ejemplo.setForeground(Tema.ACENTO);
        ejemplo.addActionListener(e -> {
            campoCedula.setText(CEDULA_EJEMPLO);
            campoCedula.requestFocusInWindow();
        });
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        fila.setOpaque(false);
        fila.add(ejemplo);
        vacioConSugerencia.add(fila, BorderLayout.SOUTH);

        JPanel datos = new JPanel(new BorderLayout(0, ESPACIO));
        datos.setOpaque(false);
        JPanel cabeceraDatos = new JPanel();
        cabeceraDatos.setOpaque(false);
        cabeceraDatos.setLayout(new BoxLayout(cabeceraDatos, BoxLayout.Y_AXIS));
        cabeceraDatos.add(etiquetaCliente);
        cabeceraDatos.add(resumen);
        datos.add(cabeceraDatos, BorderLayout.NORTH);
        datos.add(desplazable, BorderLayout.CENTER);

        resultado.setOpaque(false);
        resultado.add(vacioConSugerencia, CARTA_VACIO);
        resultado.add(new PanelTarjeta(new PanelTarjeta.Esqueleto(5)), CARTA_CARGANDO);
        resultado.add(datos, CARTA_DATOS);
        add(resultado, BorderLayout.CENTER);

        JPanel oculto = new JPanel(new BorderLayout());
        oculto.add(PanelTarjeta.desplazable(tabla), BorderLayout.CENTER);
        oculto.setVisible(false);
        add(oculto, BorderLayout.SOUTH);

        campoCedula.addActionListener(e -> buscar());
        botonBuscar.addActionListener(e -> buscar());
    }

    /**
     * Valida la cédula y consulta las cuentas. Con la cédula vacía, con
     * espacios o de más de 20 caracteres muestra el aviso y no llama al
     * servicio.
     */
    public void buscar() {
        if (enCurso) {
            return;
        }
        String error = PanelTarjeta.errorDeCodigo(campoCedula.getText(), "la cédula del cliente");
        if (error != null) {
            mostrarEstado(error, true);
            return;
        }
        String cedula = campoCedula.getText().trim();
        enCurso = true;
        mostrarEstado("", false);
        PanelTarjeta.ocupado(botonBuscar, TEXTO_BOTON, true);
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_CARGANDO);
        TareaAsincrona.lanzar(() -> controlador.listarCuentas(cedula), this::alResponder, this::alFallar);
    }

    private void alResponder(RespuestaCuentas respuesta) {
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
        PanelTarjeta.textoVacio(vacio, "Sin resultados todavía", "Escriba la cédula de un cliente y pulse Buscar.");
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
    }

    private void terminar() {
        enCurso = false;
        PanelTarjeta.ocupado(botonBuscar, TEXTO_BOTON, false);
    }

    /**
     * Pone las cuentas de la respuesta en la tabla (modelo de datos) y en la
     * rejilla de tarjetas. Si no hay ninguna muestra el estado vacío.
     *
     * @param respuesta sobre con las cuentas del cliente
     */
    public void llenarTabla(RespuestaCuentas respuesta) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        grilla.removeAll();
        List<CuentaDTO> cuentas = respuesta == null ? null : respuesta.getCuentas();
        if (cuentas == null || cuentas.isEmpty()) {
            etiquetaCliente.setText("");
            resumen.setText("");
            PanelTarjeta.textoVacio(vacio, "El cliente no tiene cuentas", "Pruebe con otra cédula.");
            ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
            return;
        }
        String nombre = Formato.texto(respuesta.getNombreCliente());
        etiquetaCliente.setText(nombre);
        resumen.setText(resumenDe(cuentas));
        for (CuentaDTO cuenta : cuentas) {
            modelo.addRow(new Object[] {
                    Formato.texto(cuenta.getCodigo()),
                    Formato.texto(cuenta.getDescripcionMoneda()),
                    PanelTarjeta.dinero(cuenta.getSaldo()),
                    Formato.texto(cuenta.getEstado()),
                    Formato.texto(cuenta.getCodigoSucursal()),
                    Formato.texto(cuenta.getFechaCreacion()),
                    Formato.entero(cuenta.getOperacionesRealizadas())});
            JPanel envoltura = new JPanel(new BorderLayout());
            envoltura.setOpaque(false);
            envoltura.add(new TarjetaCuenta(nombre, cuenta, this::accionDeTarjeta), BorderLayout.NORTH);
            grilla.add(envoltura);
        }
        ((CardLayout) resultado.getLayout()).show(resultado, CARTA_DATOS);
    }

    /**
     * Resumen de la lista: cuántas cuentas hay y el saldo total por moneda.
     *
     * @param cuentas cuentas del cliente
     * @return texto como {@code 2 cuentas · Saldo: Soles 6,800.00 · Dólares 1,200.00}
     */
    public static String resumenDe(List<CuentaDTO> cuentas) {
        if (cuentas == null || cuentas.isEmpty()) {
            return "";
        }
        Map<String, BigDecimal> porMoneda = new LinkedHashMap<>();
        for (CuentaDTO cuenta : cuentas) {
            String moneda = Formato.texto(cuenta.getDescripcionMoneda());
            BigDecimal saldo = cuenta.getSaldo() == null ? BigDecimal.ZERO : cuenta.getSaldo();
            porMoneda.merge(moneda, saldo, BigDecimal::add);
        }
        StringBuilder texto = new StringBuilder();
        texto.append(cuentas.size()).append(cuentas.size() == 1 ? " cuenta" : " cuentas");
        texto.append(" · Saldo: ");
        boolean primera = true;
        for (Map.Entry<String, BigDecimal> total : porMoneda.entrySet()) {
            if (!primera) {
                texto.append(" · ");
            }
            texto.append(total.getKey().isEmpty() ? "Sin moneda" : total.getKey())
                    .append(' ').append(PanelTarjeta.dinero(total.getValue()));
            primera = false;
        }
        return texto.toString();
    }

    /** Rejilla de tarjetas: ocupa el ancho de la página y reparte las tarjetas en tantas columnas como quepan (mínimo 280 px). */
    private static final class Rejilla extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;
        private int columnas = 1;

        Rejilla() {
            super(new GridLayout(0, 1, ESPACIO, ESPACIO));
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            int nuevas = Math.max(1, getWidth() / (TarjetaCuenta.ANCHO_MINIMO + ESPACIO));
            if (nuevas != columnas) {
                columnas = nuevas;
                setLayout(new GridLayout(0, columnas, ESPACIO, ESPACIO));
            }
            super.doLayout();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int sentido) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int sentido) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private void accionDeTarjeta(TarjetaCuenta.Accion accion, String codigo) {
        if (alNavegar != null) {
            alNavegar.accept(accion, codigo);
        }
    }

    /**
     * Quien muestra esta pestaña decide a dónde lleva cada acción de una tarjeta.
     *
     * @param accion recibe lo que pidió el empleado y el código de la cuenta
     */
    public void setAlNavegar(BiConsumer<TarjetaCuenta.Accion, String> accion) {
        this.alNavegar = accion;
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
