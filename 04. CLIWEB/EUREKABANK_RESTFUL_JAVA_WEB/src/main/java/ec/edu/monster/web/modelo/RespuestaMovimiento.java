package ec.edu.monster.web.modelo;

import java.math.BigDecimal;

/**
 * Respuesta de las operaciones {@code realizarDeposito} y
 * {@code realizarRetiro}: extiende {@link RespuestaBase} con el efecto de la
 * operación.
 */
public class RespuestaMovimiento extends RespuestaBase {

    /** Movimiento registrado por la operación. */
    private MovimientoDTO movimiento;

    /** Saldo de la cuenta después de la operación. */
    private BigDecimal saldoNuevo;

    /** Costo por movimiento aplicado. */
    private BigDecimal costoAplicado;

    /** Impuesto a las transacciones aplicado. */
    private BigDecimal itfAplicado;

    public RespuestaMovimiento() {
    }

    public RespuestaMovimiento(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
    }

    public MovimientoDTO getMovimiento() {
        return movimiento;
    }

    public void setMovimiento(MovimientoDTO movimiento) {
        this.movimiento = movimiento;
    }

    public BigDecimal getSaldoNuevo() {
        return saldoNuevo;
    }

    public void setSaldoNuevo(BigDecimal saldoNuevo) {
        this.saldoNuevo = saldoNuevo;
    }

    public BigDecimal getCostoAplicado() {
        return costoAplicado;
    }

    public void setCostoAplicado(BigDecimal costoAplicado) {
        this.costoAplicado = costoAplicado;
    }

    public BigDecimal getItfAplicado() {
        return itfAplicado;
    }

    public void setItfAplicado(BigDecimal itfAplicado) {
        this.itfAplicado = itfAplicado;
    }
}
