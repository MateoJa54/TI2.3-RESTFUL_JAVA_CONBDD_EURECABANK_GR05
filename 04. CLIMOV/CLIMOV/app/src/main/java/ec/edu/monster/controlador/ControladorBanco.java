package ec.edu.monster.controlador;

import ec.edu.monster.modelo.RespuestaBanco;
import ec.edu.monster.util.HashClave;
import ec.edu.monster.ws.ClienteEurekaBank;
import ec.edu.monster.ws.TransporteEurekaBank;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Coordina sesión y operaciones del banco sin acoplar la vista al transporte HTTP. */
public final class ControladorBanco {
    private final TransporteEurekaBank cliente;
    private String token;

    public ControladorBanco(String baseUrl) { this(new ClienteEurekaBank(baseUrl)); }

    /** Constructor de prueba; permite simular respuestas sin tocar la red ni la base. */
    public ControladorBanco(TransporteEurekaBank cliente) {
        if(cliente==null) throw new IllegalArgumentException("El transporte del servicio es obligatorio.");
        this.cliente=cliente;
    }

    public RespuestaBanco autenticar(String usuario, String clave) throws IOException {
        RespuestaBanco respuesta = cliente.enviar("POST", RUTA_LOGIN, null,
                ClienteEurekaBank.json("usuario", usuario.trim(), "clave", HashClave.sha256(clave)));
        token = respuesta.esCorrecta() && !respuesta.getToken().isEmpty() ? respuesta.getToken() : null;
        return respuesta;
    }

    public RespuestaBanco cerrarSesion() throws IOException {
        exigirSesion();
        RespuestaBanco respuesta = cliente.enviar("POST", RUTA_LOGOUT, token, ClienteEurekaBank.json());
        if (respuesta.esCorrecta() || "AUTH-003".equals(respuesta.getCodigo()) || "AUTH-004".equals(respuesta.getCodigo())) token = null;
        return respuesta;
    }

    public RespuestaBanco listarCuentas(String cedula) throws IOException {
        return protegido("GET", rutaCuentas(cedula), null);
    }
    public RespuestaBanco consultarCuenta(String cuenta) throws IOException {
        return protegido("GET", rutaCuenta(cuenta), null);
    }
    public RespuestaBanco consultarMovimientos(String cuenta) throws IOException {
        return protegido("GET", rutaMovimientos(cuenta), null);
    }
    public RespuestaBanco depositar(String cuenta, String monto) throws IOException {
        return movimiento(cuenta, monto, "depositos");
    }
    public RespuestaBanco retirar(String cuenta, String monto) throws IOException {
        return movimiento(cuenta, monto, "retiros");
    }
    public RespuestaBanco transferir(String origen, String destino, String monto) throws IOException {
        exigirSesion();
        BigDecimal importe = convertirImporte(monto);
        return cliente.enviar("POST", RUTA_TRANSFERENCIA, token,
                ClienteEurekaBank.json("cuentaOrigen", origen.trim(), "cuentaDestino", destino.trim(), "monto", importe));
    }
    public RespuestaBanco consultarCostos(String cuenta) throws IOException {
        return protegido("GET", rutaCostos(cuenta), null);
    }

    private RespuestaBanco movimiento(String cuenta, String monto, String accion) throws IOException {
        exigirSesion();
        String ruta = "depositos".equals(accion) ? rutaDeposito(cuenta) : rutaRetiro(cuenta);
        return cliente.enviar("POST", ruta, token, ClienteEurekaBank.json("monto", convertirImporte(monto)));
    }

    private RespuestaBanco protegido(String metodo, String ruta, JSONObject cuerpo) throws IOException {
        exigirSesion();
        RespuestaBanco respuesta = cliente.enviar(metodo, ruta, token, cuerpo);
        if ("AUTH-003".equals(respuesta.getCodigo()) || "AUTH-004".equals(respuesta.getCodigo())) token = null;
        return respuesta;
    }

    private void exigirSesion() {
        if (token == null || token.isEmpty()) throw new IllegalStateException("La sesión terminó. Inicia sesión nuevamente.");
    }

    public static final String RUTA_LOGIN = "/auth/login";
    public static final String RUTA_LOGOUT = "/auth/logout";
    public static final String RUTA_TRANSFERENCIA = "/transferencias";

    public static String rutaCuentas(String cedula) { return "/clientes/" + ClienteEurekaBank.codificarSegmento(cedula) + "/cuentas"; }
    public static String rutaCuenta(String cuenta) { return "/cuentas/" + ClienteEurekaBank.codificarSegmento(cuenta); }
    public static String rutaMovimientos(String cuenta) { return rutaCuenta(cuenta) + "/movimientos"; }
    public static String rutaDeposito(String cuenta) { return rutaCuenta(cuenta) + "/depositos"; }
    public static String rutaRetiro(String cuenta) { return rutaCuenta(cuenta) + "/retiros"; }
    public static String rutaCostos(String cuenta) { return rutaCuenta(cuenta) + "/costos"; }

    public static BigDecimal convertirImporte(String texto) {
        if (texto == null || texto.trim().isEmpty()) throw new IllegalArgumentException("Ingresa el monto.");
        try { return new BigDecimal(texto.trim().replace(',', '.')).setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("El monto debe ser numérico.", e); }
    }

    public boolean haySesion() { return token != null && !token.isEmpty(); }
}
