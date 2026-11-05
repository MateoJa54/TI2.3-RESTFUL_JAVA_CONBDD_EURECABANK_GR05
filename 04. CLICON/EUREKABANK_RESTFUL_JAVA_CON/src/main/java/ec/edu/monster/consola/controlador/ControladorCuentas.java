package ec.edu.monster.consola.controlador;

import ec.edu.monster.consola.cliente.IClienteEUREKABANK;
import ec.edu.monster.consola.modelo.RespuestaCuenta;
import ec.edu.monster.consola.modelo.RespuestaCuentas;
import ec.edu.monster.consola.modelo.SesionUsuario;

/**
 * Controlador de las consultas de cuentas (CU-03).
 *
 * <p>No decide nada sobre el negocio: el servidor es quien dice qué cuentas
 * existen y en qué estado están.
 */
public class ControladorCuentas extends ControladorProtegido {

    /**
     * @param cliente cliente del servicio, obligatorio
     * @param sesion  sesión local del usuario, obligatoria
     */
    public ControladorCuentas(IClienteEUREKABANK cliente, SesionUsuario sesion) {
        super(cliente, sesion);
    }

    /**
     * Lista las cuentas de un cliente.
     *
     * @param cedula cédula del cliente
     * @return el sobre del servidor
     */
    public RespuestaCuentas listarCuentas(String cedula) {
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaCuentas());
        }
        return revisarSesion(cliente.listarCuentasPorCliente(cedula, obtenerToken()));
    }

    /**
     * Consulta una cuenta por su código.
     *
     * @param cuenta código de la cuenta
     * @return el sobre del servidor
     */
    public RespuestaCuenta consultarCuenta(String cuenta) {
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaCuenta());
        }
        return revisarSesion(cliente.consultarCuenta(cuenta, obtenerToken()));
    }
}
