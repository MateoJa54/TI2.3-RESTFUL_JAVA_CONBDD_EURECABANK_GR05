package ec.edu.monster.pruebas;

import ec.edu.monster.datos.CostoDAO;
import ec.edu.monster.datos.ProveedorConexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Utilidades para las pruebas de integracion contra la base de datos real.
 *
 * <p>Las pruebas se saltan (no fallan) cuando no hay base de datos de pruebas
 * disponible: la variable de entorno {@code EUREKABANK_DB_URL} decide.
 */
public final class Integracion {

    /** Variable de entorno con la URL del JDBC de pruebas. */
    public static final String VARIABLE_URL = "EUREKABANK_DB_URL";

    /** Variable de entorno con el usuario de la base de datos de pruebas. */
    public static final String VARIABLE_USUARIO = "EUREKABANK_DB_USUARIO";

    /** Variable de entorno con la clave de la base de datos de pruebas. */
    public static final String VARIABLE_CLAVE = "EUREKABANK_DB_CLAVE";

    /** Variable de entorno con el usuario administrador de la base de datos. */
    public static final String VARIABLE_ADMIN = "EUREKABANK_DB_ADMIN_USUARIO";

    /** Variable de entorno con la clave del usuario administrador. */
    public static final String VARIABLE_ADMIN_CLAVE = "EUREKABANK_DB_ADMIN_CLAVE";

    private Integracion() {
    }

    /**
     * @return {@code true} si hay una base de datos de pruebas configurada
     */
    public static boolean hayBaseDeDatos() {
        String url = System.getenv(VARIABLE_URL);
        return url != null && !url.isBlank();
    }

    /**
     * @return el proveedor de conexiones de la base de datos de pruebas
     */
    public static ProveedorConexion proveedor() {
        return new ProveedorConexion(System.getenv(VARIABLE_URL),
                valor(VARIABLE_USUARIO, "eurekabank_app"), valor(VARIABLE_CLAVE, "EurekaBank2026!"));
    }

    /**
     * Ejecuta una consulta y devuelve la primera columna de la primera fila.
     *
     * @param consulta sentencia SQL
     * @param valores  parametros de la sentencia
     * @return el valor leido, o {@code null} si no hay filas
     * @throws SQLException si la base de datos devuelve un error
     */
    public static Object valorUnico(String consulta, Object... valores) throws SQLException {
        try (Connection conexion = proveedor().obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(consulta)) {
            for (int i = 0; i < valores.length; i++) {
                sentencia.setObject(i + 1, valores[i]);
            }
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getObject(1) : null;
            }
        }
    }

    /**
     * Devuelve el valor de la primera columna de la primera fila, que en una
     * consulta {@code COUNT(*)} es el numero de filas contadas.
     *
     * @param consulta sentencia SQL
     * @param valores  parametros de la sentencia
     * @return el numero de filas
     * @throws SQLException si la base de datos devuelve un error
     */
    public static int contar(String consulta, Object... valores) throws SQLException {
        try (Connection conexion = proveedor().obtenerConexion();
                PreparedStatement sentencia = conexion.prepareStatement(consulta)) {
            for (int i = 0; i < valores.length; i++) {
                sentencia.setObject(i + 1, valores[i]);
            }
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getInt(1);
                }
                return 0;
            }
        }
    }

    /**
     * Ejecuta una sentencia de actualizacion.
     *
     * @param sentencia sentencia SQL
     * @throws SQLException si la base de datos devuelve un error
     */
    public static void ejecutar(String sentencia) throws SQLException {
        try (Connection conexion = proveedor().obtenerConexion();
                Statement orden = conexion.createStatement()) {
            orden.execute(sentencia);
        }
    }

    /**
     * Vuelve a cargar los datos de prueba de la base de datos del laboratorio.
     *
     * <p>El procedimiento {@code sp_cargar_datos_prueba()} es una utilidad de
     * laboratorio: lo ejecuta el administrador de la base de datos, no el
     * usuario de aplicacion (que solo tiene permisos de lectura y escritura).
     *
     * @throws SQLException si la base de datos devuelve un error
     */
    public static void recargarDatosDePrueba() throws SQLException {
        ProveedorConexion administrador = new ProveedorConexion(System.getenv(VARIABLE_URL),
                valor(VARIABLE_ADMIN, "root"), valor(VARIABLE_ADMIN_CLAVE, ""));
        try (Connection conexion = administrador.obtenerConexion();
                Statement orden = conexion.createStatement()) {
            orden.execute("CALL sp_cargar_datos_prueba()");
        }
    }

    /**
     * Devuelve el saldo actual de una cuenta.
     *
     * @param codigo codigo de la cuenta
     * @return el saldo con dos decimales
     * @throws SQLException si la base de datos devuelve un error
     */
    public static java.math.BigDecimal saldoDe(String codigo) throws SQLException {
        Object valor = valorUnico("SELECT dec_cuensaldo FROM cuenta WHERE chr_cuencodigo = ?",
                codigo);
        return valor == null ? null : new java.math.BigDecimal(valor.toString());
    }

    private static String valor(String variable, String porDefecto) {
        String valor = System.getenv(variable);
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }

    /**
     * Devuelve el valor de un Optional como texto, vacio si no hay valor.
     *
     * @param <T>    tipo del valor
     * @param valor  optional
     * @return el texto del valor
     */
    public static <T> String texto(Optional<T> valor) {
        return valor.map(String::valueOf).orElse("");
    }
}
