package ec.edu.monster.consola.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de la operación {@code consultarMovimientos}: extiende
 * {@link RespuestaBase} con el libro de movimientos de una cuenta.
 */
public class RespuestaMovimientos extends RespuestaBase {

    /** Código de la cuenta cuyos movimientos se consultan. */
    private String cuenta;

    /** Lista de movimientos de la cuenta. */
    private List<MovimientoDTO> movimientos = new ArrayList<>();

    public RespuestaMovimientos() {
    }

    public RespuestaMovimientos(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
    }

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public List<MovimientoDTO> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<MovimientoDTO> movimientos) {
        this.movimientos = movimientos;
    }
}
