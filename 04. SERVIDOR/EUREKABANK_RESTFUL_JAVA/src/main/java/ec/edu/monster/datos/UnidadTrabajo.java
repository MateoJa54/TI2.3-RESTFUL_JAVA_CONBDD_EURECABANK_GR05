package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Unidad de trabajo transaccional.
 *
 * <p>Regla RB-02 del contrato congelado: un movimiento es una transaccion
 * unica. Este puerto agrupa el bloque {@code SELECT ... FOR UPDATE}, la
 * actualizacion del saldo y el {@code INSERT} del movimiento, de modo que si
 * algo falla se deshace todo.
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface UnidadTrabajo {

    /**
     * Ejecuta un bloque dentro de una transaccion.
     *
     * <p>Si el bloque termina bien se confirma (commit); si lanza una
     * excepcion se deshace (rollback) y la excepcion se propaga.
     *
     * @param <T>    tipo del resultado
     * @param cuerpo bloque de trabajo que recibe la conexion de la transaccion
     * @return el resultado del bloque
     */
    <T> T ejecutar(Cuerpo<T> cuerpo);

    /**
     * Bloque de trabajo que se ejecuta dentro de la transaccion.
     *
     * @param <T> tipo del resultado
     */
    @FunctionalInterface
    interface Cuerpo<T> {

        /**
         * Ejecuta el bloque de trabajo.
         *
         * @param conexion conexion de la transaccion en curso
         * @return el resultado del bloque
         * @throws SQLException si la base de datos devuelve un error
         */
        T aplicar(Connection conexion) throws SQLException;
    }
}
