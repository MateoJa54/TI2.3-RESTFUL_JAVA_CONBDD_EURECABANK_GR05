package ec.edu.monster.api.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/** Portada del servicio: sin ella la raiz y /api responderian 404 aunque el WAR este desplegado. */
@Path("/")
public class InicioResource {

    static final String[][] RUTAS = {
        {"POST", "/api/auth/login", "Inicia sesion y devuelve el token"},
        {"POST", "/api/auth/logout", "Cierra la sesion"},
        {"GET", "/api/clientes/{cedula}/cuentas", "Cuentas de un cliente"},
        {"GET", "/api/cuentas/{cuenta}", "Datos de una cuenta"},
        {"GET", "/api/cuentas/{cuenta}/movimientos", "Movimientos de una cuenta"},
        {"POST", "/api/cuentas/{cuenta}/depositos", "Deposito"},
        {"POST", "/api/cuentas/{cuenta}/retiros", "Retiro"},
        {"POST", "/api/transferencias", "Transferencia entre cuentas"},
        {"GET", "/api/cuentas/{cuenta}/costos", "Costos de mantenimiento"},
    };

    @GET
    @Produces("text/html; charset=utf-8")
    public Response portada() {
        StringBuilder filas = new StringBuilder();
        for (String[] r : RUTAS) {
            filas.append("<tr><td><b>").append(r[0]).append("</b></td><td><code>")
                    .append(r[1].replace("{", "&#123;").replace("}", "&#125;")).append("</code></td><td>")
                    .append(r[2]).append("</td></tr>");
        }
        String html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>EurekaBank REST</title><style>"
                + "body{font-family:\"Segoe UI\",system-ui,sans-serif;margin:0;background:#f4f7f9;color:#12202b}"
                + "main{max-width:760px;margin:48px auto;padding:0 16px}"
                + "h1{margin:0 0 8px;font-size:24px}p{color:#55636f}"
                + "table{width:100%;border-collapse:collapse;background:#fff;border:1px solid #dde5ea;border-radius:12px}"
                + "td{padding:10px 12px;border-bottom:1px solid #eef3f6}code{font-size:13px}"
                + "a{color:#0b7a8a}</style></head><body><main>"
                + "<h1>EurekaBank REST</h1>"
                + "<p>El servicio esta en marcha. Las rutas protegidas exigen la cabecera "
                + "<code>Authorization: Bearer &lt;token&gt;</code>.</p>"
                + "<table>" + filas + "</table>"
                + "<p>Cliente web: <a href=\"../EUREKABANK_RESTFUL_JAVA_WEB/\">EUREKABANK_RESTFUL_JAVA_WEB</a>. "
                + "Indice en JSON: <a href=\"api\">/api</a>.</p></main></body></html>";
        return Response.ok(html).type("text/html; charset=utf-8").build();
    }

    @GET
    @Path("api")
    @Produces("application/json; charset=utf-8")
    public Response indice() {
        StringBuilder json = new StringBuilder("{\"servicio\":\"EurekaBank REST\",\"version\":\"1.0\",\"rutas\":[");
        for (int i = 0; i < RUTAS.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"metodo\":\"").append(RUTAS[i][0]).append("\",\"ruta\":\"").append(RUTAS[i][1])
                    .append("\",\"descripcion\":\"").append(RUTAS[i][2]).append("\"}");
        }
        return Response.ok(json.append("]}").toString()).type("application/json; charset=utf-8").build();
    }
}
