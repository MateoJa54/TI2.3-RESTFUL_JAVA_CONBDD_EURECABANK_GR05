package ec.edu.monster.datos;

import java.util.Optional;

/**
 * Repositorio de usuarios del banco.
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface IUsuarioDAO {

    /**
     * Busca un usuario por su nombre de acceso.
     *
     * @param nombreUsuario nombre de usuario
     * @return los datos del usuario, o vacio si no existe
     */
    Optional<Usuario> buscarPorNombre(String nombreUsuario);

    /**
     * Datos de un usuario con su credencial y su rol.
     *
     * @param codigo        codigo del empleado
     * @param hash          hash SHA-256 hexadecimal de la clave
     * @param estado        estado del usuario (ACTIVO, ANULADO o CANCELADO)
     * @param nombreEmpleado nombre completo del empleado
     * @param rol           modulo de acceso del empleado
     */
    record Usuario(String codigo, String hash, String estado,
            String nombreEmpleado, String rol) {
    }
}
