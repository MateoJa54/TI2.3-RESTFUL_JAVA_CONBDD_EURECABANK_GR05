package ec.edu.monster.consola;

import ec.edu.monster.consola.cliente.ClienteEUREKABANK;
import ec.edu.monster.consola.cliente.IClienteEUREKABANK;
import ec.edu.monster.consola.controlador.ControladorAutenticacion;
import ec.edu.monster.consola.controlador.ControladorCostos;
import ec.edu.monster.consola.controlador.ControladorCuentas;
import ec.edu.monster.consola.controlador.ControladorMovimientos;
import ec.edu.monster.consola.modelo.SesionUsuario;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.Teclado;
import ec.edu.monster.consola.vista.VistaAutenticacion;
import ec.edu.monster.consola.vista.VistaCostos;
import ec.edu.monster.consola.vista.VistaCuentas;
import ec.edu.monster.consola.vista.VistaMenu;
import ec.edu.monster.consola.vista.VistaMovimientos;
import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

/**
 * Punto de arranque del cliente de consola de EURECABANK. Es el único lugar
 * que conoce las clases concretas: crea cada objeto y se lo entrega a quien lo
 * necesita (inyección de dependencias por constructor).
 *
 * <p>La dirección del servicio se toma, en este orden, del primer argumento
 * del programa, de la propiedad {@code -Dservicio.url}, de la variable de
 * entorno {@code SERVICIO_URL} o, si no hay ninguna, del servidor local.
 *
 * <p>El cliente arranca aunque el servidor esté apagado: el contrato OpenAPI
 * va empaquetado en el proyecto y el fallo aparece, controlado, en la
 * primera llamada.
 */
public final class Main {

    private Main() {
    }

    /**
     * @param argumentos dirección del servicio, opcional
     */
    public static void main(String[] argumentos) {
        Consola consola = new Consola(System.out);
        String url = resolverUrl(argumentos);
        consola.linea("Servidor: " + url);
        IClienteEUREKABANK cliente;
        try {
            cliente = new ClienteEUREKABANK(url);
        } catch (IllegalArgumentException e) {
            consola.linea(e.getMessage());
            consola.linea("Ejemplo: " + ClienteEUREKABANK.URL_POR_DEFECTO);
            return;
        }
        Teclado teclado = new Teclado(System.in, System.out, codificacionDelTeclado(), System.console());
        ejecutar(cliente, teclado, consola);
    }

    /**
     * Arma el cliente y lo ejecuta hasta que el usuario salga. Recibe sus
     * dependencias para poder probar el recorrido completo sin servidor ni
     * teclado.
     *
     * @param cliente cliente del servicio
     * @param teclado  teclado de la consola
     * @param consola  salida de la consola
     */
    public static void ejecutar(IClienteEUREKABANK cliente, Teclado teclado, Consola consola) {
        SesionUsuario sesion = new SesionUsuario();
        ControladorAutenticacion autenticacion = new ControladorAutenticacion(cliente, sesion);
        ControladorCuentas cuentas = new ControladorCuentas(cliente, sesion);
        ControladorMovimientos movimientos = new ControladorMovimientos(cliente, sesion);
        ControladorCostos costos = new ControladorCostos(cliente, sesion);

        VistaAutenticacion vistaAutenticacion = new VistaAutenticacion(autenticacion, teclado, consola);
        VistaCuentas vistaCuentas = new VistaCuentas(cuentas, teclado, consola);
        VistaMovimientos vistaMovimientos = new VistaMovimientos(movimientos, teclado, consola);
        VistaCostos vistaCostos = new VistaCostos(costos, teclado, consola);
        VistaMenu menu = new VistaMenu(autenticacion, vistaCuentas, vistaMovimientos,
                vistaCostos, teclado, consola);

        try {
            boolean salir = false;
            while (!salir && vistaAutenticacion.iniciar()) {
                salir = menu.ejecutar();
            }
        } catch (NoSuchElementException e) {
            consola.vacio();
        }
        consola.linea("Hasta pronto.");
    }

    /**
     * @param argumentos argumentos del programa
     * @return la dirección del servicio resuelta
     */
    public static String resolverUrl(String[] argumentos) {
        if (argumentos != null && argumentos.length > 0 && !argumentos[0].isBlank()) {
            return argumentos[0].trim();
        }
        String url = System.getProperty("servicio.url");
        if (url == null || url.isBlank()) {
            url = System.getenv("SERVICIO_URL");
        }
        return (url == null || url.isBlank()) ? ClienteEUREKABANK.URL_POR_DEFECTO : url.trim();
    }

    /**
     * En Windows el teclado de la consola no llega en UTF-8; si se leyera con
     * la codificación equivocada, una "ñ" en la contraseña llegaría dañada.
     */
    private static Charset codificacionDelTeclado() {
        Console consola = System.console();
        if (consola != null) {
            return consola.charset();
        }
        String nombre = System.getProperty("stdin.encoding", System.getProperty("native.encoding"));
        try {
            return nombre == null ? Charset.defaultCharset() : Charset.forName(nombre);
        } catch (IllegalArgumentException e) {
            return Charset.defaultCharset();
        }
    }
}
