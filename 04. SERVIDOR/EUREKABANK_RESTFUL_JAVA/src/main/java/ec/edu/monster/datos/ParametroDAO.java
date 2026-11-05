package ec.edu.monster.datos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repositorio JDBC de parametros de negocio (tabla {@code parametro}).
 */
public class ParametroDAO implements IParametroDAO {

    private static final String CONSULTA =
            "SELECT vch_paravalor FROM parametro WHERE chr_paracodigo = ? AND vch_paraestado = 'ACTIVO'";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public ParametroDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public ParametroDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Optional<BigDecimal> valor(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA)) {
            consulta.setString(1, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    String texto = resultado.getString("vch_paravalor");
                    if (texto == null || texto.isBlank()) {
                        return Optional.empty();
                    }
                    return Optional.of(new BigDecimal(texto.trim()));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            throw new IllegalStateException("No se pudo leer el parametro " + codigo, e);
        }
        return Optional.empty();
    }
}
