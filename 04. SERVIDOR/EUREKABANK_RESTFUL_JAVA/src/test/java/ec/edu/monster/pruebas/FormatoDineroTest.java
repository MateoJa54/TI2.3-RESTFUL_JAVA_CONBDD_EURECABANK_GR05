package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.utilidades.FormatoDinero;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del manejo de importes (regla RB-01).
 */
class FormatoDineroTest {

    @Test
    void todoImporteQuedaConDosDecimales() {
        assertEquals(new BigDecimal("100.00"), FormatoDinero.aDosDecimales(new BigDecimal("100")));
        assertEquals(new BigDecimal("100.50"), FormatoDinero.aDosDecimales(new BigDecimal("100.5")));
        assertEquals(new BigDecimal("100.50"), FormatoDinero.aDosDecimales(new BigDecimal("100.499")));
    }

    @Test
    void elRedondeoEsHalfUpYNoTrunca() {
        assertEquals(new BigDecimal("1.24"), FormatoDinero.aDosDecimales(new BigDecimal("1.235")));
        assertEquals(new BigDecimal("1.23"), FormatoDinero.aDosDecimales(new BigDecimal("1.234")));
        assertEquals(new BigDecimal("2.00"), FormatoDinero.aDosDecimales(new BigDecimal("1.995")));
    }

    @Test
    void elRedondeoHalfUpSubeEnElLimiteExacto() {
        assertEquals(new BigDecimal("0.13"), FormatoDinero.aDosDecimales(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.12"), FormatoDinero.aDosDecimales(new BigDecimal("0.124")));
    }

    @Test
    void unImporteNuloDevuelveNulo() {
        assertNull(FormatoDinero.aDosDecimales(null));
    }

    @Test
    void elCeroDelBancoTieneEscalaDos() {
        assertEquals(new BigDecimal("0.00"), FormatoDinero.CERO);
        assertEquals(2, FormatoDinero.CERO.scale());
    }

    @Test
    void laSumaQuedaConDosDecimales() {
        assertEquals(new BigDecimal("300.50"),
                FormatoDinero.sumar(new BigDecimal("100.25"), new BigDecimal("200.25")));
        assertEquals(new BigDecimal("0.00"),
                FormatoDinero.sumar(new BigDecimal("0.005"), new BigDecimal("-0.005")));
    }

    @Test
    void laRestaQuedaConDosDecimales() {
        assertEquals(new BigDecimal("99.75"),
                FormatoDinero.restar(new BigDecimal("100.00"), new BigDecimal("0.25")));
        assertEquals(new BigDecimal("-10.00"),
                FormatoDinero.restar(new BigDecimal("0.00"), new BigDecimal("10.00")));
    }

    @Test
    void laRestaNuncaDejaUnSaldoNegativoOCulto() {
        BigDecimal saldo = FormatoDinero.restar(new BigDecimal("50.00"), new BigDecimal("60.00"));
        assertTrue(saldo.compareTo(BigDecimal.ZERO) < 0);
        assertEquals(new BigDecimal("-10.00"), saldo);
    }

    @ParameterizedTest
    @CsvSource({
        "1000.00, 0.08, 0.80",
        "10000.00, 0.08, 8.00",
        "50.00, 0.08, 0.04",
        "0.01, 0.08, 0.00",
        "6800.00, 0.70, 47.60"
    })
    void elPorcentajeSeCalculaSobreCien(String base, String porcentaje, String esperado) {
        assertEquals(new BigDecimal(esperado),
                FormatoDinero.porcentaje(new BigDecimal(base), new BigDecimal(porcentaje)));
    }

    @Test
    void elPorcentajeConCentimosRedondeaHalfUp() {
        assertEquals(new BigDecimal("0.04"),
                FormatoDinero.porcentaje(new BigDecimal("50.00"), new BigDecimal("0.08")));
        assertEquals(new BigDecimal("0.12"),
                FormatoDinero.porcentaje(new BigDecimal("1234.00"), new BigDecimal("0.01")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.10", "1", "1.00", "100.99", "99999999.99"})
    void unMontoValidoEsElQueSuperaCeroYTieneDosDecimales(String valor) {
        assertTrue(FormatoDinero.esMontoValido(new BigDecimal(valor)), valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "0.001", "-1", "-0.01", "1.001", "10.555"})
    void unMontoInvalidoEsElQueNoSuperaCeroOTieneMasDeDosDecimales(String valor) {
        assertFalse(FormatoDinero.esMontoValido(new BigDecimal(valor)), valor);
    }

    @Test
    void unMontoNuloNoEsValido() {
        assertFalse(FormatoDinero.esMontoValido(null));
    }

    @Test
    void unTrailingCeroNoCuentaComoTercerDecimal() {
        assertTrue(FormatoDinero.esMontoValido(new BigDecimal("10.500")));
        assertTrue(FormatoDinero.esMontoValido(new BigDecimal("10.5")));
    }

    @Test
    void laEscalaYElRedondeoSonLosDelContrato() {
        assertEquals(2, FormatoDinero.ESCALA);
        assertEquals(java.math.RoundingMode.HALF_UP, FormatoDinero.REDONDEO);
    }
}
