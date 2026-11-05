package ec.edu.monster.consola.modelo;

/**
 * Respuesta de la operación {@code consultarCuenta}: extiende
 * {@link RespuestaBase} con los datos de una cuenta.
 */
public class RespuestaCuenta extends RespuestaBase {

    /** Datos de la cuenta consultada. */
    private CuentaDTO cuenta;

    public RespuestaCuenta() {
    }

    public RespuestaCuenta(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
    }

    public CuentaDTO getCuenta() {
        return cuenta;
    }

    public void setCuenta(CuentaDTO cuenta) {
        this.cuenta = cuenta;
    }
}
