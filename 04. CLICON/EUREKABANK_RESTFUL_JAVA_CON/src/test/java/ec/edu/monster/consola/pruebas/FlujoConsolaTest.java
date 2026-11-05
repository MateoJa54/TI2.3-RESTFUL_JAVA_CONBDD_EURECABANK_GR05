package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.Main;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.Teclado;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Recorre el programa completo (vistas + controladores) como lo haría un
 * usuario: cada texto es una línea escrita en el teclado y se revisa lo que
 * aparece en pantalla. El servidor es simulado.
 */
class FlujoConsolaTest {

    private final ByteArrayOutputStream pantalla = new ByteArrayOutputStream();
    private final PrintStream salida = new PrintStream(pantalla, true, StandardCharsets.UTF_8);

    private String ejecutar(ClienteFalso servidor, String... teclado) {
        String texto = String.join("\n", teclado) + "\n";
        Teclado lector = new Teclado(
                new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), salida);
        Main.ejecutar(servidor, lector, new Consola(salida));
        return pantalla.toString(StandardCharsets.UTF_8);
    }

    private int veces(String texto, String buscado) {
        return texto.split(java.util.regex.Pattern.quote(buscado), -1).length - 1;
    }

    @Test
    void conCredencialCorrectaSeMuestraElMenu() {
        String pantalla = ejecutar(new ClienteFalso(), ClienteFalso.USUARIO, ClienteFalso.CLAVE, "0");

        assertTrue(pantalla.contains("Acceso concedido."));
        assertTrue(pantalla.contains("EURECABANK - CLIENTE DE CONSOLA"));
    }

    @Test
    void conCredencialIncorrectaSeMuestraElErrorYSeVuelveAPreguntar() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, "mala", ClienteFalso.USUARIO, ClienteFalso.CLAVE, "0");

        assertTrue(pantalla.contains("[AUTH-001] El usuario o la contraseña son incorrectos"));
        assertEquals(2, veces(pantalla, "INICIO DE SESIÓN"));
        assertTrue(pantalla.contains("Acceso concedido."));
    }

    @Test
    void conUsuarioVacioElProgramaTerminaSinLlamarAlServidor() {
        ClienteFalso servidor = new ClienteFalso();

        String pantalla = ejecutar(servidor, "");

        assertTrue(pantalla.contains("INICIO DE SESIÓN"));
        assertTrue(pantalla.endsWith("Hasta pronto." + System.lineSeparator()));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void elMenuNoApareceSinAutenticarse() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, "mala", "otro", "peor", "");

        assertFalse(pantalla.contains("EURECABANK - CLIENTE DE CONSOLA"));
    }

    @Test
    void elListadoDeCuentasMuestraLaTablaCompleta() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "1", ClienteFalso.CEDULA, "0");

        assertTrue(pantalla.contains("00200002"));
        assertTrue(pantalla.contains("6800.00"));
        assertTrue(pantalla.contains("Cliente de Prueba"));
    }

    @Test
    void laConsultaDeMovimientosMuestraLaTabla() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "3", ClienteFalso.CUENTA, "0");

        assertTrue(pantalla.contains("MOVIMIENTOS DE LA CUENTA 00200002"));
        assertTrue(pantalla.contains("Apertura de Cuenta"));
        assertTrue(pantalla.contains("1000.00"));
    }

    @Test
    void unDepositoCompletoMuestraElSaldoNuevo() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "4", ClienteFalso.CUENTA, "100.00", "0");

        assertTrue(pantalla.contains("DEPÓSITO"));
        assertTrue(pantalla.contains("6900.00"));
    }

    @Test
    void unRetiroSinSaldoMuestraElCodigoDelServidor() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "5", ClienteFalso.CUENTA, "50000.00", "0");

        assertTrue(pantalla.contains("[SAL-001] El saldo de la cuenta es insuficiente para la operación"));
    }

    @Test
    void unaTransferenciaCompletaMuestraLosSaldos() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "6",
                ClienteFalso.CUENTA, "00100001", "25.00", "0");

        assertTrue(pantalla.contains("TRANSFERENCIA"));
        assertTrue(pantalla.contains("6775.00"));
        assertTrue(pantalla.contains("0.02"));
    }

    @Test
    void laConsultaDeCostosMuestraLaProyeccion() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "7", ClienteFalso.CUENTA, "0");

        assertTrue(pantalla.contains("COSTOS DE MANTENIMIENTO DE 00200002"));
        assertTrue(pantalla.contains("54.40"));
    }

    @Test
    void alSalirSeCierraLaSesionEnElServidor() {
        ClienteFalso servidor = new ClienteFalso();

        String pantalla = ejecutar(servidor, ClienteFalso.USUARIO, ClienteFalso.CLAVE, "0");

        assertTrue(pantalla.contains("Sesión cerrada: [200] Sesión cerrada correctamente"));
        assertTrue(servidor.llamadas.contains("cerrarSesion(TOKEN-1)"));
    }

    @Test
    void unaOpcionDeMenuInvalidaSeVuelveAPedir() {
        String pantalla = ejecutar(new ClienteFalso(),
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "9", "x", "", "0");

        assertEquals(3, veces(pantalla, "Opción no válida"));
    }

    @Test
    void unMontoInvalidoSeVuelveAPedirYNoSeEnvia() {
        ClienteFalso servidor = new ClienteFalso();

        String pantalla = ejecutar(servidor,
                ClienteFalso.USUARIO, ClienteFalso.CLAVE, "4", ClienteFalso.CUENTA, "abc", "10.00", "0");

        assertTrue(pantalla.contains("Monto no válido"));
        assertEquals(1, servidor.llamadas.stream().filter(l -> l.startsWith("realizarDeposito")).count());
    }

    @Test
    void siElServidorEstaApagadoAlIniciarSesionSeExplica() {
        ClienteFalso servidor = new ClienteFalso();
        servidor.servidorApagado = true;

        String pantalla = ejecutar(servidor, ClienteFalso.USUARIO, ClienteFalso.CLAVE, "");

        assertTrue(pantalla.contains("No se pudo comunicar con el servidor"));
        assertTrue(pantalla.contains("Verifique que el servidor esté encendido"));
    }

    @Test
    void siLaEntradaSeCierraEnCualquierPuntoElProgramaTerminaConOrden() {
        for (String[] teclado : new String[][] {
            {ClienteFalso.USUARIO},
            {ClienteFalso.USUARIO, ClienteFalso.CLAVE},
            {ClienteFalso.USUARIO, ClienteFalso.CLAVE, "1"},
            {ClienteFalso.USUARIO, ClienteFalso.CLAVE, "4", ClienteFalso.CUENTA}}) {
            String pantalla = ejecutar(new ClienteFalso(), teclado);

            assertTrue(pantalla.endsWith("Hasta pronto." + System.lineSeparator()),
                    "Con " + teclado.length + " líneas de entrada");
        }
    }

    @Test
    void elProgramaSiempreSeDespide() {
        for (String[] teclado : new String[][] {
            {""}, {ClienteFalso.USUARIO, ClienteFalso.CLAVE, "0"},
            {ClienteFalso.USUARIO, ClienteFalso.CLAVE, "7", ClienteFalso.CUENTA, "0"}}) {
            assertTrue(ejecutar(new ClienteFalso(), teclado)
                    .endsWith("Hasta pronto." + System.lineSeparator()));
        }
    }
}
