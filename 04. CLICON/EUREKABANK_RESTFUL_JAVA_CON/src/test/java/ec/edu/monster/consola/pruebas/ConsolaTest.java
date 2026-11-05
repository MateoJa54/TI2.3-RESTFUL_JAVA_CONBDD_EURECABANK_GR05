package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.util.Consola;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * La salida de la consola: líneas, títulos y tablas.
 */
class ConsolaTest {

    private final ByteArrayOutputStream pantalla = new ByteArrayOutputStream();
    private final Consola consola = new Consola(new PrintStream(pantalla, true, StandardCharsets.UTF_8));

    private String salida() {
        return pantalla.toString(StandardCharsets.UTF_8);
    }

    @Test
    void unaLineaSeEscribeTalCual() {
        consola.linea("Hola");

        assertEquals("Hola" + System.lineSeparator(), salida());
    }

    @Test
    void unaLineaVaciaSeEscribe() {
        consola.vacio();

        assertEquals(System.lineSeparator(), salida());
    }

    @Test
    void unTituloSeEnmarcaEntreLineasDeSignos() {
        consola.titulo("EURECABANK");

        assertTrue(salida().contains("=== EURECABANK ==="));
    }

    @Test
    void laTablaMuestraLaCabeceraYLasFilas() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] {"00200002", "Soles", "6800.00"});
        filas.add(new String[] {"00100001", "Soles", "7404.00"});

        consola.tabla(new String[] {"CODIGO", "MONEDA", "SALDO"}, filas);

        String texto = salida();
        assertTrue(texto.contains("CODIGO"));
        assertTrue(texto.contains("00200002"));
        assertTrue(texto.contains("7404.00"));
        assertTrue(texto.contains("-----"));
    }

    @Test
    void unaTablaVaciaSoloMuestraLaCabecera() {
        consola.tabla(new String[] {"CODIGO"}, new ArrayList<>());

        assertTrue(salida().contains("CODIGO"));
    }

    @Test
    void laSalidaEsObligatoria() {
        assertThrows(NullPointerException.class, () -> new Consola(null));
    }
}
