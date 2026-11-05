package ec.edu.monster.escritorio.cliente;

import ec.edu.monster.escritorio.modelo.RespuestaAutenticacion;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.modelo.RespuestaCostos;
import ec.edu.monster.escritorio.modelo.RespuestaCuenta;
import ec.edu.monster.escritorio.modelo.RespuestaCuentas;
import ec.edu.monster.escritorio.modelo.RespuestaMovimiento;
import ec.edu.monster.escritorio.modelo.RespuestaMovimientos;
import ec.edu.monster.escritorio.modelo.RespuestaTransferencia;
import ec.edu.monster.escritorio.util.ExcepcionServicio;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador REST: implementa {@link IClienteEUREKABANK} llamando al servicio
 * web con {@link HttpClient} del JDK. Es la única clase del cliente que sabe
 * que existe HTTP y JSON.
 *
 * <p>No descarga nada del servidor: el contrato OpenAPI va empaquetado en el
 * proyecto ({@code src/main/resources/contrato}) y solo se cambia la
 * dirección a la que se envían las peticiones. Así el cliente arranca aunque el
 * servidor esté apagado y el error aparece, de forma controlada, en la
 * primera llamada.
 *
 * <p>El token de sesión viaja en la cabecera {@code Authorization: Bearer
 * <token>}; nunca va en la URL, porque quedaría registrado en el log del
 * servidor.
 *
 * <p>Se leen las dos señales de la respuesta: el estado HTTP, para detectar
 * un fallo de transporte o de servicio, y el {@code codigo} del sobre, que
 * lleva el mensaje de negocio del catálogo congelado.
 */
public class ClienteEUREKABANK implements IClienteEUREKABANK {

    /** Dirección del servicio publicado en el servidor local. */
    public static final String URL_POR_DEFECTO =
            "http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api";

    /** Estado HTTP con el que el servidor declara el servicio no disponible. */
    private static final int HTTP_SERVICIO_NO_DISPONIBLE = 503;

    // Sin estos límites, un servidor que no contesta dejaría la ventana colgada.
    private static final int ESPERA_CONEXION_MS = 5_000;
    private static final int ESPERA_RESPUESTA_MS = 30_000;

    private static final String TIPO_JSON = "application/json; charset=utf-8";
    private static final String CABECERA_TOKEN = "Authorization";

    private final String url;
    private final HttpClient http;
    private final Jsonb jsonb;
    private final int esperaRespuestaMs;

    /**
     * @param url dirección base de la API, sin barra final
     */
    public ClienteEUREKABANK(String url) {
        this(url, ESPERA_CONEXION_MS, ESPERA_RESPUESTA_MS);
    }

