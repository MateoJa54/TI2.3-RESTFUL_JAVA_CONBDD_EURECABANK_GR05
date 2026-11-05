package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorCostos;
import ec.edu.monster.web.modelo.RespuestaCostos;
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
 * GET {@code /costos}: proyección de costos de mantenimiento de una cuenta. Sin el parámetro
 * {@code cuenta} solo muestra el formulario.
 */
@WebServlet(name = "CostosServlet", urlPatterns = "/costos")
public class CostosServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VISTA = "/WEB-INF/vistas/costos.jsp";

    private transient IClienteEUREKABANK cliente;

    @Override
    public void init() {
        cliente = LoginServlet.clienteCompartido(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        SesionUsuario sesion = SesionWeb.exigir(peticion, respuesta);
        if (sesion == null) {
            return;
        }
        SesionWeb.prepararVista(peticion, "costos");
        Map<String, Object> mensaje = SesionWeb.tomarMensaje(peticion);
        String enviada = peticion.getParameter("cuenta");
        if (enviada != null) {
            String cuenta = SesionWeb.normalizar(SesionWeb.quitarControl(enviada));
            peticion.setAttribute("consulta", cuenta);
            if (cuenta.isEmpty()) {
                mensaje = SesionWeb.mensajeError(null, "Ingrese el código de la cuenta");
            } else if (!SesionWeb.esEntradaSegura(cuenta)) {
                mensaje = SesionWeb.mensajeError(null, "El código de cuenta ingresado no tiene un formato válido");
            } else {
                try {
                    RespuestaCostos resultado = new ControladorCostos(cliente, sesion).consultarCostos(cuenta);
                    if (SesionWeb.esRechazoDeSesion(resultado)) {
                        SesionWeb.expulsar(peticion, respuesta, resultado);
                        return;
                    }
                    if ("OK".equals(resultado.getEstado())) {
                        peticion.setAttribute("resultado", resultado);
                    } else {
                        mensaje = SesionWeb.mensajeDe(resultado);
                    }
                } catch (ExcepcionServicio e) {
                    mensaje = SesionWeb.mensajeServicioCaido();
                }
            }
        }
        peticion.setAttribute("mensaje", mensaje);
        peticion.getRequestDispatcher(VISTA).forward(peticion, respuesta);
    }
}
