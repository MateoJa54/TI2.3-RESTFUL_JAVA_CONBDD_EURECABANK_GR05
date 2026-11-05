package ec.edu.monster.datos;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proveedor de conexiones JDBC a la base de datos EURECABANK.
 *
 * <p>La configuracion se resuelve siempre en el mismo orden:
 * variable de entorno, propiedad del sistema, fichero
 * {@code eurekabank.properties} del classpath y, como ultimo recurso, el
 * valor por defecto. Asi el mismo WAR sirve para el Payara del laboratorio y
 * para el servidor de pruebas sin recompilar.
 */
public class ProveedorConexion {

    private static final Logger LOGGER = Logger.getLogger(ProveedorConexion.class.getName());

    /** Variable de entorno con la URL del JDBC. */
    public static final String VARIABLE_URL = "EUREKABANK_DB_URL";

    /** Variable de entorno con el usuario de la base de datos. */
    public static final String VARIABLE_USUARIO = "EUREKABANK_DB_USUARIO";

    /** Variable de entorno con la clave de la base de datos. */
    public static final String VARIABLE_CLAVE = "EUREKABANK_DB_CLAVE";

    /** Propiedad del sistema con la URL del JDBC. */
    public static final String PROPIEDAD_URL = "eurekabank.db.url";

    /** Propiedad del sistema con el usuario de la base de datos. */
    public static final String PROPIEDAD_USUARIO = "eurekabank.db.usuario";

    /** Propiedad del sistema con la clave de la base de datos. */
    public static final String PROPIEDAD_CLAVE = "eurekabank.db.clave";

    /** Nombre del fichero de propiedades del classpath. */
    public static final String FICHERO_PROPIEDADES = "eurekabank.properties";

    /** URL por defecto (MySQL local del banco). */
    public static final String URL_POR_DEFECTO =
            "jdbc:mysql://127.0.0.1:3306/eurekabank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil";

    /** Usuario por defecto de la aplicacion. */
    public static final String USUARIO_POR_DEFECTO = "eurekabank_app";

    /** Clave por defecto de la aplicacion. */
    public static final String CLAVE_POR_DEFECTO = "EurekaBank2026!";

    /** Clase del driver de MySQL 8.4. */
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private final String url;
    private final String usuario;
    private final String clave;

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING,
                    "No se pudo cargar el driver " + DRIVER + ": se delegara en el classloader", e);
        }
    }

    /**
     * Constructor por defecto: resuelve la configuracion del entorno.
     */
    public ProveedorConexion() {
        this(leer(VARIABLE_URL, PROPIEDAD_URL, "eurekabank.db.url", URL_POR_DEFECTO),
                leer(VARIABLE_USUARIO, PROPIEDAD_USUARIO, "eurekabank.db.usuario", USUARIO_POR_DEFECTO),
                leer(VARIABLE_CLAVE, PROPIEDAD_CLAVE, "eurekabank.db.clave", CLAVE_POR_DEFECTO));
    }

    /**
     * Constructor con configuracion explicita, usado por las pruebas.
     *
     * @param url     URL del JDBC
     * @param usuario usuario de la base de datos
     * @param clave   clave de la base de datos
     */
    public ProveedorConexion(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    /**
     * Abre una conexion nueva con la base de datos.
     *
     * @return la conexion abierta
     * @throws SQLException si la base de datos no responde
     */
    public Connection obtenerConexion() throws SQLException {
        Properties propiedades = new Properties();
        if (usuario != null) {
            propiedades.setProperty("user", usuario);
        }
        if (clave != null) {
            propiedades.setProperty("password", clave);
        }
        return DriverManager.getConnection(url, propiedades);
    }

    /**
     * @return la URL del JDBC en uso
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return el usuario de la base de datos en uso
     */
    public String getUsuario() {
        return usuario;
    }

    /**
     * Resuelve un valor de configuracion.
     *
     * <p>Una variable de entorno <em>definida</em> manda sobre las demas
     * fuentes aunque su valor este vacio: asi se puede expressar una clave de
     * base de datos vacia, por ejemplo contra el MySQL de pruebas que no tiene
     * clave. Solo cuando la variable <em>no</em> esta definida se pasa a la
     * propiedad del sistema y, despues, al fichero de propiedades; en esas dos
     * fuentes un valor en blanco si se ignora y se busca en la fuente
     * siguiente. El valor por defecto se usa solo cuando ninguna fuente
     * aporta nada.
     *
     * @param variable  nombre de la variable de entorno
     * @param propiedad nombre de la propiedad del sistema
     * @param clave     clave dentro del fichero de propiedades
     * @param porDefecto valor usado si no aparece en ninguna fuente
     * @return el valor resuelto
     */
    public static String leer(String variable, String propiedad, String clave, String porDefecto) {
        String valor = System.getenv(variable);
        if (valor != null) {
            return valor.trim();
        }
        valor = System.getProperty(propiedad);
        if ((valor == null || valor.isBlank()) && clave != null) {
            valor = leerPropiedades(clave);
        }
        if (valor == null || valor.isBlank()) {
            valor = porDefecto;
        }
        return valor == null ? null : valor.trim();
    }

    private static String leerPropiedades(String clave) {
        Properties propiedades = new Properties();
        try (InputStream entrada = ProveedorConexion.class.getClassLoader()
                .getResourceAsStream(FICHERO_PROPIEDADES)) {
            if (entrada == null) {
                return null;
            }
            propiedades.load(entrada);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo leer " + FICHERO_PROPIEDADES, e);
            return null;
        }
        return propiedades.getProperty(clave);
    }
}
