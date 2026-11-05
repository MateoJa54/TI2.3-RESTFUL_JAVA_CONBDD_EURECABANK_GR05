package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Cuerpo de las peticiones de movimiento de dinero ({@code POST
 * /api/cuentas/{cuenta}/depositos} y {@code POST /api/cuentas/{cuenta}/retiros}).
 *
 * <p>POJO plano: solo atributos, constructor vacio, constructor completo,
 * getters y setters. El importe viaja como {@code BigDecimal} (regla RB-01).
 */
public class MontoRequest {

    /** Importe de la operacion con dos decimales. */
    private BigDecimal monto;

    public MontoRequest() {
    }

    /**
     * Constructor completo.
     *
     * @param monto importe de la operacion
     */
    public MontoRequest(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}
