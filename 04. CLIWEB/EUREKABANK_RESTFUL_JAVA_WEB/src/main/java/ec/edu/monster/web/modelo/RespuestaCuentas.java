package ec.edu.monster.web.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de la operación {@code listarCuentasPorCliente}: extiende
 * {@link RespuestaBase} con los datos del cliente y la lista de sus cuentas.
 */
public class RespuestaCuentas extends RespuestaBase {

    /** Cédula del cliente consultado. */
    private String cedula;

    /** Nombre completo del cliente consultado. */
    private String nombreCliente;

    /** Lista de cuentas del cliente con su estado visible. */
    private List<CuentaDTO> cuentas = new ArrayList<>();

    public RespuestaCuentas() {
    }

    public RespuestaCuentas(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
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
