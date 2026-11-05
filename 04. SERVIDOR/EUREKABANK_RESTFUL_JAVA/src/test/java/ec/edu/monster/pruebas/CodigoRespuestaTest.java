package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CodigoRespuesta;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pruebas del catalogo de codigos de resultado.
 *
 * <p>Comprueban los 17 codigos del contrato congelado con su mensaje literal.
 */
class CodigoRespuestaTest {

    @Test
    void elCatalogoTieneLosDiecisieteCodigosDelContrato() {
        assertEquals(17, CodigoRespuesta.values().length);
    }

    @Test
    void elCodigoDeExitoSeLlamaDosCien() {
        assertEquals("200", CodigoRespuesta.OK_200.getCodigo());
        assertEquals("OK", CodigoRespuesta.OK_200.getEstado());
        assertTrue(CodigoRespuesta.OK_200.esExitoso());
    }

    @Test
    void todosLosCodigosDeErrorLlevanEstadoError() {
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            if (!codigo.esExitoso()) {
                assertEquals("ERROR", codigo.getEstado(),
                        "El codigo " + codigo + " deberia llevar el estado ERROR");
                assertFalse(codigo.esExitoso());
            }
        }
    }

    @Test
    void losCodigosSonUnicos() {
        Set<String> codigos = new HashSet<>();
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            assertTrue(codigos.add(codigo.getCodigo()),
                    "El codigo " + codigo.getCodigo() + " esta repetido");
        }
    }

    @Test
    void losMensajesSonUnicos() {
        Set<String> mensajes = new HashSet<>();
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            assertTrue(mensajes.add(codigo.getMensaje()),
                    "El mensaje de " + codigo.getCodigo() + " esta repetido");
        }
    }

    @Test
    void ningunCodigoTieneMensajeVacio() {
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            assertNotNull(codigo.getMensaje());
            assertFalse(codigo.getMensaje().isBlank(),
                    "El codigo " + codigo.getCodigo() + " no tiene mensaje");
        }
    }

    @ParameterizedTest
    @EnumSource(CodigoRespuesta.class)
    void todoCodigoTieneEstadoCodigoYMensaje(CodigoRespuesta codigo) {
        assertNotNull(codigo.getEstado());
        assertNotNull(codigo.getCodigo());
        assertNotNull(codigo.getMensaje());
        assertFalse(codigo.getEstado().isBlank());
        assertFalse(codigo.getCodigo().isBlank());
    }

    @Test
    void losMensajesLiteralesSonLosDelContrato() {
        assertEquals("Los datos de la petición están incompletos o son incorrectos",
                CodigoRespuesta.DAT_001.getMensaje());
        assertEquals("El usuario o la contraseña son incorrectos",
                CodigoRespuesta.AUTH_001.getMensaje());
        assertEquals("El usuario se encuentra inactivo",
                CodigoRespuesta.AUTH_002.getMensaje());
        assertEquals("El token de sesión no es válido",
                CodigoRespuesta.AUTH_003.getMensaje());
        assertEquals("El token de sesión ha expirado, inicie sesión nuevamente",
                CodigoRespuesta.AUTH_004.getMensaje());
        assertEquals("La cuenta solicitada no existe",
                CodigoRespuesta.CTA_001.getMensaje());
        assertEquals("La cuenta no se encuentra en estado ACTIVO",
                CodigoRespuesta.CTA_002.getMensaje());
        assertEquals("La cuenta origen y la cuenta destino no pueden ser iguales",
                CodigoRespuesta.CTA_003.getMensaje());
        assertEquals("La cuenta de destino pertenece a una moneda distinta",
                CodigoRespuesta.CTA_004.getMensaje());
        assertEquals("El cliente solicitado no existe",
                CodigoRespuesta.CLI_001.getMensaje());
        assertEquals("El saldo de la cuenta es insuficiente para la operación",
                CodigoRespuesta.SAL_001.getMensaje());
        assertEquals("El monto debe ser mayor que cero y con máximo dos decimales",
                CodigoRespuesta.MON_001.getMensaje());
        assertEquals("La moneda de la cuenta no está soportada por el sistema",
                CodigoRespuesta.MON_002.getMensaje());
        assertEquals("El monto supera el límite máximo de mantenimiento de la cuenta",
                CodigoRespuesta.LIM_001.getMensaje());
        assertEquals("El servicio no está disponible en este momento",
                CodigoRespuesta.SER_001.getMensaje());
        assertEquals("Ocurrió un error interno, intente nuevamente",
                CodigoRespuesta.ERR_001.getMensaje());
    }

    @Test
    void losCodigosDeErrorSiguenElFormatoDelCatalogo() {
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            if (codigo.esExitoso()) {
                continue;
            }
            assertTrue(codigo.getCodigo().matches("[A-Z]{3,4}-\\d{3}"),
                    "El codigo " + codigo.getCodigo() + " no sigue el formato del catalogo");
        }
    }

    @Test
    void soloElCodigoDeExitoEsExitoso() {
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            assertEquals(codigo == CodigoRespuesta.OK_200, codigo.esExitoso());
        }
    }
}
