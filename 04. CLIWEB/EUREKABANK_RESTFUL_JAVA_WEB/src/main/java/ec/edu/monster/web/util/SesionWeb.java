package ec.edu.monster.web.util;

import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.SesionUsuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Todo lo que la capa web guarda en la {@link HttpSession}: la {@link SesionUsuario} (con el
 * token del servicio), el token CSRF, el mensaje de una sola vez (flash) y los identificadores de
 * envío de formularios.
 *
 * <p>El token del servicio vive solo aquí: nunca va en una URL, en una cookie propia ni en un
 * campo oculto.
 */
public final class SesionWeb {

    /** Atributo de sesión donde se guarda la {@link SesionUsuario}. */
    public static final String ATRIBUTO = "ec.edu.monster.web.sesionUsuario";

    /** Atributo de sesión con el token CSRF. */
    public static final String ATRIBUTO_CSRF = "ec.edu.monster.web.csrf";

    /** Atributo de sesión con el mensaje que se muestra una sola vez. */
    public static final String ATRIBUTO_FLASH = "ec.edu.monster.web.flash";

    /** Atributo de sesión con los identificadores de envío todavía válidos. */
    public static final String ATRIBUTO_ENVIOS = "ec.edu.monster.web.envios";

    /** Nombre del campo oculto que lleva el token CSRF en todos los formularios POST. */
    public static final String CAMPO_CSRF = "_csrf";

    /** Longitud máxima que se acepta en un campo de texto corto (usuario, cédula, cuenta). */
    public static final int LONGITUD_MAXIMA = 64;

    /** Cuántos formularios de operación pueden estar abiertos a la vez (varias pestañas). */
    public static final int ENVIOS_PENDIENTES_MAXIMOS = 10;

    /** Código del servidor cuando el token no es válido. */
    public static final String CODIGO_TOKEN_INVALIDO = "AUTH-003";

    /** Código del servidor cuando el token expiró. */
    public static final String CODIGO_TOKEN_EXPIRADO = "AUTH-004";

    /** Mensaje cuando la sesión HTTP caducó antes de que el servidor lo avise. */
    public static final String MENSAJE_SESION_CADUCADA =
            "El token de sesión ha expirado, inicie sesión nuevamente";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SesionWeb() {
    }

    /**
     * Guarda la sesión del usuario recién autenticado. Cambia el identificador de la sesión HTTP
     * (contra la fijación de sesión) y genera un token CSRF nuevo.
     *
     * @param peticion petición actual
     * @param sesion   datos del usuario, con el token del servicio
     */
    public static void iniciar(HttpServletRequest peticion, SesionUsuario sesion) {
        Objects.requireNonNull(sesion, "La sesión del usuario es obligatoria");
        peticion.getSession(true);
        peticion.changeSessionId();
        HttpSession http = peticion.getSession(true);
        http.setAttribute(ATRIBUTO, sesion);
        http.setAttribute(ATRIBUTO_CSRF, aleatorio(32));
    }

    /**
     * @param peticion petición actual
     * @return la sesión del usuario, o {@code null} si no hay sesión HTTP o no ha ingresado
     */
    public static SesionUsuario sesionDe(HttpServletRequest peticion) {
        HttpSession http = peticion.getSession(false);
        if (http == null) {
            return null;
        }
        Object valor = http.getAttribute(ATRIBUTO);
        return valor instanceof SesionUsuario ? (SesionUsuario) valor : null;
    }

    /**
     * @param peticion petición actual
     * @return el token del servicio, o {@code null} si no hay sesión vigente
     */
    public static String tokenDe(HttpServletRequest peticion) {
        SesionUsuario sesion = sesionDe(peticion);
        return sesion != null && sesion.estaVigente() ? sesion.getToken() : null;
    }

    /**
     * Invalida la sesión HTTP, si existe.
     *
     * @param peticion petición actual
     */
    public static void cerrar(HttpServletRequest peticion) {
        HttpSession http = peticion.getSession(false);
        if (http != null) {
            http.invalidate();
        }
    }

    /**
     * @param peticion petición actual
     * @return {@code true} si el navegador mandó una cookie de una sesión que ya no existe
     */
    public static boolean esSesionCaducada(HttpServletRequest peticion) {
        return peticion.getRequestedSessionId() != null && !peticion.isRequestedSessionIdValid();
    }

