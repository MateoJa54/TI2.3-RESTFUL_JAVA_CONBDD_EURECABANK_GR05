package ec.edu.monster.datos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Repositorio JDBC de costos e intereses del banco.
 */
public class CostoDAO implements ICostoDAO {

    private static final String CONSULTA_MANTENIMIENTO =
            "SELECT dec_cargMontoMaximo, dec_cargImporte FROM cargomantenimiento WHERE chr_monecodigo = ?";

    private static final String CONSULTA_MOVIMIENTO =
            "SELECT dec_costimporte FROM costomovimiento WHERE chr_monecodigo = ?";

    private static final String CONSULTA_INTERES =
            "SELECT dec_inteimporte FROM interesmensual WHERE chr_monecodigo = ?";

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public CostoDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public CostoDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public Optional<CostoMantenimiento> costoMantenimiento(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA_MANTENIMIENTO)) {
            consulta.setString(1, moneda);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(new CostoMantenimiento(
                            resultado.getBigDecimal("dec_cargMontoMaximo"),
                            resultado.getBigDecimal("dec_cargImporte")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el cargo de mantenimiento de la moneda " + moneda, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BigDecimal> costoMovimiento(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA_MOVIMIENTO)) {
            consulta.setString(1, moneda);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.ofNullable(resultado.getBigDecimal("dec_costimporte"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el costo por movimiento de la moneda " + moneda, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BigDecimal> interesMensual(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA_INTERES)) {
            consulta.setString(1, moneda);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.ofNullable(resultado.getBigDecimal("dec_inteimporte"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el interes mensual de la moneda " + moneda, e);
        }
        return Optional.empty();
    }
}
