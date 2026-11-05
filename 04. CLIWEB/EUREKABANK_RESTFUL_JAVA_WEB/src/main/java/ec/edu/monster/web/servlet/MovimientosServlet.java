package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorMovimientos;
import ec.edu.monster.web.modelo.MovimientoDTO;
import ec.edu.monster.web.modelo.RespuestaMovimientos;
import ec.edu.monster.web.modelo.SesionUsuario;
import ec.edu.monster.web.util.ExcepcionServicio;
import ec.edu.monster.web.util.SesionWeb;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET {@code /movimientos}: libro de movimientos de una cuenta. Sin el parámetro {@code cuenta}
 * solo muestra el formulario.
 */
@WebServlet(name = "MovimientosServlet", urlPatterns = "/movimientos")
public class MovimientosServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VISTA = "/WEB-INF/vistas/movimientos.jsp";

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
        SesionWeb.prepararVista(peticion, "movimientos");
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
                    RespuestaMovimientos resultado =
                            new ControladorMovimientos(cliente, sesion).consultarMovimientos(cuenta);
                    if (SesionWeb.esRechazoDeSesion(resultado)) {
                        SesionWeb.expulsar(peticion, respuesta, resultado);
                        return;
                    }
                    if ("OK".equals(resultado.getEstado())) {
                        peticion.setAttribute("resultado", resultado);
                        peticion.setAttribute("resumen", resumir(resultado.getMovimientos()));
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

    /**
     * Resume los movimientos de una cuenta: lo que entró, lo que salió y cuántos hay. Los importes
     * vienen siempre en positivo y su sentido lo dice {@code tipoAccion} (INGRESO o SALIDA). Una
     * lista vacía da cero en todo.
     *
     * @param movimientos movimientos recibidos del servicio, puede estar vacía
     * @return {@code ingresos} y {@code salidas} (importes), y {@code cantidad} (entero)
     */
    public static Map<String, Object> resumir(List<MovimientoDTO> movimientos) {
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal salidas = BigDecimal.ZERO;
        int cantidad = 0;
        if (movimientos != null) {
            for (MovimientoDTO movimiento : movimientos) {
                if (movimiento == null) {
                    continue;
                }
                cantidad++;
                if (movimiento.getImporte() == null) {
                    continue;
                }
                if ("INGRESO".equals(movimiento.getTipoAccion())) {
                    ingresos = ingresos.add(movimiento.getImporte());
                } else if ("SALIDA".equals(movimiento.getTipoAccion())) {
                    salidas = salidas.add(movimiento.getImporte());
                }
            }
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("ingresos", ingresos);
        resumen.put("salidas", salidas);
        resumen.put("cantidad", cantidad);
        return resumen;
    }
}
