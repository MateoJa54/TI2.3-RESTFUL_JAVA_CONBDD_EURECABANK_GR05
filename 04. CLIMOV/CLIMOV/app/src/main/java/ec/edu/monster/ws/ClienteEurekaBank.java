package ec.edu.monster.ws;

import ec.edu.monster.modelo.RespuestaBanco;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Adaptador HTTP/JSON; concentra las rutas REST y la cabecera Bearer. */
public final class ClienteEurekaBank implements TransporteEurekaBank {
    public static final String URL_EMULADOR = "http://10.0.2.2:8080/EUREKABANK_RESTFUL_JAVA/api";
    private static final int ESPERA_CONEXION_MS = 7000;
    private static final int ESPERA_RESPUESTA_MS = 20000;
    private final String baseUrl;

    public ClienteEurekaBank(String baseUrl) {
        this.baseUrl = normalizarBase(baseUrl);
    }

    public String getBaseUrl() { return baseUrl; }

    /** Valida y normaliza la dirección base configurable del servidor. */
    public static String normalizarBase(String base) {
        if (base == null || base.trim().isEmpty()) throw new IllegalArgumentException("Ingresa la URL del servidor.");
        String valor = base.trim();
        try {
            URL url = new URL(valor);
            if (!("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()))
                    || url.getHost().isEmpty()) throw new IllegalArgumentException("La URL debe comenzar con http:// o https://.");
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            throw new IllegalArgumentException("La URL del servidor no es válida.", e);
        }
        while (valor.endsWith("/")) valor = valor.substring(0, valor.length() - 1);
        return valor;
    }

    /** Método puro para verificar en pruebas el armado de direcciones. */
    public static String construirUrl(String base, String ruta) {
        return normalizarBase(base) + (ruta.startsWith("/") ? ruta : "/" + ruta);
    }

    public static String codificarSegmento(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override public RespuestaBanco enviar(String metodo, String ruta, String token, JSONObject cuerpo) throws IOException {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(construirUrl(baseUrl, ruta)).openConnection();
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(ESPERA_CONEXION_MS);
            conexion.setReadTimeout(ESPERA_RESPUESTA_MS);
            conexion.setInstanceFollowRedirects(false);
            conexion.setRequestProperty("Accept", "application/json");
            if (token != null && !token.trim().isEmpty()) {
                conexion.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (cuerpo != null) {
                byte[] bytes = cuerpo.toString().getBytes(StandardCharsets.UTF_8);
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conexion.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream salida = conexion.getOutputStream()) { salida.write(bytes); }
            }
            int status = conexion.getResponseCode();
            InputStream flujo = status >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            String contenido = leer(flujo);
            try {
                return RespuestaBanco.parsear(status, contenido);
            } catch (JSONException e) {
                throw new IOException("El servidor respondió HTTP " + status + " con un formato inesperado.", e);
            }
        } finally {
            if (conexion != null) conexion.disconnect();
        }
    }

    private static String leer(InputStream flujo) throws IOException {
        if (flujo == null) return "";
        try (InputStream entrada = flujo; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = entrada.read(buffer)) != -1) salida.write(buffer, 0, leidos);
            return new String(salida.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static JSONObject json(Object... pares) {
        JSONObject objeto = new JSONObject();
        try {
            for (int i = 0; i + 1 < pares.length; i += 2) objeto.put(String.valueOf(pares[i]), pares[i + 1]);
            return objeto;
        } catch (JSONException e) {
            throw new IllegalArgumentException("No se pudo preparar la petición JSON.", e);
        }
    }
}
