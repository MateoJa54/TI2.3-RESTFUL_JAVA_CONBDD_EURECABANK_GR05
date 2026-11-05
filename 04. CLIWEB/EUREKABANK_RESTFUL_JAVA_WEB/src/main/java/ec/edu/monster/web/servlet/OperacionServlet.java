package ec.edu.monster.web.servlet;

import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.controlador.ControladorMovimientos;
import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.RespuestaMovimiento;
import ec.edu.monster.web.modelo.RespuestaTransferencia;
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
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code /operacion}: depósito, retiro y transferencia en un solo formulario.
 *
 * <p>GET muestra el formulario y el resultado de la última operación. POST ejecuta la operación
 * y redirige a GET (Post/Redirect/Get), así recargar la página no la repite. Cada formulario
 * lleva un identificador de envío que sirve una sola vez.
 */
@WebServlet(name = "OperacionServlet", urlPatterns = "/operacion")
public class OperacionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VISTA = "/WEB-INF/vistas/operacion.jsp";

    private static final String DEPOSITO = "deposito";
    private static final String RETIRO = "retiro";
    private static final String TRANSFERENCIA = "transferencia";

    /** Número decimal plano: signo opcional, hasta 15 enteros y hasta 10 decimales. */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d{1,15}(\\.\\d{1,10})?");

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
        SesionWeb.prepararVista(peticion, "operacion");
        peticion.setAttribute("mensaje", SesionWeb.tomarMensaje(peticion));
        peticion.setAttribute("envio", SesionWeb.nuevoEnvio(peticion));
        peticion.getRequestDispatcher(VISTA).forward(peticion, respuesta);
    }

    @Override
    protected void doPost(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws ServletException, IOException {
        SesionUsuario sesion = SesionWeb.exigir(peticion, respuesta);
        if (sesion == null) {
            return;
        }
        if (!SesionWeb.csrfValido(peticion)) {
            SesionWeb.denegar(peticion, respuesta);
            return;
        }
        String modo = SesionWeb.normalizar(SesionWeb.quitarControl(peticion.getParameter("modo")));
        String cuenta = SesionWeb.normalizar(SesionWeb.quitarControl(peticion.getParameter("cuenta")));
        String destino = SesionWeb.normalizar(SesionWeb.quitarControl(peticion.getParameter("destino")));
        String textoMonto = SesionWeb.normalizar(SesionWeb.quitarControl(peticion.getParameter("monto")));

        String error = validar(modo, cuenta, destino);
        BigDecimal monto = null;
        if (error == null) {
            try {
                monto = interpretarMonto(textoMonto);
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
            }
        }
        if (error != null) {
            terminar(peticion, respuesta, SesionWeb.mensajeError(null, error), modo, cuenta, destino, textoMonto);
            return;
        }
        if (!SesionWeb.consumirEnvio(peticion, peticion.getParameter("envio"))) {
            terminar(peticion, respuesta, SesionWeb.mensajeError(null,
                    "Esta operación ya se envió o el formulario caducó. "
                    + "Revise los movimientos de la cuenta antes de repetirla"),
                    "", "", "", "");
            return;
        }

        ControladorMovimientos controlador = new ControladorMovimientos(cliente, sesion);
        Map<String, Object> mensaje;
        try {
            RespuestaBase resultado;
            if (TRANSFERENCIA.equals(modo)) {
                RespuestaTransferencia transferencia = controlador.transferir(cuenta, destino, monto);
                resultado = transferencia;
            } else if (RETIRO.equals(modo)) {
                resultado = controlador.retirar(cuenta, monto);
            } else {
                resultado = controlador.depositar(cuenta, monto);
            }
            if (SesionWeb.esRechazoDeSesion(resultado)) {
                SesionWeb.expulsar(peticion, respuesta, resultado);
                return;
            }
            mensaje = SesionWeb.mensajeDe(resultado);
            if ("OK".equals(resultado.getEstado())) {
                agregarDetalle(mensaje, resultado);
                terminar(peticion, respuesta, mensaje, "", "", "", "");
                return;
            }
        } catch (ExcepcionServicio e) {
            mensaje = SesionWeb.mensajeServicioCaido();
        }
        terminar(peticion, respuesta, mensaje, modo, cuenta, destino, textoMonto);
    }

    /**
     * Convierte el texto del monto en número. Solo acepta un decimal plano con punto: rechaza
     * vacío, coma, notación científica, {@code NaN}, infinitos y texto. Los negativos, el cero
     * y los montos con más de dos decimales sí pasan, porque el servicio los rechaza con su
     * propio mensaje (MON-001).
     *
     * @param texto texto escrito por el usuario
     * @return el monto
     * @throws IllegalArgumentException con el mensaje que se muestra al usuario, si el texto no
     *                                  es un número decimal plano
     */
    public static BigDecimal interpretarMonto(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Ingrese el monto");
        }
        if (limpio.indexOf(',') >= 0) {
            throw new IllegalArgumentException(
                    "Use punto como separador decimal y no escriba separador de miles, por ejemplo 1500.50");
        }
        if (!DECIMAL.matcher(limpio).matches()) {
            throw new IllegalArgumentException(
                    "El monto debe ser un número con punto decimal, por ejemplo 150.00");
        }
        return new BigDecimal(limpio);
    }

    private static String validar(String modo, String cuenta, String destino) {
        if (!DEPOSITO.equals(modo) && !RETIRO.equals(modo) && !TRANSFERENCIA.equals(modo)) {
            return "Elija una operación: depósito, retiro o transferencia";
        }
        if (cuenta.isEmpty()) {
            return TRANSFERENCIA.equals(modo) ? "Ingrese la cuenta de origen" : "Ingrese el código de la cuenta";
        }
        if (!SesionWeb.esEntradaSegura(cuenta)) {
            return "El código de cuenta ingresado no tiene un formato válido";
        }
        if (TRANSFERENCIA.equals(modo)) {
            if (destino.isEmpty()) {
                return "Ingrese la cuenta de destino";
            }
            if (!SesionWeb.esEntradaSegura(destino)) {
                return "El código de la cuenta de destino no tiene un formato válido";
            }
        }
        return null;
    }

    private static void agregarDetalle(Map<String, Object> mensaje, RespuestaBase resultado) {
        if (resultado instanceof RespuestaMovimiento) {
            RespuestaMovimiento movimiento = (RespuestaMovimiento) resultado;
            mensaje.put("saldoNuevo", movimiento.getSaldoNuevo());
            mensaje.put("costoAplicado", movimiento.getCostoAplicado());
            mensaje.put("itfAplicado", movimiento.getItfAplicado());
        } else if (resultado instanceof RespuestaTransferencia) {
            RespuestaTransferencia transferencia = (RespuestaTransferencia) resultado;
            mensaje.put("saldoOrigen", transferencia.getSaldoOrigen());
            mensaje.put("saldoDestino", transferencia.getSaldoDestino());
            mensaje.put("itfAplicado", transferencia.getItfAplicado());
        }
    }

    /** Guarda el mensaje (con los valores escritos si hubo error) y redirige a GET. */
    private static void terminar(HttpServletRequest peticion, HttpServletResponse respuesta,
            Map<String, Object> mensaje, String modo, String cuenta, String destino, String monto)
            throws IOException {
        if (!"exito".equals(mensaje.get("tipo"))) {
            mensaje.put("modo", modo);
            mensaje.put("cuenta", cuenta);
            mensaje.put("destino", destino);
            mensaje.put("monto", monto);
        }
        SesionWeb.guardarMensaje(peticion, mensaje);
        respuesta.sendRedirect(peticion.getContextPath() + "/operacion");
    }
}
