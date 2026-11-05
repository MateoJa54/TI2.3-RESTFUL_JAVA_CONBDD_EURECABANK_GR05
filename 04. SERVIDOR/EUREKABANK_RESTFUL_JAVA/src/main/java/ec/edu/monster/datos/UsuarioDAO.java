package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repositorio JDBC de usuarios del banco (tablas {@code usuario},
 * {@code empleado}, {@code permiso} y {@code modulo}).
 */
public class UsuarioDAO implements IUsuarioDAO {

    private static final String CONSULTA =
            "SELECT u.chr_emplcodigo, u.vch_emplclave, u.vch_emplestado, "
            + "CONCAT(e.vch_emplnombre, ' ', e.vch_emplpaterno, ' ', e.vch_emplmaterno) "
            + "AS vch_emplnombrecompleto, "
            + "(SELECT m.vch_modunombre FROM permiso p "
            + "   JOIN modulo m ON m.int_moducodigo = p.int_moducodigo "
            + "   WHERE p.chr_emplcodigo = u.chr_emplcodigo AND p.vch_permestado = 'ACTIVO' "
            + "   ORDER BY m.int_moducodigo LIMIT 1) AS vch_modunombre "
            + "FROM usuario u "
            + "JOIN empleado e ON e.chr_emplcodigo = u.chr_emplcodigo "
            + "WHERE u.vch_emplusuario = ?";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public UsuarioDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public UsuarioDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA)) {
            consulta.setString(1, nombreUsuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(new Usuario(
                            resultado.getString("chr_emplcodigo"),
                            resultado.getString("vch_emplclave"),
                            resultado.getString("vch_emplestado"),
                            resultado.getString("vch_emplnombrecompleto"),
                            resultado.getString("vch_modunombre")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el usuario " + nombreUsuario, e);
        }
        return Optional.empty();
    }
}
