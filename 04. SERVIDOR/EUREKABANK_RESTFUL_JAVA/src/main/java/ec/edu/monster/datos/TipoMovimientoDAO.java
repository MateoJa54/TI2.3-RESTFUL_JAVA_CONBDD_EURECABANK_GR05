package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repositorio JDBC de tipos de movimiento (tabla {@code tipomovimiento}).
 */
public class TipoMovimientoDAO implements ITipoMovimientoDAO {

    private static final String CONSULTA =
            "SELECT chr_tipocodigo, vch_tipodescripcion, vch_tipoaccion "
            + "FROM tipomovimiento WHERE chr_tipocodigo = ?";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public TipoMovimientoDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public TipoMovimientoDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Optional<TipoMovimiento> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA)) {
            consulta.setString(1, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(new TipoMovimiento(
                            resultado.getString("chr_tipocodigo"),
                            resultado.getString("vch_tipodescripcion"),
                            resultado.getString("vch_tipoaccion")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el tipo de movimiento " + codigo, e);
        }
        return Optional.empty();
    }
}
