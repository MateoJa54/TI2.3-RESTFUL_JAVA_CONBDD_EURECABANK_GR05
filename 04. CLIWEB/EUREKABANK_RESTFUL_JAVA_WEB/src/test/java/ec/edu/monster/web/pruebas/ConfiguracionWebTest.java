package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.cliente.ClienteEUREKABANK;
import ec.edu.monster.web.cliente.IClienteEUREKABANK;
import ec.edu.monster.web.servlet.LoginServlet;
import ec.edu.monster.web.util.ConfiguracionWeb;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** La URL del servicio sale de cuatro fuentes, en orden de prioridad. */
class ConfiguracionWebTest {

    private static final UnaryOperator<String> NADA = nombre -> null;

    private static UnaryOperator<String> con(String nombre, String valor) {
        return Map.of(nombre, valor)::get;
    }

    @Test
    void sinNingunaFuenteUsaElValorPorDefecto() {
        ServletFalso f = ServletFalso.crear();
        assertEquals("http://localhost:8080/EUREKABANK_RESTFUL_JAVA/api",
                ConfiguracionWeb.resolverUrl(f.contexto(), NADA, NADA));
        assertEquals(ConfiguracionWeb.URL_POR_DEFECTO, ConfiguracionWeb.resolverUrl(f.contexto(), NADA, NADA));
    }

    @Test
    void elParametroDeContextoManda() {
        ServletFalso f = ServletFalso.crear().parametroContexto("servicio.url", "http://ctx:1/api");
        assertEquals("http://ctx:1/api", ConfiguracionWeb.resolverUrl(f.contexto(),
                con("servicio.url", "http://prop:2/api"), con("SERVICIO_URL", "http://env:3/api")));
    }

    @Test
    void sinParametroManda_laPropiedadDeLaJvm() {
        ServletFalso f = ServletFalso.crear();
        assertEquals("http://prop:2/api", ConfiguracionWeb.resolverUrl(f.contexto(),
                con("servicio.url", "http://prop:2/api"), con("SERVICIO_URL", "http://env:3/api")));
    }

    @Test
    void sinParametroNiPropiedadManda_laVariableDeEntorno() {
        ServletFalso f = ServletFalso.crear();
        assertEquals("http://env:3/api",
                ConfiguracionWeb.resolverUrl(f.contexto(), NADA, con("SERVICIO_URL", "http://env:3/api")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void losValoresVaciosSeIgnoran(String vacio) {
        ServletFalso f = ServletFalso.crear();
        if (vacio != null) {
            f.parametroContexto("servicio.url", vacio);
        }
        assertEquals("http://prop:2/api", ConfiguracionWeb.resolverUrl(f.contexto(),
                con("servicio.url", "http://prop:2/api"), NADA));
    }

    @Test
    void laPropiedadVaciaCaeALaVariable() {
        ServletFalso f = ServletFalso.crear();
        UnaryOperator<String> propiedades = con("servicio.url", "  ");
        assertEquals("http://env:3/api",
                ConfiguracionWeb.resolverUrl(f.contexto(), propiedades, con("SERVICIO_URL", "http://env:3/api")));
    }

    @Test
    void sinContextoSigueFuncionando() {
        assertEquals("http://prop:2/api",
                ConfiguracionWeb.resolverUrl(null, con("servicio.url", "http://prop:2/api"), NADA));
        assertEquals(ConfiguracionWeb.URL_POR_DEFECTO, ConfiguracionWeb.resolverUrl(null, NADA, NADA));
    }

    @Test
    void sequitanLosEspaciosDeLosExtremos() {
        ServletFalso f = ServletFalso.crear().parametroContexto("servicio.url", "  http://ctx:1/api \n");
        assertEquals("http://ctx:1/api", ConfiguracionWeb.resolverUrl(f.contexto(), NADA, NADA));
    }

    @Test
    void laVersionDeUnSoloArgumentoUsaLaJvmReal() {
        ServletFalso f = ServletFalso.crear().parametroContexto("servicio.url", "http://ctx:1/api");
        assertEquals("http://ctx:1/api", ConfiguracionWeb.resolverUrl(f.contexto()));
    }

    @Test
    void losNombresDeLasFuentesSonLosDelContrato() {
        assertEquals("servicio.url", ConfiguracionWeb.PARAMETRO);
        assertEquals("SERVICIO_URL", ConfiguracionWeb.VARIABLE_ENTORNO);
    }

    // ------------------------------------------------------------------ cliente compartido

    @Test
    void elClienteSeCreaUnaVezYSeComparte() {
        ServletFalso f = ServletFalso.crear().parametroContexto("servicio.url", "http://ctx:1/api");
        IClienteEUREKABANK primero = LoginServlet.clienteCompartido(f.contexto());
        IClienteEUREKABANK segundo = LoginServlet.clienteCompartido(f.contexto());
        assertSame(primero, segundo);
        assertTrue(primero instanceof ClienteEUREKABANK);
        assertEquals("http://ctx:1/api", ((ClienteEUREKABANK) primero).getUrl());
    }

    @Test
    void siElContextoYaTieneClienteSeRespeta() {
        ClienteFalso falso = new ClienteFalso();
        ServletFalso f = ServletFalso.crear().cliente(falso);
        assertSame(falso, LoginServlet.clienteCompartido(f.contexto()));
        assertNotNull(f.contexto().getAttribute(ConfiguracionWeb.ATRIBUTO_CLIENTE));
    }

    @Test
    void contextosDistintosTienenClientesDistintos() {
        IClienteEUREKABANK a = LoginServlet.clienteCompartido(ServletFalso.crear().contexto());
        IClienteEUREKABANK b = LoginServlet.clienteCompartido(ServletFalso.crear().contexto());
        assertTrue(a != b);
    }
}
