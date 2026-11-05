package ec.edu.monster.escritorio.pruebas;

import ec.edu.monster.escritorio.cliente.IClienteEUREKABANK;
import ec.edu.monster.escritorio.modelo.CuentaDTO;
import ec.edu.monster.escritorio.modelo.MovimientoDTO;
import ec.edu.monster.escritorio.modelo.RespuestaAutenticacion;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.modelo.RespuestaCostos;
import ec.edu.monster.escritorio.modelo.RespuestaCuenta;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.modelo.RespuestaMovimiento;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.modelo.RespuestaTransferencia;
import ec.edu.monster.escritorio.util.ExcepcionServicio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servidor simulado para probar controladores y vistas sin red.
 *
 * <p>Se comporta como el servidor real en lo que el cliente necesita: la
 * contraseña llega hasheada, hay token, hay los códigos de error del catálogo
 * y los mensajes literales. Las pruebas pueden "vencer" la sesión o "apagar"
 * el servidor.
 */
class ClienteFalso implements IClienteEUREKABANK {

    /** Usuario válido de la semilla del contrato. */
    static final String USUARIO = "cromero";

    /** Contraseña en claro del usuario de la semilla del contrato. */
    static final String CLAVE = "Cromero2026";

    /** Hash SHA-256 de la contraseña, que es lo que recibe el servidor. */
    static final String HASH_CLAVE =
            "891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4";

    /** Expiración que devuelve el servidor al autenticar. */
    static final String EXPIRACION = "2026-11-03T12:15:00Z";

    /** Cédula de un cliente que sí existe en la base de datos. */
    static final String CEDULA = "06914897";

    /** Código de la cuenta del cliente anterior. */
    static final String CUENTA = "00200002";

    /** Código de una cuenta de otra moneda, para la prueba de CTA-004. */
    static final String CUENTA_DOLARES = "00100002";

    static final String ESTADO_OK = "OK";
    static final String ESTADO_ERROR = "ERROR";

    /** Llamadas registradas, en el orden en que llegaron. */
    final List<String> llamadas = new ArrayList<>();

    /** Cuando está apagado, toda llamada falla como si el servidor no existiera. */
    boolean servidorApagado;

    private int tokensEmitidos;
    private String tokenVigente;
    private String tokenVencido;

    /** El token actual deja de servir: el próximo uso responde AUTH-004. */
    void vencerSesion() {
        tokenVencido = tokenVigente;
        tokenVigente = null;
    }

    /** El token actual deja de existir en el registro: responde AUTH-003. */
    void perderSesion() {
        tokenVigente = null;
    }

    @Override
    public RespuestaAutenticacion autenticar(String usuario, String clave) {
        registrar("autenticar(" + usuario + "," + clave + ")");
        RespuestaAutenticacion respuesta = new RespuestaAutenticacion();
        if (USUARIO.equals(usuario) && HASH_CLAVE.equals(clave)) {
            tokenVigente = "TOKEN-" + (++tokensEmitidos);
            llenar(respuesta, ESTADO_OK, "200", "Autenticación exitosa");
            respuesta.setToken(tokenVigente);
            respuesta.setEmpleado("0001");
            respuesta.setNombreEmpleado("Carlos Romero");
            respuesta.setExpiracion(EXPIRACION);
            respuesta.setRol("Consultas");
        } else {
            llenar(respuesta, ESTADO_ERROR, "AUTH-001",
                    "El usuario o la contraseña son incorrectos");
        }
        return respuesta;
    }

    @Override
    public RespuestaBase cerrarSesion(String token) {
        registrar("cerrarSesion(" + token + ")");
        RespuestaBase respuesta = new RespuestaBase();
        if (token != null && token.equals(tokenVigente)) {
            tokenVigente = null;
            llenar(respuesta, ESTADO_OK, "200", "Sesión cerrada correctamente");
        } else {
            llenar(respuesta, ESTADO_ERROR, "AUTH-003", "El token de sesión no es válido");
        }
        return respuesta;
    }

