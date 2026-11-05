package ec.edu.monster.consola.vista;

import ec.edu.monster.consola.controlador.ControladorMovimientos;
import ec.edu.monster.consola.modelo.MovimientoDTO;
import ec.edu.monster.consola.modelo.RespuestaMovimiento;
import ec.edu.monster.consola.modelo.RespuestaMovimientos;
import ec.edu.monster.consola.modelo.RespuestaTransferencia;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.Formato;
import ec.edu.monster.consola.util.Teclado;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de movimientos (CU-04 a CU-07): consulta, depósito, retiro y
 * transferencia.
 */
public class VistaMovimientos {

    private final ControladorMovimientos controlador;
    private final Teclado teclado;
    private final Consola consola;

    /**
     * @param controlador controlador de movimientos, obligatorio
     * @param teclado     teclado de la consola, obligatorio
     * @param consola     salida de la consola, obligatoria
     */
    public VistaMovimientos(ControladorMovimientos controlador, Teclado teclado, Consola consola) {
        this.controlador = controlador;
        this.teclado = teclado;
        this.consola = consola;
    }

    /**
     * Pide una cuenta y muestra su libro de movimientos.
     */
    public void consultarMovimientos() {
        String cuenta = teclado.texto("Código de la cuenta");
        if (cuenta.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        RespuestaMovimientos respuesta = controlador.consultarMovimientos(cuenta);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudieron consultar los movimientos: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        consola.titulo("MOVIMIENTOS DE LA CUENTA " + Formato.texto(respuesta.getCuenta()));
        List<MovimientoDTO> movimientos = respuesta.getMovimientos();
        if (movimientos == null || movimientos.isEmpty()) {
            consola.linea("La cuenta no tiene movimientos registrados.");
            return;
        }
        List<String[]> filas = new ArrayList<>();
        for (MovimientoDTO movimiento : movimientos) {
            filas.add(new String[] {
                    Formato.entero(movimiento.getNumero()),
                    Formato.texto(movimiento.getFecha()),
                    Formato.texto(movimiento.getTipoCodigo()) + " " + Formato.texto(movimiento.getTipoDescripcion()),
                    Formato.texto(movimiento.getTipoAccion()),
                    Formato.monto(movimiento.getImporte()),
                    Formato.texto(movimiento.getReferencia())});
        }
        consola.tabla(new String[] {"NUMERO", "FECHA", "TIPO", "ACCION", "IMPORTE", "REFERENCIA"}, filas);
    }

    /**
     * Pide una cuenta y un monto y realiza un depósito.
     */
    public void depositar() {
        String cuenta = teclado.texto("Código de la cuenta");
        if (cuenta.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        BigDecimal monto = teclado.monto("Monto del depósito");
        RespuestaMovimiento respuesta = controlador.depositar(cuenta, monto);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudo realizar el depósito: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        mostrarEfecto("DEPÓSITO", respuesta);
    }

    /**
     * Pide una cuenta y un monto y realiza un retiro.
     */
    public void retirar() {
        String cuenta = teclado.texto("Código de la cuenta");
        if (cuenta.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        BigDecimal monto = teclado.monto("Monto del retiro");
        RespuestaMovimiento respuesta = controlador.retirar(cuenta, monto);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudo realizar el retiro: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        mostrarEfecto("RETIRO", respuesta);
    }

    /**
     * Pide las dos cuentas y el monto y realiza una transferencia.
     */
    public void transferir() {
        String origen = teclado.texto("Cuenta origen");
        if (origen.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        String destino = teclado.texto("Cuenta destino");
        if (destino.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        BigDecimal monto = teclado.monto("Monto de la transferencia");
        RespuestaTransferencia respuesta = controlador.transferir(origen, destino, monto);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudo realizar la transferencia: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        consola.titulo("TRANSFERENCIA");
        consola.linea("  Movimiento origen:  " + Formato.entero(respuesta.getNumeroOrigen()));
        consola.linea("  Movimiento destino: " + Formato.entero(respuesta.getNumeroDestino()));
        consola.linea("  Saldo origen:       " + Formato.monto(respuesta.getSaldoOrigen()));
        consola.linea("  Saldo destino:      " + Formato.monto(respuesta.getSaldoDestino()));
        consola.linea("  ITF aplicado:        " + Formato.monto(respuesta.getItfAplicado()));
    }

    private void mostrarEfecto(String titulo, RespuestaMovimiento respuesta) {
        consola.titulo(titulo);
        MovimientoDTO movimiento = respuesta.getMovimiento();
        if (movimiento != null) {
            consola.linea("  Movimiento:   " + Formato.entero(movimiento.getNumero())
                    + " - " + Formato.texto(movimiento.getTipoDescripcion()));
            consola.linea("  Fecha:        " + Formato.texto(movimiento.getFecha()));
            consola.linea("  Importe:      " + Formato.monto(movimiento.getImporte()));
        }
        consola.linea("  Saldo nuevo:  " + Formato.monto(respuesta.getSaldoNuevo()));
        consola.linea("  Costo:        " + Formato.monto(respuesta.getCostoAplicado()));
        consola.linea("  ITF:          " + Formato.monto(respuesta.getItfAplicado()));
    }
}
