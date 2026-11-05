package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.ClienteEUREKABANK;
import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorAutenticacion;
import ec.edu.monster.web.modelo.RespuestaAutenticacion;
import ec.edu.monster.web.modelo.SesionUsuario;
import ec.edu.monster.web.util.ConfiguracionWeb;
import ec.edu.monster.web.util.ExcepcionServicio;
import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * GET {@code /login} muestra el formulario; POST {@code /login} autentica y abre la sesión HTTP.
 *
 * <p>La clave se hashea con SHA-256 dentro de {@link ControladorAutenticacion} y nunca se
 * devuelve al navegador. Tras autenticar se cambia el identificador de la sesión HTTP.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VISTA = "/WEB-INF/vistas/login.jsp";
    private static final int LONGITUD_MAXIMA_CLAVE = 128;

    /** Único cliente del servicio de la aplicación, compartido entre servlets. */
    private transient IClienteEUREKABANK cliente;

    @Override
    public void init() {
        cliente = clienteCompartido(getServletContext());
    }

    /**
     * Devuelve el único cliente del servicio de la aplicación, y lo crea la primera vez con la
     * URL de {@link ConfiguracionWeb#resolverUrl(ServletContext)}.
     *
     * @param contexto contexto de la aplicación
     * @return el cliente compartido
     */
    public static IClienteEUREKABANK clienteCompartido(ServletContext contexto) {
        synchronized (contexto) {
            Object actual = contexto.getAttribute(ConfiguracionWeb.ATRIBUTO_CLIENTE);
            if (actual instanceof IClienteEUREKABANK) {
                return (IClienteEUREKABANK) actual;
            }
            IClienteEUREKABANK nuevo = new ClienteEUREKABANK(ConfiguracionWeb.resolverUrl(contexto));
            contexto.setAttribute(ConfiguracionWeb.ATRIBUTO_CLIENTE, nuevo);
            return nuevo;
        }
    }

    @Override
    protected void doGet(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        if (SesionWeb.tokenDe(peticion) != null) {
            respuesta.sendRedirect(peticion.getContextPath() + "/cuentas");
            return;
        }
        peticion.setAttribute("csrf", SesionWeb.tokenCsrf(peticion));
        peticion.setAttribute("mensaje", SesionWeb.tomarMensaje(peticion));
        peticion.getRequestDispatcher(VISTA).forward(peticion, respuesta);
    }

    @Override
    protected void doPost(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        if (!SesionWeb.csrfValido(peticion)) {
            SesionWeb.denegar(peticion, respuesta);
            return;
        }
        String usuario = SesionWeb.normalizar(peticion.getParameter("usuario"));
        String clave = peticion.getParameter("clave");
        if (usuario.isEmpty() || clave == null || clave.isEmpty()) {
            volverConError(peticion, respuesta, usuario, "Ingrese el usuario y la clave");
            return;
        }
        if (!SesionWeb.esEntradaSegura(usuario) || clave.length() > LONGITUD_MAXIMA_CLAVE) {
            volverConError(peticion, respuesta, "", "El usuario o la clave no tienen un formato válido");
            return;
        }
        SesionUsuario nueva = new SesionUsuario();
        ControladorAutenticacion controlador = new ControladorAutenticacion(cliente, nueva);
        RespuestaAutenticacion resultado;
        try {
            resultado = controlador.autenticar(usuario, clave);
        } catch (ExcepcionServicio e) {
            Map<String, Object> mensaje = SesionWeb.mensajeServicioCaido();
            mensaje.put("usuario", usuario);
            SesionWeb.guardarMensaje(peticion, mensaje);
            respuesta.sendRedirect(peticion.getContextPath() + "/login");
            return;
        }
        if (controlador.haySesionActiva()) {
            SesionWeb.iniciar(peticion, nueva);
            respuesta.sendRedirect(peticion.getContextPath() + "/cuentas");
            return;
        }
        Map<String, Object> mensaje = "OK".equals(resultado.getEstado())
                ? SesionWeb.mensajeError(null, "El servidor no entregó una sesión válida")
                : SesionWeb.mensajeDe(resultado);
        mensaje.put("usuario", usuario);
        SesionWeb.guardarMensaje(peticion, mensaje);
        respuesta.sendRedirect(peticion.getContextPath() + "/login");
    }

    private static void volverConError(HttpServletRequest peticion, HttpServletResponse respuesta,
            String usuario, String texto) throws IOException {
        Map<String, Object> mensaje = SesionWeb.mensajeError(null, texto);
        mensaje.put("usuario", usuario);
        SesionWeb.guardarMensaje(peticion, mensaje);
        respuesta.sendRedirect(peticion.getContextPath() + "/login");
    }
}
