package ec.edu.monster.modelos;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de la operacion {@code consultarMovimientos}: extiende
 * {@link RespuestaBase} con el libro de movimientos de una cuenta.
 */
public class RespuestaMovimientos extends RespuestaBase {

    /** Codigo de la cuenta cuyos movimientos se consultan. */
    private String cuenta;

    /** Lista de movimientos de la cuenta. */
    private List<MovimientoDTO> movimientos = new ArrayList<>();

    public RespuestaMovimientos() {
    }

    /**
     * Constructor completo.
     *
     * @param estado      {@code OK} o {@code ERROR}
     * @param codigo      codigo de resultado del catalogo
     * @param mensaje     mensaje literal del catalogo
     * @param cuenta      codigo de la cuenta
     * @param movimientos lista de movimientos
     */
    public RespuestaMovimientos(String estado, String codigo, String mensaje,
            String cuenta, List<MovimientoDTO> movimientos) {
        super(estado, codigo, mensaje);
        this.cuenta = cuenta;
        this.movimientos = movimientos;
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
