package ec.edu.monster.seguridad;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Añade las cabeceras CORS a las respuestas de los orígenes permitidos.
 *
 * <p>Los orígenes salen de la variable {@link #VARIABLE_ORIGENES} (lista separada
 * por comas). Si no existe, el unico origen permitido es el cliente web en el
 * puerto 4200.
 */
@Provider
public class FiltroCors implements ContainerResponseFilter {

    /** Variable de entorno con la lista de orígenes permitidos. */
    public static final String VARIABLE_ORIGENES = "EUREKABANK_ORIGENES_CORS";

    /** Origen que se usa cuando la variable no está definida. */
    public static final String ORIGEN_POR_DEFECTO = "http://localhost:4200";

    private static final String CABECERA_ORIGEN = "Origin";

    private final Set<String> origenes;

    public FiltroCors() {
        this(System.getenv(VARIABLE_ORIGENES));
    }

    /**
     * @param listaOrigenes orígenes separados por comas; vacío o nulo usa el valor por defecto
     */
    public FiltroCors(String listaOrigenes) {
        this.origenes = Collections.unmodifiableSet(parsear(listaOrigenes));
    }

    /**
     * @param origen valor de la cabecera {@code Origin} de la petición
     * @return {@code true} si el origen está en la lista permitida
     */
    public boolean esOrigenPermitido(String origen) {
        return origen != null && origenes.contains(origen.strip());
    }

    @Override
    public void filter(ContainerRequestContext peticion, ContainerResponseContext respuesta) {
        String origen = peticion.getHeaderString(CABECERA_ORIGEN);
        if (!esOrigenPermitido(origen)) {
            return;
        }
        MultivaluedMap<String, Object> cabeceras = respuesta.getHeaders();
        cabeceras.putSingle("Access-Control-Allow-Origin", origen.strip());
        cabeceras.putSingle("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        cabeceras.putSingle("Access-Control-Allow-Headers", "Authorization, Content-Type");
    }

    private static Set<String> parsear(String lista) {
        String texto = (lista == null || lista.isBlank()) ? ORIGEN_POR_DEFECTO : lista;
        Set<String> resultado = new LinkedHashSet<>();
        Arrays.stream(texto.split(","))
                .map(String::strip)
                .filter(origen -> !origen.isEmpty())
                .forEach(resultado::add);
        return resultado;
    }
}
