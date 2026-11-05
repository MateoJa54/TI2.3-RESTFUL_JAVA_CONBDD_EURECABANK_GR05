package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
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
 * Cuentas de un cliente por cédula: formulario compacto arriba y la tabla
 * debajo, con los estados cargando, vacío, error y éxito.
 */
public class PanelCuentas extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TEXTO_BOTON = "Buscar";
    private static final String CARTA_VACIO = "vacio";
    private static final String CARTA_CARGANDO = "cargando";
    private static final String CARTA_DATOS = "datos";
    private static final String[] COLUMNAS = {"Cuenta", "Moneda", "Saldo", "Estado", "Sucursal", "Creada", "Operaciones"};

    private final ControladorCuentas controlador;
    private final Consumer<String> alSesionCaducada;
    private final JTextField campoCedula = new JTextField(20);
    private final JButton botonBuscar = PanelTarjeta.boton("botonBuscar", TEXTO_BOTON);
    private final JTable tabla = PanelTarjeta.tabla("tabla", COLUMNAS, 2, 6);
    private final JLabel etiquetaEstado = PanelTarjeta.crearEstado("etiquetaEstado");
    private final JLabel etiquetaCliente = new JLabel("");
    private final JPanel resultado = new JPanel(new CardLayout());
    private final JPanel vacio = PanelTarjeta.vacio("Sin resultados todavía",
            "Escriba la cédula de un cliente y pulse Buscar.");
    private boolean enCurso;

    /**
     * @param controlador      controlador de cuentas, obligatorio
     * @param alSesionCaducada recibe el mensaje del servidor cuando la sesión ya no sirve; puede ser {@code null}
     */
    public PanelCuentas(ControladorCuentas controlador, Consumer<String> alSesionCaducada) {
        super(new BorderLayout(0, 16));
        this.controlador = Objects.requireNonNull(controlador, "El controlador es obligatorio");
        this.alSesionCaducada = alSesionCaducada;
        setBackground(Tema.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        campoCedula.setName("campoCedula");
        etiquetaCliente.setFont(Tema.fuenteNegrita(15f));
        etiquetaCliente.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(new PanelTarjeta(PanelTarjeta.formulario(
                PanelTarjeta.campo(PanelTarjeta.etiqueta("Cédula del cliente"), campoCedula),
                botonBuscar, etiquetaEstado)), BorderLayout.NORTH);
        JPanel datos = new JPanel(new BorderLayout());
        datos.add(etiquetaCliente, BorderLayout.NORTH);
        datos.add(PanelTarjeta.desplazable(tabla), BorderLayout.CENTER);
        resultado.add(vacio, CARTA_VACIO);
        resultado.add(new PanelTarjeta.Esqueleto(5), CARTA_CARGANDO);
        resultado.add(datos, CARTA_DATOS);
        add(new PanelTarjeta(resultado), BorderLayout.CENTER);
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
     * Pone las cuentas de la respuesta en la tabla. Si no hay ninguna muestra
     * el estado vacío.
     *
     * @param respuesta sobre con las cuentas del cliente
     */
    public void llenarTabla(RespuestaCuentas respuesta) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        List<CuentaDTO> cuentas = respuesta == null ? null : respuesta.getCuentas();
        if (cuentas == null || cuentas.isEmpty()) {
            etiquetaCliente.setText("");
            PanelTarjeta.textoVacio(vacio, "El cliente no tiene cuentas", "Pruebe con otra cédula.");
            ((CardLayout) resultado.getLayout()).show(resultado, CARTA_VACIO);
            return;
        }
        etiquetaCliente.setText(Formato.texto(respuesta.getNombreCliente()));
        for (CuentaDTO cuenta : cuentas) {
            modelo.addRow(new Object[] {
                    Formato.texto(cuenta.getCodigo()),
                    Formato.texto(cuenta.getDescripcionMoneda()),
                    PanelTarjeta.dinero(cuenta.getSaldo()),
                    Formato.texto(cuenta.getEstado()),
                    Formato.texto(cuenta.getCodigoSucursal()),
                    Formato.texto(cuenta.getFechaCreacion()),
                    Formato.entero(cuenta.getOperacionesRealizadas())});
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
