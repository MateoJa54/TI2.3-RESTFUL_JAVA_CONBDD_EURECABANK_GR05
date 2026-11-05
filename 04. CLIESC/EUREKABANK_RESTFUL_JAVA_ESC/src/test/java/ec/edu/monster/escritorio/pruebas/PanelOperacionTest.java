package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.RespuestaMovimiento;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import ec.edu.monster.escritorio.vista.PanelOperacion;
import ec.edu.monster.escritorio.vista.Tema;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Depósito, retiro y transferencia: validación local, montos borde, resultados y sesión caducada. */
class PanelOperacionTest {

    private static final int DEPOSITO = 0;
    private static final int RETIRO = 1;
    private static final int TRANSFERENCIA = 2;

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private PanelOperacion panel;
    private JComboBox<?> modo;
    private JTextField cuenta;
    private JTextField destino;
    private JTextField monto;
    private JButton boton;
    private JLabel estado;
    private List<String> avisosDeSesion;

    @BeforeAll
    static void tema() {
        Swing.enEdt(Tema::instalar);
    }

    @BeforeEach
    void preparar() {
        armar(new ClienteFalso());
    }

    private void armar(ClienteFalso falso) {
        servidor = falso;
        sesion = new SesionUsuario();
        new ControladorAutenticacion(servidor, sesion).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
        avisosDeSesion = new ArrayList<>();
        Swing.enEdt(() -> panel = new PanelOperacion(new ControladorMovimientos(servidor, sesion), avisosDeSesion::add));
        modo = Swing.buscar(panel, "selectorModo", JComboBox.class);
        cuenta = Swing.buscar(panel, "campoCuenta", JTextField.class);
        destino = Swing.buscar(panel, "campoCuentaDestino", JTextField.class);
        monto = Swing.buscar(panel, "campoMonto", JTextField.class);
        boton = Swing.buscar(panel, "botonEjecutar", JButton.class);
        estado = Swing.buscar(panel, "etiquetaEstado", JLabel.class);
    }

    private void ejecutar(int m, String c, String d, String importe) {
        Swing.enEdt(() -> {
            modo.setSelectedIndex(m);
            cuenta.setText(c);
            destino.setText(d);
            monto.setText(importe);
            panel.ejecutar();
        });
        Swing.esperar(() -> boton.isEnabled());
    }

    private String valor(String etiqueta) {
        return Swing.buscar(panel, "valor." + etiqueta, JLabel.class).getText();
    }

    @Test
    void elSelectorTieneLasTresOperaciones() {
        assertEquals(3, modo.getItemCount());
        assertEquals("Depósito", modo.getItemAt(0));
        assertEquals("Retiro", modo.getItemAt(1));
        assertEquals("Transferencia", modo.getItemAt(2));
    }

    @Test
    void elBotonDiceEjecutar() {
        assertEquals("Ejecutar", boton.getText());
    }

    @Test
    void laCuentaDeDestinoSoloSeVeEnLaTransferencia() {
        assertFalse(destino.getParent().isVisible());
        Swing.enEdt(() -> modo.setSelectedIndex(TRANSFERENCIA));
        assertTrue(destino.getParent().isVisible());
        Swing.enEdt(() -> modo.setSelectedIndex(RETIRO));
        assertFalse(destino.getParent().isVisible());
    }

