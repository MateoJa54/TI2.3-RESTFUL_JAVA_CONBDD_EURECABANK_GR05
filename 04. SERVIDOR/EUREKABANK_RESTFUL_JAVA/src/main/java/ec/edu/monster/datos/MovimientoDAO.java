package ec.edu.monster.datos;

import ec.edu.monster.modelos.MovimientoDTO;
import ec.edu.monster.utilidades.FormatoDinero;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio JDBC de movimientos del banco (tabla {@code movimiento}).
 */
public class MovimientoDAO implements IMovimientoDAO {

    private static final String CONSULTA =
            "SELECT m.int_movinumero, m.dtt_movifecha, m.chr_tipocodigo, "
            + "t.vch_tipodescripcion, t.vch_tipoaccion, m.dec_moviimporte, "
            + "m.chr_cuenreferencia, m.chr_emplcodigo "
            + "FROM movimiento m "
            + "JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo "
            + "WHERE m.chr_cuencodigo = ? ORDER BY m.int_movinumero";

    private static final String INSERCION =
            "INSERT INTO movimiento (chr_cuencodigo, int_movinumero, dtt_movifecha, "
            + "chr_emplcodigo, chr_tipocodigo, dec_moviimporte, chr_cuenreferencia) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public MovimientoDAO() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public MovimientoDAO(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public List<MovimientoDTO> listarPorCuenta(String codigoCuenta) {
        List<MovimientoDTO> movimientos = new ArrayList<>();
        if (codigoCuenta == null || codigoCuenta.isBlank()) {
            return movimientos;
        }
        try (Connection conexion = proveedor.obtenerConexion();
                PreparedStatement consulta = conexion.prepareStatement(CONSULTA)) {
            consulta.setString(1, codigoCuenta);
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    Date fecha = resultado.getDate("dtt_movifecha");
                    movimientos.add(new MovimientoDTO(
                            resultado.getInt("int_movinumero"),
                            fecha == null ? null : FORMATO_FECHA.format(fecha.toLocalDate()),
                            resultado.getString("chr_tipocodigo"),
                            resultado.getString("vch_tipodescripcion"),
                            resultado.getString("vch_tipoaccion"),
                            FormatoDinero.aDosDecimales(resultado.getBigDecimal("dec_moviimporte")),
                            resultado.getString("chr_cuenreferencia"),
                            resultado.getString("chr_emplcodigo")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron listar los movimientos de la cuenta " + codigoCuenta, e);
        }
        return movimientos;
    }

    @Override
    public void insertar(Connection conexion, String codigoCuenta, MovimientoDTO movimiento) {
        try (PreparedStatement insercion = conexion.prepareStatement(INSERCION)) {
            insercion.setString(1, codigoCuenta);
            insercion.setInt(2, movimiento.getNumero());
            insercion.setDate(3, movimiento.getFecha() == null
                    ? null : Date.valueOf(movimiento.getFecha()));
            insercion.setString(4, movimiento.getEmpleado());
            insercion.setString(5, movimiento.getTipoCodigo());
            insercion.setBigDecimal(6, FormatoDinero.aDosDecimales(movimiento.getImporte()));
            insercion.setString(7, movimiento.getReferencia());
            insercion.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo registrar el movimiento " + movimiento.getNumero(), e);
        }
    }
}
