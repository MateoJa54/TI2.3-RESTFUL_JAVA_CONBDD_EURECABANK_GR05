package ec.edu.monster.pruebas;

import ec.edu.monster.controlador.ControladorBanco;
import ec.edu.monster.modelo.RespuestaBanco;
import ec.edu.monster.ws.TransporteEurekaBank;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Prueba las nueve operaciones MVC contra un transporte falso, sin alterar el banco. */
public class ControladorBancoTest {
    @Test public void loginEnviaHashSha256YGuardaToken() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=new ControladorBanco(f);
        RespuestaBanco r=c.autenticar("cromero","Cromero2026");
        assertTrue(r.esCorrecta()); assertTrue(c.haySesion());
        assertEquals("POST",f.ultima().metodo); assertEquals("/auth/login",f.ultima().ruta);
        assertEquals("891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4",f.ultima().cuerpo.getString("clave"));
        assertFalse(f.ultima().cuerpo.getString("clave").contains("Cromero2026"));
    }

    @Test public void logoutUsaBearerYObjetoVacio() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=autenticado(f);
        c.cerrarSesion();
        assertEquals("POST",f.ultima().metodo); assertEquals("/auth/logout",f.ultima().ruta);
        assertEquals("token-demo",f.ultima().token); assertEquals(0,f.ultima().cuerpo.length());
        assertFalse(c.haySesion());
    }

    @Test public void consultasProtegidasUsanRutasYToken() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=autenticado(f);
        c.listarCuentas("06914897"); assertUltima(f,"GET","/clientes/06914897/cuentas");
        c.consultarCuenta("00200002"); assertUltima(f,"GET","/cuentas/00200002");
        c.consultarMovimientos("00200002"); assertUltima(f,"GET","/cuentas/00200002/movimientos");
        c.consultarCostos("00200002"); assertUltima(f,"GET","/cuentas/00200002/costos");
    }

    @Test public void movimientosEnviaImporteDecimalConEscalaDos() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=autenticado(f);
        c.depositar("00200002","10,125");
        assertUltima(f,"POST","/cuentas/00200002/depositos");
        assertEquals(new BigDecimal("10.13"),f.ultima().cuerpo.opt("monto"));
        c.retirar("00200002","4.5");
        assertUltima(f,"POST","/cuentas/00200002/retiros");
        assertEquals(new BigDecimal("4.50"),f.ultima().cuerpo.opt("monto"));
    }

    @Test public void transferenciaEnviaOrigenDestinoImporteYToken() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=autenticado(f);
        c.transferir("00200002","00100002","25.50");
        assertUltima(f,"POST","/transferencias");
        assertEquals("00200002",f.ultima().cuerpo.getString("cuentaOrigen"));
        assertEquals("00100002",f.ultima().cuerpo.getString("cuentaDestino"));
        assertEquals(new BigDecimal("25.50"),f.ultima().cuerpo.opt("monto"));
    }

    @Test public void montoNoNumericoSeRechazaAntesDeEnviar() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=autenticado(f); int anteriores=f.peticiones.size();
        try { c.depositar("00200002","diez"); fail("Debió rechazar el monto"); }
        catch(IllegalArgumentException esperado){assertTrue(esperado.getMessage().contains("numérico"));}
        assertEquals(anteriores,f.peticiones.size());
    }

    @Test public void sesionExpiradaInvalidaTokenLocal() throws Exception {
        TransporteFalso f=new TransporteFalso(); ControladorBanco c=autenticado(f);
        f.respuesta="{\"estado\":\"ERROR\",\"codigo\":\"AUTH-004\",\"mensaje\":\"Sesión expirada\"}"; f.status=401;
        c.consultarCuenta("00200002"); assertFalse(c.haySesion());
        try { c.consultarCuenta("00200002"); fail("Debió requerir sesión"); }
        catch(IllegalStateException esperado){assertTrue(esperado.getMessage().contains("sesión"));}
    }

    private static ControladorBanco autenticado(TransporteFalso f) throws Exception {
        ControladorBanco c=new ControladorBanco(f); c.autenticar("cromero","Cromero2026"); f.peticiones.clear(); return c;
    }
    private static void assertUltima(TransporteFalso f,String metodo,String ruta) {
        assertEquals(metodo,f.ultima().metodo); assertEquals(ruta,f.ultima().ruta); assertEquals("token-demo",f.ultima().token);
    }

    private static final class TransporteFalso implements TransporteEurekaBank {
        final List<P> peticiones=new ArrayList<>(); int status=200;
        String respuesta="{\"estado\":\"OK\",\"codigo\":\"200\",\"mensaje\":\"Correcto\"}";
        @Override public RespuestaBanco enviar(String metodo,String ruta,String token,JSONObject cuerpo)throws IOException {
            peticiones.add(new P(metodo,ruta,token,cuerpo==null?new JSONObject():cuerpo));
            String json="/auth/login".equals(ruta)
                    ?"{\"estado\":\"OK\",\"codigo\":\"200\",\"mensaje\":\"Autenticado\",\"token\":\"token-demo\"}"
                    :respuesta;
            try{return RespuestaBanco.parsear(status,json);}
            catch(JSONException e){throw new IOException(e);}
        }
        P ultima(){return peticiones.get(peticiones.size()-1);}
    }
    private static final class P {
        final String metodo,ruta,token; final JSONObject cuerpo;
        P(String metodo,String ruta,String token,JSONObject cuerpo){this.metodo=metodo;this.ruta=ruta;this.token=token;this.cuerpo=cuerpo;}
    }
}