    @Override
    public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
        registrar("listarCuentasPorCliente(" + cedula + "," + token + ")");
        RespuestaCuentas respuesta = new RespuestaCuentas();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (CEDULA.equals(cedula)) {
            llenar(respuesta, ESTADO_OK, "200", "Cuentas consultadas correctamente");
            respuesta.setCedula(cedula);
            respuesta.setNombreCliente("Cliente de Prueba");
            respuesta.setCuentas(List.of(cuenta(CUENTA, "01", "Soles", "6800.00", "ACTIVO", 7)));
        } else {
            llenar(respuesta, ESTADO_ERROR, "CLI-001", "El cliente solicitado no existe");
        }
        return respuesta;
    }

    @Override
    public RespuestaCuenta consultarCuenta(String cuenta, String token) {
        registrar("consultarCuenta(" + cuenta + "," + token + ")");
        RespuestaCuenta respuesta = new RespuestaCuenta();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (CUENTA.equals(cuenta)) {
            llenar(respuesta, ESTADO_OK, "200", "Cuenta consultada correctamente");
            respuesta.setCuenta(cuenta(CUENTA, "01", "Soles", "6800.00", "ACTIVO", 7));
        } else {
            llenar(respuesta, ESTADO_ERROR, "CTA-001", "La cuenta solicitada no existe");
        }
        return respuesta;
    }

    @Override
    public RespuestaMovimientos consultarMovimientos(String cuenta, String token) {
        registrar("consultarMovimientos(" + cuenta + "," + token + ")");
        RespuestaMovimientos respuesta = new RespuestaMovimientos();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (CUENTA.equals(cuenta)) {
            llenar(respuesta, ESTADO_OK, "200", "Movimientos consultados correctamente");
            respuesta.setCuenta(cuenta);
            respuesta.setMovimientos(List.of(movimiento(1, "2026-01-05", "001", "Apertura de Cuenta",
                    "INGRESO", "1000.00", "")));
        } else {
            llenar(respuesta, ESTADO_ERROR, "CTA-001", "La cuenta solicitada no existe");
        }
        return respuesta;
    }

    @Override
    public RespuestaMovimiento realizarDeposito(String cuenta, BigDecimal monto, String token) {
        registrar("realizarDeposito(" + cuenta + "," + monto + "," + token + ")");
        RespuestaMovimiento respuesta = new RespuestaMovimiento();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (monto == null || monto.signum() <= 0 || monto.scale() > 2) {
            llenar(respuesta, ESTADO_ERROR, "MON-001",
                    "El monto debe ser mayor que cero y con máximo dos decimales");
            return respuesta;
        }
        llenar(respuesta, ESTADO_OK, "200", "Depósito realizado correctamente");
        respuesta.setSaldoNuevo(new BigDecimal("6900.00"));
        respuesta.setCostoAplicado(BigDecimal.ZERO);
        respuesta.setItfAplicado(BigDecimal.ZERO);
        respuesta.setMovimiento(movimiento(8, "2026-11-03", "003", "Deposito", "INGRESO",
                monto.toPlainString(), ""));
        return respuesta;
    }

    @Override
    public RespuestaMovimiento realizarRetiro(String cuenta, BigDecimal monto, String token) {
        registrar("realizarRetiro(" + cuenta + "," + monto + "," + token + ")");
        RespuestaMovimiento respuesta = new RespuestaMovimiento();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (monto == null || monto.signum() <= 0 || monto.scale() > 2) {
            llenar(respuesta, ESTADO_ERROR, "MON-001",
                    "El monto debe ser mayor que cero y con máximo dos decimales");
            return respuesta;
        }
        if (monto.compareTo(new BigDecimal("10000.00")) > 0) {
            llenar(respuesta, ESTADO_ERROR, "SAL-001",
                    "El saldo de la cuenta es insuficiente para la operación");
            return respuesta;
        }
        llenar(respuesta, ESTADO_OK, "200", "Retiro realizado correctamente");
        respuesta.setSaldoNuevo(new BigDecimal("6750.00"));
        respuesta.setCostoAplicado(BigDecimal.ZERO);
        respuesta.setItfAplicado(BigDecimal.ZERO);
        respuesta.setMovimiento(movimiento(9, "2026-11-03", "004", "Retiro", "SALIDA",
                monto.toPlainString(), ""));
        return respuesta;
    }

    @Override
    public RespuestaTransferencia realizarTransferencia(String cuentaOrigen, String cuentaDestino,
            BigDecimal monto, String token) {
        registrar("realizarTransferencia(" + cuentaOrigen + "," + cuentaDestino + "," + monto + "," + token + ")");
        RespuestaTransferencia respuesta = new RespuestaTransferencia();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (cuentaOrigen != null && cuentaOrigen.equals(cuentaDestino)) {
            llenar(respuesta, ESTADO_ERROR, "CTA-003",
                    "La cuenta origen y la cuenta destino no pueden ser iguales");
            return respuesta;
        }
        if (CUENTA_DOLARES.equals(cuentaDestino)) {
            llenar(respuesta, ESTADO_ERROR, "CTA-004",
                    "La cuenta de destino pertenece a una moneda distinta");
            return respuesta;
        }
        llenar(respuesta, ESTADO_OK, "200", "Transferencia realizada correctamente");
        respuesta.setNumeroOrigen(10);
        respuesta.setNumeroDestino(25);
        respuesta.setSaldoOrigen(new BigDecimal("6775.00"));
        respuesta.setSaldoDestino(new BigDecimal("7429.00"));
        respuesta.setItfAplicado(new BigDecimal("0.02"));
        return respuesta;
    }

    @Override
    public RespuestaCostos consultarCostosMantenimiento(String cuenta, String token) {
        registrar("consultarCostosMantenimiento(" + cuenta + "," + token + ")");
        RespuestaCostos respuesta = new RespuestaCostos();
        if (!validarToken(respuesta, token)) {
            return respuesta;
        }
        if (!CUENTA.equals(cuenta)) {
            llenar(respuesta, ESTADO_ERROR, "CTA-001", "La cuenta solicitada no existe");
            return respuesta;
        }
        llenar(respuesta, ESTADO_OK, "200", "Costos de mantenimiento consultados correctamente");
        respuesta.setSaldoActual(new BigDecimal("6800.00"));
        respuesta.setCostoMantenimiento(new BigDecimal("5.00"));
        respuesta.setCostoMovimiento(new BigDecimal("0.50"));
        respuesta.setInteresProyectado(new BigDecimal("54.40"));
        respuesta.setLimiteMantenimiento(new BigDecimal("100.00"));
        respuesta.setOperacionesSinCosto(15);
        return respuesta;
    }

    private void registrar(String llamada) {
        if (servidorApagado) {
            throw new ExcepcionServicio("No se pudo comunicar con el servidor en http://falso", null);
        }
        llamadas.add(llamada);
    }

    private boolean validarToken(RespuestaBase respuesta, String token) {
        if (token != null && token.equals(tokenVigente)) {
            llenar(respuesta, ESTADO_OK, "200", "Operación realizada correctamente");
            return true;
        }
        if (token != null && token.equals(tokenVencido)) {
            tokenVencido = null;
            llenar(respuesta, ESTADO_ERROR, "AUTH-004",
                    "El token de sesión ha expirado, inicie sesión nuevamente");
        } else {
            llenar(respuesta, ESTADO_ERROR, "AUTH-003", "El token de sesión no es válido");
        }
        return false;
    }

    private static void llenar(RespuestaBase respuesta, String estado, String codigo, String mensaje) {
        respuesta.setEstado(estado);
        respuesta.setCodigo(codigo);
        respuesta.setMensaje(mensaje);
    }

    private static CuentaDTO cuenta(String codigo, String moneda, String descripcionMoneda,
            String saldo, String estado, int operaciones) {
        CuentaDTO cuenta = new CuentaDTO();
        cuenta.setCodigo(codigo);
        cuenta.setMoneda(moneda);
        cuenta.setDescripcionMoneda(descripcionMoneda);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta.setEstado(estado);
        cuenta.setCodigoCliente("00001");
        cuenta.setNombreCliente("Cliente de Prueba");
        cuenta.setCodigoSucursal("001");
        cuenta.setFechaCreacion("2020-01-01");
        cuenta.setClave("ABC123");
        cuenta.setOperacionesRealizadas(operaciones);
        return cuenta;
    }

    private static MovimientoDTO movimiento(int numero, String fecha, String tipoCodigo,
            String tipoDescripcion, String tipoAccion, String importe, String referencia) {
        MovimientoDTO movimiento = new MovimientoDTO();
        movimiento.setNumero(numero);
        movimiento.setFecha(fecha);
        movimiento.setTipoCodigo(tipoCodigo);
        movimiento.setTipoDescripcion(tipoDescripcion);
        movimiento.setTipoAccion(tipoAccion);
        movimiento.setImporte(new BigDecimal(importe));
        movimiento.setReferencia(referencia);
        movimiento.setEmpleado("0001");
        return movimiento;
    }
}
