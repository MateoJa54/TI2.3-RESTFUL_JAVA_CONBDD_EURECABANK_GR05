package ec.edu.monster;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import ec.edu.monster.modelo.RespuestaBanco;
import ec.edu.monster.util.FormatoRespuesta;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Usa el JSON de Android real para probar los sobres y su presentación. */
@RunWith(AndroidJUnit4.class)
public class RespuestaBancoInstrumentadaTest {
    @Test public void interpretaLoginYConservaTokenYMensaje() throws Exception {
        RespuestaBanco r=RespuestaBanco.parsear(200,"{\"estado\":\"OK\",\"codigo\":\"200\",\"mensaje\":\"Autenticación exitosa\",\"token\":\"abc\"}");
        assertTrue(r.esCorrecta());
        assertEquals("abc",r.getToken());
        assertTrue(FormatoRespuesta.presentar(r.getDatos()).contains("Autenticación exitosa"));
    }

    @Test public void conservaCodigoYMensajeLiteralDeError() throws Exception {
        RespuestaBanco r=RespuestaBanco.parsear(409,"{\"estado\":\"ERROR\",\"codigo\":\"SAL-001\",\"mensaje\":\"Saldo insuficiente\"}");
        assertFalse(r.esCorrecta());
        assertEquals("SAL-001",r.getCodigo());
        assertTrue(FormatoRespuesta.presentar(r.getDatos()).contains("Saldo insuficiente"));
    }

    @Test public void rechazaCuerposQueNoCumplenElSobre() {
        try { RespuestaBanco.parsear(200,"{\"ok\":true}"); fail("Debió rechazar un sobre incompleto"); }
        catch (org.json.JSONException esperado) { assertTrue(esperado.getMessage().contains("sobre")); }
    }
}
