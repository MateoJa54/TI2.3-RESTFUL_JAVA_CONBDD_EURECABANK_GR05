package ec.edu.monster.web.modelo;

import java.math.BigDecimal;

/**
 * Respuesta de la operación {@code realizarTransferencia}: extiende
 * {@link RespuestaBase} con el detalle de la transferencia.
 */
public class RespuestaTransferencia extends RespuestaBase {

    /** Número del movimiento de salida en la cuenta origen. */
    private Integer numeroOrigen;

    /** Número del movimiento de ingreso en la cuenta destino. */
    private Integer numeroDestino;

    /** Saldo de la cuenta origen después de la transferencia. */
    private BigDecimal saldoOrigen;

    /** Saldo de la cuenta destino después de la transferencia. */
    private BigDecimal saldoDestino;

    /** Impuesto a las transacciones descontado en la cuenta origen. */
    private BigDecimal itfAplicado;

    public RespuestaTransferencia() {
    }

    public RespuestaTransferencia(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
    }

    public Integer getNumeroOrigen() {
        return numeroOrigen;
    }

    public void setNumeroOrigen(Integer numeroOrigen) {
        this.numeroOrigen = numeroOrigen;
    }

    public Integer getNumeroDestino() {
        return numeroDestino;
    }

    public void setNumeroDestino(Integer numeroDestino) {
        this.numeroDestino = numeroDestino;
    }

    public BigDecimal getSaldoOrigen() {
        return saldoOrigen;
    }

    public void setSaldoOrigen(BigDecimal saldoOrigen) {
        this.saldoOrigen = saldoOrigen;
    }

    public BigDecimal getSaldoDestino() {
        return saldoDestino;
    }

    public void setSaldoDestino(BigDecimal saldoDestino) {
        this.saldoDestino = saldoDestino;
    }

    public BigDecimal getItfAplicado() {
        return itfAplicado;
    }

    public void setItfAplicado(BigDecimal itfAplicado) {
        this.itfAplicado = itfAplicado;
    }
}
