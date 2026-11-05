package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCostos;
import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaCostos;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelCostos;
import ec.edu.monster.escritorio.vista.PanelCuentas;
import ec.edu.monster.escritorio.vista.PanelLogin;
import ec.edu.monster.escritorio.vista.PanelMovimientos;
import ec.edu.monster.escritorio.vista.PanelOperacion;
import ec.edu.monster.escritorio.vista.TarjetaCuenta;
import ec.edu.monster.escritorio.vista.Tema;
import ec.edu.monster.escritorio.vista.VentanaPrincipal;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.table.TableCellRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del rediseño v2: tokens, encabezados, rejilla de cuentas, resumen de
 * movimientos, control segmentado, cifras de costos, clave visible y navegación
 * entre pestañas. Sin ventanas reales, salvo donde se pide un JFrame.
 */
class DisenoV2Test {

    @BeforeAll
    static void tema() {
        Swing.enEdt(Tema::instalar);
    }

    private static SesionUsuario sesionIniciada(ClienteFalso servidor) {
        SesionUsuario sesion = new SesionUsuario();
        new ControladorAutenticacion(servidor, sesion).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
        return sesion;
    }

    private static CuentaDTO cuenta(String codigo, String moneda, String saldo) {
        CuentaDTO cuenta = new CuentaDTO();
        cuenta.setCodigo(codigo);
        cuenta.setDescripcionMoneda(moneda);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.setEstado("ACTIVO");
        cuenta.setOperacionesRealizadas(3);
        return cuenta;
    }

    private static MovimientoDTO movimiento(int numero, String tipo, String importe) {
        MovimientoDTO m = new MovimientoDTO();
        m.setNumero(numero);
        m.setFecha("2026-01-05");
        m.setTipoCodigo("001");
        m.setTipoDescripcion("Movimiento");
        m.setTipoAccion(tipo);
        m.setImporte(new BigDecimal(importe));
        m.setReferencia("");
        m.setEmpleado("cromero");
        return m;
    }

    private static boolean oculta(Component componente) {
        for (Component c = componente; c != null; c = c.getParent()) {
            if (!c.isVisible()) {
                return true;
            }
        }
        return false;
    }

    private static double contraste(Color a, Color b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(Color c) {
        double[] canales = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        double total = 0;
        double[] pesos = {0.2126, 0.7152, 0.0722};
        for (int i = 0; i < 3; i++) {
            double v = canales[i];
            v = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
            total += pesos[i] * v;
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Tokens y medidas

    @Test
    void lasMedidasDelDisenoV2() {
        assertEquals(44, Tema.ALTO_CAMPO);
        assertEquals(220, Tema.ANCHO_BARRA);
        assertEquals(24f, Tema.TAMANO_TITULO, 0.001f);
        assertEquals(13f, Tema.TAMANO_AYUDA, 0.001f);
        assertEquals(32f, Tema.TAMANO_CIFRA, 0.001f);
        assertEquals(new java.awt.Dimension(1100, 720), Tema.VENTANA_INICIAL);
        assertEquals(new java.awt.Dimension(960, 640), Tema.VENTANA_MINIMA);
    }

    @Test
    void elTinteDelAcentoSeLeeConTextoOscuro() {
        assertEquals(new Color(0xE3F3F5), Tema.TINTE);
        assertTrue(contraste(Tema.TEXTO, Tema.TINTE) >= 4.5, "texto sobre el tinte");
        assertTrue(contraste(Tema.ACENTO, Tema.SUPERFICIE) >= 4.5, "acento sobre blanco (control segmentado)");
        assertTrue(contraste(Tema.EXITO, Tema.EXITO_FONDO) >= 4.5, "éxito sobre su fondo");
        assertTrue(contraste(Tema.ERROR, Tema.ERROR_FONDO) >= 4.5, "error sobre su fondo");
    }

    @Test
    void elEncabezadoDePaginaTieneTituloDe24Y13() {
        PanelCuentas panel = new PanelCuentas(new ControladorCuentas(new ClienteFalso(), new SesionUsuario()), null);
        JLabel titulo = Swing.buscar(panel, "titulo", JLabel.class);
        assertEquals("Cuentas", titulo.getText());
        assertEquals(24f, titulo.getFont().getSize2D(), 0.001f);
        JLabel ayuda = Swing.buscar(panel, "ayuda", JLabel.class);
        assertEquals(13f, ayuda.getFont().getSize2D(), 0.001f);
    }

    // ------------------------------------------------------------------
    // Inicio de sesión

    @Test
    void laClaveTieneBotonParaVerlaYOcultarla() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelLogin> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelLogin(new ControladorAutenticacion(servidor, sesion))));
        PanelLogin panel = ref.get();
        JPasswordField clave = Swing.buscar(panel, "campoClave", JPasswordField.class);
        JToggleButton ojo = Swing.buscar(panel, "botonMostrarClave", JToggleButton.class);
        char eco = clave.getEchoChar();
        assertTrue(eco != 0);
        Swing.enEdt(ojo::doClick);
        assertEquals(0, clave.getEchoChar());
        assertEquals("Ocultar clave", ojo.getToolTipText());
        Swing.enEdt(ojo::doClick);
        assertEquals(eco, clave.getEchoChar());
        assertEquals("Mostrar clave", ojo.getToolTipText());
        assertTrue(servidor.llamadas.isEmpty(), "mostrar la clave no llama al servicio");
    }

