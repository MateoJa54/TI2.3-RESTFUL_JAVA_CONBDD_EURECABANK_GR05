package ec.edu.monster.datos;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repositorio de costos e intereses del banco (tablas {@code cargomantenimiento},
 * {@code costomovimiento} e {@code interesmensual}).
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface ICostoDAO {

    /**
     * Umbral e importe del cargo por mantenimiento de una moneda.
     *
     * @param moneda codigo de la moneda
     * @return los dos valores, o vacio si la moneda no tiene cargo de mantenimiento
     */
    Optional<CostoMantenimiento> costoMantenimiento(String moneda);

    /**
     * Costo por movimiento de una moneda.
     *
     * @param moneda codigo de la moneda
     * @return el importe, o vacio si la moneda no tiene costo por movimiento
     */
    Optional<BigDecimal> costoMovimiento(String moneda);

    /**
     * Porcentaje de interes mensual de una moneda.
     *
     * @param moneda codigo de la moneda
     * @return el porcentaje, o vacio si la moneda no tiene interes
     */
    Optional<BigDecimal> interesMensual(String moneda);

    /**
     * Valores del cargo por mantenimiento.
     *
     * @param montoMaximo saldo maximo que no supera el cargo (inclusive)
     * @param importe     importe fijo del cargo
     */
    record CostoMantenimiento(BigDecimal montoMaximo, BigDecimal importe) {
    }
}
