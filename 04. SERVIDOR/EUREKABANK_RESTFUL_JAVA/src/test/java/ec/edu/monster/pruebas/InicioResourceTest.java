package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.api.rest.InicioResource;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class InicioResourceTest {

    private final InicioResource recurso = new InicioResource();

    @Test
    void laPortadaResponde200ConHtmlYLasNueveRutas() {
        Response r = recurso.portada();
        String html = (String) r.getEntity();
        assertEquals(200, r.getStatus());
        assertTrue(r.getMediaType().toString().startsWith("text/html"));
        assertTrue(html.contains("/api/auth/login"));
        assertTrue(html.contains("/api/transferencias"));
        assertEquals(9, html.split("<tr>").length - 1);
    }

    @Test
    void elIndiceJsonListaLasNueveRutas() {
        Response r = recurso.indice();
        String json = (String) r.getEntity();
        assertEquals(200, r.getStatus());
        assertTrue(r.getMediaType().toString().startsWith("application/json"));
        assertEquals(9, json.split("\"metodo\"").length - 1);
        assertTrue(json.startsWith("{\"servicio\":\"EurekaBank REST\""));
    }

    @Test
    void ningunaSalidaLlevaGuionLargo() {
        char largo = (char) 0x2014;
        assertFalse(((String) recurso.portada().getEntity()).indexOf(largo) >= 0);
        assertFalse(((String) recurso.indice().getEntity()).indexOf(largo) >= 0);
    }
}