    @Test
    void elTituloDelLoginEsIngresoDeEmpleados() {
        PanelLogin panel = new PanelLogin(new ControladorAutenticacion(new ClienteFalso(), new SesionUsuario()));
        assertTrue(Swing.todos(panel).stream().anyMatch(c -> c instanceof JLabel l
                && "Ingreso de empleados".equals(l.getText())));
    }

    // ------------------------------------------------------------------
    // Cuentas: rejilla de tarjetas, resumen y navegación

    @Test
    void elResumenDeCuentasSumaPorMoneda() {
        List<CuentaDTO> cuentas = List.of(
                cuenta("00200002", "Soles", "6800"),
                cuenta("00200003", "Soles", "200.50"),
                cuenta("00100002", "Dólares", "1200"));
        assertEquals("3 cuentas · Saldo: Soles 7,000.50 · Dólares 1,200.00",
                PanelCuentas.resumenDe(cuentas));
        assertEquals("1 cuenta · Saldo: Soles 6,800.00", PanelCuentas.resumenDe(List.of(cuentas.get(0))));
        assertEquals("", PanelCuentas.resumenDe(List.of()));
        assertEquals("", PanelCuentas.resumenDe(null));
    }

    @Test
    void lasCuentasSeMuestranComoTarjetasYLaTablaNoSeVe() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelCuentas> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelCuentas(new ControladorCuentas(servidor, sesion), null)));
        PanelCuentas panel = ref.get();
        RespuestaCuentas respuesta = new RespuestaCuentas("OK", "OK", "Cuentas consultadas correctamente");
        respuesta.setNombreCliente("Carlos Romero");
        respuesta.setCuentas(List.of(cuenta("00200002", "Soles", "6800"),
                cuenta("00200003", "Soles", "10"), cuenta("00100002", "Dólares", "20")));
        Swing.enEdt(() -> panel.llenarTabla(respuesta));
        long tarjetas = Swing.todos(panel).stream().filter(c -> c instanceof TarjetaCuenta).count();
        assertEquals(3, tarjetas);
        assertTrue(oculta(Swing.buscar(panel, "tabla", JTable.class)),
                "la tabla es el modelo y queda oculta");
        assertEquals("Carlos Romero", Swing.buscar(panel, "etiquetaCliente", JLabel.class).getText());
        assertTrue(Swing.buscar(panel, "resumen", JLabel.class).getText().startsWith("3 cuentas"));
    }

    @Test
    void lasAccionesDeLaTarjetaLlevanElCodigoAlDestino() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelCuentas> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelCuentas(new ControladorCuentas(servidor, sesion), null)));
        PanelCuentas panel = ref.get();
        List<String> recibido = new ArrayList<>();
        Swing.enEdt(() -> panel.setAlNavegar((accion, codigo) -> recibido.add(accion + " " + codigo)));
        RespuestaCuentas respuesta = new RespuestaCuentas("OK", "OK", "ok");
        respuesta.setNombreCliente("Carlos Romero");
        respuesta.setCuentas(List.of(cuenta("00200002", "Soles", "6800")));
        Swing.enEdt(() -> panel.llenarTabla(respuesta));
        TarjetaCuenta tarjeta = (TarjetaCuenta) Swing.todos(panel).stream()
                .filter(c -> c instanceof TarjetaCuenta).findFirst().orElseThrow();
        Swing.enEdt(() -> {
            Swing.buscar(tarjeta, "accionMovimientos", JButton.class).doClick();
            Swing.buscar(tarjeta, "accionOperar", JButton.class).doClick();
        });
        assertEquals(List.of("MOVIMIENTOS 00200002", "OPERAR 00200002"), recibido);
        assertEquals(0, servidor.llamadas.stream().filter(l -> l.startsWith("listarCuentasPorCliente")).count(),
                "las acciones no consultan el servicio");
    }

    @Test
    void elEstadoVacioOfreceLaCedulaDeEjemplo() {
        PanelCuentas panel = new PanelCuentas(new ControladorCuentas(new ClienteFalso(), new SesionUsuario()), null);
        JTextField campo = Swing.buscar(panel, "campoCedula", JTextField.class);
        Swing.enEdt(() -> Swing.buscar(panel, "botonCedulaEjemplo", JButton.class).doClick());
        assertEquals(PanelCuentas.CEDULA_EJEMPLO, campo.getText());
        assertEquals("06914897", campo.getText());
    }

    // ------------------------------------------------------------------
    // Movimientos: resumen y chips

    @Test
    void elResumenDeMovimientosSumaIngresosSalidasYCantidad() {
        List<MovimientoDTO> lista = List.of(
                movimiento(1, "INGRESO", "1000.00"),
                movimiento(2, "SALIDA", "50.50"),
                movimiento(3, "INGRESO", "0.25"));
        PanelMovimientos.Resumen r = PanelMovimientos.resumen(lista);
        assertEquals(0, new BigDecimal("1000.25").compareTo(r.ingresos()));
        assertEquals(0, new BigDecimal("50.50").compareTo(r.salidas()));
        assertEquals(3, r.cantidad());
    }

    @Test
    void elResumenIgnoraImportesVaciosYListasVacias() {
        MovimientoDTO sinImporte = movimiento(1, "INGRESO", "1");
        sinImporte.setImporte(null);
        PanelMovimientos.Resumen r = PanelMovimientos.resumen(List.of(sinImporte));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.ingresos()));
        assertEquals(1, r.cantidad());
        PanelMovimientos.Resumen vacio = PanelMovimientos.resumen(List.of());
        assertEquals(0, vacio.cantidad());
        assertEquals(0, BigDecimal.ZERO.compareTo(vacio.salidas()));
    }

    @Test
    void lasCifrasDelResumenSeVenEnLaPantalla() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelMovimientos> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null)));
        PanelMovimientos panel = ref.get();
        RespuestaMovimientos respuesta = new RespuestaMovimientos("OK", "OK", "ok");
        respuesta.setCuenta("00200002");
        respuesta.setMovimientos(List.of(movimiento(1, "INGRESO", "1000.00"), movimiento(2, "SALIDA", "50.50")));
        Swing.enEdt(() -> panel.llenarTabla(respuesta));
        assertEquals("1,000.00", Swing.buscar(panel, "valor.Ingresos", JLabel.class).getText());
        assertEquals("50.50", Swing.buscar(panel, "valor.Salidas", JLabel.class).getText());
        assertEquals("2", Swing.buscar(panel, "valor.Movimientos", JLabel.class).getText());
    }

    @Test
    void elTipoSeVeComoChipYElImporteConSigno() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelMovimientos> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null)));
        PanelMovimientos panel = ref.get();
        RespuestaMovimientos respuesta = new RespuestaMovimientos("OK", "OK", "ok");
        respuesta.setCuenta("00200002");
        respuesta.setMovimientos(List.of(movimiento(1, "INGRESO", "1000.00"), movimiento(2, "SALIDA", "50.50")));
        Swing.enEdt(() -> panel.llenarTabla(respuesta));
        JTable tabla = Swing.buscar(panel, "tabla", JTable.class);
        assertEquals("Ingreso", texto(tabla, 0, 3));
        assertEquals("Salida", texto(tabla, 1, 3));
        assertEquals("+1,000.00", texto(tabla, 0, 4));
        assertEquals("-50.50", texto(tabla, 1, 4));
        assertEquals(Tema.EXITO, colorTexto(tabla, 0, 4));
        assertEquals(Tema.ERROR, colorTexto(tabla, 1, 4));
        assertEquals("1,000.00", tabla.getValueAt(0, 4), "el modelo conserva el importe sin signo");
    }

    private static String texto(JTable tabla, int fila, int columna) {
        return ((JLabel) renderizar(tabla, fila, columna)).getText();
    }

    private static Color colorTexto(JTable tabla, int fila, int columna) {
        return ((JLabel) renderizar(tabla, fila, columna)).getForeground();
    }

    private static Component renderizar(JTable tabla, int fila, int columna) {
        TableCellRenderer render = tabla.getColumnModel().getColumn(columna).getCellRenderer();
        return render.getTableCellRendererComponent(tabla, tabla.getValueAt(fila, columna), false, false, fila, columna);
    }

    @Test
    void cargarCuentaRellenaElCampoSinConsultar() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelMovimientos> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null)));
        PanelMovimientos panel = ref.get();
        Swing.enEdt(() -> panel.cargarCuenta(" 00200002 "));
        assertEquals("00200002", Swing.buscar(panel, "campoCuenta", JTextField.class).getText());
        assertTrue(servidor.llamadas.isEmpty());
    }

    // ------------------------------------------------------------------
    // Operaciones: control segmentado y reglas

    @Test
    void elControlSegmentadoEligeLaOperacionYMuestraElDestino() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelOperacion> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelOperacion(new ControladorMovimientos(servidor, sesion), null)));
        PanelOperacion panel = ref.get();
        JToggleButton deposito = Swing.buscar(panel, "modoDeposito", JToggleButton.class);
        JToggleButton retiro = Swing.buscar(panel, "modoRetiro", JToggleButton.class);
        JToggleButton transferencia = Swing.buscar(panel, "modoTransferencia", JToggleButton.class);
        JTextField destino = Swing.buscar(panel, "campoCuentaDestino", JTextField.class);
        JButton boton = Swing.buscar(panel, "botonEjecutar", JButton.class);

        assertTrue(deposito.isSelected());
        assertFalse(destino.getParent().isVisible());
        assertEquals("Depositar", boton.getText());

        Swing.enEdt(transferencia::doClick);
        assertTrue(transferencia.isSelected());
        assertFalse(deposito.isSelected(), "solo una opción queda marcada");
        assertTrue(destino.getParent().isVisible());
        assertEquals("Transferir", boton.getText());
        assertEquals(2, Swing.buscar(panel, "selectorModo", javax.swing.JComboBox.class).getSelectedIndex());

        Swing.enEdt(retiro::doClick);
        assertTrue(retiro.isSelected());
        assertFalse(destino.getParent().isVisible());
        assertEquals("Retirar", boton.getText());
    }

    @Test
    void lasReglasAntesDeConfirmarEstanVisibles() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        PanelOperacion panel = new PanelOperacion(new ControladorMovimientos(servidor, sesion), null);
        assertNotNull(Swing.buscar(panel, "reglasConfirmacion", JPanel.class));
        assertTrue(Swing.todos(panel).stream().anyMatch(c -> c instanceof JLabel l
                && "Antes de confirmar".equals(l.getText())));
        assertEquals("$", Swing.buscar(panel, "prefijoMonto", JLabel.class).getText());
        assertEquals("Máximo dos decimales", Swing.buscar(panel, "ayudaMonto", JLabel.class).getText());
    }

    @Test
    void cargarCuentaEnOperacionesRellenaElOrigen() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        PanelOperacion panel = new PanelOperacion(new ControladorMovimientos(servidor, sesion), null);
        Swing.enEdt(() -> panel.cargarCuenta("00200002"));
        assertEquals("00200002", Swing.buscar(panel, "campoCuenta", JTextField.class).getText());
        assertTrue(servidor.llamadas.isEmpty());
    }

    // ------------------------------------------------------------------
    // Costos: seis cifras

    @Test
    void costosMuestraSeisTarjetasDeCifra() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<PanelCostos> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new PanelCostos(new ControladorCostos(servidor, sesion), null)));
        PanelCostos panel = ref.get();
        RespuestaCostos respuesta = new RespuestaCostos("OK", "OK", "ok");
        respuesta.setSaldoActual(new BigDecimal("6800"));
        respuesta.setCostoMantenimiento(new BigDecimal("2.50"));
        respuesta.setCostoMovimiento(new BigDecimal("0.30"));
        respuesta.setInteresProyectado(new BigDecimal("1.00"));
        respuesta.setLimiteMantenimiento(new BigDecimal("500"));
        respuesta.setOperacionesSinCosto(4);
        Swing.enEdt(() -> panel.mostrarDetalle(respuesta));
        long cifras = Swing.todos(panel).stream()
                .filter(c -> c instanceof JLabel l && l.getName() != null && l.getName().startsWith("valor.")).count();
        assertEquals(6, cifras);
        assertEquals("6,800.00", Swing.buscar(panel, "valor.Saldo actual", JLabel.class).getText());
        assertEquals("4", Swing.buscar(panel, "valor.Operaciones sin costo", JLabel.class).getText());
    }

    @Test
    void cargarCuentaEnCostosRellenaElCampo() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        PanelCostos panel = new PanelCostos(new ControladorCostos(servidor, sesion), null);
        Swing.enEdt(() -> panel.cargarCuenta("00200002"));
        assertEquals("00200002", Swing.buscar(panel, "campoCuenta", JTextField.class).getText());
        assertTrue(servidor.llamadas.isEmpty());
    }

    // ------------------------------------------------------------------
    // Ventana: iniciales y barra lateral

    @Test
    void lasInicialesSalenDeLasDosPrimerasPalabras() {
        assertEquals("CR", VentanaPrincipal.iniciales("Carlos Romero"));
        assertEquals("AM", VentanaPrincipal.iniciales("ana maría López"));
        assertEquals("A", VentanaPrincipal.iniciales("ana"));
        assertEquals("?", VentanaPrincipal.iniciales(""));
        assertEquals("?", VentanaPrincipal.iniciales(null));
    }

    @Test
    void laBarraLateralNavegaEntreLasPaginas() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "hace falta un JFrame");
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        AtomicReference<VentanaPrincipal> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new VentanaPrincipal(sesion, null,
                new PanelCuentas(new ControladorCuentas(servidor, sesion), null),
                new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null),
                new PanelOperacion(new ControladorMovimientos(servidor, sesion), null),
                new PanelCostos(new ControladorCostos(servidor, sesion), null))));
        VentanaPrincipal ventana = ref.get();
        try {
            JToggleButton movimientos = Swing.buscar(ventana.getRootPane(), "navMovimientos", JToggleButton.class);
            Swing.enEdt(movimientos::doClick);
            assertEquals(1, ventana.getPestanas().getSelectedIndex());
            assertTrue(movimientos.isSelected());
            JToggleButton costos = Swing.buscar(ventana.getRootPane(), "navCostos", JToggleButton.class);
            Swing.enEdt(costos::doClick);
            assertEquals(3, ventana.getPestanas().getSelectedIndex());
            assertTrue(costos.isSelected());
            assertFalse(movimientos.isSelected());
        } finally {
            Swing.enEdt(ventana::dispose);
        }
    }

    @Test
    void unaCuentaLlevaASuPaginaYRellenaElCodigo() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "hace falta un JFrame");
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = sesionIniciada(servidor);
        PanelCuentas cuentas = new PanelCuentas(new ControladorCuentas(servidor, sesion), null);
        PanelMovimientos movimientos = new PanelMovimientos(new ControladorMovimientos(servidor, sesion), null);
        PanelOperacion operacion = new PanelOperacion(new ControladorMovimientos(servidor, sesion), null);
        PanelCostos costos = new PanelCostos(new ControladorCostos(servidor, sesion), null);
        AtomicReference<VentanaPrincipal> ref = new AtomicReference<>();
        Swing.enEdt(() -> ref.set(new VentanaPrincipal(sesion, null, cuentas, movimientos, operacion, costos)));
        VentanaPrincipal ventana = ref.get();
        try {
            RespuestaCuentas respuesta = new RespuestaCuentas("OK", "OK", "ok");
            respuesta.setNombreCliente("Carlos Romero");
            respuesta.setCuentas(List.of(cuenta("00200002", "Soles", "6800")));
            Swing.enEdt(() -> cuentas.llenarTabla(respuesta));
            TarjetaCuenta tarjeta = (TarjetaCuenta) Swing.todos(cuentas).stream()
                    .filter(c -> c instanceof TarjetaCuenta).findFirst().orElseThrow();
            Swing.enEdt(() -> Swing.buscar(tarjeta, "accionCostos", JButton.class).doClick());
            assertEquals(3, ventana.getPestanas().getSelectedIndex());
            assertEquals("00200002", Swing.buscar(costos, "campoCuenta", JTextField.class).getText());
            assertEquals(0, servidor.llamadas.stream().filter(l -> l.startsWith("consultarCostosMantenimiento")).count());
        } finally {
            Swing.enEdt(ventana::dispose);
        }
    }
}
