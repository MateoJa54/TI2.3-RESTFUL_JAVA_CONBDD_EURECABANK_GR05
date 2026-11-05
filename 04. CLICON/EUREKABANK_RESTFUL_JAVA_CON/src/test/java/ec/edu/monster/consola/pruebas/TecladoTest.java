package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.util.Teclado;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Lo que el usuario escribe: texto, opciones de menú e importes.
 */
class TecladoTest {

    private final ByteArrayOutputStream pantalla = new ByteArrayOutputStream();
    private final PrintStream salida = new PrintStream(pantalla, true, StandardCharsets.UTF_8);

    private Teclado teclado(String... lineas) {
        String texto = lineas.length == 0 ? "" : String.join("\n", lineas) + "\n";
        return new Teclado(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), salida);
    }

    private String pantalla() {
        return pantalla.toString(StandardCharsets.UTF_8);
    }

    @Test
    void loQueSeEscribeSeDevuelveSinEspaciosSobrantes() {
        assertEquals("cromero", teclado("   cromero   ").texto("Usuario"));
    }

    @Test
    void unaLineaVaciaSeDevuelveVacia() {
        assertEquals("", teclado("").texto("Usuario"));
    }

    @Test
    void elAvisoSeEscribeEnPantalla() {
        Teclado t = teclado("hola");

        t.aviso("  Aviso de prueba");

        assertTrue(pantalla().contains("Aviso de prueba"));
    }

    @Test
    void laPreguntaSeEscribeEnPantalla() {
        teclado("").texto("Usuario");

        assertTrue(pantalla().contains("Usuario: "));
    }

    @Test
    void unaOpcionValidaSeAcepta() {
        assertEquals(3, teclado("3").opcion("Opción", 0, 7));
    }

    @Test
    void unaOpcionFueraDeRangoSeVuelveAPedir() {
        Teclado t = teclado("9", "2");

        assertEquals(2, t.opcion("Opción", 0, 7));
        assertTrue(pantalla().contains("Opción no válida"));
    }

    @Test
    void unaOpcionQueNoEsNumeroSeVuelveAPedir() {
        Teclado t = teclado("abc", "0");

        assertEquals(0, t.opcion("Opción", 0, 7));
        assertTrue(pantalla().contains("Opción no válida"));
    }

    @Test
    void unaOpcionConSignoNoSeAcepta() {
        Teclado t = teclado("-1", "1");

        assertEquals(1, t.opcion("Opción", 0, 7));
    }

    @Test
    void laContrasenaSeLeeSinMostrarEnPantalla() {
        assertEquals("secreto", teclado("secreto").secreto("Contraseña"));
    }

    @Test
    void unImporteEnteroSeAcepta() {
        assertEquals(new BigDecimal("100.00"), teclado("100").monto("Monto"));
    }

    @Test
    void unImporteConDosDecimalesSeAcepta() {
        assertEquals(new BigDecimal("100.55"), teclado("100.55").monto("Monto"));
    }

    @Test
    void unImporteConComaDecimalSeAcepta() {
        assertEquals(new BigDecimal("100.55"), teclado("100,55").monto("Monto"));
    }

    @Test
    void unImporteConTresDecimalesSeRechaza() {
        Teclado t = teclado("100.555", "10");

        assertEquals(new BigDecimal("10.00"), t.monto("Monto"));
        assertTrue(pantalla().contains("Monto no válido"));
    }

    @Test
    void unImporteNegativoSeRechaza() {
        Teclado t = teclado("-5", "10");

        assertEquals(new BigDecimal("10.00"), t.monto("Monto"));
    }

    @Test
    void unImporteCeroSeRechaza() {
        Teclado t = teclado("0", "10");

        assertEquals(new BigDecimal("10.00"), t.monto("Monto"));
    }

    @Test
    void unImporteVacioSeRechaza() {
        Teclado t = teclado("", "10");

        assertEquals(new BigDecimal("10.00"), t.monto("Monto"));
    }

    @Test
    void unImporteQueNoEsNumeroSeRechaza() {
        Teclado t = teclado("abc", "10");

        assertEquals(new BigDecimal("10.00"), t.monto("Monto"));
    }

    @Test
    void unImporteCienMilVecesMayorQueCeroSeAcepta() {
        assertEquals(new BigDecimal("1000000.00"), teclado("1000000").monto("Monto"));
    }

    @ParameterizedTest
    @CsvSource({
        "100, 100.00",
        "100.55, 100.55",
        "0.01, 0.01",
        "12345.67, 12345.67"})
    void losImportesValidosSeNormalizanADosDecimales(String escrito, String esperado) {
        assertEquals(new BigDecimal(esperado), Teclado.interpretarMonto(escrito));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "-1", "0", "0.001", "1.234", "1e3", "1,2,3", "$5", "10 USD"})
    void losImportesInvalidosNoSeAceptan(String escrito) {
        assertNull(Teclado.interpretarMonto(escrito));
    }

    @Test
    void unImporteNuloNoSeAcepta() {
        assertNull(Teclado.interpretarMonto(null));
    }

    @Test
    void cuandoLaEntradaSeCierraElTecladoAvisa() {
        Teclado t = teclado();

        assertThrows(NoSuchElementException.class, () -> t.texto("Usuario"));
    }

    @Test
    void cuandoLaEntradaSeCierraEnElMontoTambienAvisa() {
        Teclado t = teclado();

        assertThrows(NoSuchElementException.class, () -> t.monto("Monto"));
    }
}
