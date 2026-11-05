package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.monster.api.rest.ManejadorExcepcion;
import ec.edu.monster.modelos.CodigoRespuesta;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Mapa de los 17 codigos del catalogo al HTTP status (contrato, seccion 3). */
class MapeoCodigoHttpTest {

    private static final Map<String, Integer> ESPERADO = Map.ofEntries(
            Map.entry("200", 200),
            Map.entry("DAT-001", 400),
            Map.entry("AUTH-001", 401),
            Map.entry("AUTH-002", 403),
            Map.entry("AUTH-003", 401),
            Map.entry("AUTH-004", 401),
            Map.entry("CTA-001", 404),
            Map.entry("CTA-002", 409),
            Map.entry("CTA-003", 400),
            Map.entry("CTA-004", 409),
            Map.entry("CLI-001", 404),
            Map.entry("SAL-001", 409),
            Map.entry("MON-001", 400),
            Map.entry("MON-002", 409),
            Map.entry("LIM-001", 409),
            Map.entry("SER-001", 503),
            Map.entry("ERR-001", 500));

    @ParameterizedTest
    @CsvSource({
        "200,200", "DAT-001,400", "AUTH-001,401", "AUTH-002,403", "AUTH-003,401",
        "AUTH-004,401", "CTA-001,404", "CTA-002,409", "CTA-003,400", "CTA-004,409",
        "CLI-001,404", "SAL-001,409", "MON-001,400", "MON-002,409", "LIM-001,409",
        "SER-001,503", "ERR-001,500"
    })
    void cadaCodigoTieneSuStatus(String codigo, int status) {
        assertEquals(status, ManejadorExcepcion.estadoHttp(codigo));
    }

    @Test
    void elCatalogoTieneDiecisieteCodigosYTodosTienenStatus() {
        assertEquals(17, CodigoRespuesta.values().length);
        for (CodigoRespuesta codigo : CodigoRespuesta.values()) {
            assertEquals(ESPERADO.get(codigo.getCodigo()).intValue(),
                    ManejadorExcepcion.estadoHttp(codigo.getCodigo()), codigo.getCodigo());
        }
    }

    @Test
    void unCodigoDesconocidoOUnNuloDevuelve500() {
        assertEquals(500, ManejadorExcepcion.estadoHttp("XYZ-999"));
        assertEquals(500, ManejadorExcepcion.estadoHttp(null));
    }
}
