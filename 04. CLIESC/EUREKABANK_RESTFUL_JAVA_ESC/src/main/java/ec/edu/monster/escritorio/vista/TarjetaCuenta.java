package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.formdev.flatlaf.FlatClientProperties;

/**
 * Tarjeta de una cuenta: código en fuente monoespaciada, cliente, chips de
 * moneda y de estado, saldo grande y tres acciones. Las acciones no llaman al
 * servicio: avisan a quien mostró la tarjeta, que cambia de página y rellena
 * el código de la cuenta.
 */
public class TarjetaCuenta extends PanelTarjeta {

    private static final long serialVersionUID = 1L;

    /** Ancho mínimo de una tarjeta en la rejilla, en píxeles. */
    public static final int ANCHO_MINIMO = 280;

    /** Destino al que lleva cada acción de la tarjeta. */
    public enum Accion {
        /** Ver los movimientos de la cuenta. */
        MOVIMIENTOS,
        /** Ver los costos de mantenimiento de la cuenta. */
        COSTOS,
        /** Operar la cuenta (depósito, retiro o transferencia). */
        OPERAR
    }

    /**
     * @param nombreCliente nombre del cliente titular, tal como lo envió el servidor
     * @param cuenta        datos de la cuenta
     * @param alAccion      recibe la acción pulsada y el código de la cuenta; puede ser {@code null}
     */
    public TarjetaCuenta(String nombreCliente, CuentaDTO cuenta, BiConsumer<Accion, String> alAccion) {
        super(construir(nombreCliente, cuenta, alAccion));
        setName("tarjetaCuenta");
    }

    private static JComponent construir(String nombreCliente, CuentaDTO cuenta, BiConsumer<Accion, String> alAccion) {
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta es obligatoria");
        }
        String codigo = Formato.texto(cuenta.getCodigo());
        String moneda = Formato.texto(cuenta.getDescripcionMoneda());
        String estado = Formato.texto(cuenta.getEstado());
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setOpaque(false);

        JLabel etiquetaCodigo = new JLabel(codigo);
        etiquetaCodigo.setName("codigo");
        etiquetaCodigo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        etiquetaCodigo.setForeground(Tema.TEXTO);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        chips.setOpaque(false);
        JLabel chipMoneda = PanelTarjeta.chip(moneda, Tema.NEUTRO, Tema.NEUTRO_FONDO);
        chipMoneda.setName("moneda");
        JLabel chipEstado = PanelTarjeta.chip(estado, "ACTIVO".equals(estado) ? Tema.EXITO : Tema.ERROR,
                "ACTIVO".equals(estado) ? Tema.EXITO_FONDO : Tema.ERROR_FONDO);
        chipEstado.setName("estado");
        chips.add(chipMoneda);
        chips.add(chipEstado);
        JPanel cabecera = new JPanel(new BorderLayout(8, 0));
        cabecera.setOpaque(false);
        cabecera.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabecera.add(etiquetaCodigo, BorderLayout.WEST);
        cabecera.add(chips, BorderLayout.EAST);
        contenido.add(cabecera);

        JLabel cliente = new JLabel(Formato.texto(nombreCliente));
        cliente.setName("cliente");
        cliente.setForeground(Tema.TEXTO_SUAVE);
        cliente.setAlignmentX(Component.LEFT_ALIGNMENT);
        cliente.setBorder(BorderFactory.createEmptyBorder(6, 0, 14, 0));
        contenido.add(cliente);

        JLabel saldo = new JLabel(PanelTarjeta.dinero(cuenta.getSaldo()));
        saldo.setName("saldo");
        saldo.setFont(Tema.fuenteNegrita(Tema.TAMANO_CIFRA));
        saldo.setForeground(Tema.TEXTO);
        saldo.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(saldo);

        JLabel operaciones = new JLabel(Formato.entero(cuenta.getOperacionesRealizadas()) + " operaciones");
        operaciones.setName("operaciones");
        operaciones.setForeground(Tema.TEXTO_SUAVE);
        operaciones.setFont(Tema.fuente(Tema.TAMANO_AYUDA));
        operaciones.setAlignmentX(Component.LEFT_ALIGNMENT);
        operaciones.setBorder(BorderFactory.createEmptyBorder(2, 0, 16, 0));
        contenido.add(operaciones);

        JPanel acciones = new JPanel();
        acciones.setLayout(new BoxLayout(acciones, BoxLayout.Y_AXIS));
        acciones.setOpaque(false);
        acciones.setAlignmentX(Component.LEFT_ALIGNMENT);
        acciones.add(accion("accionMovimientos", "Ver movimientos", Accion.MOVIMIENTOS, codigo, alAccion));
        acciones.add(accion("accionCostos", "Ver costos", Accion.COSTOS, codigo, alAccion));
        acciones.add(accion("accionOperar", "Operar", Accion.OPERAR, codigo, alAccion));
        contenido.add(acciones);
        contenido.add(Box.createVerticalGlue());
        return contenido;
    }

    private static JButton accion(String nombre, String texto, Accion accion, String codigo,
            BiConsumer<Accion, String> alAccion) {
        JButton boton = new JButton(texto);
        boton.setName(nombre);
        boton.setForeground(Tema.ACENTO);
        boton.setFont(Tema.fuenteNegrita(12f));
        boton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        boton.setMargin(new java.awt.Insets(4, 0, 4, 0));
        boton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        boton.addActionListener(e -> {
            if (alAccion != null) {
                alAccion.accept(accion, codigo);
            }
        });
        return boton;
    }
}
