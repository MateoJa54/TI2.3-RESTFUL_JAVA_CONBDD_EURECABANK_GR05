package ec.edu.monster.datos;

import ec.edu.monster.modelos.CuentaDTO;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de cuentas del banco.
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface ICuentaDAO {

    /**
     * Lista las cuentas de un cliente, con su estado visible.
     *
     * @param codigoCliente codigo del cliente titular
     * @return lista de cuentas (vacia si el cliente no tiene ninguna)
     */
    List<CuentaDTO> listarPorCliente(String codigoCliente);

    /**
     * Busca una cuenta por su codigo.
     *
     * @param codigo codigo de la cuenta
     * @return la cuenta, o vacio si no existe
     */
    Optional<CuentaDTO> buscarPorCodigo(String codigo);

    /**
     * Busca una cuenta y la bloquea para actualizarla ({@code SELECT ... FOR UPDATE}).
     *
     * <p>Regla RB-03: el saldo se bloquea antes de operar para que dos
     * operaciones simultaneas no gasten el mismo saldo.
     *
     * @param conexion conexion de la transaccion en curso
     * @param codigo   codigo de la cuenta
     * @return la cuenta bloqueada, o vacio si no existe
     */
    Optional<CuentaDTO> bloquearPorCodigo(Connection conexion, String codigo);

    /**
     * Actualiza el saldo y el contador de movimientos de una cuenta.
     *
     * @param conexion  conexion de la transaccion en curso
     * @param codigo    codigo de la cuenta
     * @param saldo     saldo nuevo
     * @param contador  numero de movimientos de la cuenta
     */
    void actualizarSaldoYContador(Connection conexion, String codigo,
            java.math.BigDecimal saldo, int contador);
}
