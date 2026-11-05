package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.utilidades.Codigos;
import ec.edu.monster.utilidades.Textos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de las utilidades de texto y de las constantes de mensaje.
 */
class TextosYCodigosTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", "\u00A0"})
    void unTextoVacioOConSoloEspaciosEstaVacio(String texto) {
        assertTrue(Textos.estaVacio(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cromero", "a", "  cromero  ", "0"})
    void unTextoConContenidoNoEstaVacio(String texto) {
        assertFalse(Textos.estaVacio(texto));
    }

    @Test
    void recortarQuitaLosEspaciosExteriores() {
        assertEquals("cromero", Textos.recortar("  cromero  "));
        assertNull(Textos.recortar(null));
    }

    @Test
    void losMensajesDeExitoSonLosDelContrato() {
        assertEquals("Autenticación exitosa", Codigos.MENSAJE_AUTENTICACION_OK);
        assertEquals("Sesión cerrada correctamente", Codigos.MENSAJE_CERRAR_SESION_OK);
        assertEquals("Cuentas consultadas correctamente", Codigos.MENSAJE_CUENTAS_OK);
        assertEquals("Cuenta consultada correctamente", Codigos.MENSAJE_CUENTA_OK);
        assertEquals("Movimientos consultados correctamente", Codigos.MENSAJE_MOVIMIENTOS_OK);
        assertEquals("Depósito realizado correctamente", Codigos.MENSAJE_DEPOSITO_OK);
        assertEquals("Retiro realizado correctamente", Codigos.MENSAJE_RETIRO_OK);
        assertEquals("Transferencia realizada correctamente", Codigos.MENSAJE_TRANSFERENCIA_OK);
        assertEquals("Costos de mantenimiento consultados correctamente",
                Codigos.MENSAJE_COSTOS_OK);
    }

    @Test
    void losEstadosYElCodigoDeExitoSonLosDelContrato() {
        assertEquals("OK", Codigos.ESTADO_OK);
        assertEquals("ERROR", Codigos.ESTADO_ERROR);
        assertEquals("200", Codigos.CODIGO_OK);
    }

    @Test
    void losFormatosDeFechaSonLosDelContrato() {
        assertEquals("yyyy-MM-dd", Codigos.FORMATO_FECHA);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", Codigos.FORMATO_FECHA_HORA);
    }
}
