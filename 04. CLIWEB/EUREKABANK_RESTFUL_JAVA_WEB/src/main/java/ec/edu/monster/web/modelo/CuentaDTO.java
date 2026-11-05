package ec.edu.monster.web.modelo;

import java.math.BigDecimal;

/**
 * Datos de una cuenta del banco que viajan dentro de las respuestas.
 *
 * <p>Los nombres de los atributos son exactamente los nombres de los elementos
 * del contrato congelado.
 */
public class CuentaDTO {

    /** Código de la cuenta. */
    private String codigo;

    /** Código de la moneda de la cuenta, por ejemplo {@code 01}. */
    private String moneda;

    /** Nombre oficial de la moneda de la cuenta. */
    private String descripcionMoneda;

    /** Saldo disponible de la cuenta. */
    private BigDecimal saldo;

    /** Estado de la cuenta: ACTIVO, ANULADO o CANCELADO. */
    private String estado;

    /** Código del cliente titular de la cuenta. */
    private String codigoCliente;

    /** Nombre completo del cliente titular de la cuenta. */
    private String nombreCliente;

    /** Código de la sucursal que administra la cuenta. */
    private String codigoSucursal;

    /** Fecha en que se abrió la cuenta, en ISO-8601. */
    private String fechaCreacion;

    /** Clave de identificación de la cuenta usada por el cajero del banco. */
    private String clave;

    /** Cantidad de movimientos registrados en la cuenta. */
    private Integer operacionesRealizadas;

    public CuentaDTO() {
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getDescripcionMoneda() {
        return descripcionMoneda;
    }

    public void setDescripcionMoneda(String descripcionMoneda) {
        this.descripcionMoneda = descripcionMoneda;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCodigoSucursal() {
        return codigoSucursal;
    }

    public void setCodigoSucursal(String codigoSucursal) {
        this.codigoSucursal = codigoSucursal;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public Integer getOperacionesRealizadas() {
        return operacionesRealizadas;
    }

    public void setOperacionesRealizadas(Integer operacionesRealizadas) {
        this.operacionesRealizadas = operacionesRealizadas;
    }
}
