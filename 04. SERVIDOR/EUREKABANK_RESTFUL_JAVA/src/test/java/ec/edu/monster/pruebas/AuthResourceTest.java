package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ec.edu.monster.api.rest.AuthResource;
import ec.edu.monster.api.rest.ServiciosEurekaBank;
import ec.edu.monster.modelos.AutenticacionRequest;
import ec.edu.monster.modelos.RespuestaAutenticacion;
import ec.edu.monster.modelos.RespuestaBase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Operaciones 1 y 2 (contrato, seccion 6.2 y 6.4). */
class AuthResourceTest {

    private ServiciosEurekaBank servicios;
    private AuthResource recurso;

    @BeforeEach
    void preparar() {
        servicios = SimuladosRest.servicios();
        recurso = new AuthResource(servicios);
    }

    @Test
    void loginSinCuerpoDevuelveDat001() {
        Response respuesta = recurso.autenticar(null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void loginSinUsuarioDevuelveDat001(String usuario) {
        Response respuesta = recurso.autenticar(new AutenticacionRequest(usuario, SimuladosRest.CLAVE));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void loginSinClaveDevuelveDat001(String clave) {
        Response respuesta = recurso.autenticar(new AutenticacionRequest(SimuladosRest.USUARIO, clave));

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void loginCorrectoDevuelve200ConToken() {
        Response respuesta = recurso.autenticar(new AutenticacionRequest(SimuladosRest.USUARIO, SimuladosRest.CLAVE));

        assertEquals(200, respuesta.getStatus());
        RespuestaAutenticacion cuerpo = assertInstanceOf(RespuestaAutenticacion.class, respuesta.getEntity());
        assertEquals("200", cuerpo.getCodigo());
        assertEquals("Autenticación exitosa", cuerpo.getMensaje());
        assertFalse(cuerpo.getToken().isBlank());
    }

    @Test
    void claveIncorrectaDevuelve401ConAuth001() {
        Response respuesta = recurso.autenticar(new AutenticacionRequest(SimuladosRest.USUARIO, "mala"));

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void usuarioInactivoDevuelve403ConAuth002() {
        Response respuesta = recurso.autenticar(new AutenticacionRequest(SimuladosRest.INACTIVO, SimuladosRest.CLAVE));

        assertEquals(403, respuesta.getStatus());
        assertEquals("AUTH-002", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void logoutSinCabeceraDevuelveDat001() {
        Response respuesta = recurso.cerrarSesion(null);

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void logoutConCabeceraMalFormadaDevuelveDat001() {
        Response respuesta = recurso.cerrarSesion("Basic abc");

        assertEquals(400, respuesta.getStatus());
        assertEquals("DAT-001", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void logoutConTokenInexistenteDevuelve401ConAuth003() {
        Response respuesta = recurso.cerrarSesion("Bearer no-existe");

        assertEquals(401, respuesta.getStatus());
        assertEquals("AUTH-003", ((RespuestaBase) respuesta.getEntity()).getCodigo());
    }

    @Test
    void logoutConTokenValidoCierraLaSesion() {
        RespuestaAutenticacion login = (RespuestaAutenticacion) recurso
                .autenticar(new AutenticacionRequest(SimuladosRest.USUARIO, SimuladosRest.CLAVE)).getEntity();

        Response cierre = recurso.cerrarSesion("Bearer " + login.getToken());
        Response segundoCierre = recurso.cerrarSesion("Bearer " + login.getToken());

        assertEquals(200, cierre.getStatus());
        assertEquals("Sesión cerrada correctamente", ((RespuestaBase) cierre.getEntity()).getMensaje());
        assertEquals(401, segundoCierre.getStatus());
        assertEquals("AUTH-003", ((RespuestaBase) segundoCierre.getEntity()).getCodigo());
    }
}
