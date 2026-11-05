package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.seguridad.FiltroCors;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Cabeceras CORS segun los origenes permitidos. */
class FiltroCorsTest {

    @Test
    void porDefectoSoloPermiteElClienteWebEn4200() {
        FiltroCors filtro = new FiltroCors(null);

        assertTrue(filtro.esOrigenPermitido("http://localhost:4200"));
        assertFalse(filtro.esOrigenPermitido("http://localhost:4201"));
        assertFalse(filtro.esOrigenPermitido("http://evil.test"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unaListaVaciaUsaElValorPorDefecto(String lista) {
        assertTrue(new FiltroCors(lista).esOrigenPermitido("http://localhost:4200"));
    }

    @Test
    void laListaSeparadaPorComasPermiteVariosOrigenes() {
        FiltroCors filtro = new FiltroCors("http://a.test, https://b.test ,");

        assertTrue(filtro.esOrigenPermitido("http://a.test"));
        assertTrue(filtro.esOrigenPermitido("https://b.test"));
        assertFalse(filtro.esOrigenPermitido("http://localhost:4200"));
    }

    @Test
    void unOrigenNuloNoEsPermitido() {
        assertFalse(new FiltroCors("http://a.test").esOrigenPermitido(null));
    }

    @Test
    void origenPermitidoRecibeLasTresCabeceras() throws Exception {
        MultivaluedMap<String, Object> cabeceras = SimuladosRest.cabecerasVacias();
        SimuladosRest.PeticionFalsa peticion = new SimuladosRest.PeticionFalsa()
                .conCabecera("Origin", "http://localhost:4200");

        new FiltroCors(null).filter(peticion.contexto(), SimuladosRest.respuestaFalsa(cabeceras));

        assertEquals("http://localhost:4200", cabeceras.getFirst("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, OPTIONS", cabeceras.getFirst("Access-Control-Allow-Methods"));
        assertEquals("Authorization, Content-Type", cabeceras.getFirst("Access-Control-Allow-Headers"));
    }

    @Test
    void origenNoPermitidoNoRecibeCabeceras() throws Exception {
        MultivaluedMap<String, Object> cabeceras = SimuladosRest.cabecerasVacias();
        SimuladosRest.PeticionFalsa peticion = new SimuladosRest.PeticionFalsa()
                .conCabecera("Origin", "http://evil.test");

        new FiltroCors(null).filter(peticion.contexto(), SimuladosRest.respuestaFalsa(cabeceras));

        assertTrue(cabeceras.isEmpty());
    }

    @Test
    void peticionSinOrigenNoRecibeCabeceras() throws Exception {
        MultivaluedMap<String, Object> cabeceras = SimuladosRest.cabecerasVacias();

        new FiltroCors(null).filter(new SimuladosRest.PeticionFalsa().contexto(),
                SimuladosRest.respuestaFalsa(cabeceras));

        assertTrue(cabeceras.isEmpty());
    }
}
