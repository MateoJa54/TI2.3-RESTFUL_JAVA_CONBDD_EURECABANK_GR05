package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Respuesta de las operaciones {@code realizarDeposito} y {@code realizarRetiro}:
 * extiende {@link RespuestaBase} con el efecto de la operacion.
 */
public class RespuestaMovimiento extends RespuestaBase {

    /** Movimiento registrado por la operacion. */
    private MovimientoDTO movimiento;

    /** Saldo de la cuenta despues de la operacion. */
    private BigDecimal saldoNuevo;

    /** Costo por movimiento aplicado. */
    private BigDecimal costoAplicado;

    /** Impuesto a las transacciones aplicado. */
    private BigDecimal itfAplicado;

    public RespuestaMovimiento() {
    }

    /**
     * Constructor completo.
     *
     * @param estado       {@code OK} o {@code ERROR}
     * @param codigo       codigo de resultado del catalogo
     * @param mensaje      mensaje literal del catalogo
     * @param movimiento   movimiento registrado
     * @param saldoNuevo   saldo posterior a la operacion
     * @param costoAplicado costo por movimiento aplicado
     * @param itfAplicado  impuesto aplicado
     */
    public RespuestaMovimiento(String estado, String codigo, String mensaje,
            MovimientoDTO movimiento, BigDecimal saldoNuevo,
            BigDecimal costoAplicado, BigDecimal itfAplicado) {
        super(estado, codigo, mensaje);
        this.movimiento = movimiento;
        this.saldoNuevo = saldoNuevo;
        this.costoAplicado = costoAplicado;
        this.itfAplicado = itfAplicado;
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
