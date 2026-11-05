package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Datos de una cuenta del banco que viajan dentro de las respuestas.
 *
 * <p>Los nombres de los atributos son exactamente los nombres de los elementos
 * del XSD congelado.
 */
public class CuentaDTO {

    /** Codigo de la cuenta. */
    private String codigo;

    /** Codigo de la moneda de la cuenta, por ejemplo 01 o 02. */
    private String moneda;

    /** Nombre oficial de la moneda de la cuenta. */
    private String descripcionMoneda;

    /** Saldo disponible de la cuenta con dos decimales. */
    private BigDecimal saldo;

    /** Estado de la cuenta: ACTIVO, ANULADO o CANCELADO. */
    private String estado;

    /** Codigo del cliente titular de la cuenta. */
    private String codigoCliente;

    /** Nombre completo del cliente titular de la cuenta. */
    private String nombreCliente;

    /** Codigo de la sucursal que administra la cuenta. */
    private String codigoSucursal;

    /** Fecha en que se abrio la cuenta, en ISO-8601. */
    private String fechaCreacion;

    /** Clave de identificacion de la cuenta usada por el cajero del banco. */
    private String clave;

    /** Cantidad de movimientos registrados en la cuenta. */
    private Integer operacionesRealizadas;

    public CuentaDTO() {
    }

    /**
     * Constructor completo.
     *
     * @param codigo               codigo de la cuenta
     * @param moneda               codigo de la moneda
     * @param descripcionMoneda    nombre de la moneda
     * @param saldo                saldo de la cuenta
     * @param estado               estado de la cuenta
     * @param codigoCliente        codigo del cliente titular
     * @param nombreCliente        nombre del cliente titular
     * @param codigoSucursal       codigo de la sucursal
     * @param fechaCreacion        fecha de apertura en ISO-8601
     * @param clave                clave de la cuenta
     * @param operacionesRealizadas cantidad de movimientos
     */
    public CuentaDTO(String codigo, String moneda, String descripcionMoneda,
            BigDecimal saldo, String estado, String codigoCliente, String nombreCliente,
            String codigoSucursal, String fechaCreacion, String clave,
            Integer operacionesRealizadas) {
        this.codigo = codigo;
        this.moneda = moneda;
        this.descripcionMoneda = descripcionMoneda;
        this.saldo = saldo;
        this.estado = estado;
        this.codigoCliente = codigoCliente;
        this.nombreCliente = nombreCliente;
        this.codigoSucursal = codigoSucursal;
        this.fechaCreacion = fechaCreacion;
        this.clave = clave;
        this.operacionesRealizadas = operacionesRealizadas;
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
