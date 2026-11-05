package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorCuentas;
import ec.edu.monster.web.modelo.RespuestaCuentas;
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
 * GET {@code /cuentas}: cuentas de un cliente por cédula. Sin el parámetro {@code cedula} solo
 * muestra el formulario.
 */
@WebServlet(name = "CuentasServlet", urlPatterns = "/cuentas")
public class CuentasServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VISTA = "/WEB-INF/vistas/cuentas.jsp";

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
        SesionWeb.prepararVista(peticion, "cuentas");
        Map<String, Object> mensaje = SesionWeb.tomarMensaje(peticion);
        String enviada = peticion.getParameter("cedula");
        if (enviada != null) {
            String cedula = SesionWeb.normalizar(SesionWeb.quitarControl(enviada));
            peticion.setAttribute("consulta", cedula);
            if (cedula.isEmpty()) {
                mensaje = SesionWeb.mensajeError(null, "Ingrese la cédula del cliente");
            } else if (!SesionWeb.esEntradaSegura(cedula)) {
                mensaje = SesionWeb.mensajeError(null, "La cédula ingresada no tiene un formato válido");
            } else {
                try {
                    RespuestaCuentas resultado = new ControladorCuentas(cliente, sesion).listarCuentas(cedula);
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
