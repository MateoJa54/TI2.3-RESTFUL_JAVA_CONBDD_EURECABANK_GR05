package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.monster.web.util.Formato;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Los importes se muestran con dos decimales, que es la escala de la base de
 * datos (regla RB-01 del contrato).
 */
class FormatoTest {

    @Test
    void unImporteEnteroSeMuestraConDosDecimales() {
        assertEquals("100.00", Formato.monto(new BigDecimal("100")));
    }

    @Test
    void unImporteNuloSeMuestraComoCero() {
        assertEquals("0.00", Formato.monto(null));
    }

    @Test
    void unImporteConUnDecimalSeMuestraConDos() {
        assertEquals("1.50", Formato.monto(new BigDecimal("1.5")));
    }

    @Test
    void unImporteNegativoSeMuestraConElSigno() {
        assertEquals("-10.00", Formato.monto(new BigDecimal("-10")));
    }

    @Test
    void elCeroSeMuestraConDosDecimales() {
        assertEquals("0.00", Formato.monto(BigDecimal.ZERO));
    }

    @Test
    void elRedondeoEsHalfUp() {
        assertEquals("1.01", Formato.monto(new BigDecimal("1.005")));
        assertEquals("1.00", Formato.monto(new BigDecimal("1.004")));
    }

    @Test
    void losImportesGrandesNoSalenEnNotacionCientifica() {
        assertEquals("10000000000.00", Formato.monto(new BigDecimal("1E+10")));
    }

    @Test
    void losImportesMuyPequenosNoSePierden() {
        assertEquals("0.01", Formato.monto(new BigDecimal("0.01")));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0.00",
        "1, 1.00",
        "100, 100.00",
        "100.5, 100.50",
        "100.55, 100.55",
        "6800, 6800.00",
        "1234567.89, 1234567.89"})
    void elImporteSeMuestraSiempreConDosDecimales(String entrada, String esperado) {
        assertEquals(esperado, Formato.monto(new BigDecimal(entrada)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "7", "15", "1000"})
    void unEnteroSeMuestraTalCual(int valor) {
        assertEquals(String.valueOf(valor), Formato.entero(valor));
    }

    @Test
    void unEnteroAusenteSeMuestraComoCero() {
        assertEquals("0", Formato.entero(null));
    }

    @Test
    void unTextoAusenteSeMuestraComoCadenaVacia() {
        assertEquals("", Formato.texto(null));
        assertEquals("ABC", Formato.texto("ABC"));
    }

    @Test
    void elResultadoSeMuestraConElCodigoYElMensajeDelServidor() {
        assertEquals("[AUTH-001] El usuario o la contraseña son incorrectos",
                Formato.resultado("AUTH-001", "El usuario o la contraseña son incorrectos"));
    }

    @Test
    void unResultadoSinCodigoNiMensajeNoRompeLaPantalla() {
        assertEquals("[] ", Formato.resultado(null, null));
    }
}
