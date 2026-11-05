package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import ec.edu.monster.seguridad.FiltroToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Extraccion del token de la cabecera Authorization (contrato, seccion 6.5). */
class FiltroTokenTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "bearer abc", "BEARER abc", "Bearer", "Bearer ", "Bearer   ",
            "Bearer  abc", "Bearer\tabc", "Token abc", " Bearer abc", "BearerX"})
    void cabeceraAusenteVaciaOMalFormadaNoDevuelveToken(String cabecera) {
        assertNull(FiltroToken.extraerToken(cabecera));
    }

    @Test
    void bearerValidoDevuelveElToken() {
        assertEquals("abc", FiltroToken.extraerToken("Bearer abc"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer a b", "Bearer  abc", "Bearer abc "})
    void tokenConEspaciosDevuelveNull(String cabecera) {
        assertNull(FiltroToken.extraerToken(cabecera));
    }

    @Test
    void bearerSinEspaciosExtraDevuelveElToken() {
        assertEquals("abc", FiltroToken.extraerToken("Bearer abc"));
    }

    @Test
    void sinCabeceraElFiltroNoFijaPropiedadNiAborta() throws Exception {
        SimuladosRest.PeticionFalsa peticion = new SimuladosRest.PeticionFalsa();

        new FiltroToken().filter(peticion.contexto());

        assertNull(peticion.propiedad(FiltroToken.PROPIEDAD_TOKEN));
        assertFalse(peticion.abortada());
    }

    @Test
    void conBearerValidoElFiltroGuardaElToken() throws Exception {
        SimuladosRest.PeticionFalsa peticion = new SimuladosRest.PeticionFalsa()
                .conCabecera("Authorization", "Bearer abc");

        new FiltroToken().filter(peticion.contexto());

        assertEquals("abc", peticion.propiedad(FiltroToken.PROPIEDAD_TOKEN));
        assertFalse(peticion.abortada());
    }

    @Test
    void conCabeceraMalFormadaElFiltroNoAbortaNiGuardaNada() throws Exception {
        SimuladosRest.PeticionFalsa peticion = new SimuladosRest.PeticionFalsa()
                .conCabecera("Authorization", "Basic abc");

        new FiltroToken().filter(peticion.contexto());

        assertNull(peticion.propiedad(FiltroToken.PROPIEDAD_TOKEN));
        assertFalse(peticion.abortada());
    }
}
