package ec.edu.monster.datos;

/**
 * Repositorio de auditoria de sesiones (tabla {@code logsession}).
 *
 * <p>Es un puerto (interfaz) para que las pruebas unitarias puedan usar un
 * doble en lugar de la base de datos.
 */
public interface ILogSessionDAO {

    /**
     * Registra el inicio de una sesion.
     *
     * @param empleado codigo del empleado que inicia la sesion
     * @param ip       direccion IP del cliente
     * @param hostname nombre del host del cliente
     * @return el identificador de la sesion abierta, o {@code null} si no se pudo registrar
     */
    Integer registrarIngreso(String empleado, String ip, String hostname);

    /**
     * Registra el cierre de una sesion.
     *
     * @param idSesion identificador de la sesion
     * @return {@code true} si se actualizo alguna fila
     */
    boolean registrarSalida(Integer idSesion);
}
