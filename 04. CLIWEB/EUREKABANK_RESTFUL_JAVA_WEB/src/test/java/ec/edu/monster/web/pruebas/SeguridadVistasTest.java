package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.filtro.FiltroSesion;
import ec.edu.monster.web.servlet.OperacionServlet;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Seguridad de la capa de presentación sin contenedor: las vistas escapan toda salida, no tienen
 * scripts ni manejadores en línea, no hay guiones largos, las cabeceras llegan también a las
 * respuestas de error y la cookie de sesión está configurada como debe.
 *
 * <p>El escape real lo hace el motor JSP al desplegar; aquí se comprueba la fuente de cada vista,
 * que es lo que decide si una expresión sale escapada.
 */
class SeguridadVistasTest {

    private static final Path VISTAS = Path.of("src", "main", "webapp", "WEB-INF", "vistas");
    private static final Path WEB_XML = Path.of("src", "main", "webapp", "WEB-INF", "web.xml");
    private static final Path FUENTES = Path.of("src");

    /**
     * Etiquetas cuyo ${...} no imprime texto del usuario: condiciones, bucles, URLs o salida escapada.
     * {@code c:param} solo añade un parámetro a una URL que {@code c:url} codifica (por ejemplo el
     * código de cuenta de un enlace "Ver movimientos").
     */
    private static final Pattern ETIQUETA_CON_EXPRESION = Pattern.compile(
            "<(c:if|c:when|c:set|c:forEach|c:out|c:url|c:param|fmt:formatNumber|jsp:param)\\b[^>]*>");

    /** Expresión que sí escapa: {@code ${fn:escapeXml(...)}} dentro del texto. */
    private static final Pattern FN_ESCAPE = Pattern.compile("\\$\\{fn:escapeXml\\([^}]*\\)}");

    private static final Pattern SCRIPT_EN_LINEA =
            Pattern.compile("<script(?![^>]*\\bsrc\\s*=)[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANEJADOR_EN_LINEA = Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESCAPE_DESACTIVADO =
            Pattern.compile("escapeXml\\s*=\\s*[\"']false", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPTLET = Pattern.compile("<%(?![@-])");

    /** Guion largo (em dash). El diseño lo prohíbe en todo texto visible, comentario o archivo. */
    private static final String GUION_LARGO = "\u2014";

    // ------------------------------------------------------------------ escape en las vistas

    @Test
    void ningunaExpresionDelUsuarioSaleSinEscapar() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path vista : jsps()) {
            String resto = ETIQUETA_CON_EXPRESION.matcher(leer(vista)).replaceAll("");
            resto = FN_ESCAPE.matcher(resto).replaceAll("");
            for (String linea : resto.split("\\R")) {
                if (linea.contains("${")) {
                    fallos.add(vista.getFileName() + ": " + linea.strip());
                }
            }
        }
        assertEquals(List.of(), fallos, "expresiones que imprimen sin c:out ni fn:escapeXml");
    }

    @Test
    void lasVistasNoDesactivanElEscapeNiUsanScriptletsNiScriptsEnLinea() throws IOException {
        for (Path vista : jsps()) {
            String texto = leer(vista);
            String nombre = vista.getFileName().toString();
            assertFalse(ESCAPE_DESACTIVADO.matcher(texto).find(), nombre + " desactiva el escape");
            assertFalse(SCRIPTLET.matcher(texto).find(), nombre + " tiene scriptlets o expresiones Java");
            assertFalse(SCRIPT_EN_LINEA.matcher(texto).find(), nombre + " tiene un script en línea");
            assertFalse(MANEJADOR_EN_LINEA.matcher(texto).find(), nombre + " tiene un manejador de evento en línea");
        }
    }

    // ------------------------------------------------------------------ texto

