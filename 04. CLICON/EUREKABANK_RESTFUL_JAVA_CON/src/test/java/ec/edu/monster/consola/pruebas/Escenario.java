package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Utilidades compartidas por las pruebas de las vistas.
 */
final class Escenario {

    /**
     * Prepara las vistas con un cliente simulado y un teclado preparado.
     *
     * @param servidor cliente simulado
     * @param teclado  líneas que el usuario escribe
     * @return las piezas del escenario
     */
    static Escenario crear(ClienteFalso servidor, String... teclado) {
        ByteArrayOutputStream pantalla = new ByteArrayOutputStream();
        PrintStream salida = new PrintStream(pantalla, true, StandardCharsets.UTF_8);
        String texto = String.join("\n", teclado) + "\n";
        Teclado lector = new Teclado(
                new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), salida);
        Consola consola = new Consola(salida);
        SesionUsuario sesion = new SesionUsuario();
        ControladorAutenticacion autenticacion = new ControladorAutenticacion(servidor, sesion);
        ControladorCuentas cuentas = new ControladorCuentas(servidor, sesion);
        ControladorMovimientos movimientos = new ControladorMovimientos(servidor, sesion);
        ControladorCostos costos = new ControladorCostos(servidor, sesion);
        return new Escenario(servidor, sesion, autenticacion,
                new VistaAutenticacion(autenticacion, lector, consola),
                new VistaCuentas(cuentas, lector, consola),
                new VistaMovimientos(movimientos, lector, consola),
                new VistaCostos(costos, lector, consola),
                new VistaMenu(autenticacion,
                        new VistaCuentas(cuentas, lector, consola),
                        new VistaMovimientos(movimientos, lector, consola),
                        new VistaCostos(costos, lector, consola),
                        lector, consola),
                pantalla);
    }

    private final ClienteFalso servidor;
    private final SesionUsuario sesion;
    private final ControladorAutenticacion autenticacion;
    private final VistaAutenticacion vistaAutenticacion;
    private final VistaCuentas vistaCuentas;
    private final VistaMovimientos vistaMovimientos;
    private final VistaCostos vistaCostos;
    private final VistaMenu vistaMenu;
    private final ByteArrayOutputStream pantalla;

    private Escenario(ClienteFalso servidor, SesionUsuario sesion,
            ControladorAutenticacion autenticacion, VistaAutenticacion vistaAutenticacion,
            VistaCuentas vistaCuentas, VistaMovimientos vistaMovimientos,
            VistaCostos vistaCostos, VistaMenu vistaMenu, ByteArrayOutputStream pantalla) {
        this.servidor = servidor;
        this.sesion = sesion;
        this.autenticacion = autenticacion;
        this.vistaAutenticacion = vistaAutenticacion;
        this.vistaCuentas = vistaCuentas;
        this.vistaMovimientos = vistaMovimientos;
        this.vistaCostos = vistaCostos;
        this.vistaMenu = vistaMenu;
        this.pantalla = pantalla;
    }

    ClienteFalso getServidor() {
        return servidor;
    }

    SesionUsuario getSesion() {
        return sesion;
    }

    ControladorAutenticacion getAutenticacion() {
        return autenticacion;
    }

    VistaAutenticacion getVistaAutenticacion() {
        return vistaAutenticacion;
    }

    VistaCuentas getVistaCuentas() {
        return vistaCuentas;
    }

    VistaMovimientos getVistaMovimientos() {
        return vistaMovimientos;
    }

    VistaCostos getVistaCostos() {
        return vistaCostos;
    }

    VistaMenu getVistaMenu() {
        return vistaMenu;
    }

    /** @return lo que se ve en pantalla */
    String pantalla() {
        return pantalla.toString(StandardCharsets.UTF_8);
    }

    /** @param buscado texto a contar @return cuántas veces aparece */
    int veces(String buscado) {
        return pantalla().split(java.util.regex.Pattern.quote(buscado), -1).length - 1;
    }

    /** Autentica de verdad, con el hash, para dejar la sesión lista. */
    void autenticar() {
        autenticacion.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.llamadas.clear();
    }

    /** @return el texto que se ve en pantalla */
    boolean contiene(String texto) {
        return normalizar(pantalla()).contains(normalizar(texto));
    }

    /** Colapsa los espacios para no depender de la sangría de la vista. */
    private static String normalizar(String texto) {
        return texto.replaceAll("\\s+", " ").trim();
    }
}
