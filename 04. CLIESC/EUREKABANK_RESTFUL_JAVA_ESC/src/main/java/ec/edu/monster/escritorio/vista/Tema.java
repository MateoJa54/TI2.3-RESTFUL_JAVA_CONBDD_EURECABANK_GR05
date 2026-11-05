package ec.edu.monster.escritorio.vista;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.UIManager;

/**
 * Paleta, radio y tipografía únicos de la aplicación (diseño Monster).
 *
 * <p>FlatLaf claro es la base. {@link #instalar()} aplica el acento teal,
 * los radios de 8 px (controles) y 12 px (tarjetas), la fuente Segoe UI de
 * 13 pt y las medidas de las tablas.
 */
public final class Tema {

    /** Acento único. Con texto blanco encima el contraste es mayor que 4.5. */
    public static final Color ACENTO = new Color(0x0B7A8A);
    /** Acento al pasar el puntero. */
    public static final Color ACENTO_HOVER = new Color(0x096574);
    /** Fondo de la ventana. */
    public static final Color FONDO = new Color(0xF4F7F9);
    /** Fondo de las tarjetas, las tablas y los campos. */
    public static final Color SUPERFICIE = new Color(0xFFFFFF);
    /** Texto principal. */
    public static final Color TEXTO = new Color(0x12202B);
    /** Texto secundario, con contraste mayor que 4.5 sobre blanco. */
    public static final Color TEXTO_SUAVE = new Color(0x55636F);
    /** Borde fino de tarjetas, tablas y campos. */
    public static final Color BORDE = new Color(0xDDE5EA);
    /** Mensajes de error. */
    public static final Color ERROR = new Color(0xB42318);
    /** Mensajes de éxito. */
    public static final Color EXITO = new Color(0x1E7A4F);
    /** Radio de los controles, en píxeles. */
    public static final int RADIO = 8;
    /** Radio de las tarjetas, en píxeles. */
    public static final int RADIO_TARJETA = 12;
    /** Tamaño base de la letra, en puntos. */
    public static final float TAMANO_BASE = 13f;
    /** Alto de las filas de las tablas, en píxeles. */
    public static final int ALTO_FILA = 40;
    /** Tinte del acento: fondo suave del destino activo y de la selección. */
    public static final Color TINTE = new Color(0xE3F3F5);
    /** Fondo de los avisos de éxito y de las etiquetas de estado correctas. */
    public static final Color EXITO_FONDO = new Color(0xE6F4EC);
    /** Fondo de los avisos de error y de las etiquetas de estado incorrectas. */
    public static final Color ERROR_FONDO = new Color(0xFDECEA);
    /** Texto neutro (chips de moneda). */
    public static final Color NEUTRO = new Color(0x55636F);
    /** Fondo neutro (chips de moneda). */
    public static final Color NEUTRO_FONDO = new Color(0xEEF3F6);
    /** Tamaño del título de página, en puntos. */
    public static final float TAMANO_TITULO = 24f;
    /** Tamaño de la ayuda bajo el título de página, en puntos. */
    public static final float TAMANO_AYUDA = 13f;
    /** Tamaño de los títulos de tarjeta, en puntos. */
    public static final float TAMANO_TARJETA = 18f;
    /** Tamaño de las cifras principales, en puntos. */
    public static final float TAMANO_CIFRA = 32f;
    /** Alto de los campos de texto, en píxeles. */
    public static final int ALTO_CAMPO = 44;
    /** Ancho de la barra lateral, en píxeles. */
    public static final int ANCHO_BARRA = 220;
    /** Tamaño inicial de las ventanas, en píxeles. */
    public static final Dimension VENTANA_INICIAL = new Dimension(1100, 720);
    /** Tamaño mínimo de las ventanas, en píxeles. */
    public static final Dimension VENTANA_MINIMA = new Dimension(960, 640);

