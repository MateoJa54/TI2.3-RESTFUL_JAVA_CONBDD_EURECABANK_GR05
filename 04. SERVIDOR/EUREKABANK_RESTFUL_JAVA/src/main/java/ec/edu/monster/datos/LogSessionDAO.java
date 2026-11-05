package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Repositorio JDBC de auditoria de sesiones (tabla {@code logsession}).
 */
public class LogSessionDAO implements ILogSessionDAO {

    private static final String INSERCION =
            "INSERT INTO logsession (chr_emplcodigo, fec_ingreso, ip, hostname) VALUES (?, NOW(), ?, ?)";

    private static final String ACTUALIZACION =
            "UPDATE logsession SET fec_salida = NOW() WHERE ID = ? AND fec_salida IS NULL";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public LogSessionDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public LogSessionDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Integer registrarIngreso(String empleado, String ip, String hostname) {
        if (empleado == null || empleado.isBlank()) {
            return null;
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement insercion = conexion.prepareStatement(
                        INSERCION, Statement.RETURN_GENERATED_KEYS)) {
            insercion.setString(1, empleado);
            insercion.setString(2, ip == null ? "NONE" : ip);
            insercion.setString(3, hostname == null ? "NONE" : hostname);
            insercion.executeUpdate();
            try (ResultSet claves = insercion.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo registrar el inicio de la sesion del empleado " + empleado, e);
        }
        return null;
    }

    @Override
    public boolean registrarSalida(Integer idSesion) {
        if (idSesion == null) {
            return false;
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement actualizacion = conexion.prepareStatement(ACTUALIZACION)) {
            actualizacion.setInt(1, idSesion);
            return actualizacion.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo registrar el cierre de la sesion " + idSesion, e);
        }
    }
}
