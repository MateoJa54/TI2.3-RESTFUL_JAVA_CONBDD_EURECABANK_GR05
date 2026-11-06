package ec.edu.monster.modelo;

import org.json.JSONException;
import org.json.JSONObject;

/** Sobre REST común. Conserva los datos del servidor junto con su estado HTTP. */
public final class RespuestaBanco {
    private final int estadoHttp;
    private final JSONObject datos;

    public RespuestaBanco(int estadoHttp, JSONObject datos) {
        this.estadoHttp = estadoHttp;
        this.datos = datos == null ? new JSONObject() : datos;
    }

    public static RespuestaBanco parsear(int estadoHttp, String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) throw new JSONException("El servidor respondió sin contenido.");
        JSONObject objeto = new JSONObject(json);
        if (!objeto.has("estado") || !objeto.has("codigo") || !objeto.has("mensaje")) {
            throw new JSONException("La respuesta no contiene el sobre del contrato EURECABANK.");
        }
        return new RespuestaBanco(estadoHttp, objeto);
    }

    public int getEstadoHttp() { return estadoHttp; }
    public JSONObject getDatos() { return datos; }
    public String getEstado() { return datos.optString("estado", "ERROR"); }
    public String getCodigo() { return datos.optString("codigo", "ERR-001"); }
    public String getMensaje() { return datos.optString("mensaje", "Respuesta incompleta del servidor."); }
    public String getToken() { return datos.optString("token", ""); }
    public boolean esCorrecta() { return "OK".equalsIgnoreCase(getEstado()) && estadoHttp >= 200 && estadoHttp < 300; }
}
