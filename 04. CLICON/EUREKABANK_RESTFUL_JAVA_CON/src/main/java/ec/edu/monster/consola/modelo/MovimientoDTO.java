package ec.edu.monster.consola.modelo;

import java.math.BigDecimal;

/**
 * Datos de un movimiento del libro de la cuenta que viajan dentro de las
 * respuestas.
 *
 * <p>Los nombres de los atributos son exactamente los nombres de los elementos
 * del XSD congelado.
 */
public class MovimientoDTO {

    /** Número correlativo del movimiento dentro de la cuenta. */
    private Integer numero;

    /** Fecha en que se registró el movimiento, en ISO-8601. */
    private String fecha;

    /** Código del tipo de movimiento, por ejemplo {@code 003}. */
    private String tipoCodigo;

    /** Descripción oficial del tipo de movimiento. */
    private String tipoDescripcion;

    /** Acción del tipo de movimiento: INGRESO o SALIDA. */
    private String tipoAccion;

    /** Importe del movimiento. */
    private BigDecimal importe;

    /** Cuenta referida en una transferencia; vacía en el resto de movimientos. */
    private String referencia;

    /** Código del empleado que registró el movimiento. */
    private String empleado;

    public MovimientoDTO() {
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
