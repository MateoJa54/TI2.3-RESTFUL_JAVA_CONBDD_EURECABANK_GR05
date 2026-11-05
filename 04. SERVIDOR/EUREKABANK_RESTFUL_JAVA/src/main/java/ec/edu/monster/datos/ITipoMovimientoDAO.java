package ec.edu.monster.datos;

import java.util.Optional;

/**
 * Repositorio de tipos de movimiento (tabla {@code tipomovimiento}).
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface ITipoMovimientoDAO {

    /**
     * Busca un tipo de movimiento por su codigo.
     *
     * @param codigo codigo del tipo ({@code 003} deposito, {@code 004} retiro,
     *               {@code 007} ITF, {@code 008} transferencia de entrada,
     *               {@code 009} transferencia de salida, {@code 010} cargo por
     *               movimiento)
     * @return el tipo de movimiento, o vacio si no existe
     */
    Optional<TipoMovimiento> buscarPorCodigo(String codigo);

    /**
     * Datos de un tipo de movimiento.
     *
     * @param codigo      codigo del tipo
     * @param descripcion descripcion oficial
     * @param accion      INGRESO o SALIDA
     */
    record TipoMovimiento(String codigo, String descripcion, String accion) {
    }
}
