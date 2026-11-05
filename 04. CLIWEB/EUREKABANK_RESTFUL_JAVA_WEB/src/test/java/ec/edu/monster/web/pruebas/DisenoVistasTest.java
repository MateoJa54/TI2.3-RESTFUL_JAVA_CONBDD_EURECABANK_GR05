package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Comprobaciones de la fuente del rediseño (diseno_v2.md): etiquetas en cada campo, control
 * segmentado sin JavaScript, CSRF en cada POST, tarjeta de resultado escapada, tokens y radios
 * del CSS, sin estilos en línea y sin recursos externos.
 *
 * <p>No renderiza las páginas: el motor JSP escapa al desplegar, y aquí se revisa la fuente que
 * decide si cada expresión sale escapada.
 */
class DisenoVistasTest {

    private static final Path VISTAS = Path.of("src", "main", "webapp", "WEB-INF", "vistas");
    private static final Path CSS = Path.of("src", "main", "webapp", "css", "app.css");
    private static final Path JS = Path.of("src", "main", "webapp", "js", "app.js");

    private static final Pattern INPUT = Pattern.compile("<input\\b[^>]*>");
    private static final Pattern ID = Pattern.compile("\\sid=\"([^\"]+)\"");
    private static final Pattern RADIO = Pattern.compile(
            "<input type=\"radio\" id=\"modo-(\\w+)\" name=\"modo\" value=\"(\\w+)\"");
    private static final Pattern ESTILO_EN_LINEA = Pattern.compile("\\sstyle\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern BORDE_REDONDEADO = Pattern.compile("border-radius\\s*:\\s*([^;}]+);");
    private static final Pattern PROPIEDAD_ACENTO = Pattern.compile("--acento[a-z-]*");

    // ------------------------------------------------------------------ formularios

    @Test
    void cadaCampoVisibleTieneSuEtiquetaConFor() throws IOException {
        List<String> fallos = new ArrayList<>();
        for (Path vista : vistas()) {
            String texto = leer(vista);
            Matcher entrada = INPUT.matcher(texto);
            while (entrada.find()) {
                String etiqueta = entrada.group();
                if (etiqueta.contains("type=\"hidden\"")) {
                    continue;
                }
                Matcher id = ID.matcher(etiqueta);
                if (id.find() && !texto.contains("<label for=\"" + id.group(1) + "\">")) {
                    fallos.add(vista.getFileName() + ": " + id.group(1));
                }
            }
        }
        assertEquals(List.of(), fallos, "campos sin <label for>");
    }

    @Test
    void elControlDeOperacionEsUnSegmentadoDeTresRadiosSinJavaScript() throws IOException {
        String operacion = leer(VISTAS.resolve("operacion.jsp"));
        Matcher radio = RADIO.matcher(operacion);
        List<String> valores = new ArrayList<>();
        while (radio.find()) {
            valores.add(radio.group(2));
            assertEquals(radio.group(1), radio.group(2), "el id y el valor del radio coinciden");
        }
        assertEquals(List.of("deposito", "retiro", "transferencia"), valores);
        assertTrue(operacion.contains("<fieldset>"), "el grupo de radios va en un fieldset");
        assertTrue(operacion.contains("<legend>Tipo de operación</legend>"));
    }

    @Test
    void elCampoDeDestinoSoloSeMuestraEnTransferenciaSinDependerDeJavaScript() throws IOException {
        String operacion = leer(VISTAS.resolve("operacion.jsp"));
        String css = leer(CSS);
        assertTrue(operacion.contains("campo campo-destino"));
        assertTrue(css.contains("@supports selector(:has(*))"), "la regla depende de :has con respaldo");
        assertTrue(css.contains("#modo-transferencia:checked) .campo-destino"));
    }

    @Test
    void cadaFormularioPostLlevaElTokenCsrf() throws IOException {
        int posts = 0;
        int csrf = 0;
        for (Path vista : vistas()) {
            String texto = leer(vista);
            posts += contar(texto, "method=\"post\"");
            csrf += contar(texto, "name=\"_csrf\"");
        }
        assertTrue(posts >= 3, "login, operación y cierre de sesión deben ser POST");
        assertEquals(posts, csrf, "cada POST lleva su campo _csrf");
    }

