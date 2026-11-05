package ec.edu.monster.modelos;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de la operacion {@code listarCuentasPorCliente}: extiende
 * {@link RespuestaBase} con los datos del cliente y la lista de sus cuentas.
 */
public class RespuestaCuentas extends RespuestaBase {

    /** Cedula del cliente consultado. */
    private String cedula;

    /** Nombre completo del cliente consultado. */
    private String nombreCliente;

    /** Lista de cuentas del cliente con su estado visible. */
    private List<CuentaDTO> cuentas = new ArrayList<>();

    public RespuestaCuentas() {
    }

    /**
     * Constructor completo.
     *
     * @param estado        {@code OK} o {@code ERROR}
     * @param codigo        codigo de resultado del catalogo
     * @param mensaje       mensaje literal del catalogo
     * @param cedula        cedula del cliente
     * @param nombreCliente nombre completo del cliente
     * @param cuentas       lista de cuentas del cliente
     */
    public RespuestaCuentas(String estado, String codigo, String mensaje,
            String cedula, String nombreCliente, List<CuentaDTO> cuentas) {
        super(estado, codigo, mensaje);
        this.cedula = cedula;
        this.nombreCliente = nombreCliente;
        this.cuentas = cuentas;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public List<CuentaDTO> getCuentas() {
        return cuentas;
    }

    public void setCuentas(List<CuentaDTO> cuentas) {
        this.cuentas = cuentas;
    }
}
