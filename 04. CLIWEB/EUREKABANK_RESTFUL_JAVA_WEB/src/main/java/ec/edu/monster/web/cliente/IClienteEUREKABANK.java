package ec.edu.monster.web.cliente;

import ec.edu.monster.web.modelo.RespuestaAutenticacion;
import ec.edu.monster.web.modelo.RespuestaBase;
import ec.edu.monster.web.modelo.RespuestaCostos;
import ec.edu.monster.web.modelo.RespuestaCuenta;
import ec.edu.monster.web.modelo.RespuestaCuentas;
import ec.edu.monster.web.modelo.RespuestaMovimiento;
import ec.edu.monster.web.modelo.RespuestaMovimientos;
import ec.edu.monster.web.modelo.RespuestaTransferencia;
import ec.edu.monster.web.util.ExcepcionServicio;
import java.math.BigDecimal;

/**
 * Lo que los controladores necesitan del servidor, sin saber que detrás hay
 * REST (patrón Puerto y Adaptador).
 *
 * <p>Los controladores dependen de esta interfaz y no de
 * {@link ClienteEUREKABANK}; por eso se pueden probar con un cliente falso,
 * sin servidor.
 *
 * <p>Todos los métodos lanzan {@link ExcepcionServicio} si no se pudo obtener
 * una respuesta del servidor.
 */
public interface IClienteEUREKABANK {

    /**
     * Opera 1: autentica al empleado.
     *
     * @param usuario usuario del empleado
     * @param clave   hash SHA-256 hexadecimal de la contraseña
     * @return el sobre con los datos de la sesión
     */
    RespuestaAutenticacion autenticar(String usuario, String clave);

    /**
     * Opera 2: cierra la sesión abierta con el token recibido.
     *
     * @param token token de sesión
     * @return el sobre con el estado, el código y el mensaje
     */
    RespuestaBase cerrarSesion(String token);

    /**
     * Opera 3: lista las cuentas de un cliente.
     *
     * @param cedula cédula del cliente
     * @param token  token de sesión
     * @return el sobre con los datos del cliente y sus cuentas
     */
    RespuestaCuentas listarCuentasPorCliente(String cedula, String token);

    /**
     * Opera 4: consulta una cuenta por su código.
     *
     * @param cuenta código de la cuenta
     * @param token  token de sesión
     * @return el sobre con los datos de la cuenta
     */
    RespuestaCuenta consultarCuenta(String cuenta, String token);

    /**
     * Opera 5: consulta los movimientos de una cuenta.
     *
     * @param cuenta código de la cuenta
     * @param token  token de sesión
     * @return el sobre con la lista de movimientos
     */
    RespuestaMovimientos consultarMovimientos(String cuenta, String token);

    /**
     * Opera 6: realiza un depósito.
     *
     * @param cuenta código de la cuenta
     * @param monto  importe del depósito
     * @param token  token de sesión
     * @return el sobre con el movimiento y el saldo nuevo
     */
    RespuestaMovimiento realizarDeposito(String cuenta, BigDecimal monto, String token);

    /**
     * Opera 7: realiza un retiro.
     *
     * @param cuenta código de la cuenta
     * @param monto  importe del retiro
     * @param token  token de sesión
     * @return el sobre con el movimiento y el saldo nuevo
     */
    RespuestaMovimiento realizarRetiro(String cuenta, BigDecimal monto, String token);

    /**
     * Opera 8: transfiere un monto entre dos cuentas.
     *
     * @param cuentaOrigen  cuenta de la que sale el monto
     * @param cuentaDestino cuenta que recibe el monto
     * @param monto         importe de la transferencia
     * @param token         token de sesión
     * @return el sobre con los saldos y el ITF aplicados
     */
    RespuestaTransferencia realizarTransferencia(String cuentaOrigen, String cuentaDestino,
            BigDecimal monto, String token);

    /**
     * Opera 9: proyecta los costos de mantenimiento de una cuenta.
     *
     * @param cuenta código de la cuenta
     * @param token  token de sesión
     * @return el sobre con la proyección de costos
     */
    RespuestaCostos consultarCostosMantenimiento(String cuenta, String token);
}
