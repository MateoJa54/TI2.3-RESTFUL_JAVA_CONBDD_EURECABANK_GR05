package ec.edu.monster.datos;

import ec.edu.monster.modelos.MovimientoDTO;
import java.sql.Connection;
import java.util.List;

/**
 * Repositorio de movimientos del banco.
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface IMovimientoDAO {

    /**
     * Lista los movimientos de una cuenta, del mas antiguo al mas reciente.
     *
     * @param codigoCuenta codigo de la cuenta
     * @return lista de movimientos (vacia si la cuenta no tiene ninguno)
     */
    List<MovimientoDTO> listarPorCuenta(String codigoCuenta);

    /**
     * Inserta un movimiento en el libro de la cuenta.
     *
     * @param conexion    conexion de la transaccion en curso
     * @param codigoCuenta codigo de la cuenta a la que pertenece el movimiento
     * @param movimiento  movimiento a insertar
     */
    void insertar(Connection conexion, String codigoCuenta, MovimientoDTO movimiento);
}
