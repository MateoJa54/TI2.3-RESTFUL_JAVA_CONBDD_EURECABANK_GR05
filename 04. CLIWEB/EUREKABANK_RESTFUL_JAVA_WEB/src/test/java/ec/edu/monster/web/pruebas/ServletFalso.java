package ec.edu.monster.web.pruebas;

import ec.edu.monster.web.controlador.ControladorAutenticacion;
import ec.edu.monster.web.modelo.SesionUsuario;
import ec.edu.monster.web.util.ConfiguracionWeb;
import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Petición, respuesta, sesión, contexto y configuración falsos para probar servlets y filtro sin
 * contenedor. Cada interfaz de Jakarta Servlet se fabrica con {@link Proxy}.
 *
 * <p>La sesión se comporta como la real en lo que importa: no existe hasta que alguien la pide
 * con {@code getSession(true)}, cambia de identificador con {@code changeSessionId} y lanza
 * {@link IllegalStateException} si se usa después de invalidarla.
 */
final class ServletFalso {

    static final String CONTEXTO = "/web";

    private final Map<String, String> parametros = new HashMap<>();
    private final Map<String, Object> atributos = new HashMap<>();
    private final Map<String, Object> atributosContexto = new HashMap<>();
    private final Map<String, String> parametrosContexto = new HashMap<>();
    private final Map<String, String> cabecerasPeticion = new HashMap<>();
    private final Map<String, List<String>> cabecerasRespuesta = new LinkedHashMap<>();
    private final List<String> reenvios = new ArrayList<>();
    private final List<Sesion> sesionesCreadas = new ArrayList<>();

    private String metodo = "GET";
    private String uri = CONTEXTO + "/prueba";
    private String idSolicitado;
    private boolean idSolicitadoValido;
    private int estado = 200;
    private String redireccion;
    private int errorEnviado;
    private boolean comprometida;
    private int cambiosDeId;
    private boolean cadenaEjecutada;
    private Sesion sesion;

    private final HttpServletRequest peticion;
    private final HttpServletResponse respuesta;
    private final ServletContext contexto;
    private final ServletConfig configuracion;

    /** Sesión HTTP falsa. */
    private final class Sesion {
        final Map<String, Object> atributos = new HashMap<>();
        String id;
        boolean invalida;
        int inactividad;
        HttpSession proxy;

