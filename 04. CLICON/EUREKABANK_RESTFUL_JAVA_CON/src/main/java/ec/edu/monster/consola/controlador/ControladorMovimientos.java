package ec.edu.monster.consola.controlador;

import ec.edu.monster.consola.cliente.IClienteEUREKABANK;
import ec.edu.monster.consola.modelo.RespuestaMovimiento;
import ec.edu.monster.consola.modelo.RespuestaMovimientos;
import ec.edu.monster.consola.modelo.RespuestaTransferencia;
import ec.edu.monster.consola.modelo.SesionUsuario;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Controlador de los movimientos de las cuentas (CU-04 a CU-07).
 *
 * <p>El saldo, el costo por movimiento y el ITF los calcula el servidor; el
 * cliente solo envía la operación y muestra lo que le devuelven.
 */
public class ControladorMovimientos extends ControladorProtegido {

    /**
     * @param cliente cliente del servicio, obligatorio
     * @param sesion  sesión local del usuario, obligatoria
     */
    public ControladorMovimientos(IClienteEUREKABANK cliente, SesionUsuario sesion) {
        super(cliente, sesion);
    }

    /**
     * Consulta los movimientos de una cuenta.
     *
     * @param cuenta código de la cuenta
     * @return el sobre del servidor
     */
    public RespuestaMovimientos consultarMovimientos(String cuenta) {
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaMovimientos());
        }
        return revisarSesion(cliente.consultarMovimientos(cuenta, obtenerToken()));
    }

    /**
     * Realiza un depósito.
     *
     * @param cuenta código de la cuenta
     * @param monto  importe del depósito
     * @return el sobre del servidor
     */
    public RespuestaMovimiento depositar(String cuenta, BigDecimal monto) {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaMovimiento());
        }
        return revisarSesion(cliente.realizarDeposito(cuenta, monto, obtenerToken()));
    }

    /**
     * Realiza un retiro.
     *
     * @param cuenta código de la cuenta
     * @param monto  importe del retiro
     * @return el sobre del servidor
     */
    public RespuestaMovimiento retirar(String cuenta, BigDecimal monto) {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaMovimiento());
        }
        return revisarSesion(cliente.realizarRetiro(cuenta, monto, obtenerToken()));
    }

    /**
     * Realiza una transferencia entre dos cuentas.
     *
     * @param cuentaOrigen  cuenta de la que sale el monto
     * @param cuentaDestino cuenta que recibe el monto
     * @param monto         importe de la transferencia
     * @return el sobre del servidor
     */
    public RespuestaTransferencia transferir(String cuentaOrigen, String cuentaDestino,
            BigDecimal monto) {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaTransferencia());
        }
        return revisarSesion(
                cliente.realizarTransferencia(cuentaOrigen, cuentaDestino, monto, obtenerToken()));
    }
}
