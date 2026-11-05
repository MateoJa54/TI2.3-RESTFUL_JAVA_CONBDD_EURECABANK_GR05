package ec.edu.monster.consola.vista;

import ec.edu.monster.consola.controlador.ControladorAutenticacion;
import ec.edu.monster.consola.modelo.RespuestaAutenticacion;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.ExcepcionServicio;
import ec.edu.monster.consola.util.Formato;
import ec.edu.monster.consola.util.Teclado;

/**
 * Pantalla de inicio de sesión (CU-01).
 *
 * <p>Muestra el código y el mensaje del servidor tal como los entrega: no los
 * reescribe.
 */
public class VistaAutenticacion {

    private final ControladorAutenticacion controlador;
    private final Teclado teclado;
    private final Consola consola;

    /**
     * @param controlador controlador de autenticación, obligatorio
     * @param teclado     teclado de la consola, obligatorio
     * @param consola     salida de la consola, obligatoria
     */
    public VistaAutenticacion(ControladorAutenticacion controlador, Teclado teclado, Consola consola) {
        this.controlador = controlador;
        this.teclado = teclado;
        this.consola = consola;
    }

    /**
     * Pide usuario y contraseña hasta que el servidor los acepte.
     *
     * @return {@code true} si hay sesión iniciada; {@code false} si el usuario
     *         decidió salir (dejó el usuario vacío)
     */
    public boolean iniciar() {
        while (true) {
            consola.titulo("INICIO DE SESIÓN - EURECABANK");
            String usuario = teclado.texto("Usuario (vacío para salir)");
            if (usuario.isEmpty()) {
                return false;
            }
            String clave = teclado.secreto("Contraseña");
            try {
                RespuestaAutenticacion respuesta = controlador.autenticar(usuario, clave);
                if ("OK".equals(respuesta.getEstado()) && controlador.haySesionActiva()) {
                    mostrarSesion(respuesta);
                    return true;
                }
                consola.linea("No se pudo iniciar sesión: "
                        + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            } catch (ExcepcionServicio e) {
                consola.linea(e.getMessage());
                consola.linea("Verifique que el servidor esté encendido e intente de nuevo.");
            }
        }
    }

    private void mostrarSesion(RespuestaAutenticacion respuesta) {
        consola.linea("Acceso concedido.");
        consola.linea("  Token:      " + Formato.texto(respuesta.getToken()));
        consola.linea("  Empleado:   " + Formato.texto(respuesta.getEmpleado())
                + " - " + Formato.texto(respuesta.getNombreEmpleado()));
        consola.linea("  Rol:        " + Formato.texto(respuesta.getRol()));
        consola.linea("  Expiración: " + Formato.texto(respuesta.getExpiracion()));
    }
}
