package ec.edu.monster.web.filtro;

import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Exige sesión en todas las rutas salvo {@code /login} y los recursos estáticos, y añade las
 * cabeceras de seguridad a toda respuesta.
 *
 * <p>La ruta {@code /} redirige a {@code /cuentas} si hay sesión o a {@code /login} si no.
 */
@WebFilter(filterName = "FiltroSesion", urlPatterns = "/*")
public class FiltroSesion implements Filter {

    /** Política de contenido: sin scripts ni estilos en línea, nada de orígenes externos. */
    public static final String POLITICA_CONTENIDO =
            "default-src 'self'; img-src 'self' data:; style-src 'self'; frame-ancestors 'none'"
            + "; base-uri 'self'; form-action 'self'";

    /** Nombre de la cookie de sesión que el contenedor crea. */
    static final String COOKIE_SESION = "JSESSIONID";

    /** Métodos que atiende la aplicación. Cualquier otro responde 405 con la página de error. */
    private static final Set<String> METODOS_PERMITIDOS = Set.of("GET", "HEAD", "POST");

    /** Rutas sin sesión. Un sufijo {@code /*} cubre todo lo que cuelga de esa carpeta. */
    private final Set<String> rutasPublicas = Set.of("/login", "/css/*", "/img/*", "/js/*");