    @Test
    void elAvisoDeLaSesionTieneRolEnLaVistaCompartida() throws IOException {
        String mensaje = leer(VISTAS.resolve("_mensaje.jsp"));
        assertTrue(mensaje.contains("role=\"status\""), "el éxito se anuncia como status");
        assertTrue(mensaje.contains("role=\"alert\""), "el error se anuncia como alert");
    }

    // ------------------------------------------------------------------ escape de los datos de resultado

    @Test
    void laTarjetaDeResultadoEscapaCadaDatoConCOut() throws IOException {
        String operacion = leer(VISTAS.resolve("operacion.jsp"));
        for (String dato : List.of("mensaje.numero", "mensaje.numeroOrigen", "mensaje.numeroDestino")) {
            assertTrue(operacion.contains("<c:out value=\"${" + dato + "}\"/>"), dato + " sin c:out");
        }
        String mensaje = leer(VISTAS.resolve("_mensaje.jsp"));
        assertTrue(mensaje.contains("<c:out value=\"${mensaje.texto}\"/>"), "el texto del servidor sale escapado");
        assertTrue(mensaje.contains("<c:out value=\"${mensaje.codigo}\"/>"), "el código sale escapado");
    }

    @Test
    void lasCifrasDeLaTarjetaSeFormateanConFmtSinTextoLibre() throws IOException {
        String operacion = leer(VISTAS.resolve("operacion.jsp"));
        assertTrue(operacion.contains("<fmt:formatNumber value=\"${mensaje.saldoNuevo}\""));
        assertTrue(operacion.contains("<fmt:formatNumber value=\"${mensaje.saldoOrigen}\""));
    }

    @Test
    void ningunaVistaNiScriptNiArchivoJsUsaEstiloEnLineaNiInnerHtml() throws IOException {
        for (Path vista : vistas()) {
            assertFalse(ESTILO_EN_LINEA.matcher(leer(vista)).find(), vista.getFileName() + " tiene style=");
        }
        String js = leer(JS);
        assertFalse(js.contains("innerHTML"), "app.js no inserta HTML");
        assertFalse(js.contains("eval("), "app.js no evalúa código");
    }

    // ------------------------------------------------------------------ CSS

    @Test
    void losRadiosSonSoloOchoYDoce() throws IOException {
        Matcher radio = BORDE_REDONDEADO.matcher(leer(CSS));
        List<String> usados = new ArrayList<>();
        while (radio.find()) {
            usados.add(radio.group(1).strip());
        }
        assertFalse(usados.isEmpty());
        for (String valor : usados) {
            assertTrue(valor.equals("var(--radio-control)") || valor.equals("var(--radio-tarjeta)"),
                    "radio no permitido: " + valor);
        }
    }

    @Test
    void elAcentoEsUnaSolaVariableConSusTonosDerivados() throws IOException {
        Set<String> nombres = new TreeSet<>();
        Matcher m = PROPIEDAD_ACENTO.matcher(leer(CSS));
        while (m.find()) {
            nombres.add(m.group());
        }
        assertEquals(Set.of("--acento", "--acento-hover", "--acento-tinte"), nombres);
    }

    @Test
    void lasFichasDelDisenoEstanEnLosTokens() throws IOException {
        String css = leer(CSS);
        for (String token : List.of("#0B7A8A", "#E3F3F5", "#1E7A4F", "#E6F4EC", "#B42318", "#FDECEA",
                "#55636F", "#EEF3F6", "#6FD3A0", "#12301F", "#FF9A8F", "#3A1713", "#9FB0BB", "#1C2B35",
                "#12323A", "#3CC0D0")) {
            assertTrue(css.contains(token), "falta el token " + token);
        }
    }

    @Test
    void elCssNoCargaNadaDeFueraNiUsaImportacionesNiUrlExternas() throws IOException {
        String css = leer(CSS);
        assertFalse(css.contains("http"), "sin recursos remotos");
        assertFalse(css.contains("@import"), "sin importaciones");
        assertFalse(css.contains("url("), "sin url(): el CSS no carga imágenes ni fuentes");
    }

