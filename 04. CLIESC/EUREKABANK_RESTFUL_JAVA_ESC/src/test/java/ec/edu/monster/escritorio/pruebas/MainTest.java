package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.Main;
import ec.edu.monster.escritorio.cliente.ClienteEUREKABANK;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** La dirección del servicio sale, en orden, del argumento, de la propiedad, de la variable de entorno o del valor por defecto. */
class MainTest {

    private String propiedadOriginal;

    @BeforeEach
    void guardarPropiedad() {
        propiedadOriginal = System.getProperty("servicio.url");
        System.clearProperty("servicio.url");
    }

    @AfterEach
    void restaurarPropiedad() {
        if (propiedadOriginal == null) {
            System.clearProperty("servicio.url");
        } else {
            System.setProperty("servicio.url", propiedadOriginal);
        }
    }

    /** Ejecuta Main.resolverUrl en otro proceso con el entorno y la propiedad indicados. */
    private static String enProceso(String entorno, String propiedad, String... argumentos)
            throws IOException, InterruptedException {
        List<String> orden = new ArrayList<>();
        orden.add(Path.java());
        if (propiedad != null) {
            orden.add("-Dservicio.url=" + propiedad);
        }
        orden.add("-cp");
        orden.add(System.getProperty("java.class.path"));
        orden.add(ResolverUrlProceso.class.getName());
        for (String a : argumentos) {
            orden.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(orden);
        pb.environment().remove("SERVICIO_URL");
        if (entorno != null) {
            pb.environment().put("SERVICIO_URL", entorno);
        }
        pb.redirectErrorStream(true);
        Process proceso = pb.start();
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertEquals(0, proceso.waitFor(), salida);
        return salida;
    }

    /** Ruta del ejecutable de Java del proceso actual. */
    private static final class Path {
        static String java() {
            return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }
    }

    @Test
    void sinNadaUsaElServidorLocal() {
        assertEquals("http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api", Main.resolverUrl(new String[0]));
    }

    @Test
    void elValorPorDefectoEsElDeLaConsola() {
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, Main.resolverUrl(new String[0]));
    }

    @Test
    void elArgumentoCeroManda() {
        assertEquals("http://servidor:9090/api", Main.resolverUrl(new String[] {"http://servidor:9090/api"}));
    }

    @Test
    void elArgumentoSeRecortaDeEspacios() {
        assertEquals("http://servidor/api", Main.resolverUrl(new String[] {"  http://servidor/api  "}));
    }

    @Test
    void elArgumentoGanaALaPropiedad() {
        System.setProperty("servicio.url", "http://propiedad/api");
        assertEquals("http://arg/api", Main.resolverUrl(new String[] {"http://arg/api"}));
    }

    @Test
    void sinArgumentoMandaLaPropiedad() {
        System.setProperty("servicio.url", "http://propiedad/api");
        assertEquals("http://propiedad/api", Main.resolverUrl(new String[0]));
    }

    @Test
    void laPropiedadSeRecortaDeEspacios() {
        System.setProperty("servicio.url", "  http://propiedad/api ");
        assertEquals("http://propiedad/api", Main.resolverUrl(new String[0]));
    }

    @Test
    void unArgumentoEnBlancoSeIgnora() {
        System.setProperty("servicio.url", "http://propiedad/api");
        assertEquals("http://propiedad/api", Main.resolverUrl(new String[] {"   "}));
    }

    @Test
    void unArgumentoVacioSeIgnora() {
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, Main.resolverUrl(new String[] {""}));
    }

    @Test
    void unaPropiedadEnBlancoSeIgnora() {
        System.setProperty("servicio.url", "   ");
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, Main.resolverUrl(new String[0]));
    }

    @Test
    void sinArgumentosNulosNoFalla() {
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, Main.resolverUrl(null));
    }

    @Test
    void soloCuentaElArgumentoCero() {
        assertEquals("http://uno/api", Main.resolverUrl(new String[] {"http://uno/api", "http://dos/api"}));
    }

    @Test
    void laVariableDeEntornoSeUsaSiNoHayArgumentoNiPropiedad() throws Exception {
        assertEquals("http://entorno/api", enProceso("http://entorno/api", null));
    }

    @Test
    void laVariableDeEntornoSeRecortaDeEspacios() throws Exception {
        assertEquals("http://entorno/api", enProceso("  http://entorno/api ", null));
    }

    @Test
    void laPropiedadGanaALaVariableDeEntorno() throws Exception {
        assertEquals("http://propiedad/api", enProceso("http://entorno/api", "http://propiedad/api"));
    }

    @Test
    void elArgumentoGanaALaVariableDeEntorno() throws Exception {
        assertEquals("http://arg/api", enProceso("http://entorno/api", null, "http://arg/api"));
    }

    @Test
    void elArgumentoGanaALasTresFuentes() throws Exception {
        assertEquals("http://arg/api", enProceso("http://entorno/api", "http://propiedad/api", "http://arg/api"));
    }

    @Test
    void unaVariableDeEntornoEnBlancoSeIgnora() throws Exception {
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, enProceso("   ", null));
    }

    @Test
    void sinNingunaFuenteElProcesoUsaElValorPorDefecto() throws Exception {
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, enProceso(null, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void losArgumentosSinTextoNoCuentan(String argumento) {
        String[] argumentos = {argumento};
        assertEquals(ClienteEUREKABANK.URL_POR_DEFECTO, Main.resolverUrl(argumentos));
    }

    @ParameterizedTest
    @ValueSource(strings = {"localhost:8080", "ftp://servidor/api", "servidor", "http://"})
    void unaDireccionMalFormadaLaRechazaElClienteNoMain(String direccion) {
        // Main solo resuelve el texto; el cliente es quien valida la dirección.
        assertEquals(direccion, Main.resolverUrl(new String[] {direccion}));
        assertThrows(IllegalArgumentException.class, () -> new ClienteEUREKABANK(direccion));
    }

    @Test
    void mainTieneLasOperacionesDelDiagrama() throws NoSuchMethodException {
        assertNotNull(Main.class.getMethod("main", String[].class));
        assertNotNull(Main.class.getMethod("resolverUrl", String[].class));
        assertNotNull(Main.class.getMethod("abrirLogin"));
        assertNotNull(Main.class.getMethod("abrirPrincipal"));
        assertTrue(java.lang.reflect.Modifier.isStatic(Main.class.getMethod("main", String[].class).getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(Main.class.getMethod("resolverUrl", String[].class).getModifiers()));
    }
}