    /**
     * Exige un usuario con sesión vigente. Si no lo hay, redirige al login.
     *
     * @param peticion  petición actual
     * @param respuesta respuesta actual
     * @return la sesión del usuario, o {@code null} si ya se redirigió al login
     * @throws IOException si falla la redirección
     */
    public static SesionUsuario exigir(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws IOException {
        SesionUsuario sesion = sesionDe(peticion);
        if (sesion == null || !sesion.estaVigente()) {
            respuesta.sendRedirect(peticion.getContextPath() + "/login");
            return null;
        }
        return sesion;
    }

    // ------------------------------------------------------------------ CSRF

    /**
     * @param peticion petición actual
     * @return el token CSRF de la sesión; crea la sesión y el token si hacen falta
     */
    public static String tokenCsrf(HttpServletRequest peticion) {
        HttpSession http = peticion.getSession(true);
        Object actual = http.getAttribute(ATRIBUTO_CSRF);
        if (actual instanceof String && !((String) actual).isEmpty()) {
            return (String) actual;
        }
        String nuevo = aleatorio(32);
        http.setAttribute(ATRIBUTO_CSRF, nuevo);
        return nuevo;
    }

    /**
     * @param peticion petición POST actual
     * @return {@code true} si el campo {@code _csrf} coincide con el token de la sesión
     */
    public static boolean csrfValido(HttpServletRequest peticion) {
        HttpSession http = peticion.getSession(false);
        if (http == null) {
            return false;
        }
        Object esperado = http.getAttribute(ATRIBUTO_CSRF);
        String recibido = peticion.getParameter(CAMPO_CSRF);
        if (!(esperado instanceof String) || recibido == null || recibido.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(((String) esperado).getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Responde 403 con la página de error, sin llamar al servicio.
     *
     * @param peticion  petición actual
     * @param respuesta respuesta actual
     * @throws ServletException si falla el reenvío
     * @throws IOException      si falla el reenvío
     */
    public static void denegar(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        respuesta.setStatus(HttpServletResponse.SC_FORBIDDEN);
        peticion.setAttribute("estado", HttpServletResponse.SC_FORBIDDEN);
        peticion.setAttribute("titulo", "Solicitud no verificada");
        peticion.setAttribute("detalle",
                "No se pudo verificar la solicitud. Vuelva a la página anterior, recárguela e intente de nuevo.");
        peticion.getRequestDispatcher("/WEB-INF/vistas/error.jsp").forward(peticion, respuesta);
    }

    // ------------------------------------------------------------------ mensaje de una sola vez

    /**
     * Guarda un mensaje que se muestra en la siguiente página y luego desaparece (patrón
     * Post/Redirect/Get).
     *
     * @param peticion petición actual
     * @param mensaje  mensaje, tal como lo arman {@link #mensajeExito} y {@link #mensajeError}
     */
    public static void guardarMensaje(HttpServletRequest peticion, Map<String, Object> mensaje) {
        peticion.getSession(true).setAttribute(ATRIBUTO_FLASH, mensaje);
    }

    /**
     * @param peticion petición actual
     * @return el mensaje pendiente, que se borra al leerlo, o {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> tomarMensaje(HttpServletRequest peticion) {
        HttpSession http = peticion.getSession(false);
        if (http == null) {
            return null;
        }
        Object valor = http.getAttribute(ATRIBUTO_FLASH);
        http.removeAttribute(ATRIBUTO_FLASH);
        return valor instanceof Map ? (Map<String, Object>) valor : null;
    }

    /**
     * @param codigo código del servidor, puede ser {@code null}
     * @param texto  mensaje literal
     * @return un mensaje de éxito
     */
    public static Map<String, Object> mensajeExito(String codigo, String texto) {
        return mensaje("exito", codigo, texto);
    }

    /**
     * @param codigo código del servidor, puede ser {@code null} si el error es del cliente
     * @param texto  mensaje literal
     * @return un mensaje de error
     */
    public static Map<String, Object> mensajeError(String codigo, String texto) {
        return mensaje("error", codigo, texto);
    }

    /**
     * @param respuesta sobre del servidor
     * @return un mensaje con el código y el texto literales del servidor; es de éxito solo si el
     *         estado es {@code OK}
     */
    public static Map<String, Object> mensajeDe(RespuestaBase respuesta) {
        boolean ok = respuesta != null && "OK".equals(respuesta.getEstado());
        return mensaje(ok ? "exito" : "error", respuesta == null ? null : respuesta.getCodigo(),
                respuesta == null ? null : respuesta.getMensaje());
    }

    /**
     * @return el mensaje que se muestra cuando el servicio no responde (SER-001), sin detalles
     *         internos ni trazas
     */
    public static Map<String, Object> mensajeServicioCaido() {
        return mensajeError(ExcepcionServicio.CODIGO, ExcepcionServicio.MENSAJE);
    }

    private static Map<String, Object> mensaje(String tipo, String codigo, String texto) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("tipo", tipo);
        mensaje.put("codigo", codigo == null ? "" : codigo);
        mensaje.put("texto", texto == null ? "" : texto);
        return mensaje;
    }

    // ------------------------------------------------------------------ rechazo del servidor

    /**
     * @param respuesta sobre del servidor
     * @return {@code true} si el servidor dice que el token no es válido o expiró
     */
    public static boolean esRechazoDeSesion(RespuestaBase respuesta) {
        if (respuesta == null) {
            return false;
        }
        return CODIGO_TOKEN_INVALIDO.equals(respuesta.getCodigo())
                || CODIGO_TOKEN_EXPIRADO.equals(respuesta.getCodigo());
    }

    /**
     * Invalida la sesión y vuelve al login con el mensaje del servidor.
     *
     * @param peticion  petición actual
     * @param respuesta respuesta actual
     * @param rechazo   sobre del servidor con AUTH-003 o AUTH-004
     * @throws IOException si falla la redirección
     */
    public static void expulsar(HttpServletRequest peticion, HttpServletResponse respuesta,
            RespuestaBase rechazo) throws IOException {
        cerrar(peticion);
        guardarMensaje(peticion, mensajeDe(rechazo));
        respuesta.sendRedirect(peticion.getContextPath() + "/login");
    }

    // ------------------------------------------------------------------ vistas

    /**
     * Deja en la petición lo que toda página del sistema necesita: el módulo activo, el usuario
     * y el token CSRF para el formulario de cierre de sesión.
     *
     * @param peticion petición actual
     * @param modulo   nombre del módulo activo
     */
    public static void prepararVista(HttpServletRequest peticion, String modulo) {
        peticion.setAttribute("modulo", modulo);
        peticion.setAttribute("usuario", sesionDe(peticion));
        peticion.setAttribute("csrf", tokenCsrf(peticion));
    }

    // ------------------------------------------------------------------ envíos únicos

    /**
     * Crea un identificador de envío para un formulario de operación. Cada identificador sirve
     * una sola vez, así recargar o pulsar dos veces no repite un depósito.
     *
     * @param peticion petición actual
     * @return el identificador, que va en un campo oculto
     */
    public static String nuevoEnvio(HttpServletRequest peticion) {
        HttpSession http = peticion.getSession(true);
        String id = aleatorio(16);
        synchronized (http) {
            Set<String> envios = enviosDe(http);
            envios.add(id);
            Iterator<String> it = envios.iterator();
            while (envios.size() > ENVIOS_PENDIENTES_MAXIMOS && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return id;
    }

    /**
     * @param peticion petición actual
     * @param id       identificador recibido en el formulario
     * @return {@code true} la primera vez que se presenta un identificador válido; después
     *         {@code false}
     */
    public static boolean consumirEnvio(HttpServletRequest peticion, String id) {
        HttpSession http = peticion.getSession(false);
        if (http == null || id == null || id.isEmpty()) {
            return false;
        }
        synchronized (http) {
            return enviosDe(http).remove(id);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> enviosDe(HttpSession http) {
        Object actual = http.getAttribute(ATRIBUTO_ENVIOS);
        if (actual instanceof Set) {
            return (Set<String>) actual;
        }
        Set<String> nuevo = new LinkedHashSet<>();
        http.setAttribute(ATRIBUTO_ENVIOS, nuevo);
        return nuevo;
    }

    // ------------------------------------------------------------------ entradas de texto

    /**
     * @param valor texto recibido, puede ser {@code null}
     * @return el texto sin espacios en los extremos, o cadena vacía si era {@code null}
     */
    public static String normalizar(String valor) {
        return valor == null ? "" : valor.strip();
    }

    /**
     * Quita los caracteres de control (U+0000 a U+001F y U+007F), como el NUL, el tabulador o el
     * retorno de carro, antes de que un valor se refleje en la vista o se envíe al servicio.
     *
     * @param valor texto recibido, puede ser {@code null}
     * @return el texto sin caracteres de control, o cadena vacía si era {@code null}
     */
    public static String quitarControl(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder limpio = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c > 0x1F && c != 0x7F) {
                limpio.append(c);
            }
        }
        return limpio.toString();
    }

    /**
     * @param valor texto ya normalizado
     * @return {@code true} si cabe en {@value #LONGITUD_MAXIMA} caracteres y no tiene caracteres
     *         de control
     */
    public static boolean esEntradaSegura(String valor) {
        if (valor == null || valor.length() > LONGITUD_MAXIMA) {
            return false;
        }
        for (int i = 0; i < valor.length(); i++) {
            if (Character.isISOControl(valor.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String aleatorio(int bytes) {
        byte[] datos = new byte[bytes];
        ALEATORIO.nextBytes(datos);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }
}