    @Override
    public void doFilter(ServletRequest peticion, ServletResponse respuesta, FilterChain cadena)
            throws IOException, ServletException {
        if (!(peticion instanceof HttpServletRequest) || !(respuesta instanceof HttpServletResponse)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        HttpServletRequest http = (HttpServletRequest) peticion;
        HttpServletResponse salida = (HttpServletResponse) respuesta;
        aplicarCabeceras(salida);
        RespuestaSesion envuelta = new RespuestaSesion(salida);

        String metodo = http.getMethod() == null ? "" : http.getMethod();
        if (!METODOS_PERMITIDOS.contains(metodo)) {
            rechazarMetodo(http, envuelta);
            return;
        }

        String ruta = rutaDe(http);
        if (ruta.isEmpty() || "/".equals(ruta)) {
            boolean conSesion = SesionWeb.tokenDe(http) != null;
            envuelta.sendRedirect(http.getContextPath() + (conSesion ? "/cuentas" : "/login"));
            return;
        }
        if (!esEstatica(ruta)) {
            envuelta.setHeader("Cache-Control", "no-store");
        }
        if (esRutaPublica(ruta)) {
            cadena.doFilter(peticion, envuelta);
            reforzarSameSite(salida);
            return;
        }
        if (SesionWeb.tokenDe(http) == null) {
            if (SesionWeb.esSesionCaducada(http)) {
                SesionWeb.guardarMensaje(http, SesionWeb.mensajeError(
                        SesionWeb.CODIGO_TOKEN_EXPIRADO, SesionWeb.MENSAJE_SESION_CADUCADA));
            }
            envuelta.sendRedirect(http.getContextPath() + "/login");
            return;
        }
        cadena.doFilter(peticion, envuelta);
        reforzarSameSite(salida);
    }

    /**
     * Responde 405 con la página de error del diseño, sin traza ni cuerpo vacío, y sin llamar a
     * ningún servlet.
     */
    private static void rechazarMetodo(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        respuesta.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        peticion.setAttribute("estado", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        peticion.getRequestDispatcher("/WEB-INF/vistas/error.jsp").forward(peticion, respuesta);
    }

    /**
     * Pone {@code SameSite=Strict} en la cookie {@code JSESSIONID} de la respuesta. El contenedor
     * no aplica el atributo que se declara en {@code web.xml}, así que se reescribe la cabecera
     * {@code Set-Cookie}: se quita cualquier {@code SameSite} previo y se añade uno solo. Los demás
     * atributos (HttpOnly, Path, Secure) se conservan y las demás cookies no se tocan.
     *
     * <p>Solo tiene efecto si la cookie ya está en las cabeceras; no crea sesiones ni cookies.
     *
     * @param respuesta respuesta, todavía sin enviar
     */
    public static void reforzarSameSite(HttpServletResponse respuesta) {
        if (respuesta.isCommitted()) {
            return;
        }
        Collection<String> actuales = respuesta.getHeaders("Set-Cookie");
        if (actuales == null || actuales.isEmpty()) {
            return;
        }
        List<String> nuevas = new ArrayList<>();
        boolean cambio = false;
        for (String cookie : actuales) {
            if (cookie == null || !esCookieDeSesion(cookie)) {
                nuevas.add(cookie);
                continue;
            }
            String ajustada = sinAtributoSameSite(cookie) + "; SameSite=Strict";
            cambio |= !ajustada.equals(cookie);
            nuevas.add(ajustada);
        }
        if (!cambio) {
            return;
        }
        respuesta.setHeader("Set-Cookie", nuevas.get(0));
        for (String cookie : nuevas.subList(1, nuevas.size())) {
            respuesta.addHeader("Set-Cookie", cookie);
        }
    }

    private static boolean esCookieDeSesion(String cookie) {
        String prefijo = COOKIE_SESION + "=";
        return cookie.regionMatches(true, 0, prefijo, 0, prefijo.length());
    }

    private static String sinAtributoSameSite(String cookie) {
        List<String> conservados = new ArrayList<>();
        for (String atributo : cookie.split(";", -1)) {
            if (!atributo.strip().regionMatches(true, 0, "SameSite=", 0, "SameSite=".length())) {
                conservados.add(atributo);
            }
        }
        return String.join(";", conservados);
    }

    /**
     * Respuesta que reescribe la cookie de sesión justo antes de que la respuesta salga (redirección,
     * error, vaciado del búfer o primera escritura), porque después el contenedor ya no deja tocar
     * las cabeceras. Así el {@code Set-Cookie} que crea el login o el primer acceso también lleva
     * {@code SameSite=Strict}.
     */
    private static final class RespuestaSesion extends HttpServletResponseWrapper {

        private final HttpServletResponse original;

        RespuestaSesion(HttpServletResponse original) {
            super(original);
            this.original = original;
        }

        @Override
        public void sendRedirect(String ubicacion) throws IOException {
            reforzarSameSite(original);
            super.sendRedirect(ubicacion);
        }

        @Override
        public void sendError(int codigo) throws IOException {
            reforzarSameSite(original);
            super.sendError(codigo);
        }

        @Override
        public void sendError(int codigo, String mensaje) throws IOException {
            reforzarSameSite(original);
            super.sendError(codigo, mensaje);
        }

        @Override
        public void flushBuffer() throws IOException {
            reforzarSameSite(original);
            super.flushBuffer();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            reforzarSameSite(original);
            return super.getWriter();
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() throws IOException {
            reforzarSameSite(original);
            return super.getOutputStream();
        }
    }

    /**
     * @param ruta ruta de la petición sin el contexto de la aplicación, por ejemplo {@code /login}
     * @return {@code true} si la ruta se atiende sin sesión
     */
    public boolean esRutaPublica(String ruta) {
        if (ruta == null || ruta.isEmpty() || tieneRecorrido(ruta)) {
            return false;
        }
        for (String publica : rutasPublicas) {
            if (publica.endsWith("/*")) {
                String carpeta = publica.substring(0, publica.length() - 1);
                if (ruta.startsWith(carpeta) && ruta.length() > carpeta.length()) {
                    return true;
                }
            } else if (publica.equals(ruta)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cabeceras que protegen toda respuesta: sin adivinar tipos, sin marcos, sin referencia y con
     * política de contenido.
     *
     * @param respuesta respuesta a la que se añaden
     */
    public static void aplicarCabeceras(HttpServletResponse respuesta) {
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        respuesta.setHeader("X-Frame-Options", "DENY");
        respuesta.setHeader("Referrer-Policy", "no-referrer");
        respuesta.setHeader("Content-Security-Policy", POLITICA_CONTENIDO);
    }

    private static boolean esEstatica(String ruta) {
        return ruta.startsWith("/css/") || ruta.startsWith("/img/") || ruta.startsWith("/js/");
    }

    private static boolean tieneRecorrido(String ruta) {
        for (String segmento : ruta.split("/", -1)) {
            if (segmento.equals("..") || segmento.equals(".")) {
                return true;
            }
        }
        return ruta.indexOf('\\') >= 0 || ruta.toLowerCase().contains("%2e") || ruta.toLowerCase().contains("%2f");
    }

    private static String rutaDe(HttpServletRequest peticion) {
        String uri = peticion.getRequestURI() == null ? "" : peticion.getRequestURI();
        String contexto = peticion.getContextPath() == null ? "" : peticion.getContextPath();
        String ruta = uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
        int parametros = ruta.indexOf(';');
        if (parametros >= 0) {
            ruta = ruta.substring(0, parametros);
        }
        int consulta = ruta.indexOf('?');
        if (consulta >= 0) {
            ruta = ruta.substring(0, consulta);
        }
        return ruta;
    }
}
