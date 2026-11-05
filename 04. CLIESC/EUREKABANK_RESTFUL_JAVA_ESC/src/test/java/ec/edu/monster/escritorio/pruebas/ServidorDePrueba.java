package ec.edu.monster.escritorio.pruebas;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Servidor HTTP de pruebas: simula el servicio REST del banco sin Payara ni
 * base de datos.
 *
 * <p>Se registra una respuesta por cada ruta y método, y se guarda todo lo
 * que llega (ruta, cabeceras y cuerpo) para poder comprobar que el token
 * viaja en la cabecera {@code Authorization} y no en la URL.
 */
class ServidorDePrueba implements AutoCloseable {

    private final HttpServer servidor;
    private final Map<String, Respuesta> rutas = new ConcurrentHashMap<>();
    private final List<Peticion> peticiones = Collections.synchronizedList(new ArrayList<>());

    ServidorDePrueba() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
    }

    /**
     * @return la raíz del servidor, sin barra final
     */
    String url() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    /**
     * @param prefijo ruta que se quiere anteponer a la raíz
     * @return la dirección completa del prefijo
     */
    String url(String prefijo) {
        return url() + prefijo;
    }

    /**
     * @param metodo verbo HTTP
     * @param ruta   ruta completa, desde la barra
     * @param estado estado HTTP de la respuesta
     * @param cuerpo cuerpo de la respuesta, en JSON
     */
    void registrar(String metodo, String ruta, int estado, String cuerpo) {
        registrar(metodo, ruta, estado, "application/json; charset=utf-8", cuerpo);
    }

    /**
     * @param metodo verbo HTTP
     * @param ruta   ruta completa, desde la barra
     * @param estado estado HTTP de la respuesta
     * @param tipo   tipo de contenido de la respuesta
     * @param cuerpo cuerpo de la respuesta
     */
    void registrar(String metodo, String ruta, int estado, String tipo, String cuerpo) {
        rutas.put(clave(metodo, ruta), new Respuesta(estado, tipo, cuerpo, null));
    }

    /**
     * Registra una respuesta que se arma con lo que llegó en la petición.
     *
     * @param metodo    verbo HTTP
     * @param ruta      ruta completa, desde la barra
     * @param estado    estado HTTP de la respuesta
     * @param generador función que arma el cuerpo a partir de la petición
     */
    void registrarDinamico(String metodo, String ruta, int estado, Function<Peticion, String> generador) {
        rutas.put(clave(metodo, ruta), new Respuesta(estado, "application/json; charset=utf-8", null, generador));
    }

    /**
     * @return todas las peticiones recibidas, en orden
     */
    List<Peticion> peticiones() {
        return new ArrayList<>(peticiones);
    }

    /**
     * @return la última petición recibida
     */
    Peticion ultimaPeticion() {
        List<Peticion> todas = peticiones();
        if (todas.isEmpty()) {
            throw new IllegalStateException("El servidor no recibió ninguna petición");
        }
        return todas.get(todas.size() - 1);
    }

    /**
     * @param metodo verbo HTTP
     * @param ruta   ruta completa
     * @return la última petición recibida con ese verbo y esa ruta
     */
    Peticion ultimaPeticion(String metodo, String ruta) {
        List<Peticion> todas = peticiones();
        for (int i = todas.size() - 1; i >= 0; i--) {
            Peticion peticion = todas.get(i);
            if (peticion.metodo.equals(metodo) && peticion.ruta.equals(ruta)) {
                return peticion;
            }
        }
        throw new IllegalStateException("No llegó ninguna petición " + metodo + " " + ruta);
    }

    @Override
    public void close() {
        servidor.stop(0);
    }

    private static String clave(String metodo, String ruta) {
        return metodo + " " + ruta;
    }

    private void atender(HttpExchange intercambio) throws IOException {
        String metodo = intercambio.getRequestMethod();
        String ruta = intercambio.getRequestURI().getPath();
        String rutaCruda = intercambio.getRequestURI().getRawPath();
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Peticion peticion = new Peticion(metodo, ruta, rutaCruda, cuerpo,
                cabecera(intercambio, "Authorization"), cabecera(intercambio, "Content-Type"));
        peticiones.add(peticion);

        Respuesta respuesta = rutas.get(clave(metodo, ruta));
        if (respuesta == null) {
            // Página de error del contenedor: no es el sobre del banco.
            responder(intercambio, 404, "text/html; charset=utf-8", "<h1>404 - No existe</h1>");
            return;
        }
        String contenido = respuesta.generador == null ? respuesta.cuerpo : respuesta.generador.apply(peticion);
        responder(intercambio, respuesta.estado, respuesta.tipo, contenido);
    }

    private static String cabecera(HttpExchange intercambio, String nombre) {
        List<String> valores = intercambio.getRequestHeaders().get(nombre);
        return valores == null || valores.isEmpty() ? null : valores.get(0);
    }

    private static void responder(HttpExchange intercambio, int estado, String tipo, String cuerpo)
            throws IOException {
        byte[] bytes = cuerpo == null ? new byte[0] : cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        }
        intercambio.close();
    }

    /**
     * Arma el sobre de respuesta del banco: estado, código y mensaje.
     *
     * @param estado  {@code OK} o {@code ERROR}
     * @param codigo  código del catálogo congelado
     * @param mensaje mensaje literal del catálogo
     * @return el JSON del sobre
     */
    static String sobre(String estado, String codigo, String mensaje) {
        return "{\"estado\":\"" + estado + "\",\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\"}";
    }

    /**
     * Arma el sobre de respuesta con campos de payload.
     *
     * @param estado  {@code OK} o {@code ERROR}
     * @param codigo  código del catálogo congelado
     * @param mensaje mensaje literal del catálogo
     * @param extra   campos adicionales, ya en JSON, sin la llave final
     * @return el JSON del sobre
     */
    static String sobre(String estado, String codigo, String mensaje, String extra) {
        return "{\"estado\":\"" + estado + "\",\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\""
                + (extra.isEmpty() ? "" : "," + extra) + "}";
    }

    /**
     * Escapa un texto para meterlo dentro de un JSON.
     *
     * @param texto texto a escapar
     * @return el texto escapado
     */
    static String texto(String texto) {
        if (texto == null) {
            return "<nulo>";
        }
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Lee un campo del cuerpo JSON de la petición.
     *
     * @param peticion petición recibida
     * @param nombre   nombre del campo
     * @return el valor del campo, o {@code <nulo>} si no llegó
     */
    static String campo(Peticion peticion, String nombre) {
        try (JsonReader lector = Json.createReader(new StringReader(peticion.getCuerpo()))) {
            JsonObject objeto = lector.readObject();
            if (objeto == null) {
                return "<nulo>";
            }
            JsonValue valor = objeto.get(nombre);
            if (valor == null || valor.getValueType() == JsonValue.ValueType.NULL) {
                return "<nulo>";
            }
            if (valor.getValueType() == JsonValue.ValueType.STRING) {
                return ((JsonString) valor).getString();
            }
            return valor.toString();
        } catch (RuntimeException e) {
            return "<nulo>";
        }
    }

    /** Respuesta preparada para una ruta. */
    private static final class Respuesta {
        private final int estado;
        private final String tipo;
        private final String cuerpo;
        private final Function<Peticion, String> generador;

        private Respuesta(int estado, String tipo, String cuerpo, Function<Peticion, String> generador) {
            this.estado = estado;
            this.tipo = tipo;
            this.cuerpo = cuerpo;
            this.generador = generador;
        }
    }

    /** Petición recibida por el servidor. */
    static final class Peticion {
        private final String metodo;
        private final String ruta;
        private final String rutaCruda;
        private final String cuerpo;
        private final String autorizacion;
        private final String tipoContenido;

        private Peticion(String metodo, String ruta, String rutaCruda, String cuerpo,
                String autorizacion, String tipoContenido) {
            this.metodo = metodo;
            this.ruta = ruta;
            this.rutaCruda = rutaCruda;
            this.cuerpo = cuerpo;
            this.autorizacion = autorizacion;
            this.tipoContenido = tipoContenido;
        }

        String getMetodo() {
            return metodo;
        }

        String getRuta() {
            return ruta;
        }

        String getRutaCruda() {
            return rutaCruda;
        }

        String getCuerpo() {
            return cuerpo;
        }

        String getAutorizacion() {
            return autorizacion;
        }

        String getTipoContenido() {
            return tipoContenido;
        }
    }
}