        Sesion(String id) {
            this.id = id;
            proxy = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                    new Class<?>[] {HttpSession.class}, (p, m, a) -> {
                        if (invalida && !m.getName().equals("isNew")) {
                            throw new IllegalStateException("La sesión ya fue invalidada");
                        }
                        switch (m.getName()) {
                        case "getAttribute":
                            return this.atributos.get(a[0]);
                        case "setAttribute":
                            this.atributos.put((String) a[0], a[1]);
                            return null;
                        case "removeAttribute":
                            this.atributos.remove(a[0]);
                            return null;
                        case "getId":
                            return this.id;
                        case "invalidate":
                            invalida = true;
                            this.atributos.clear();
                            if (ServletFalso.this.sesion == this) {
                                ServletFalso.this.sesion = null;
                            }
                            return null;
                        case "setMaxInactiveInterval":
                            inactividad = (Integer) a[0];
                            return null;
                        case "getMaxInactiveInterval":
                            return inactividad;
                        default:
                            return porDefecto(m);
                        }
                    });
        }
    }

    private ServletFalso() {
        contexto = (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[] {ServletContext.class}, (p, m, a) -> {
                    switch (m.getName()) {
                    case "getAttribute":
                        return atributosContexto.get(a[0]);
                    case "setAttribute":
                        atributosContexto.put((String) a[0], a[1]);
                        return null;
                    case "getInitParameter":
                        return parametrosContexto.get(a[0]);
                    case "getContextPath":
                        return CONTEXTO;
                    default:
                        return porDefecto(m);
                    }
                });
        configuracion = (ServletConfig) Proxy.newProxyInstance(ServletConfig.class.getClassLoader(),
                new Class<?>[] {ServletConfig.class}, (p, m, a) -> {
                    switch (m.getName()) {
                    case "getServletContext":
                        return contexto;
                    case "getServletName":
                        return "prueba";
                    default:
                        return porDefecto(m);
                    }
                });
        peticion = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (p, m, a) -> {
                    switch (m.getName()) {
                    case "getParameter":
                        return parametros.get(a[0]);
                    case "getHeader":
                        return cabecerasPeticion.get(a[0]);
                    case "getAttribute":
                        return atributos.get(a[0]);
                    case "setAttribute":
                        atributos.put((String) a[0], a[1]);
                        return null;
                    case "removeAttribute":
                        atributos.remove(a[0]);
                        return null;
                    case "getMethod":
                        return metodo;
                    case "getRequestURI":
                        return uri;
                    case "getContextPath":
                        return CONTEXTO;
                    case "getServletPath":
                        return uri.startsWith(CONTEXTO) ? uri.substring(CONTEXTO.length()) : uri;
                    case "getProtocol":
                        return "HTTP/1.1";
                    case "getServletContext":
                        return contexto;
                    case "getSession":
                        boolean crear = a == null || a.length == 0 || (Boolean) a[0];
                        if (sesion == null && crear) {
                            sesion = new Sesion("SESION-" + (sesionesCreadas.size() + 1));
                            sesionesCreadas.add(sesion);
                        }
                        return sesion == null ? null : sesion.proxy;
                    case "changeSessionId":
                        if (sesion == null) {
                            throw new IllegalStateException("No hay sesión");
                        }
                        cambiosDeId++;
                        sesion.id = "SESION-ID-" + cambiosDeId;
                        return sesion.id;
                    case "getRequestedSessionId":
                        return idSolicitado;
                    case "isRequestedSessionIdValid":
                        return idSolicitadoValido;
                    case "getRequestDispatcher":
                        String ruta = (String) a[0];
                        return Proxy.newProxyInstance(RequestDispatcher.class.getClassLoader(),
                                new Class<?>[] {RequestDispatcher.class}, (p2, m2, a2) -> {
                                    if (m2.getName().equals("forward")) {
                                        reenvios.add(ruta);
                                    }
                                    return null;
                                });
                    default:
                        return porDefecto(m);
                    }
                });
        respuesta = (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class}, (p, m, a) -> {
                    switch (m.getName()) {
                    case "setStatus":
                        estado = (Integer) a[0];
                        return null;
                    case "getStatus":
                        return estado;
                    case "setHeader":
                        // Como el contenedor: sustituye todos los valores de esa cabecera.
                        cabecerasRespuesta.put((String) a[0], new ArrayList<>(List.of(String.valueOf(a[1]))));
                        return null;
                    case "addHeader":
                        cabecerasRespuesta.computeIfAbsent((String) a[0], k -> new ArrayList<>())
                                .add(String.valueOf(a[1]));
                        return null;
                    case "getHeader":
                        List<String> valores = cabecerasRespuesta.get(a[0]);
                        return valores == null || valores.isEmpty() ? null : valores.get(valores.size() - 1);
                    case "getHeaders":
                        List<String> todos = cabecerasRespuesta.get(a[0]);
                        return todos == null ? new ArrayList<String>() : new ArrayList<>(todos);
                    case "containsHeader":
                        return cabecerasRespuesta.containsKey(a[0]);
                    case "isCommitted":
                        return comprometida;
                    case "flushBuffer":
                        comprometida = true;
                        return null;
                    case "getWriter":
                        return new java.io.PrintWriter(new java.io.StringWriter());
                    case "sendRedirect":
                        redireccion = (String) a[0];
                        estado = 302;
                        comprometida = true;
                        return null;
                    case "sendError":
                        errorEnviado = (Integer) a[0];
                        estado = errorEnviado;
                        comprometida = true;
                        return null;
                    case "encodeRedirectURL":
                    case "encodeURL":
                        return a[0];
                    default:
                        return porDefecto(m);
                    }
                });
    }

    static ServletFalso crear() {
        return new ServletFalso();
    }

    // ------------------------------------------------------------------ preparación

    ServletFalso metodo(String metodo) {
        this.metodo = metodo;
        return this;
    }

    /** Ruta dentro de la aplicación, por ejemplo {@code /cuentas}. */
    ServletFalso ruta(String ruta) {
        this.uri = CONTEXTO + ruta;
        return this;
    }

    ServletFalso uriCruda(String uri) {
        this.uri = uri;
        return this;
    }

    ServletFalso parametro(String nombre, String valor) {
        parametros.put(nombre, valor);
        return this;
    }

    ServletFalso parametroContexto(String nombre, String valor) {
        parametrosContexto.put(nombre, valor);
        return this;
    }

    ServletFalso cliente(ec.edu.monster.web.cliente.IClienteEUREKABANK cliente) {
        atributosContexto.put(ConfiguracionWeb.ATRIBUTO_CLIENTE, cliente);
        return this;
    }

    /** El navegador manda una cookie de una sesión que el servidor ya no tiene. */
    ServletFalso conSesionCaducada() {
        idSolicitado = "SESION-VIEJA";
        idSolicitadoValido = false;
        return this;
    }

    /** Crea una sesión HTTP anónima (como la que crea GET /login). */
    ServletFalso conSesionAnonima() {
        peticion.getSession(true);
        idSolicitado = sesion.id;
        idSolicitadoValido = true;
        return this;
    }

    /** Autentica contra el cliente falso y deja la sesión de usuario en la sesión HTTP. */
    ServletFalso conUsuarioAutenticado(ClienteFalso servidor) {
        SesionUsuario usuario = new SesionUsuario();
        new ControladorAutenticacion(servidor, usuario).autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        peticion.getSession(true).setAttribute(SesionWeb.ATRIBUTO, usuario);
        SesionWeb.tokenCsrf(peticion);
        idSolicitado = sesion.id;
        idSolicitadoValido = true;
        return this;
    }

    /** Deja en la sesión HTTP un usuario con el token indicado, sin pasar por el servicio. */
    ServletFalso conUsuarioConToken(String token) {
        peticion.getSession(true).setAttribute(SesionWeb.ATRIBUTO,
                new SesionUsuario(token, "0001", "Carlos Romero", "2026-11-03T12:15:00Z", "Consultas"));
        SesionWeb.tokenCsrf(peticion);
        idSolicitado = sesion.id;
        idSolicitadoValido = true;
        return this;
    }

    /** Agrega el token CSRF correcto de la sesión como parámetro del formulario. */
    ServletFalso conCsrfCorrecto() {
        parametros.put(SesionWeb.CAMPO_CSRF, csrf());
        return this;
    }

    // ------------------------------------------------------------------ ejecución

    /** Crea, inicializa y ejecuta el servlet con la petición actual. */
    void ejecutar(HttpServlet servlet) throws Exception {
        servlet.init(configuracion);
        servlet.service(peticion, respuesta);
    }

    FilterChain cadena() {
        return (req, res) -> cadenaEjecutada = true;
    }

    // ------------------------------------------------------------------ acceso

    HttpServletRequest peticion() {
        return peticion;
    }

    HttpServletResponse respuesta() {
        return respuesta;
    }

    ServletContext contexto() {
        return contexto;
    }

    ServletConfig configuracion() {
        return configuracion;
    }

    int estado() {
        return estado;
    }

    int errorEnviado() {
        return errorEnviado;
    }

    String redireccion() {
        return redireccion;
    }

    /** Último valor de la cabecera de respuesta, o {@code null} si no se envió. */
    String cabecera(String nombre) {
        List<String> valores = cabecerasRespuesta.get(nombre);
        return valores == null || valores.isEmpty() ? null : valores.get(valores.size() - 1);
    }

    /** Todos los valores de la cabecera de respuesta, en el orden en que se enviaron. */
    List<String> cabeceras(String nombre) {
        List<String> valores = cabecerasRespuesta.get(nombre);
        return valores == null ? new ArrayList<>() : new ArrayList<>(valores);
    }

    boolean comprometida() {
        return comprometida;
    }

    List<String> reenvios() {
        return reenvios;
    }

    String reenvio() {
        return reenvios.isEmpty() ? null : reenvios.get(reenvios.size() - 1);
    }

    boolean cadenaEjecutada() {
        return cadenaEjecutada;
    }

    Object atributo(String nombre) {
        return atributos.get(nombre);
    }

    boolean haySesion() {
        return sesion != null;
    }

    String idDeSesion() {
        return sesion == null ? null : sesion.id;
    }

    int cambiosDeId() {
        return cambiosDeId;
    }

    int sesionesCreadas() {
        return sesionesCreadas.size();
    }

    /** Cuántas sesiones se invalidaron. */
    int sesionesInvalidadas() {
        int total = 0;
        for (Sesion s : sesionesCreadas) {
            if (s.invalida) {
                total++;
            }
        }
        return total;
    }

    Object atributoDeSesion(String nombre) {
        return sesion == null ? null : sesion.atributos.get(nombre);
    }

    void ponerAtributoDeSesion(String nombre, Object valor) {
        peticion.getSession(true).setAttribute(nombre, valor);
    }

    String csrf() {
        return (String) atributoDeSesion(SesionWeb.ATRIBUTO_CSRF);
    }

    SesionUsuario usuario() {
        return (SesionUsuario) atributoDeSesion(SesionWeb.ATRIBUTO);
    }

    /** Mensaje flash pendiente en la sesión (sin consumirlo). */
    @SuppressWarnings("unchecked")
    Map<String, Object> flash() {
        return (Map<String, Object>) atributoDeSesion(SesionWeb.ATRIBUTO_FLASH);
    }

    private static Object porDefecto(Method m) {
        Class<?> tipo = m.getReturnType();
        if (tipo == boolean.class) {
            return Boolean.FALSE;
        }
        if (tipo == int.class) {
            return 0;
        }
        if (tipo == long.class) {
            return 0L;
        }
        return null;
    }
}