    @Test
    void elCssTieneModoOscuroYMovimientoReducido() throws IOException {
        String css = leer(CSS);
        assertTrue(css.contains("@media (prefers-color-scheme: dark)"));
        assertTrue(css.contains("@media (prefers-reduced-motion: reduce)"));
        assertTrue(css.contains("@media (max-width: 859px)"), "la barra inferior empieza por debajo de 860 px");
    }

    @Test
    void lasAnimacionesSonSoloDeOpacidadYTransformacionSinBucles() throws IOException {
        String css = leer(CSS);
        assertFalse(css.contains("infinite"), "sin animaciones infinitas");
        assertFalse(css.contains("@keyframes pulso"), "el esqueleto es estático");
        assertTrue(css.contains("@keyframes entrar"));
    }

    // ------------------------------------------------------------------ navegación

    @Test
    void laBarraLateralTieneLosCuatroDestinosYElCierreDeSesion() throws IOException {
        String inicio = leer(VISTAS.resolve("_inicio.jsp"));
        for (String ruta : List.of("/cuentas", "/movimientos", "/operacion", "/costos")) {
            assertTrue(inicio.contains("<c:url value='" + ruta + "'/>"), ruta + " falta en la navegación");
        }
        assertTrue(inicio.contains("aria-current=\"page\""));
        assertTrue(inicio.contains("class=\"lateral-movil\""), "la barra inferior trae su propio cierre de sesión");
        assertTrue(inicio.contains("<c:out value=\"${iniciales}\"/>"));
    }

    @Test
    void elLoginTraeLaClaveConMostrarOcultarOcultoHastaQueCorraElScript() throws IOException {
        String login = leer(VISTAS.resolve("login.jsp"));
        assertTrue(login.contains("id=\"mostrar-clave\""));
        assertTrue(login.contains("aria-pressed=\"false\""));
        assertTrue(login.contains("id=\"mostrar-clave\" aria-controls=\"clave\" aria-pressed=\"false\" hidden"),
                "sin JavaScript no aparece un botón que no hace nada");
        assertTrue(login.contains("<span class=\"texto-procesando\">Procesando</span>"));
    }

    @Test
    void lasCuentasSeAbrenConSuCodigoEnLaUrlCodificadaPorCUrl() throws IOException {
        String cuentas = leer(VISTAS.resolve("cuentas.jsp"));
        for (String ruta : List.of("/movimientos", "/costos", "/operacion")) {
            assertTrue(cuentas.contains("<c:url value='" + ruta + "'><c:param name='cuenta' value='${cuenta.codigo}'/></c:url>"),
                    ruta + " debe llevar la cuenta por c:param");
        }
    }

    // ------------------------------------------------------------------ texto

    @Test
    void lasOpcionesDeMovimientosMarcanElSignoYLaPalabra() throws IOException {
        String movimientos = leer(VISTAS.resolve("movimientos.jsp"));
        assertTrue(movimientos.contains(">+<fmt:formatNumber"), "el ingreso lleva el signo");
        assertTrue(movimientos.contains(">-<fmt:formatNumber"), "la salida lleva el signo");
        assertTrue(movimientos.contains("<span class=\"chip chip-exito\">Ingreso</span>"));
        assertTrue(movimientos.contains("<span class=\"chip chip-error\">Salida</span>"));
    }

    @Test
    void losTextosDeLosEstadosVaciosSonLosDelDiseno() throws IOException {
        String cuentas = leer(VISTAS.resolve("cuentas.jsp"));
        assertTrue(cuentas.contains("Busque un cliente"));
        assertTrue(leer(VISTAS.resolve("movimientos.jsp")).contains("Esta cuenta aún no tiene movimientos"));
    }

    // ------------------------------------------------------------------ utilidades

    private static int contar(String texto, String fragmento) {
        int total = 0;
        int desde = 0;
        while ((desde = texto.indexOf(fragmento, desde)) >= 0) {
            total++;
            desde += fragmento.length();
        }
        return total;
    }

    private static List<Path> vistas() throws IOException {
        try (Stream<Path> archivos = Files.list(VISTAS)) {
            return archivos.filter(p -> p.getFileName().toString().endsWith(".jsp")).sorted().toList();
        }
    }

    private static String leer(Path archivo) throws IOException {
        return Files.readString(archivo, StandardCharsets.UTF_8);
    }
}
