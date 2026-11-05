package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorCostos;
import ec.edu.monster.escritorio.modelo.RespuestaCostos;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelCostos;
import ec.edu.monster.escritorio.vista.Tema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Proyección de costos: validación, detalle, estados y sesión caducada. */
class PanelCostosTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private PanelCostos panel;
    private JTextField campo;
    private JButton boton;
    private JLabel estado;
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
        Swing.enEdt(() -> panel = new PanelCostos(new ControladorCostos(servidor, sesion), avisosDeSesion::add));
        campo = Swing.buscar(panel, "campoCuenta", JTextField.class);
        boton = Swing.buscar(panel, "botonConsultar", JButton.class);
        estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
    }

    private void consultarYEsperar(String cuenta) {
        Swing.enEdt(() -> {
            campo.setText(cuenta);
            panel.consultar();
        });
        Swing.esperar(() -> boton.isEnabled());
    }

    private String valor(String etiqueta) {
        return Swing.buscar(panel, "valor." + etiqueta, JLabel.class).getText();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "0020 0002", "123456789012345678901"})
    void cuentasInvalidasNoLlamanAlServicio(String cuenta) {
        Swing.enEdt(() -> {
            campo.setText(cuenta);
            panel.consultar();
        });
        assertFalse(estado.getText().isEmpty());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(servidor.llamadas.isEmpty());
        assertTrue(boton.isEnabled());
    }

    @Test
    void unaConsultaCorrectaMuestraElDetalleConDosDecimales() {
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("6,800.00", valor("Saldo actual"));
        assertEquals("5.00", valor("Costo de mantenimiento"));
        assertEquals("0.50", valor("Costo por movimiento"));
        assertEquals("54.40", valor("Interés proyectado"));
        assertEquals("100.00", valor("Límite de mantenimiento"));
        assertEquals("15", valor("Operaciones sin costo"));
    }

    @Test
    void elExitoMuestraElMensajeDelServidor() {
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("Costos de mantenimiento consultados correctamente", estado.getText());
        assertEquals(Tema.EXITO, estado.getForeground());
    }

    @Test
    void cuentaInexistenteMuestraElErrorDelServidor() {
        consultarYEsperar("99999999");
        assertEquals("La cuenta solicitada no existe", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
    }

    @Test
    void sesionPerdidaAvisaAlLogin() {
        servidor.perderSesion();
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals(List.of("El token de sesión no es válido"), avisosDeSesion);
    }

    @Test
    void tokenExpiradoAvisaAlLogin() {
        servidor.vencerSesion();
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals(List.of("El token de sesión ha expirado, inicie sesión nuevamente"), avisosDeSesion);
    }

    @Test
    void servidorApagadoMuestraSer001() {
        servidor.servidorApagado = true;
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("El servicio no está disponible en este momento", estado.getText());
        assertEquals("Consultar", boton.getText());
        assertTrue(boton.isEnabled());
    }

    @Test
    void mostrarDetalleConValoresNulosMuestraCeros() {
        Swing.enEdt(() -> panel.mostrarDetalle(new RespuestaCostos()));
        assertEquals("0.00", valor("Saldo actual"));
        assertEquals("0", valor("Operaciones sin costo"));
    }

    @Test
    void mostrarDetalleDosVecesReemplazaElAnterior() {
        RespuestaCostos primera = new RespuestaCostos();
        primera.setSaldoActual(new BigDecimal("1"));
        RespuestaCostos segunda = new RespuestaCostos();
        segunda.setSaldoActual(new BigDecimal("2000000.505"));
        Swing.enEdt(() -> panel.mostrarDetalle(primera));
        Swing.enEdt(() -> panel.mostrarDetalle(segunda));
        assertEquals("2,000,000.51", valor("Saldo actual"));
        long etiquetas = Swing.todos(panel).stream()
                .filter(c -> c instanceof JLabel l && "valor.Saldo actual".equals(l.getName())).count();
        assertEquals(1, etiquetas);
    }

    @Test
    void elTokenViajaDesdeLaSesion() {
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("consultarCostosMantenimiento(" + ClienteFalso.CUENTA + ",TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void elBotonVuelveASuTextoDespuesDeConsultar() {
        consultarYEsperar(ClienteFalso.CUENTA);
        assertEquals("Consultar", boton.getText());
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
}
