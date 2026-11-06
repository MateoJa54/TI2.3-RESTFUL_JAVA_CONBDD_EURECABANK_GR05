package ec.edu.monster.ws;

import ec.edu.monster.modelo.RespuestaBanco;
import org.json.JSONObject;
import java.io.IOException;

/** Puerto de transporte que permite probar el controlador sin red ni servidor. */
public interface TransporteEurekaBank {
    RespuestaBanco enviar(String metodo, String ruta, String token, JSONObject cuerpo) throws IOException;
}
