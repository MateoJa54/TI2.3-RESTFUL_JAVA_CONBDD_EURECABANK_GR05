package ec.edu.monster.consola.vista;

import ec.edu.monster.consola.controlador.ControladorCostos;
import ec.edu.monster.consola.modelo.RespuestaCostos;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.Formato;
import ec.edu.monster.consola.util.Teclado;

/**
 * Pantalla de costos de mantenimiento (CU-08).
 */
public class VistaCostos {

    private final ControladorCostos controlador;
    private final Teclado teclado;
    private final Consola consola;

    /**
     * @param controlador controlador de costos, obligatorio
     * @param teclado     teclado de la consola, obligatorio
     * @param consola     salida de la consola, obligatoria
     */
    public VistaCostos(ControladorCostos controlador, Teclado teclado, Consola consola) {
        this.controlador = controlador;
        this.teclado = teclado;
        this.consola = consola;
    }

    /**
     * Pide una cuenta y muestra la proyección de costos.
     */
    public void consultarCostos() {
        String cuenta = teclado.texto("Código de la cuenta");
        if (cuenta.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        RespuestaCostos respuesta = controlador.consultarCostos(cuenta);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudieron consultar los costos: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        consola.titulo("COSTOS DE MANTENIMIENTO DE " + Formato.texto(cuenta));
        consola.linea("  Saldo actual:          " + Formato.monto(respuesta.getSaldoActual()));
        consola.linea("  Costo de mantenimiento: " + Formato.monto(respuesta.getCostoMantenimiento()));
        consola.linea("  Costo por movimiento:  " + Formato.monto(respuesta.getCostoMovimiento()));
        consola.linea("  Interés proyectado:     " + Formato.monto(respuesta.getInteresProyectado()));
        consola.linea("  Límite de mantenimiento: " + Formato.monto(respuesta.getLimiteMantenimiento()));
        consola.linea("  Operaciones sin costo:  " + Formato.entero(respuesta.getOperacionesSinCosto()));
    }
}
