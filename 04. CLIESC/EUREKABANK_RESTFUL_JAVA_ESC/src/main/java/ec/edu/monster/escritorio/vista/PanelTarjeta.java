package ec.edu.monster.escritorio.vista;

import ec.edu.monster.escritorio.controlador.ControladorProtegido;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.util.ExcepcionServicio;
import ec.edu.monster.escritorio.util.Formato;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Contenedor redondeado (12 px) con borde fino y sombra suave que agrupa un
 * formulario o un resultado. También reúne los pequeños constructores que
 * comparten los paneles (campo con etiqueta encima, estado en línea, tabla,
 * detalle y esqueleto de carga).
 */
public class PanelTarjeta extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Texto del botón mientras la llamada de red está en curso. */
    static final String TEXTO_PROCESANDO = "Procesando";

    /** Longitud máxima de una cédula o de un código de cuenta. */
    static final int LARGO_MAXIMO_CODIGO = 20;

    private static final Color SOMBRA = new Color(0x12, 0x20, 0x2B, 14);

    private final JComponent contenido;

    /**
     * @param contenido componente que se muestra dentro de la tarjeta
     */
    public PanelTarjeta(JComponent contenido) {
        super(new BorderLayout());
        this.contenido = contenido;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 22, 20));
        contenido.setOpaque(false);
        add(contenido, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int ancho = getWidth() - 2;
            int alto = getHeight() - 3;
            int arco = Tema.RADIO_TARJETA * 2;
            g2.setColor(SOMBRA);
            g2.fillRoundRect(0, 2, ancho + 1, alto + 1, arco, arco);
            g2.setColor(Tema.SUPERFICIE);
            g2.fillRoundRect(0, 0, ancho, alto, arco, arco);
            g2.setColor(Tema.BORDE);
            g2.drawRoundRect(0, 0, ancho - 1, alto - 1, arco, arco);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Marcador de posición con la forma final de una tabla, para el estado de
     * carga. No se anima: solo cambia el contenido cuando llegan los datos.
     */
    public static final class Esqueleto extends JComponent {

        private static final long serialVersionUID = 1L;
        private static final int[] ANCHOS = {92, 70, 84, 58, 78, 66};

        private final int filas;

        /**
         * @param filas cantidad de filas de relleno
         */
        public Esqueleto(int filas) {
            this.filas = filas;
            setPreferredSize(new Dimension(300, filas * Tema.ALTO_FILA + 12));
        }

        /**
         * @return cantidad de filas de relleno
         */
        public int getFilas() {
            return filas;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Tema.BORDE);
                for (int i = 0; i < filas; i++) {
                    int ancho = (int) ((getWidth() - 16) * ANCHOS[i % ANCHOS.length] / 100.0);
                    g2.fillRoundRect(8, 12 + i * Tema.ALTO_FILA, Math.max(ancho, 20), 12, 12, 12);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * @return el componente que contiene la tarjeta
     */
    JComponent getContenido() {
        return contenido;
    }

    // ------------------------------------------------------------------
    // Constructores compartidos por los paneles

    /** Etiqueta pequeña que va siempre encima del campo. */
    static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
        etiqueta.setForeground(Tema.TEXTO_SUAVE);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return etiqueta;
    }

    /** Columna con la etiqueta encima del campo. */
    static JPanel campo(JLabel etiqueta, JComponent componente) {
        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        etiqueta.setLabelFor(componente);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferido = componente.getPreferredSize();
        componente.setPreferredSize(new Dimension(preferido.width, Tema.ALTO_CAMPO));
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTO_CAMPO));
        columna.add(etiqueta);
        columna.add(componente);
        return columna;
    }

    /** Etiqueta de estado en línea: vacía hasta que haya algo que decir. */
    static JLabel crearEstado(String nombre) {
        JLabel estado = new JLabel("");
        estado.setName(nombre);
        estado.setFont(Tema.fuente(Tema.TAMANO_BASE));
        estado.setAlignmentX(Component.LEFT_ALIGNMENT);
        estado.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        estado.getAccessibleContext().setAccessibleName("Estado de la operación");
        return estado;
    }

    /** Muestra un mensaje en línea; un texto vacío lo limpia. */
    static void mostrar(JLabel estado, String mensaje, boolean error) {
        estado.setForeground(error ? Tema.ERROR : Tema.EXITO);
        boolean vacio = mensaje == null || mensaje.isEmpty();
        estado.setText(vacio ? "" : mensaje);
        estado.setIcon(vacio ? null : Iconos.de(error ? Iconos.Tipo.ERROR : Iconos.Tipo.EXITO, 16,
                error ? Tema.ERROR : Tema.EXITO));
        estado.setIconTextGap(8);
    }

    /** Botón de acción principal (el CTA de la pantalla). */
    static JButton boton(String nombre, String texto) {
        JButton boton = new JButton(texto);
        boton.setName(nombre);
        boton.setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
        boton.setMargin(new Insets(8, 22, 8, 22));
        return boton;
    }

    /** Cambia el botón a "Procesando" y lo deshabilita, o lo devuelve a su texto. */
    static void ocupado(JButton boton, String textoOriginal, boolean ocupado) {
        boton.setEnabled(!ocupado);
        boton.setText(ocupado ? TEXTO_PROCESANDO : textoOriginal);
    }

    /** Formulario de una línea: el campo con su etiqueta, el botón y el estado debajo. */
    static JPanel formulario(JPanel campo, JButton boton, JLabel estado) {
        JPanel fila = new JPanel(new BorderLayout(12, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(campo, BorderLayout.CENTER);
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setOpaque(false);
        lateral.add(boton, BorderLayout.SOUTH);
        fila.add(lateral, BorderLayout.EAST);
        JPanel formulario = new JPanel();
        formulario.setOpaque(false);
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.add(fila);
        formulario.add(estado);
        return formulario;
    }

    /** Estado vacío: un mensaje compuesto y una pista de qué hacer. */
    static JPanel vacio(String titulo, String pista) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        JLabel principal = new JLabel(titulo, SwingConstants.CENTER);
        principal.setFont(Tema.fuenteNegrita(15f));
        principal.setForeground(Tema.TEXTO);
        principal.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel secundaria = new JLabel(pista, SwingConstants.CENTER);
        secundaria.setForeground(Tema.TEXTO_SUAVE);
        secundaria.setAlignmentX(Component.CENTER_ALIGNMENT);
        texto.add(principal);
        texto.add(Box.createVerticalStrut(6));
        texto.add(secundaria);
        panel.add(texto, new GridBagConstraints());
        panel.putClientProperty("titulo", principal);
        panel.putClientProperty("pista", secundaria);
        return panel;
    }

    /** Cambia el texto de un estado vacío ya construido. */
    static void textoVacio(JPanel panel, String titulo, String pista) {
        ((JLabel) panel.getClientProperty("titulo")).setText(titulo);
        ((JLabel) panel.getClientProperty("pista")).setText(pista);
    }

    /** Tabla de solo lectura, con encabezado claro y los importes a la derecha. */
    static JTable tabla(String nombre, String[] columnas, int... columnasNumericas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setName(nombre);
        tabla.setRowHeight(Tema.ALTO_FILA);
        tabla.setFillsViewportHeight(true);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(Tema.BORDE);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(Tema.fuenteNegrita(12f));
        tabla.getAccessibleContext().setAccessibleName(nombre);
        DefaultTableCellRenderer izquierda = new DefaultTableCellRenderer();
        izquierda.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        derecha.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        TableCellRenderer base = tabla.getTableHeader().getDefaultRenderer();
        for (int i = 0; i < columnas.length; i++) {
            boolean numerica = false;
            for (int n : columnasNumericas) {
                numerica |= n == i;
            }
            tabla.getColumnModel().getColumn(i).setCellRenderer(numerica ? derecha : izquierda);
            final int alineacion = numerica ? SwingConstants.RIGHT : SwingConstants.LEFT;
            tabla.getColumnModel().getColumn(i).setHeaderRenderer((t, valor, sel, foco, fila, col) -> {
                Component c = base.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
                if (c instanceof JLabel etiqueta) {
                    etiqueta.setHorizontalAlignment(alineacion);
                    etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                }
                return c;
            });
        }
        return tabla;
    }

    /** Tabla dentro de un desplazador con borde fino. */
    static JScrollPane desplazable(JTable tabla) {
        JScrollPane panel = new JScrollPane(tabla);
        panel.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        panel.getViewport().setBackground(Tema.SUPERFICIE);
        return panel;
    }

    /** Lista de pares etiqueta y valor; los valores van alineados a la derecha. */
    static JPanel filasDetalle(String[][] pares) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(6, 0, 6, 24);
        c.anchor = GridBagConstraints.WEST;
        for (String[] par : pares) {
            JLabel nombre = new JLabel(par[0]);
            nombre.setForeground(Tema.TEXTO_SUAVE);
            JLabel valor = new JLabel(par[1], SwingConstants.RIGHT);
            valor.setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
            valor.setName("valor." + par[0]);
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            panel.add(nombre, c);
            c.gridx = 1;
            c.weightx = 0;
            c.insets = new Insets(6, 0, 6, 0);
            panel.add(valor, c);
            c.insets = new Insets(6, 0, 6, 24);
            c.gridy++;
        }
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), c);
        return panel;
    }

    // ------------------------------------------------------------------
    // Piezas de diseño v2: encabezado de página, chips, cifras e insignias

    /**
     * Encabezado de página: título de 24 px y una línea de ayuda de 13 px.
     *
     * @param titulo título de la página
     * @param ayuda  línea de ayuda, una sola frase
     * @return el encabezado
     */
    static JPanel encabezado(String titulo, String ayuda) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setName("titulo");
        etiquetaTitulo.setFont(Tema.fuenteNegrita(Tema.TAMANO_TITULO));
        etiquetaTitulo.setForeground(Tema.TEXTO);
        etiquetaTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel etiquetaAyuda = new JLabel(ayuda);
        etiquetaAyuda.setName("ayuda");
        etiquetaAyuda.setFont(Tema.fuente(Tema.TAMANO_AYUDA));
        etiquetaAyuda.setForeground(Tema.TEXTO_SUAVE);
        etiquetaAyuda.setAlignmentX(Component.LEFT_ALIGNMENT);
        etiquetaAyuda.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(etiquetaTitulo);
        panel.add(etiquetaAyuda);
        return panel;
    }

    /**
     * Cabecera de una página: el encabezado arriba y el formulario debajo.
     *
     * @param titulo     título de la página
     * @param ayuda      línea de ayuda
     * @param formulario tarjeta del formulario
     * @return el bloque superior de la página
     */
    static JPanel cabecera(String titulo, String ayuda, JComponent formulario) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.add(encabezado(titulo, ayuda), BorderLayout.NORTH);
        panel.add(formulario, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Chip: etiqueta redondeada de 8 px con texto y fondo de color.
     *
     * @param texto      texto del chip
     * @param colorTexto color del texto
     * @param fondo      color del fondo
     * @return el chip
     */
    static JLabel chip(String texto, Color colorTexto, Color fondo) {
        Chip chip = new Chip(texto == null ? "" : texto, fondo);
        chip.setFont(Tema.fuenteNegrita(12f));
        chip.setForeground(colorTexto);
        chip.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return chip;
    }

    /**
     * Cifra destacada: etiqueta pequeña arriba y cifra grande abajo, en una tarjeta.
     *
     * @param etiqueta etiqueta de la cifra
     * @param valor    valor ya formateado
     * @return la tarjeta; su valor se llama {@code valor.<etiqueta>}
     */
    static PanelTarjeta cifra(String etiqueta, String valor) {
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        JLabel nombre = new JLabel(etiqueta);
        nombre.setForeground(Tema.TEXTO_SUAVE);
        nombre.setFont(Tema.fuente(Tema.TAMANO_AYUDA));
        nombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cifra = new JLabel(valor);
        cifra.setName("valor." + etiqueta);
        cifra.setFont(Tema.fuenteNegrita(Tema.TAMANO_CIFRA));
        cifra.setForeground(Tema.TEXTO);
        cifra.setAlignmentX(Component.LEFT_ALIGNMENT);
        cifra.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        contenido.add(nombre);
        contenido.add(cifra);
        contenido.add(Box.createVerticalGlue());
        return new PanelTarjeta(contenido);
    }

    /** Etiqueta con fondo redondeado de 8 px. */
    static final class Chip extends JLabel {

        private static final long serialVersionUID = 1L;
        private final transient Color fondo;

        Chip(String texto, Color fondo) {
            super(texto);
            this.fondo = fondo;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Tema.RADIO * 2, Tema.RADIO * 2);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Celda de tabla que muestra el tipo de un movimiento como chip: Ingreso
     * en verde y Salida en rojo, siempre con la palabra.
     */
    static final class Insignia extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        Insignia() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            String texto = valor == null ? "" : valor.toString();
            boolean ingreso = "INGRESO".equalsIgnoreCase(texto);
            boolean salida = "SALIDA".equalsIgnoreCase(texto);
            setText(ingreso ? "Ingreso" : salida ? "Salida" : texto);
            setFont(Tema.fuenteNegrita(12f));
            setForeground(ingreso ? Tema.EXITO : salida ? Tema.ERROR : Tema.NEUTRO);
            setBackground(seleccionada ? tabla.getSelectionBackground() : Tema.SUPERFICIE);
            setOpaque(true);
            return this;
        }
    }

    /** Renderer de importes: signo más en los ingresos y menos en las salidas, con color y la misma cifra. */
    static final class ImporteConSigno extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        private final int columnaTipo;

        /**
         * @param columnaTipo columna del tipo (INGRESO o SALIDA) de la misma fila
         */
        ImporteConSigno(int columnaTipo) {
            this.columnaTipo = columnaTipo;
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            String cifra = valor == null ? "" : valor.toString();
            String tipo = String.valueOf(tabla.getValueAt(fila, columnaTipo));
            boolean ingreso = "INGRESO".equalsIgnoreCase(tipo);
            boolean salida = "SALIDA".equalsIgnoreCase(tipo);
            String signo = ingreso && !cifra.isEmpty() ? "+" : salida && !cifra.isEmpty() ? "-" : "";
            setText(signo + cifra);
            setFont(Tema.fuenteNegrita(Tema.TAMANO_BASE));
            setForeground(ingreso ? Tema.EXITO : salida ? Tema.ERROR : Tema.TEXTO);
            setBackground(seleccionada ? tabla.getSelectionBackground() : Tema.SUPERFICIE);
            setOpaque(true);
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Reglas de presentación y validación compartidas

    /** Importe con dos decimales y separador de miles: {@code 6,800.00}. */
    static String dinero(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        formato.setParseBigDecimal(true);
        return formato.format(new BigDecimal(Formato.monto(valor)));
    }

    /**
     * Valida una cédula o un código de cuenta antes de llamar al servicio.
     *
     * @param texto  lo escrito por el usuario
     * @param nombre nombre del dato en el mensaje, por ejemplo "la cédula"
     * @return el mensaje de error, o {@code null} si el valor es válido
     */
    static String errorDeCodigo(String texto, String nombre) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return "Escriba " + nombre + ".";
        }
        if (limpio.length() > LARGO_MAXIMO_CODIGO || limpio.chars().anyMatch(Character::isWhitespace)) {
            return "Revise " + nombre + ": no puede tener espacios ni más de "
                    + LARGO_MAXIMO_CODIGO + " caracteres.";
        }
        return null;
    }

    /** Mensaje literal del catálogo para un error interno. */
    static final String MENSAJE_ERROR_INTERNO = "Ocurrió un error interno, intente nuevamente";

    /** Mensaje en línea para un fallo de comunicación o un error inesperado. */
    static String mensajeDe(Throwable fallo) {
        if (fallo instanceof ExcepcionServicio) {
            return ExcepcionServicio.MENSAJE;
        }
        return MENSAJE_ERROR_INTERNO;
    }

    /** Mensaje en línea para una respuesta de error: el campo {@code mensaje} del servidor, sin reescribir. */
    static String mensajeDe(RespuestaBase respuesta) {
        if (respuesta == null || respuesta.getMensaje() == null || respuesta.getMensaje().isBlank()) {
            return MENSAJE_ERROR_INTERNO;
        }
        return respuesta.getMensaje();
    }

    /** {@code true} si el servidor dice que el token no es válido o ya expiró. */
    static boolean sesionCaducada(RespuestaBase respuesta) {
        return respuesta != null && (ControladorProtegido.CODIGO_TOKEN_INVALIDO.equals(respuesta.getCodigo())
                || ControladorProtegido.CODIGO_TOKEN_EXPIRADO.equals(respuesta.getCodigo()));
    }

    /** {@code true} si la respuesta es un éxito. */
    static boolean esExito(RespuestaBase respuesta) {
        return respuesta != null && "OK".equals(respuesta.getEstado());
    }
}