    private static final String FAMILIA_PREFERIDA = "Segoe UI";
    private static final String FAMILIA_RESPALDO = Font.SANS_SERIF;
    private static final Color SELECCION = new Color(0xDCEFF2);
    private static final Color DESHABILITADO = new Color(0xCFE3E7);
    private static final Color TEXTO_DESHABILITADO = new Color(0x3B5A61);
    private static final Color ENCABEZADO = new Color(0xEEF3F6);

    private static String familia;

    private Tema() {
    }

    /**
     * Instala FlatLaf claro y aplica la paleta, los radios y la tipografía.
     * Debe llamarse una vez, antes de crear ningún componente.
     */
    public static void instalar() {
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#0B7A8A"));
        UIManager.put("defaultFont", fuente(TAMANO_BASE));
        UIManager.put("Component.focusColor", ACENTO);
        UIManager.put("Component.focusedBorderColor", ACENTO);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderColor", BORDE);
        UIManager.put("Component.arc", RADIO);
        UIManager.put("Button.arc", RADIO);
        UIManager.put("TextComponent.arc", RADIO);
        UIManager.put("CheckBox.arc", RADIO);
        UIManager.put("ComboBox.arc", RADIO);
        UIManager.put("Button.background", ACENTO);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.hoverBackground", ACENTO_HOVER);
        UIManager.put("Button.pressedBackground", ACENTO_HOVER);
        UIManager.put("Button.focusedBackground", ACENTO);
        UIManager.put("Button.disabledBackground", DESHABILITADO);
        UIManager.put("Button.disabledText", TEXTO_DESHABILITADO);
        UIManager.put("Button.borderWidth", 0);
        UIManager.put("Panel.background", FONDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("TextField.background", SUPERFICIE);
        UIManager.put("TextField.foreground", TEXTO);
        UIManager.put("PasswordField.background", SUPERFICIE);
        UIManager.put("ComboBox.background", SUPERFICIE);
        UIManager.put("Table.background", SUPERFICIE);
        UIManager.put("Table.foreground", TEXTO);
        UIManager.put("Table.rowHeight", ALTO_FILA);
        UIManager.put("Table.gridColor", BORDE);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.selectionBackground", SELECCION);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.selectionInactiveBackground", SELECCION);
        UIManager.put("Table.selectionInactiveForeground", TEXTO);
        UIManager.put("TableHeader.background", ENCABEZADO);
        UIManager.put("TableHeader.foreground", TEXTO_SUAVE);
        UIManager.put("TableHeader.separatorColor", ENCABEZADO);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDE);
        UIManager.put("TableHeader.height", ALTO_FILA);
        UIManager.put("ScrollPane.background", SUPERFICIE);
        UIManager.put("TabbedPane.background", FONDO);
        UIManager.put("TabbedPane.selectedForeground", ACENTO);
        UIManager.put("TabbedPane.underlineColor", ACENTO);
        UIManager.put("TabbedPane.inactiveUnderlineColor", ACENTO);
        UIManager.put("TabbedPane.hoverColor", SELECCION);
        UIManager.put("TabbedPane.contentAreaColor", BORDE);
        UIManager.put("TabbedPane.tabHeight", 40);
        FlatLightLaf.setup();
    }

    /**
     * @param tamano tamaño en puntos
     * @return Segoe UI (o la sans-serif del sistema si no existe) normal
     */
    public static Font fuente(float tamano) {
        return new Font(familia(), Font.PLAIN, Math.round(tamano)).deriveFont(tamano);
    }

    /**
     * @param tamano tamaño en puntos
     * @return la misma fuente en negrita, para títulos y encabezados
     */
    public static Font fuenteNegrita(float tamano) {
        return new Font(familia(), Font.BOLD, Math.round(tamano)).deriveFont(Font.BOLD, tamano);
    }

    private static synchronized String familia() {
        if (familia == null) {
            String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            familia = Arrays.asList(disponibles).contains(FAMILIA_PREFERIDA)
                    ? FAMILIA_PREFERIDA : FAMILIA_RESPALDO;
        }
        return familia;
    }
}
