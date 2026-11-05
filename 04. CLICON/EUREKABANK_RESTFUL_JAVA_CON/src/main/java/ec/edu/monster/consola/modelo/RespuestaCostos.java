package ec.edu.monster.consola.modelo;

import java.math.BigDecimal;

/**
 * Respuesta de la operación {@code consultarCostosMantenimiento}: extiende
 * {@link RespuestaBase} con la proyección de costos de la cuenta.
 */
public class RespuestaCostos extends RespuestaBase {

    /** Saldo actual de la cuenta consultada. */
    private BigDecimal saldoActual;

    /** Costo de mantenimiento proyectado para la cuenta. */
    private BigDecimal costoMantenimiento;

    /** Costo por movimiento vigente en la moneda de la cuenta. */
    private BigDecimal costoMovimiento;

    /** Interés mensual proyectado sobre el saldo actual. */
    private BigDecimal interesProyectado;

    /** Saldo máximo que no supera el umbral de cobro de mantenimiento. */
    private BigDecimal limiteMantenimiento;

    /** Número de operaciones sin costo permitido en la cuenta. */
    private Integer operacionesSinCosto;

    public RespuestaCostos() {
    }

    public RespuestaCostos(String estado, String codigo, String mensaje) {
        super(estado, codigo, mensaje);
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(BigDecimal saldoActual) {
        this.saldoActual = saldoActual;
    }

    public BigDecimal getCostoMantenimiento() {
        return costoMantenimiento;
    }

    public void setCostoMantenimiento(BigDecimal costoMantenimiento) {
        this.costoMantenimiento = costoMantenimiento;
    }

    public BigDecimal getCostoMovimiento() {
        return costoMovimiento;
    }

    public void setCostoMovimiento(BigDecimal costoMovimiento) {
        this.costoMovimiento = costoMovimiento;
    }

    public BigDecimal getInteresProyectado() {
        return interesProyectado;
    }

    public void setInteresProyectado(BigDecimal interesProyectado) {
        this.interesProyectado = interesProyectado;
    }

    public BigDecimal getLimiteMantenimiento() {
        return limiteMantenimiento;
    }

    public void setLimiteMantenimiento(BigDecimal limiteMantenimiento) {
        this.limiteMantenimiento = limiteMantenimiento;
    }

    public Integer getOperacionesSinCosto() {
        return operacionesSinCosto;
    }

    public void setOperacionesSinCosto(Integer operacionesSinCosto) {
        this.operacionesSinCosto = operacionesSinCosto;
    }
}