    @Test
    void mostrarDestinoCambiaLaEtiquetaDeLaCuenta() {
        Swing.enEdt(() -> panel.mostrarDestino(true));
        assertTrue(destino.getParent().isVisible());
        assertTrue(Swing.todos(panel).stream().anyMatch(c -> c instanceof JLabel l && "Cuenta de origen".equals(l.getText())));
        Swing.enEdt(() -> panel.mostrarDestino(false));
        assertFalse(destino.getParent().isVisible());
        assertTrue(Swing.todos(panel).stream().anyMatch(c -> c instanceof JLabel l && "Código de la cuenta".equals(l.getText())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "0020 0002", "123456789012345678901"})
    void cuentaInvalidaNoLlamaAlServicio(String c) {
        ejecutar(DEPOSITO, c, "", "10");
        assertFalse(estado.getText().isEmpty());
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(servidor.llamadas.isEmpty());
        assertTrue(boton.isEnabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "0010 0002", "123456789012345678901"})
    void destinoInvalidoEnTransferenciaNoLlamaAlServicio(String d) {
        ejecutar(TRANSFERENCIA, ClienteFalso.CUENTA, d, "10");
        assertFalse(estado.getText().isEmpty());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void elDestinoSeIgnoraEnDepositoYRetiro() {
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "destino con espacios", "10");
        assertEquals(1, servidor.llamadas.size());
        ejecutar(RETIRO, ClienteFalso.CUENTA, "otro destino", "10");
        assertEquals(2, servidor.llamadas.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "-5", "-0.01", "0", "0.00", "0,00", "1.234", "1,234", "1e3", "1E3", "1e-2",
            "NaN", "nan", "Infinity", "-Infinity", "--5", "1,2,3", "1.2.3", "12abc", "$5", "0x10", "5 0", "٣", "+", ".", ",",
            "10.001", "0.001"})
    void montosInvalidosMuestranElMensajeYNoLlamanAlServicio(String texto) {
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", texto);
        assertEquals("El monto debe ser mayor que cero y con máximo dos decimales", estado.getText(), "monto '" + texto + "'");
        assertEquals(Tema.ERROR, estado.getForeground());
        assertTrue(servidor.llamadas.isEmpty(), "no debía llamar con '" + texto + "'");
        assertTrue(boton.isEnabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "99999999999999999999", "100000000000000000000000000000.00"})
    void montosMuyGrandesSeRechazanLocalmente(String texto) {
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", texto);
        assertEquals("El monto no puede tener más de 10 dígitos enteros", estado.getText());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"10,10.00", "10.5,10.50", "10;5,10.50", "0.01,0.01", "'  25.00  ',25.00", ".5,0.50", "5.,5.00",
            "1234567890.99,1234567890.99", "+7,7.00", "3.10,3.10", "3.100,3.10", "0010,10.00", "1.0,1.00"},
            delimiter = ',')
    void montosValidosLlegaranAlServicioConDosDecimales(String texto, String enviado) {
        String entrada = texto.replace(';', ',');
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", entrada);
        assertEquals(1, servidor.llamadas.size(), "monto '" + entrada + "'");
        assertEquals("realizarDeposito(" + ClienteFalso.CUENTA + "," + enviado + ",TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void elMontoConComaDecimalSeEnvia() {
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "7,25");
        assertEquals("realizarRetiro(" + ClienteFalso.CUENTA + ",7.25,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void unDepositoCorrectoMuestraElDetalleYElMensajeDelServidor() {
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", "100");
        assertEquals("Depósito realizado correctamente", estado.getText());
        assertEquals(Tema.EXITO, estado.getForeground());
        assertEquals("8", valor("Movimiento"));
        assertEquals("2026-11-03", valor("Fecha"));
        assertEquals("100.00", valor("Importe"));
        assertEquals("6,900.00", valor("Saldo nuevo"));
        assertEquals("0.00", valor("Costo aplicado"));
        assertEquals("0.00", valor("ITF aplicado"));
    }

    @Test
    void unRetiroCorrectoMuestraElDetalle() {
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "50");
        assertEquals("Retiro realizado correctamente", estado.getText());
        assertEquals("6,750.00", valor("Saldo nuevo"));
        assertEquals("9", valor("Movimiento"));
    }

