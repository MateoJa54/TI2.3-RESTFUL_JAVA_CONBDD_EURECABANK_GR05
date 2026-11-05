package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorCuentas;
import ec.edu.monster.web.modelo.CuentaDTO;
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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
                        peticion.setAttribute("resumen", resumir(resultado.getCuentas()));
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
     * Resume las cuentas de un cliente: cuántas son y el saldo total de cada moneda. Las cuentas
     * sin saldo no suman. Las monedas se nombran con su descripción, o con el código si no hay.
     *
     * @param cuentas cuentas recibidas del servicio, puede estar vacía
     * @return {@code cantidad} (entero) y {@code saldos} (moneda, saldo total), en orden alfabético
     */
    public static Map<String, Object> resumir(List<CuentaDTO> cuentas) {
        Map<String, BigDecimal> saldos = new TreeMap<>();
        for (CuentaDTO cuenta : cuentas == null ? List.<CuentaDTO>of() : cuentas) {
            if (cuenta == null || cuenta.getSaldo() == null) {
                continue;
            }
            String moneda = nombreMoneda(cuenta);
            saldos.merge(moneda, cuenta.getSaldo(), BigDecimal::add);
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("cantidad", cuentas == null ? 0 : cuentas.size());
        resumen.put("saldos", saldos);
        return resumen;
    }

    private static String nombreMoneda(CuentaDTO cuenta) {
        String descripcion = cuenta.getDescripcionMoneda();
        if (descripcion != null && !descripcion.isBlank()) {
            return descripcion.strip();
        }
        return cuenta.getMoneda() == null || cuenta.getMoneda().isBlank() ? "Sin moneda" : cuenta.getMoneda().strip();
    }
}
