package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Datos de un movimiento del libro de la cuenta que viajan dentro de las
 * respuestas.
 *
 * <p>Los nombres de los atributos son exactamente los nombres de los elementos
 * del XSD congelado.
 */
public class MovimientoDTO {

    /** Numero correlativo del movimiento dentro de la cuenta. */
    private Integer numero;

    /** Fecha en que se registro el movimiento, en ISO-8601. */
    private String fecha;

    /** Codigo del tipo de movimiento, por ejemplo 003 o 004. */
    private String tipoCodigo;

    /** Descripcion oficial del tipo de movimiento. */
    private String tipoDescripcion;

    /** Accion del tipo de movimiento: INGRESO o SALIDA. */
    private String tipoAccion;

    /** Importe del movimiento con dos decimales. */
    private BigDecimal importe;

    /** Cuenta referida en una transferencia; vacia en el resto de movimientos. */
    private String referencia;

    /** Codigo del empleado que registro el movimiento. */
    private String empleado;

    public MovimientoDTO() {
    }

    /**
     * Constructor completo.
     *
     * @param numero         numero correlativo del movimiento
     * @param fecha          fecha del movimiento en ISO-8601
     * @param tipoCodigo     codigo del tipo de movimiento
     * @param tipoDescripcion descripcion del tipo de movimiento
     * @param tipoAccion     INGRESO o SALIDA
     * @param importe        importe del movimiento
     * @param referencia     cuenta referida
     * @param empleado       codigo del empleado
     */
    public MovimientoDTO(Integer numero, String fecha, String tipoCodigo,
            String tipoDescripcion, String tipoAccion, BigDecimal importe,
            String referencia, String empleado) {
        this.numero = numero;
        this.fecha = fecha;
        this.tipoCodigo = tipoCodigo;
        this.tipoDescripcion = tipoDescripcion;
        this.tipoAccion = tipoAccion;
        this.importe = importe;
        this.referencia = referencia;
        this.empleado = empleado;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTipoCodigo() {
        return tipoCodigo;
    }

    public void setTipoCodigo(String tipoCodigo) {
        this.tipoCodigo = tipoCodigo;
    }

    public String getTipoDescripcion() {
        return tipoDescripcion;
    }

    public void setTipoDescripcion(String tipoDescripcion) {
        this.tipoDescripcion = tipoDescripcion;
    }

    public String getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(String tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getEmpleado() {
        return empleado;
    }

    public void setEmpleado(String empleado) {
        this.empleado = empleado;
    }
}