    @Test
    void ningunArchivoDelProyectoTieneGuionesLargos() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path archivo : archivosDeTexto()) {
            if (leer(archivo).contains(GUION_LARGO)) {
                fallos.add(archivo.toString());
            }
        }
        assertEquals(List.of(), fallos, "archivos con guion largo");
    }

    // ------------------------------------------------------------------ configuración de sesión

    @Test
    void laCookieDeSesionEsHttpOnlyYSameSiteStrictConTreintaMinutos() throws IOException {
        String xml = leer(WEB_XML);
        assertTrue(xml.contains("<http-only>true</http-only>"), "la cookie debe ser HttpOnly");
        assertTrue(xml.contains("<attribute-name>SameSite</attribute-name>"));
        assertTrue(xml.contains("<attribute-value>Strict</attribute-value>"));
        assertTrue(xml.contains("<session-timeout>30</session-timeout>"));
    }

    @Test
    void laPoliticaDeContenidoNoAdmiteEnLineaNiOrigenesAjenos() {
        String politica = FiltroSesion.POLITICA_CONTENIDO;
        assertTrue(politica.startsWith("default-src 'self'"));
        assertFalse(politica.contains("unsafe"), "sin unsafe-inline ni unsafe-eval");
        assertFalse(politica.contains("*"), "sin comodines");
        assertTrue(politica.contains("frame-ancestors 'none'"));
    }

    // ------------------------------------------------------------------ cabeceras en las respuestas

    @Test
    void unPostSinCsrfResponde403YLlevaLasCabecerasDeSeguridad() throws Exception {
        ClienteFalso servidor = new ClienteFalso();
        ServletFalso f = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("POST").ruta("/operacion").parametro("envio", "ENVIO-X")
                .parametro("modo", "deposito").parametro("cuenta", ClienteFalso.CUENTA).parametro("monto", "10.00");
        servidor.llamadas.clear();
        OperacionServlet servlet = new OperacionServlet();
        servlet.init(f.configuracion());
        FilterChain cadena = (peticion, respuesta) -> servlet.service(peticion, respuesta);

        new FiltroSesion().doFilter(f.peticion(), f.respuesta(), cadena);

        assertEquals(403, f.estado());
        assertEquals("nosniff", f.cabecera("X-Content-Type-Options"));
        assertEquals("DENY", f.cabecera("X-Frame-Options"));
        assertEquals("no-referrer", f.cabecera("Referrer-Policy"));
        assertEquals(FiltroSesion.POLITICA_CONTENIDO, f.cabecera("Content-Security-Policy"));
        assertEquals("no-store", f.cabecera("Cache-Control"));
        assertTrue(servidor.llamadas.stream().noneMatch(llamada -> llamada.startsWith("realizar")));
    }

    @Test
    void laRedireccionAlLoginTambienLlevaLasCabeceras() throws Exception {
        ServletFalso f = ServletFalso.crear().metodo("GET").ruta("/operacion");
        new FiltroSesion().doFilter(f.peticion(), f.respuesta(), f.cadena());
        assertEquals("/web/login", f.redireccion());
        assertEquals("DENY", f.cabecera("X-Frame-Options"));
        assertEquals("nosniff", f.cabecera("X-Content-Type-Options"));
        assertFalse(f.cadenaEjecutada());
    }

    // ------------------------------------------------------------------ utilidades

    private static List<Path> jsps() throws IOException {
        try (Stream<Path> archivos = Files.list(VISTAS)) {
            return archivos.filter(p -> p.getFileName().toString().endsWith(".jsp")).sorted().toList();
        }
    }

    private static List<Path> archivosDeTexto() throws IOException {
        List<String> extensiones = List.of(".java", ".jsp", ".css", ".js", ".svg", ".xml", ".html", ".md");
        try (Stream<Path> archivos = Files.walk(FUENTES)) {
            return archivos.filter(Files::isRegularFile)
                    .filter(p -> extensiones.stream().anyMatch(e -> p.getFileName().toString().endsWith(e)))
                    .sorted().toList();
        }
    }

    private static String leer(Path archivo) throws IOException {
        return Files.readString(archivo, StandardCharsets.UTF_8);
    }
}