    @Test
    void unRetiroMayorAlSaldoMuestraSal001() {
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "20000");
        assertEquals("El saldo de la cuenta es insuficiente para la operación", estado.getText());
        assertEquals(Tema.ERROR, estado.getForeground());
    }

    @Test
    void unaTransferenciaCorrectaMuestraLosDosSaldos() {
        ejecutar(TRANSFERENCIA, ClienteFalso.CUENTA, "00200003", "25");
        assertEquals("Transferencia realizada correctamente", estado.getText());
        assertEquals("realizarTransferencia(" + ClienteFalso.CUENTA + ",00200003,25.00,TOKEN-1)", servidor.llamadas.get(0));
        assertEquals("10", valor("Movimiento de origen"));
        assertEquals("25", valor("Movimiento de destino"));
        assertEquals("6,775.00", valor("Saldo de origen"));
        assertEquals("7,429.00", valor("Saldo de destino"));
        assertEquals("0.02", valor("ITF aplicado"));
    }

    @Test
    void transferirALaMismaCuentaMuestraCta003DelServidor() {
        ejecutar(TRANSFERENCIA, ClienteFalso.CUENTA, ClienteFalso.CUENTA, "25");
        assertEquals("La cuenta origen y la cuenta destino no pueden ser iguales", estado.getText());
        assertEquals(1, servidor.llamadas.size());
    }

    @Test
    void transferirAOtraMonedaMuestraCta004DelServidor() {
        ejecutar(TRANSFERENCIA, ClienteFalso.CUENTA, ClienteFalso.CUENTA_DOLARES, "25");
        assertEquals("La cuenta de destino pertenece a una moneda distinta", estado.getText());
    }

    @Test
    void unMontoMalEscritoNoEnviaNingunaOperacionDeTransferencia() {
        ejecutar(TRANSFERENCIA, ClienteFalso.CUENTA, "00200003", "-25");
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void sesionPerdidaAvisaAlLoginConElMensajeDelServidor() {
        servidor.perderSesion();
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", "10");
        assertEquals(List.of("El token de sesión no es válido"), avisosDeSesion);
        assertFalse(sesion.estaVigente());
    }

    @Test
    void tokenExpiradoAMitadDeLaSesionAvisaAlLoginEnCadaModo() {
        servidor.vencerSesion();
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "10");
        assertEquals(List.of("El token de sesión ha expirado, inicie sesión nuevamente"), avisosDeSesion);
    }

    @Test
    void sesionPerdidaEnTransferenciaAvisaAlLogin() {
        servidor.perderSesion();
        ejecutar(TRANSFERENCIA, ClienteFalso.CUENTA, "00200003", "10");
        assertEquals(1, avisosDeSesion.size());
    }

    @Test
    void servidorApagadoMuestraSer001EnLosTresModos() {
        servidor.servidorApagado = true;
        for (int m : new int[] {DEPOSITO, RETIRO, TRANSFERENCIA}) {
            ejecutar(m, ClienteFalso.CUENTA, "00200003", "10");
            assertEquals("El servicio no está disponible en este momento", estado.getText());
            assertEquals("Ejecutar", boton.getText());
            assertTrue(boton.isEnabled());
        }
        assertTrue(avisosDeSesion.isEmpty());
    }

    @Test
    void dobleClicEjecutaUnaSolaOperacion() throws InterruptedException {
        CountDownLatch entro = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        armar(new ClienteFalso() {
            @Override
            public RespuestaMovimiento realizarDeposito(String c, java.math.BigDecimal m, String token) {
                entro.countDown();
                try {
                    liberar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.realizarDeposito(c, m, token);
            }
        });
        Swing.enEdt(() -> {
            cuenta.setText(ClienteFalso.CUENTA);
            monto.setText("10");
            panel.ejecutar();
            panel.ejecutar();
            boton.doClick();
        });
        assertTrue(entro.await(5, TimeUnit.SECONDS));
        assertFalse(boton.isEnabled());
        assertEquals("Procesando", boton.getText());
        liberar.countDown();
        Swing.esperar(() -> boton.isEnabled());
        assertEquals(1, servidor.llamadas.size());
        assertEquals("Ejecutar", boton.getText());
    }

    @Test
    void unaExcepcionInesperadaSeMuestraComoErrorInterno() {
        armar(new ClienteFalso() {
            @Override
            public RespuestaMovimiento realizarRetiro(String c, java.math.BigDecimal m, String token) {
                throw new IllegalStateException("secreto interno");
            }
        });
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "10");
        assertEquals("Ocurrió un error interno, intente nuevamente", estado.getText());
        assertTrue(boton.isEnabled());
    }

    @Test
    void unaRespuestaNulaSeMuestraComoErrorInterno() {
        armar(new ClienteFalso() {
            @Override
            public RespuestaMovimiento realizarDeposito(String c, java.math.BigDecimal m, String token) {
                return null;
            }
        });
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", "10");
        assertEquals("Ocurrió un error interno, intente nuevamente", estado.getText());
    }

    @Test
    void unaRespuestaOkSinMovimientoNoRompeElDetalle() {
        armar(new ClienteFalso() {
            @Override
            public RespuestaMovimiento realizarDeposito(String c, java.math.BigDecimal m, String token) {
                return new RespuestaMovimiento("OK", "200", "Depósito realizado correctamente");
            }
        });
        ejecutar(DEPOSITO, ClienteFalso.CUENTA, "", "10");
        assertEquals("Depósito realizado correctamente", estado.getText());
        assertEquals("", valor("Movimiento"));
        assertEquals("0.00", valor("Saldo nuevo"));
    }

    @Test
    void elMensajeDeErrorSeLimpiaAlEjecutarDeNuevo() {
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "20000");
        assertFalse(estado.getText().isEmpty());
        ejecutar(RETIRO, ClienteFalso.CUENTA, "", "10");
        assertEquals("Retiro realizado correctamente", estado.getText());
    }

    @Test
    void enterEnElMontoEjecuta() {
        Swing.enEdt(() -> {
            cuenta.setText(ClienteFalso.CUENTA);
            monto.setText("10");
            monto.postActionEvent();
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

    @Test
    void laCuentaConEspaciosAlrededorSeRecorta() {
        ejecutar(DEPOSITO, "  " + ClienteFalso.CUENTA + "  ", "", "10");
        assertEquals("realizarDeposito(" + ClienteFalso.CUENTA + ",10.00,TOKEN-1)", servidor.llamadas.get(0));
    }
}
