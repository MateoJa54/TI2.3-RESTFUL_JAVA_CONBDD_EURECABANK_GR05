package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelMovimientos;
import ec.edu.monster.escritorio.vista.Tema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Libro de movimientos: validación, tabla, estados y sesión caducada. */
class PanelMovimientosTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private PanelMovimientos panel;
    private JTextField campo;
    private JButton boton;
    private JLabel estado;
    private JTable tabla;
    private List<String> avisosDeSesion;

    @BeforeAll
    static void tema() {
        Swing.enEdt(Tema::instalar);
    }

    @BeforeEach
    void preparar() {
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        new ControladorAutenticacion(servidor, sesion).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
        avisosDeSesion = new ArrayList<>();
        Swing.enEdt(() -> panel = new PanelMovimientos(new ControladorMovimientos(servidor, sesion), avisosDeSesion::add));
        campo = Swing.buscar(panel, "campoCuenta", JTextField.class);
        boton = Swing.buscar(panel, "botonConsultar", JButton.class);
        estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
        tabla = Swing.buscar(panel, "tabla", JTable.class);
    }

    private void consultarYEsperar(String cuenta) {
        Swing.enEdt(() -> {
            campo.setText(cuenta);
            panel.consultar();
        });
        Swing.esperar(() -> boton.isEnabled());
    }

    @Test
    void elBotonDiceConsultar() {
        assertEquals("Consultar", boton.getText());
    }

    @Test
    void laTablaTieneLasSeisColumnas() {
        assertEquals(6, tabla.getColumnCount());
        assertEquals("Importe", tabla.getColumnName(4));
        assertEquals(32, tabla.getRowHeight());
    }

    @Test
    void elImporteYElNumeroVanAlineadosALaDerecha() {
        DefaultTableCellRenderer importe = (DefaultTableCellRenderer) tabla.getColumnModel().getColumn(4).getCellRenderer();
        DefaultTableCellRenderer numero = (DefaultTableCellRenderer) tabla.getColumnModel().getColumn(0).getCellRenderer();
        DefaultTableCellRenderer fecha = (DefaultTableCellRenderer) tabla.getColumnModel().getColumn(1).getCellRenderer();
        assertEquals(SwingConstants.RIGHT, importe.getHorizontalAlignment());
        assertEquals(SwingConstants.RIGHT, numero.getHorizontalAlignment());
        assertEquals(SwingConstants.LEADING, fecha.getHorizontalAlignment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "0020 0002", "00200002 ", "123456789012345678901", "a b"})
    void cuentasInvalidasNoLlamanAlServicio(String cuenta) {
        Swing.enEdt(() -> {
            campo.setText(cuenta);
            panel.consultar();
        });
        if (cuenta.trim().equals(cuenta) || cuenta.isBlank()) {
            assertFalse(estado.getText().isEmpty());
            assertTrue(servidor.llamadas.isEmpty());
        } else {
            // "00200002 " con un espacio al final se recorta y sí es válida.
            Swing.esperar(() -> boton.isEnabled());
        }
    }

    @Test
    void unaCuentaVaciaPideEscribirla() {
        Swing.enEdt(panel::consultar);
        assertEquals("Escriba el código de la cuenta.", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void unaCuentaConEspaciosInternosExplicaElProblema() {
        Swing.enEdt(() -> {
            campo.setText("0020 0002");
            panel.consultar();
        });
        assertEquals("Revise el código de la cuenta: no puede tener espacios ni más de 20 caracteres.", estado.getText());
    }

    @Test
    void unaConsultaCorrectaLlenaLaTabla() {
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals(1, tabla.getRowCount());
        assertEquals("1", tabla.getValueAt(0, 0));
        assertEquals("2026-01-05", tabla.getValueAt(0, 1));
        assertEquals("001 Apertura de Cuenta", tabla.getValueAt(0, 2));
        assertEquals("INGRESO", tabla.getValueAt(0, 3));
        assertEquals("1,000.00", tabla.getValueAt(0, 4));
        assertEquals("", tabla.getValueAt(0, 5));
    }

    @Test
    void elExitoMuestraElMensajeDelServidor() {
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("Movimientos consultados correctamente", estado.getText());
        assertEquals(Tema.EXITO, estado.getForeground());
    }

    @Test
    void lasEspaciosAlrededorDeLaCuentaSeRecortan() {
        consultarYEsperar("  " + ClienteFalso.CUENTA + " ");
        assertEquals("consultarMovimientos(" + ClienteFalso.CUENTA + ",TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void cuentaInexistenteMuestraElErrorDelServidor() {
        consultarYEsperar("99999999");
        assertEquals("La cuenta solicitada no existe", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void sesionPerdidaAvisaAlLoginConElMensajeDelServidor() {
        servidor.perderSesion();
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals(List.of("El token de sesión no es válido"), avisosDeSesion);
    }

    @Test
    void tokenExpiradoAvisaAlLoginConElMensajeDelServidor() {
        servidor.vencerSesion();
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals(List.of("El token de sesión ha expirado, inicie sesión nuevamente"), avisosDeSesion);
        assertFalse(sesion.estaVigente());
    }

    @Test
    void servidorApagadoMuestraSer001() {
        servidor.servidorApagado = true;
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("El servicio no está disponible en este momento", estado.getText());
        assertTrue(boton.isEnabled());
        assertEquals("Consultar", boton.getText());
    }

    @Test
    void llenarTablaConNuloNoFalla() {
        Swing.enEdt(() -> panel.llenarTabla(null));
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void unaListaVaciaNoDejaFilas() {
        RespuestaMovimientos r = new RespuestaMovimientos("OK", "200", "ok");
        r.setCuenta("00200002");
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void unMovimientoConCamposNulosSeMuestraConVacios() {
        RespuestaMovimientos r = new RespuestaMovimientos("OK", "200", "ok");
        r.setMovimientos(List.of(new MovimientoDTO()));
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals("0", tabla.getValueAt(0, 0));
        assertEquals("", tabla.getValueAt(0, 2));
        assertEquals("0.00", tabla.getValueAt(0, 4));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"1000;1,000.00", "0.5;0.50", "25.999;26.00", "1234.5;1,234.50", "0;0.00"})
    void losImportesTienenDosDecimalesYSeparadorDeMiles(String importe, String esperado) {
        RespuestaMovimientos r = new RespuestaMovimientos("OK", "200", "ok");
        MovimientoDTO m = new MovimientoDTO();
        m.setNumero(3);
        m.setImporte(new BigDecimal(importe));
        r.setMovimientos(List.of(m));
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals(esperado, tabla.getValueAt(0, 4));
    }

    @Test
    void variasFilasSeMuestranEnElOrdenDelServidor() {
        RespuestaMovimientos r = new RespuestaMovimientos("OK", "200", "ok");
        List<MovimientoDTO> lista = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            MovimientoDTO m = new MovimientoDTO();
            m.setNumero(i);
            m.setImporte(BigDecimal.TEN);
            lista.add(m);
        }
        r.setMovimientos(lista);
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals(5, tabla.getRowCount());
        for (int i = 0; i < 5; i++) {
            assertEquals(String.valueOf(i + 1), tabla.getValueAt(i, 0));
        }
    }

    @Test
    void enterEnElCampoConsulta() {
        Swing.enEdt(() -> {
            campo.setText(ClienteFalso.CUENTA);
            campo.postActionEvent();
        });
        Swing.esperar(() -> boton.isEnabled());
        assertEquals(1, servidor.llamadas.size());
    }

    @Test
    void mostrarEstadoPintaErrorYExito() {
        Swing.enEdt(() -> panel.mostrarEstado("x", true));
        assertEquals(Tema.ERROR, estado.getForeground());
        Swing.enEdt(() -> panel.mostrarEstado("x", false));
        assertEquals(Tema.EXITO, estado.getForeground());
    }
}
