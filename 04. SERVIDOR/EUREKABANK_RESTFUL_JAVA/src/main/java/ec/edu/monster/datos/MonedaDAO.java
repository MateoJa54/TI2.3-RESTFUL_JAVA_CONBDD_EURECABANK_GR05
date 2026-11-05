package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repositorio JDBC de monedas (tabla {@code moneda}).
 */
public class MonedaDAO implements IMonedaDAO {

    private static final String CONSULTA_DESCRIPCION =
            "SELECT vch_monedescripcion FROM moneda WHERE chr_monecodigo = ?";

    private static final String CONSULTA_SOPORTE =
            "SELECT (SELECT COUNT(*) FROM moneda WHERE chr_monecodigo = ?) AS en_moneda, "
            + "(SELECT COUNT(*) FROM cargomantenimiento WHERE chr_monecodigo = ?) AS en_mantenimiento, "
            + "(SELECT COUNT(*) FROM costomovimiento WHERE chr_monecodigo = ?) AS en_costo, "
            + "(SELECT COUNT(*) FROM interesmensual WHERE chr_monecodigo = ?) AS en_interes";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public MonedaDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public MonedaDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Optional<String> descripcion(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA_DESCRIPCION)) {
            consulta.setString(1, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.ofNullable(resultado.getString("vch_monedescripcion"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la moneda " + codigo, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean estaSoportada(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA_SOPORTE)) {
            consulta.setString(1, codigo);
            consulta.setString(2, codigo);
            consulta.setString(3, codigo);
            consulta.setString(4, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getInt("en_moneda") > 0
                            && resultado.getInt("en_mantenimiento") > 0
                            && resultado.getInt("en_costo") > 0
                            && resultado.getInt("en_interes") > 0;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la moneda " + codigo, e);
        }
        return false;
    }
}
