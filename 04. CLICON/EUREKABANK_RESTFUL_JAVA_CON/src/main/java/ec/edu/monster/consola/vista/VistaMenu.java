package ec.edu.monster.consola.vista;

import ec.edu.monster.consola.controlador.ControladorAutenticacion;
import ec.edu.monster.consola.modelo.RespuestaBase;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.ExcepcionServicio;
import ec.edu.monster.consola.util.Formato;
import ec.edu.monster.consola.util.Teclado;

/**
 * Menú principal del cliente de consola.
 *
 * <p>Se muestra mientras haya sesión iniciada. Al salir se cierra la sesión en
 * el servidor con el token recibido.
 */
public class VistaMenu {

    private static final int SALIR = 0;
    private static final int LISTAR_CUENTAS = 1;
    private static final int CONSULTAR_CUENTA = 2;
    private static final int CONSULTAR_MOVIMIENTOS = 3;
    private static final int DEPOSITAR = 4;
    private static final int RETIRAR = 5;
    private static final int TRANSFERIR = 6;
    private static final int CONSULTAR_COSTOS = 7;

    private final ControladorAutenticacion autenticacion;
    private final VistaCuentas vistaCuentas;
    private final VistaMovimientos vistaMovimientos;
    private final VistaCostos vistaCostos;
    private final Teclado teclado;
    private final Consola consola;

    /**
     * @param autenticacion   controlador de autenticación, obligatorio
     * @param vistaCuentas    pantalla de cuentas, obligatoria
     * @param vistaMovimientos pantalla de movimientos, obligatoria
     * @param vistaCostos     pantalla de costos, obligatoria
     * @param teclado         teclado de la consola, obligatorio
     * @param consola         salida de la consola, obligatoria
     */
    public VistaMenu(ControladorAutenticacion autenticacion,
            VistaCuentas vistaCuentas,
            VistaMovimientos vistaMovimientos,
            VistaCostos vistaCostos,
            Teclado teclado,
            Consola consola) {
        this.autenticacion = autenticacion;
        this.vistaCuentas = vistaCuentas;
        this.vistaMovimientos = vistaMovimientos;
        this.vistaCostos = vistaCostos;
        this.teclado = teclado;
        this.consola = consola;
    }

    /**
     * Atiende el menú hasta que la sesión termine o el usuario salga.
     *
     * @return {@code true} si el usuario eligió salir del programa;
     *         {@code false} si solo terminó la sesión
     */
    public boolean ejecutar() {
        while (autenticacion.haySesionActiva()) {
            mostrarMenu();
            int opcion = teclado.opcion("Opción", SALIR, CONSULTAR_COSTOS);
            if (opcion == SALIR) {
                cerrarSesion();
                return true;
            }
            atender(opcion);
            if (!autenticacion.haySesionActiva()) {
                consola.linea("Debe autenticarse de nuevo para continuar.");
            }
        }
        return false;
    }

    private void mostrarMenu() {
        consola.titulo("EURECABANK - CLIENTE DE CONSOLA");
        consola.linea("  1. Listar cuentas por cédula");
        consola.linea("  2. Consultar una cuenta");
        consola.linea("  3. Consultar movimientos de una cuenta");
        consola.linea("  4. Realizar depósito");
        consola.linea("  5. Realizar retiro");
        consola.linea("  6. Realizar transferencia");
        consola.linea("  7. Consultar costos de mantenimiento");
        consola.linea("  0. Salir");
    }

    private void atender(int opcion) {
        try {
            switch (opcion) {
                case LISTAR_CUENTAS -> vistaCuentas.listarCuentas();
                case CONSULTAR_CUENTA -> vistaCuentas.consultarCuenta();
                case CONSULTAR_MOVIMIENTOS -> vistaMovimientos.consultarMovimientos();
                case DEPOSITAR -> vistaMovimientos.depositar();
                case RETIRAR -> vistaMovimientos.retirar();
                case TRANSFERIR -> vistaMovimientos.transferir();
                case CONSULTAR_COSTOS -> vistaCostos.consultarCostos();
                default -> throw new IllegalStateException("Opción no contemplada: " + opcion);
            }
        } catch (ExcepcionServicio e) {
            consola.linea(e.getMessage());
            consola.linea("Verifique que el servidor esté encendido e intente de nuevo.");
        }
    }

    private void cerrarSesion() {
        try {
            RespuestaBase respuesta = autenticacion.cerrarSesion();
            if (respuesta != null && "OK".equals(respuesta.getEstado())) {
                consola.linea("Sesión cerrada: "
                        + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            } else if (respuesta != null) {
                consola.linea("El servidor informó: "
                        + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            }
        } catch (ExcepcionServicio e) {
            consola.linea(e.getMessage());
            consola.linea("La sesión local se cerró de todos modos.");
        }
    }
}