    /**
     * @param url               dirección base de la API, sin barra final
     * @param esperaConexionMs  tiempo máximo para conectar, en milisegundos
     * @param esperaRespuestaMs tiempo máximo para recibir la respuesta, en milisegundos
     */
    public ClienteEUREKABANK(String url, int esperaConexionMs, int esperaRespuestaMs) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La dirección del servicio es obligatoria");
        }
        String direccion = url.trim();
        exigirDireccionHttp(direccion);
        this.url = quitarBarraFinal(direccion);
        this.esperaRespuestaMs = esperaRespuestaMs;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(esperaConexionMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.jsonb = JsonbBuilder.create();
    }

    /** Rechaza al arrancar una dirección mal escrita, en lugar de fallar en la primera llamada. */
    private static void exigirDireccionHttp(String direccion) {
        try {
            URI uri = new URI(direccion);
            boolean esHttp = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!esHttp || uri.getHost() == null) {
                throw new IllegalArgumentException("La dirección del servicio no es válida: " + direccion);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("La dirección del servicio no es válida: " + direccion, e);
        }
    }

    private static String quitarBarraFinal(String direccion) {
        return direccion.endsWith("/") ? direccion.substring(0, direccion.length() - 1) : direccion;
    }

    /**
     * @return la dirección base de la API a la que se envían las peticiones
     */
    public String getUrl() {
        return url;
    }

    @Override
    public RespuestaAutenticacion autenticar(String usuario, String clave) {
        Map<String, Object> cuerpo = cuerpo("usuario", usuario, "clave", clave);
        return enviar("POST", "/auth/login", null, cuerpo, RespuestaAutenticacion.class);
    }

    @Override
    public RespuestaBase cerrarSesion(String token) {
        // El cierre de sesión también viaja protegido: el token va en la cabecera.
        return enviar("POST", "/auth/logout", token, cuerpo(), RespuestaBase.class);
    }

    @Override
    public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
        return enviar("GET", "/clientes/" + segmento(cedula) + "/cuentas", token, null,
                RespuestaCuentas.class);
    }

    @Override
    public RespuestaCuenta consultarCuenta(String cuenta, String token) {
        return enviar("GET", "/cuentas/" + segmento(cuenta), token, null, RespuestaCuenta.class);
    }

    @Override
    public RespuestaMovimientos consultarMovimientos(String cuenta, String token) {
        return enviar("GET", "/cuentas/" + segmento(cuenta) + "/movimientos", token, null,
                RespuestaMovimientos.class);
    }

    @Override
    public RespuestaMovimiento realizarDeposito(String cuenta, BigDecimal monto, String token) {
        return enviar("POST", "/cuentas/" + segmento(cuenta) + "/depositos", token,
                cuerpo("monto", monto), RespuestaMovimiento.class);
    }

    @Override
    public RespuestaMovimiento realizarRetiro(String cuenta, BigDecimal monto, String token) {
        return enviar("POST", "/cuentas/" + segmento(cuenta) + "/retiros", token,
                cuerpo("monto", monto), RespuestaMovimiento.class);
    }

    @Override
    public RespuestaTransferencia realizarTransferencia(String cuentaOrigen, String cuentaDestino,
            BigDecimal monto, String token) {
        Map<String, Object> cuerpo = cuerpo("cuentaOrigen", cuentaOrigen,
                "cuentaDestino", cuentaDestino, "monto", monto);
        return enviar("POST", "/transferencias", token, cuerpo, RespuestaTransferencia.class);
    }

    @Override
    public RespuestaCostos consultarCostosMantenimiento(String cuenta, String token) {
        return enviar("GET", "/cuentas/" + segmento(cuenta) + "/costos", token, null,
                RespuestaCostos.class);
    }

    /**
     * Ejecuta la llamada HTTP y traduce cualquier fallo de comunicación o de
     * formato a una sola excepción del cliente, para que el resto del programa
     * no dependa de las excepciones de {@link HttpClient}.
     */
    private <R extends RespuestaBase> R enviar(String metodo, String ruta, String token,
            Map<String, Object> cuerpo, Class<R> tipo) {
        HttpRequest peticion = construirPeticion(metodo, ruta, token, cuerpo);
        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExcepcionServicio("No se pudo comunicar con el servidor en " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExcepcionServicio("La llamada al servidor en " + url + " fue interrumpida", e);
        }
        return interpretar(respuesta, tipo);
    }

    private HttpRequest construirPeticion(String metodo, String ruta, String token,
            Map<String, Object> cuerpo) {
        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create(url + ruta))
                .timeout(Duration.ofMillis(esperaRespuestaMs))
                .header("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            peticion.header(CABECERA_TOKEN, "Bearer " + token);
        }
        if (cuerpo == null) {
            peticion.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type", TIPO_JSON);
            peticion.method(metodo, HttpRequest.BodyPublishers.ofString(
                    jsonb.toJson(cuerpo), StandardCharsets.UTF_8));
        }
        return peticion.build();
    }

    /**
     * Interpreta la respuesta: valida el sobre y devuelve el POJO del modelo.
     * Un {@code 503} es un fallo de servicio y se lanza; cualquier otro estado
     * se devuelve tal cual, porque el código de negocio va dentro del sobre.
     */
    private <R extends RespuestaBase> R interpretar(HttpResponse<String> respuestaHttp, Class<R> tipo) {
        String cuerpo = respuestaHttp.body();
        if (respuestaHttp.statusCode() == HTTP_SERVICIO_NO_DISPONIBLE) {
            throw new ExcepcionServicio("El servidor en " + url
                    + " respondió 503: " + mensajeSiSePuede(cuerpo), null);
        }
        JsonObject sobre = leerSobre(cuerpo);
        if (sobre.isNull("estado") || sobre.isNull("codigo")) {
            throw new ExcepcionServicio(
                    "El servidor en " + url + " contestó algo que no es una respuesta del banco", null);
        }
        R envelope;
        try {
            envelope = jsonb.fromJson(cuerpo, tipo);
        } catch (RuntimeException e) {
            throw new ExcepcionServicio(
                    "El servidor en " + url + " contestó algo que no es una respuesta del banco", e);
        }
        if (envelope == null || envelope.getEstado() == null || envelope.getCodigo() == null) {
            throw new ExcepcionServicio(
                    "El servidor en " + url + " contestó algo que no es una respuesta del banco", null);
        }
        return envelope;
    }

    /** Lee el sobre y comprueba que trae las dos claves obligatorias del contrato. */
    private JsonObject leerSobre(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ExcepcionServicio(
                    "El servidor en " + url + " contestó algo que no es una respuesta del banco", null);
        }
        try (JsonReader lector = Json.createReader(new StringReader(cuerpo))) {
            JsonObject sobre = lector.readObject();
            if (sobre == null || !sobre.containsKey("estado") || !sobre.containsKey("codigo")) {
                throw new ExcepcionServicio(
                        "El servidor en " + url + " contestó algo que no es una respuesta del banco", null);
            }
            return sobre;
        } catch (ExcepcionServicio e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ExcepcionServicio(
                    "El servidor en " + url + " contestó algo que no es una respuesta del banco", e);
        }
    }

    /** Extrae el mensaje del sobre si el cuerpo es un JSON válido; si no, devuelve el cuerpo crudo. */
    private String mensajeSiSePuede(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "sin detalle";
        }
        try (JsonReader lector = Json.createReader(new StringReader(cuerpo))) {
            JsonObject sobre = lector.readObject();
            if (sobre != null && sobre.containsKey("mensaje") && !sobre.isNull("mensaje")) {
                return sobre.getString("mensaje");
            }
        } catch (RuntimeException e) {
            return cuerpo;
        }
        return cuerpo;
    }

    /** Arma el cuerpo JSON de la petición, admitiendo valores nulos. */
    private static Map<String, Object> cuerpo(Object... pares) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            mapa.put((String) pares[i], pares[i + 1]);
        }
        return mapa;
    }

    /** Codifica un valor que va en la ruta de la URL. */
    private static String segmento(String valor) {
        if (valor == null) {
            return "";
        }
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
