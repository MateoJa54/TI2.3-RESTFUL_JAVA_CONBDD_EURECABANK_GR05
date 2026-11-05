package ec.edu.monster.modelos;

/**
 * Respuesta de la operacion {@code consultarCuenta}: extiende
 * {@link RespuestaBase} con los datos de una cuenta.
 */
public class RespuestaCuenta extends RespuestaBase {

    /** Datos de la cuenta consultada. */
    private CuentaDTO cuenta;

    public RespuestaCuenta() {
    }

    /**
     * Constructor completo.
     *
     * @param estado  {@code OK} o {@code ERROR}
     * @param codigo  codigo de resultado del catalogo
     * @param mensaje mensaje literal del catalogo
     * @param cuenta  datos de la cuenta
     */
    public RespuestaCuenta(String estado, String codigo, String mensaje, CuentaDTO cuenta) {
        super(estado, codigo, mensaje);
        this.cuenta = cuenta;
    }

    public CuentaDTO getCuenta() {
        return cuenta;
    }

    public void setCuenta(CuentaDTO cuenta) {
        this.cuenta = cuenta;
    }
}
