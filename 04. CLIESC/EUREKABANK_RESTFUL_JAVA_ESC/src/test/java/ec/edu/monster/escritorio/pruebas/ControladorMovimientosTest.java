package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.controlador.ControladorMovimientos;
import ec.edu.monster.escritorio.modelo.RespuestaMovimiento;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.modelo.RespuestaTransferencia;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Los movimientos: consulta, depósito, retiro y transferencia.
 */
class ControladorMovimientosTest {

    private ClienteFalso servidor;
    private SesionUsuario sesion;
    private ControladorMovimientos controlador;

    @BeforeEach
    void preparar() {
        servidor = new ClienteFalso();
        sesion = new SesionUsuario();
        controlador = new ControladorMovimientos(servidor, sesion);
        new ControladorAutenticacion(servidor, sesion)
                .autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
    }

    @Test
    void consultarMovimientosMandaLaCuentaYElToken() {
        controlador.consultarMovimientos(ClienteFalso.CUENTA);

        assertEquals("consultarMovimientos(00200002,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void consultarMovimientosDevuelveElLibroDeMovimientos() {
        RespuestaMovimientos respuesta = controlador.consultarMovimientos(ClienteFalso.CUENTA);

        assertEquals("OK", respuesta.getEstado());
        assertEquals(1, respuesta.getMovimientos().size());
        assertEquals("001", respuesta.getMovimientos().get(0).getTipoCodigo());
    }

    @Test
    void depositarMandaLaCuentaElMontoYElToken() {
        controlador.depositar(ClienteFalso.CUENTA, new BigDecimal("100.00"));

        assertEquals("realizarDeposito(00200002,100.00,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void depositarDevuelveElSaldoNuevo() {
        RespuestaMovimiento respuesta = controlador.depositar(ClienteFalso.CUENTA, new BigDecimal("100.00"));

        assertEquals("OK", respuesta.getEstado());
        assertEquals("6900.00", respuesta.getSaldoNuevo().toPlainString());
        assertEquals("003", respuesta.getMovimiento().getTipoCodigo());
    }

    @Test
    void retirarMandaLaCuentaElMontoYElToken() {
        controlador.retirar(ClienteFalso.CUENTA, new BigDecimal("50.00"));

        assertEquals("realizarRetiro(00200002,50.00,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void unRetiroSinSaldoSuficienteDevuelveElErrorDelServidor() {
        RespuestaMovimiento respuesta = controlador.retirar(ClienteFalso.CUENTA, new BigDecimal("50000.00"));

        assertEquals("SAL-001", respuesta.getCodigo());
        assertEquals("El saldo de la cuenta es insuficiente para la operación", respuesta.getMensaje());
    }

    @Test
    void transferirMandaLasDosCuentasElMontoYElToken() {
        controlador.transferir(ClienteFalso.CUENTA, "00100001", new BigDecimal("25.00"));

        assertEquals("realizarTransferencia(00200002,00100001,25.00,TOKEN-1)", servidor.llamadas.get(0));
    }

    @Test
    void unaTransferenciaDevuelveLosSaldosYElItf() {
        RespuestaTransferencia respuesta =
                controlador.transferir(ClienteFalso.CUENTA, "00100001", new BigDecimal("25.00"));

        assertEquals("OK", respuesta.getEstado());
        assertEquals(10, respuesta.getNumeroOrigen());
        assertEquals(25, respuesta.getNumeroDestino());
        assertEquals("6775.00", respuesta.getSaldoOrigen().toPlainString());
        assertEquals("0.02", respuesta.getItfAplicado().toPlainString());
    }

    @Test
    void unaTransferenciaALaMismaCuentaDevuelveElErrorDelServidor() {
        RespuestaTransferencia respuesta =
                controlador.transferir(ClienteFalso.CUENTA, ClienteFalso.CUENTA, new BigDecimal("25.00"));

        assertEquals("CTA-003", respuesta.getCodigo());
        assertEquals("La cuenta origen y la cuenta destino no pueden ser iguales",
                respuesta.getMensaje());
    }

    @Test
    void unaTransferenciaAMonedaDistintaDevuelveElErrorDelServidor() {
        RespuestaTransferencia respuesta =
                controlador.transferir(ClienteFalso.CUENTA, ClienteFalso.CUENTA_DOLARES, new BigDecimal("25.00"));

        assertEquals("CTA-004", respuesta.getCodigo());
        assertEquals("La cuenta de destino pertenece a una moneda distinta", respuesta.getMensaje());
    }

    @Test
    void sinSesionNingunaOperacionLlamaAlServidor() {
        ControladorMovimientos sinSesion = new ControladorMovimientos(servidor, new SesionUsuario());

        assertEquals("AUTH-003", sinSesion.consultarMovimientos(ClienteFalso.CUENTA).getCodigo());
        assertEquals("AUTH-003", sinSesion.depositar(ClienteFalso.CUENTA, BigDecimal.ONE).getCodigo());
        assertEquals("AUTH-003", sinSesion.retirar(ClienteFalso.CUENTA, BigDecimal.ONE).getCodigo());
        assertEquals("AUTH-003",
                sinSesion.transferir(ClienteFalso.CUENTA, "00100001", BigDecimal.ONE).getCodigo());
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void unMontoNuloSeRechaza() {
        assertThrows(NullPointerException.class, () -> controlador.depositar(ClienteFalso.CUENTA, null));
        assertThrows(NullPointerException.class, () -> controlador.retirar(ClienteFalso.CUENTA, null));
        assertThrows(NullPointerException.class,
                () -> controlador.transferir(ClienteFalso.CUENTA, "00100001", null));
    }

    @Test
    void siElTokenExpiraSeDescartaLaSesionLocal() {
        servidor.vencerSesion();

        RespuestaMovimientos respuesta = controlador.consultarMovimientos(ClienteFalso.CUENTA);

        assertEquals("AUTH-004", respuesta.getCodigo());
        assertFalse(controlador.haySesionActiva());
        assertNull(sesion.getToken());
    }

    @Test
    void lasDependenciasSonObligatorias() {
        assertThrows(NullPointerException.class, () -> new ControladorMovimientos(null, sesion));
        assertThrows(NullPointerException.class, () -> new ControladorMovimientos(servidor, null));
    }
}
