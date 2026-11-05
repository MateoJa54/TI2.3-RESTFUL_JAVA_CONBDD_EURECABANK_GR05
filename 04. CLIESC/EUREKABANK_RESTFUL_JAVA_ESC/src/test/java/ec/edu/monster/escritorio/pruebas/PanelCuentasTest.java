package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCuentas;
import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelCuentas;
import ec.edu.monster.escritorio.vista.PanelTarjeta;
import ec.edu.monster.escritorio.vista.Tema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

/** Cuentas por cédula: validación, tabla, estados y sesión caducada. */
class PanelCuentasTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private PanelCuentas panel;
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
        armar(new ClienteFalso(), true);
    }

    private void armar(ClienteFalso falso, boolean conSesion) {
        servidor = falso;
        sesion = new SesionUsuario();
        if (conSesion) {
            new ControladorAutenticacion(servidor, sesion).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
            servidor.llamadas.clear();
        }
        avisosDeSesion = new ArrayList<>();
        Swing.enEdt(() -> panel = new PanelCuentas(new ControladorCuentas(servidor, sesion), avisosDeSesion::add));
        campo = Swing.buscar(panel, "campoCedula", JTextField.class);
        boton = Swing.buscar(panel, "botonBuscar", JButton.class);
        estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
        tabla = Swing.buscar(panel, "tabla", JTable.class);
    }

    private void buscarYEsperar(String cedula) {
        Swing.enEdt(() -> {
            campo.setText(cedula);
            panel.buscar();
        });
        Swing.esperar(() -> boton.isEnabled());
    }

    @Test
    void elBotonDiceBuscar() {
        assertEquals("Buscar", boton.getText());
    }

    @Test
    void laTablaTieneLasColumnasDeLaCuenta() {
        assertEquals(7, tabla.getColumnCount());
        assertEquals("Cuenta", tabla.getColumnName(0));
        assertEquals("Saldo", tabla.getColumnName(2));
    }

    @Test
    void laTablaEmpiezaVaciaYLasFilasMiden40() {
        assertEquals(0, tabla.getRowCount());
        assertEquals(40, tabla.getRowHeight());
    }

    @Test
    void laTablaNoSePuedeEditar() {
        buscarYEsperar(ClienteFalso.CEDULA);
        assertFalse(tabla.isCellEditable(0, 0));
        assertFalse(tabla.isCellEditable(0, 2));
    }

    @Test
    void elSaldoVaAlineadoALaDerechaYElTextoALaIzquierda() {
        DefaultTableCellRenderer saldo = (DefaultTableCellRenderer) tabla.getColumnModel().getColumn(2).getCellRenderer();
        DefaultTableCellRenderer cuenta = (DefaultTableCellRenderer) tabla.getColumnModel().getColumn(0).getCellRenderer();
        DefaultTableCellRenderer operaciones = (DefaultTableCellRenderer) tabla.getColumnModel().getColumn(6).getCellRenderer();
        assertEquals(SwingConstants.RIGHT, saldo.getHorizontalAlignment());
        assertEquals(SwingConstants.RIGHT, operaciones.getHorizontalAlignment());
        assertEquals(SwingConstants.LEADING, cuenta.getHorizontalAlignment());
    }

    @Test
    void elEncabezadoDelSaldoTambienVaALaDerecha() {
        var render = tabla.getColumnModel().getColumn(2).getHeaderRenderer();
        var componente = render.getTableCellRendererComponent(tabla, "Saldo", false, false, -1, 2);
        assertEquals(SwingConstants.RIGHT, ((JLabel) componente).getHorizontalAlignment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "0691 4897", "06914897 1", "1 2", "123456789012345678901",
            "ABCDEFGHIJKLMNOPQRSTU", "06914897\n9"})
    void cedulasInvalidasMuestranAvisoYNoLlamanAlServicio(String cedula) {
        Swing.enEdt(() -> {
            campo.setText(cedula);
            panel.buscar();
        });
        assertFalse(estado.getText().isEmpty());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(servidor.llamadas.isEmpty(), "no debía llamar al servicio");
        assertTrue(boton.isEnabled());
        assertEquals("Buscar", boton.getText());
    }

    @Test
    void unaCedulaVaciaPideEscribirla() {
        Swing.enEdt(panel::buscar);
        assertEquals("Escriba la cédula del cliente.", estado.getText());
    }

    @Test
    void unaCedulaConEspaciosInternosExplicaElProblema() {
        Swing.enEdt(() -> {
            campo.setText("0691 4897");
            panel.buscar();
        });
        assertEquals("Revise la cédula del cliente: no puede tener espacios ni más de 20 caracteres.", estado.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901234567890", "1", "ABC"})
    void cedulasDentroDelLimiteSiLlamanAlServicio(String cedula) {
        buscarYEsperar(cedula);
        assertEquals(1, servidor.llamadas.size());
        assertTrue(servidor.llamadas.get(0).startsWith("listarCuentasPorCliente(" + cedula + ","));
    }

    @Test
    void espaciosAlrededorDeLaCedulaSeRecortan() {
        buscarYEsperar("  " + ClienteFalso.CEDULA + "  ");
        assertEquals("listarCuentasPorCliente(" + ClienteFalso.CEDULA + ",TOKEN-1)", servidor.llamadas.get(0));
        assertEquals(1, tabla.getRowCount());
    }

    @Test
    void unaBusquedaCorrectaLlenaLaTablaConLosDatosDelServidor() {
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals(1, tabla.getRowCount());
        assertEquals(ClienteFalso.CUENTA, tabla.getValueAt(0, 0));
        assertEquals("Soles", tabla.getValueAt(0, 1));
        assertEquals("6,800.00", tabla.getValueAt(0, 2));
        assertEquals("ACTIVO", tabla.getValueAt(0, 3));
        assertEquals("001", tabla.getValueAt(0, 4));
        assertEquals("2020-01-01", tabla.getValueAt(0, 5));
        assertEquals("7", tabla.getValueAt(0, 6));
    }

    @Test
    void elExitoMuestraElMensajeLiteralDelServidorEnVerde() {
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals("Cuentas consultadas correctamente", estado.getText());
        assertEquals(Tema.EXITO, estado.getForeground());
    }

    @Test
    void elTokenViajaDesdeLaSesion() {
        buscarYEsperar(ClienteFalso.CEDULA);
        assertTrue(servidor.llamadas.get(0).endsWith(",TOKEN-1)"));
    }

    @Test
    void clienteInexistenteMuestraElErrorDelServidorYDejaLaTablaVacia() {
        buscarYEsperar("99999999");
        assertEquals("El cliente solicitado no existe", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void unaBusquedaFallidaDespuesDeUnaBuenaVaciaLaTabla() {
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals(1, tabla.getRowCount());
        buscarYEsperar("99999999");
        // La tabla anterior sigue en el modelo hasta que llegue una respuesta correcta; el estado vacío la tapa.
        assertEquals("El cliente solicitado no existe", estado.getText());
    }

    @Test
    void elBotonDiceProcesandoMientrasEspera() throws InterruptedException {
        CountDownLatch entro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        armar(new ClienteFalso() {
            @Override
            public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
                entro.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.listarCuentasPorCliente(cedula, token);
            }
        }, true);
        Swing.enEdt(() -> {
            campo.setText(ClienteFalso.CEDULA);
            panel.buscar();
        });
        assertTrue(entro.await(5, TimeUnit.SECONDS));
        assertFalse(boton.isEnabled());
        assertEquals("Procesando", boton.getText());
        var esqueleto = Swing.primero(panel, PanelTarjeta.Esqueleto.class);
        assertTrue(esqueleto.getParent().isVisible(), "el esqueleto debe verse mientras carga");
        liberar.countDown();
        Swing.esperar(() -> boton.isEnabled());
        assertEquals("Buscar", boton.getText());
        assertFalse(esqueleto.getParent().isVisible(), "la carta del esqueleto debe ocultarse");
    }

    @Test
    void unDobleClicEnviaUnaSolaPeticion() throws InterruptedException {
        CountDownLatch entro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        armar(new ClienteFalso() {
            @Override
            public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
                entro.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.listarCuentasPorCliente(cedula, token);
            }
        }, true);
        Swing.enEdt(() -> {
            campo.setText(ClienteFalso.CEDULA);
            panel.buscar();
            panel.buscar();
            boton.doClick();
        });
        assertTrue(entro.await(5, TimeUnit.SECONDS));
        liberar.countDown();
        Swing.esperar(() -> boton.isEnabled());
        assertEquals(1, servidor.llamadas.size());
    }

    @Test
    void sesionPerdidaAvisaAlLoginConElMensajeDelServidor() {
        servidor.perderSesion();
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals(List.of("El token de sesión no es válido"), avisosDeSesion);
        assertFalse(sesion.estaVigente());
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void tokenExpiradoAMitadDeLaSesionAvisaAlLoginConElMensajeDelServidor() {
        buscarYEsperar(ClienteFalso.CEDULA);
        assertTrue(avisosDeSesion.isEmpty());
        servidor.vencerSesion();
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals(List.of("El token de sesión ha expirado, inicie sesión nuevamente"), avisosDeSesion);
        assertFalse(sesion.estaVigente());
    }

    @Test
    void sinSesionLocalAvisaAlLoginSinLlamarAlServidor() {
        armar(new ClienteFalso(), false);
        buscarYEsperar(ClienteFalso.CEDULA);
        assertTrue(servidor.llamadas.isEmpty());
        assertEquals(1, avisosDeSesion.size());
    }

    @Test
    void sinManejadorDeSesionCaducadaNoFalla() {
        servidor.perderSesion();
        Swing.enEdt(() -> panel = new PanelCuentas(new ControladorCuentas(servidor, sesion), null));
        campo = Swing.buscar(panel, "campoCedula", JTextField.class);
        boton = Swing.buscar(panel, "botonBuscar", JButton.class);
        buscarYEsperar(ClienteFalso.CEDULA);
        assertTrue(boton.isEnabled());
    }

    @Test
    void servidorApagadoMuestraSer001YLaAplicacionSigueViva() {
        servidor.servidorApagado = true;
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals("El servicio no está disponible en este momento", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(boton.isEnabled());
        assertTrue(avisosDeSesion.isEmpty());
        servidor.servidorApagado = false;
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals("Cuentas consultadas correctamente", estado.getText());
    }

    @Test
    void unaRespuestaNulaDelClienteSeMuestraComoErrorInterno() {
        armar(new ClienteFalso() {
            @Override
            public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
                return null;
            }
        }, true);
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals("Ocurrió un error interno, intente nuevamente", estado.getText());
        assertTrue(boton.isEnabled());
    }

    @Test
    void unaExcepcionInesperadaSeMuestraComoErrorInterno() {
        armar(new ClienteFalso() {
            @Override
            public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
                throw new IllegalStateException("detalle interno");
            }
        }, true);
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals("Ocurrió un error interno, intente nuevamente", estado.getText());
        assertFalse(estado.getText().contains("detalle"));
    }

    @Test
    void unaListaVaciaMuestraElEstadoVacioYNoFilas() {
        armar(new ClienteFalso() {
            @Override
            public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
                RespuestaCuentas r = new RespuestaCuentas("OK", "200", "Cuentas consultadas correctamente");
                r.setCuentas(new ArrayList<>());
                return r;
            }
        }, true);
        buscarYEsperar(ClienteFalso.CEDULA);
        assertEquals(0, tabla.getRowCount());
        assertEquals("Cuentas consultadas correctamente", estado.getText());
    }

    @Test
    void llenarTablaConNuloNoFalla() {
        Swing.enEdt(() -> panel.llenarTabla(null));
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void llenarTablaConListaNulaNoFalla() {
        RespuestaCuentas r = new RespuestaCuentas();
        r.setCuentas(null);
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals(0, tabla.getRowCount());
    }

    @Test
    void llenarTablaReemplazaLasFilasAnteriores() {
        RespuestaCuentas r = respuestaCon("1.00", "2.00", "3.00");
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals(3, tabla.getRowCount());
        Swing.enEdt(() -> panel.llenarTabla(respuestaCon("9.00")));
        assertEquals(1, tabla.getRowCount());
    }

    @Test
    void unaCuentaConCamposNulosSeMuestraConVaciosYCeros() {
        RespuestaCuentas r = new RespuestaCuentas("OK", "200", "ok");
        r.setCuentas(List.of(new CuentaDTO()));
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals("", tabla.getValueAt(0, 0));
        assertEquals("0.00", tabla.getValueAt(0, 2));
        assertEquals("0", tabla.getValueAt(0, 6));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"0;0.00", "0.005;0.01", "0.004;0.00", "6800;6,800.00", "100;100.00",
            "999.999;1,000.00", "1234567.891;1,234,567.89", "-5;-5.00", "1000000;1,000,000.00", "12.5;12.50",
            "0.1;0.10"})
    void losImportesDeLaTablaTienenDosDecimalesYSeparadorDeMiles(String entrada, String esperado) {
        Swing.enEdt(() -> panel.llenarTabla(respuestaCon(entrada)));
        assertEquals(esperado, tabla.getValueAt(0, 2));
    }

    @Test
    void unSaldoNuloSeMuestraComoCero() {
        RespuestaCuentas r = respuestaCon("1");
        r.getCuentas().get(0).setSaldo(null);
        Swing.enEdt(() -> panel.llenarTabla(r));
        assertEquals("0.00", tabla.getValueAt(0, 2));
    }

    @Test
    void enterEnElCampoBusca() {
        Swing.enEdt(() -> {
            campo.setText(ClienteFalso.CEDULA);
            campo.postActionEvent();
        });
        Swing.esperar(() -> boton.isEnabled());
        assertEquals(1, servidor.llamadas.size());
    }

    @Test
    void mostrarEstadoPintaErrorYExito() {
        Swing.enEdt(() -> panel.mostrarEstado("Hola", true));
        assertEquals(Tema.ERROR, estado.getForeground());
        Swing.enEdt(() -> panel.mostrarEstado("Hola", false));
        assertEquals(Tema.EXITO, estado.getForeground());
        Swing.enEdt(() -> panel.mostrarEstado(null, false));
        assertEquals("", estado.getText());
    }

    @Test
    void laEtiquetaDelCampoEstaEncimaYNoEsUnMarcador() {
        var etiqueta = Swing.todos(panel).stream()
                .filter(c -> c instanceof JLabel l && "Cédula del cliente".equals(l.getText()))
                .map(c -> (JLabel) c).findFirst().orElseThrow();
        assertEquals(campo, etiqueta.getLabelFor());
        assertNull(campo.getClientProperty("JTextField.placeholderText"));
    }

    private static RespuestaCuentas respuestaCon(String... saldos) {
        RespuestaCuentas r = new RespuestaCuentas("OK", "200", "ok");
        List<CuentaDTO> cuentas = new ArrayList<>();
        int i = 0;
        for (String saldo : saldos) {
            CuentaDTO c = new CuentaDTO();
            c.setCodigo("0000000" + (++i));
            c.setSaldo(new BigDecimal(saldo));
            cuentas.add(c);
        }
        r.setCuentas(cuentas);
        return r;
    }
}
