package ec.edu.monster.web.util;

import jakarta.servlet.ServletContext;
import java.util.function.UnaryOperator;

/**
 * Configuración del cliente web: de dónde sale la URL base del servicio.
 *
 * <p>Orden de prioridad: parámetro de contexto {@code servicio.url}, propiedad
 * de la JVM {@code -Dservicio.url}, variable de entorno {@code SERVICIO_URL} y,
 * si ninguna tiene valor, la dirección del servidor local.
 */
public final class ConfiguracionWeb {

    /** Dirección del servicio publicado en el servidor local. */
    public static final String URL_POR_DEFECTO = "http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api";

    /** Nombre del parámetro de contexto y de la propiedad de la JVM. */
    public static final String PARAMETRO = "servicio.url";

    /** Nombre de la variable de entorno. */
    public static final String VARIABLE_ENTORNO = "SERVICIO_URL";

    /** Atributo del contexto donde vive el único cliente del servicio de la aplicación. */
    public static final String ATRIBUTO_CLIENTE = "ec.edu.monster.web.cliente";

    private ConfiguracionWeb() {
    }

    /**
     * @param contexto contexto de la aplicación web, puede ser {@code null}
     * @return la URL base del servicio, sin espacios en los extremos
     */
    public static String resolverUrl(ServletContext contexto) {
        return resolverUrl(contexto, System::getProperty, System::getenv);
    }

    /**
     * Igual que {@link #resolverUrl(ServletContext)} pero con las fuentes de propiedades y de
     * entorno inyectadas, para poder probar cada una sin tocar la JVM.
     *
     * @param contexto    contexto de la aplicación web, puede ser {@code null}
     * @param propiedades lectura de propiedades de la JVM
     * @param entorno     lectura de variables de entorno
     * @return la URL base del servicio
     */
    public static String resolverUrl(ServletContext contexto, UnaryOperator<String> propiedades,
            UnaryOperator<String> entorno) {
        String valor = contexto == null ? null : limpiar(contexto.getInitParameter(PARAMETRO));
        if (valor == null) {
            valor = limpiar(propiedades.apply(PARAMETRO));
        }
        if (valor == null) {
            valor = limpiar(entorno.apply(VARIABLE_ENTORNO));
        }
        return valor == null ? URL_POR_DEFECTO : valor;
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
