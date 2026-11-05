package ec.edu.monster.modelos;

import java.math.BigDecimal;

/**
 * Respuesta de la operacion {@code consultarCostosMantenimiento}: extiende
 * {@link RespuestaBase} con la proyeccion de costos de la cuenta.
 */
public class RespuestaCostos extends RespuestaBase {

    /** Saldo actual de la cuenta consultada. */
    private BigDecimal saldoActual;

    /** Costo de mantenimiento proyectado para la cuenta. */
    private BigDecimal costoMantenimiento;

    /** Costo por movimiento vigente en la moneda de la cuenta. */
    private BigDecimal costoMovimiento;

    /** Interes mensual proyectado sobre el saldo actual. */
    private BigDecimal interesProyectado;

    /** Saldo maximo que no supera el umbral de cobro de mantenimiento. */
    private BigDecimal limiteMantenimiento;

    /** Numero de operaciones sin costo permitido en la cuenta. */
    private Integer operacionesSinCosto;

    public RespuestaCostos() {
    }

    /**
     * Constructor completo.
     *
     * @param estado               {@code OK} o {@code ERROR}
     * @param codigo               codigo de resultado del catalogo
     * @param mensaje              mensaje literal del catalogo
     * @param saldoActual          saldo actual de la cuenta
     * @param costoMantenimiento   costo de mantenimiento proyectado
     * @param costoMovimiento      costo por movimiento
     * @param interesProyectado    interes mensual proyectado
     * @param limiteMantenimiento  umbral de cobro de mantenimiento
     * @param operacionesSinCosto  operaciones sin costo permitidas
     */
    public RespuestaCostos(String estado, String codigo, String mensaje,
            BigDecimal saldoActual, BigDecimal costoMantenimiento, BigDecimal costoMovimiento,
            BigDecimal interesProyectado, BigDecimal limiteMantenimiento,
            Integer operacionesSinCosto) {
        super(estado, codigo, mensaje);
        this.saldoActual = saldoActual;
        this.costoMantenimiento = costoMantenimiento;
        this.costoMovimiento = costoMovimiento;
        this.interesProyectado = interesProyectado;
        this.limiteMantenimiento = limiteMantenimiento;
        this.operacionesSinCosto = operacionesSinCosto;
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
