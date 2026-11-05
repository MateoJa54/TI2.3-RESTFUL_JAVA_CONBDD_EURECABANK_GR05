package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Cuerpo de la peticion de transferencia ({@code POST /api/transferencias}).
 *
 * <p>POJO plano: solo atributos, constructor vacio, constructor completo,
 * getters y setters.
 */
public class TransferenciaRequest {

    /** Cuenta que envia el dinero. */
    private String cuentaOrigen;

    /** Cuenta que recibe el dinero. */
    private String cuentaDestino;

    /** Importe de la transferencia con dos decimales. */
    private BigDecimal monto;

    public TransferenciaRequest() {
    }

    /**
     * Constructor completo.
     *
     * @param cuentaOrigen  cuenta que envia el dinero
     * @param cuentaDestino cuenta que recibe el dinero
     * @param monto         importe de la transferencia
     */
    public TransferenciaRequest(String cuentaOrigen, String cuentaDestino, BigDecimal monto) {
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}
