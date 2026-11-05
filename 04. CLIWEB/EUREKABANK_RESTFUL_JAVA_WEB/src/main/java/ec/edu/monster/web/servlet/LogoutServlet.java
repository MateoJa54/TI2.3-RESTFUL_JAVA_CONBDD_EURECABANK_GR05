package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorAutenticacion;
import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.SesionUsuario;
import ec.edu.monster.web.util.ExcepcionServicio;
import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * POST {@code /logout}: cierra la sesión en el servicio y en el navegador. Solo acepta POST con
 * token CSRF, así un enlace o una imagen de otra página no puede cerrar la sesión.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient IClienteEUREKABANK cliente;

    @Override
    public void init() {
        cliente = LoginServlet.clienteCompartido(getServletContext());
    }

    @Override
    protected void doPost(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        if (!SesionWeb.csrfValido(peticion)) {
            SesionWeb.denegar(peticion, respuesta);
            return;
        }
        SesionUsuario sesion = SesionWeb.sesionDe(peticion);
        Map<String, Object> mensaje;
        if (sesion == null) {
            mensaje = null;
        } else {
            ControladorAutenticacion controlador = new ControladorAutenticacion(cliente, sesion);
            try {
                RespuestaBase resultado = controlador.cerrarSesion();
                mensaje = SesionWeb.mensajeDe(resultado);
            } catch (ExcepcionServicio e) {
                // El servicio no contestó: la sesión local se cierra igual, el token vence solo.
                mensaje = SesionWeb.mensajeServicioCaido();
            }
        }
        SesionWeb.cerrar(peticion);
        if (mensaje != null) {
            SesionWeb.guardarMensaje(peticion, mensaje);
        }
        respuesta.sendRedirect(peticion.getContextPath() + "/login");
    }
}
