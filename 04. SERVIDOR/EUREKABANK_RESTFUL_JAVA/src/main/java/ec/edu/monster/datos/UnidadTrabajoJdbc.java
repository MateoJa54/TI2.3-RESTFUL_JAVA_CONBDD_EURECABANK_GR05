package ec.edu.monster.datos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementacion JDBC de la unidad de trabajo transaccional.
 *
 * <p>Abre una conexion, desactiva el auto-commit, ejecuta el bloque de
 * trabajo y confirma; si el bloque falla, deshace todos los cambios de la
 * transaccion (regla RB-02).
 */
public class UnidadTrabajoJdbc implements UnidadTrabajo {

    private static final Logger LOGGER = Logger.getLogger(UnidadTrabajoJdbc.class.getName());

    private final ProveedorConexion proveedor;

    /**
     * Constructor por defecto: usa la configuracion del entorno.
     */
    public UnidadTrabajoJdbc() {
        this(new ProveedorConexion());
    }

    /**
     * Constructor con proveedor explicito, usado por las pruebas.
     *
     * @param proveedor proveedor de conexiones
     */
    public UnidadTrabajoJdbc(ProveedorConexion proveedor) {
        this.proveedor = proveedor;
    }

    @Override
    public <T> T ejecutar(Cuerpo<T> cuerpo) {
        try (Connection conexion = proveedor.obtenerConexion()) {
            boolean autoCommitInicial = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            try {
                T resultado = cuerpo.aplicar(conexion);
                conexion.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                deshacer(conexion);
                throw e;
            } finally {
                try {
                    conexion.setAutoCommit(autoCommitInicial);
                } catch (SQLException e) {
                    LOGGER.log(Level.FINE, "No se pudo restaurar el auto-commit", e);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo abrir la conexion con la base de datos del banco", e);
        }
    }

    private static void deshacer(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "No se pudo deshacer la transaccion", e);
        }
    }
}
