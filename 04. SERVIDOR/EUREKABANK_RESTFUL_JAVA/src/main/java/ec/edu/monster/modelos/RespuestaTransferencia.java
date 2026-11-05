package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Respuesta de la operacion {@code realizarTransferencia}: extiende
 * {@link RespuestaBase} con el detalle de la transferencia.
 */
public class RespuestaTransferencia extends RespuestaBase {

    /** Numero del movimiento de salida en la cuenta origen. */
    private Integer numeroOrigen;

    /** Numero del movimiento de ingreso en la cuenta destino. */
    private Integer numeroDestino;

    /** Saldo de la cuenta origen despues de la transferencia. */
    private BigDecimal saldoOrigen;

    /** Saldo de la cuenta destino despues de la transferencia. */
    private BigDecimal saldoDestino;

    /** Impuesto a las transacciones descontado en la cuenta origen. */
    private BigDecimal itfAplicado;

    public RespuestaTransferencia() {
    }

    /**
     * Constructor completo.
     *
     * @param estado        {@code OK} o {@code ERROR}
     * @param codigo        codigo de resultado del catalogo
     * @param mensaje       mensaje literal del catalogo
     * @param numeroOrigen  numero de movimiento en la cuenta origen
     * @param numeroDestino numero de movimiento en la cuenta destino
     * @param saldoOrigen   saldo final de la cuenta origen
     * @param saldoDestino  saldo final de la cuenta destino
     * @param itfAplicado   impuesto aplicado
     */
    public RespuestaTransferencia(String estado, String codigo, String mensaje,
            Integer numeroOrigen, Integer numeroDestino,
            BigDecimal saldoOrigen, BigDecimal saldoDestino, BigDecimal itfAplicado) {
        super(estado, codigo, mensaje);
        this.numeroOrigen = numeroOrigen;
        this.numeroDestino = numeroDestino;
        this.saldoOrigen = saldoOrigen;
        this.saldoDestino = saldoDestino;
        this.itfAplicado = itfAplicado;
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
