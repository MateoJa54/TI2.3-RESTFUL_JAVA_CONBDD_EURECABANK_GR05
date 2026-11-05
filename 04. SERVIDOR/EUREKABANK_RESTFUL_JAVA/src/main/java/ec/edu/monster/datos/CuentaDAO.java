package ec.edu.monster.datos;

import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.utilidades.FormatoDinero;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JDBC de cuentas del banco (tabla {@code cuenta}).
 */
public class CuentaDAO implements ICuentaDAO {

    private static final String SELECT_BASE =
            "SELECT c.chr_cuencodigo, c.chr_monecodigo, m.vch_monedescripcion, c.dec_cuensaldo, "
            + "c.vch_cuenestado, c.chr_cliecodigo, "
            + "CONCAT(cl.vch_clienombre, ' ', cl.vch_cliepaterno, ' ', cl.vch_cliematerno) "
            + "AS vch_clienombrecompleto, "
            + "c.chr_sucucodigo, c.dtt_cuenfechacreacion, c.chr_cuenclave, c.int_cuencontmov "
            + "FROM cuenta c "
            + "JOIN cliente cl ON cl.chr_cliecodigo = c.chr_cliecodigo "
            + "JOIN moneda m ON m.chr_monecodigo = c.chr_monecodigo ";

    private static final String ACTUALIZACION =
            "UPDATE cuenta SET dec_cuensaldo = ?, int_cuencontmov = ? WHERE chr_cuencodigo = ?";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public CuentaDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public CuentaDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public List<CuentaDTO> listarPorCliente(String codigoCliente) {
        List<CuentaDTO> cuentas = new ArrayList<>();
        if (codigoCliente == null || codigoCliente.isBlank()) {
            return cuentas;
        }
        String sql = SELECT_BASE + "WHERE c.chr_cliecodigo = ? ORDER BY c.chr_cuencodigo";
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, codigoCliente);
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    cuentas.add(leerCuenta(resultado));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron listar las cuentas del cliente " + codigoCliente, e);
        }
        return cuentas;
    }

    @Override
    public Optional<CuentaDTO> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(SELECT_BASE
                        + "WHERE c.chr_cuencodigo = ?")) {
            consulta.setString(1, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(leerCuenta(resultado));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la cuenta " + codigo, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CuentaDTO> bloquearPorCodigo(Connection conexion, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        try (PreparedStatement consulta = conexion.prepareStatement(
                SELECT_BASE + "WHERE c.chr_cuencodigo = ? FOR UPDATE")) {
            consulta.setString(1, codigo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(leerCuenta(resultado));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo bloquear la cuenta " + codigo, e);
        }
        return Optional.empty();
    }

    @Override
    public void actualizarSaldoYContador(Connection conexion, String codigo,
            BigDecimal saldo, int contador) {
        try (PreparedStatement actualizacion = conexion.prepareStatement(ACTUALIZACION)) {
            actualizacion.setBigDecimal(1, FormatoDinero.aDosDecimales(saldo));
            actualizacion.setInt(2, contador);
            actualizacion.setString(3, codigo);
            if (actualizacion.executeUpdate() != 1) {
                throw new IllegalStateException(
                        "La cuenta " + codigo + " no existe y no se pudo actualizar");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo actualizar el saldo de la cuenta " + codigo, e);
        }
    }

    private static CuentaDTO leerCuenta(ResultSet resultado) throws SQLException {
        Date fecha = resultado.getDate("dtt_cuenfechacreacion");
        BigDecimal saldo = resultado.getBigDecimal("dec_cuensaldo");
        return new CuentaDTO(
                resultado.getString("chr_cuencodigo"),
                resultado.getString("chr_monecodigo"),
                resultado.getString("vch_monedescripcion"),
                FormatoDinero.aDosDecimales(saldo),
                resultado.getString("vch_cuenestado"),
                resultado.getString("chr_cliecodigo"),
                resultado.getString("vch_clienombrecompleto"),
                resultado.getString("chr_sucucodigo"),
                fecha == null ? null : FORMATO_FECHA.format(fecha.toLocalDate()),
                resultado.getString("chr_cuenclave"),
                resultado.getInt("int_cuencontmov"));
    }
}
