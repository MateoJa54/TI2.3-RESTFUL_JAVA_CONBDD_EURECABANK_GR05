package ec.edu.monster.consola.controlador;

import ec.edu.monster.consola.cliente.IClienteEUREKABANK;
import ec.edu.monster.consola.modelo.RespuestaCostos;
import ec.edu.monster.consola.modelo.SesionUsuario;

/**
 * Controlador de la proyección de costos de mantenimiento (CU-08).
 *
 * <p>El cobro del mantenimiento y el abono del interés los ejecuta un proceso
 * batch externo (regla RB-12); el servicio solo los proyecta.
 */
public class ControladorCostos extends ControladorProtegido {

    /**
     * @param cliente cliente del servicio, obligatorio
     * @param sesion  sesión local del usuario, obligatoria
     */
    public ControladorCostos(IClienteEUREKABANK cliente, SesionUsuario sesion) {
        super(cliente, sesion);
    }

    /**
     * Consulta la proyección de costos de una cuenta.
     *
     * @param cuenta código de la cuenta
     * @return el sobre del servidor
     */
    public RespuestaCostos consultarCostos(String cuenta) {
        if (!haySesionActiva()) {
            return exigirSesion(new RespuestaCostos());
        }
        return revisarSesion(cliente.consultarCostosMantenimiento(cuenta, obtenerToken()));
    }
}
