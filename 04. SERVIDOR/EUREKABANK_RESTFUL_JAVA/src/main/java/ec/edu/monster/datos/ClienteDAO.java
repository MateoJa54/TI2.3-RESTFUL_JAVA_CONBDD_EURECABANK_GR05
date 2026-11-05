package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repositorio JDBC de clientes del banco (tabla {@code cliente}).
 */
public class ClienteDAO implements IClienteDAO {

    private static final String CONSULTA =
            "SELECT c.chr_cliecodigo, "
            + "CONCAT(c.vch_clienombre, ' ', c.vch_cliepaterno, ' ', c.vch_cliematerno) "
            + "FROM cliente c WHERE c.chr_cliedni = ?";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public ClienteDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public ClienteDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Optional<DatosCliente> buscarPorDni(String dni) {
        if (dni == null || dni.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA)) {
            consulta.setString(1, dni);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(new DatosCliente(
                            resultado.getString("chr_cliecodigo"),
                            resultado.getString(2)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el cliente " + dni, e);
        }
        return Optional.empty();
    }
}
