package ec.edu.monster.datos;

import java.util.Optional;

/**
 * Repositorio de clientes del banco.
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface IClienteDAO {

    /**
     * Busca un cliente por su numero de documento.
     *
     * @param dni numero de documento de identidad
     * @return los datos del cliente, o vacio si no existe
     */
    Optional<DatosCliente> buscarPorDni(String dni);

    /**
     * Datos minimos de un cliente: el codigo para enlazar con sus cuentas y
     * el nombre completo que viaja en las respuestas.
     *
     * @param codigo codigo del cliente
     * @param nombre nombre completo del cliente
     */
    record DatosCliente(String codigo, String nombre) {
    }
}
