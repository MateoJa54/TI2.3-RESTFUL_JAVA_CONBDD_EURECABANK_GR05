package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.RespuestaAutenticacion;
import ec.edu.monster.modelos.RespuestaBase;
import ec.edu.monster.seguridad.ServicioAutenticacion;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.utilidades.HashClave;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas del servicio de autenticacion (orden de validacion 6.2, reglas RB-13 y
 * RB-14).
 */
class ServicioAutenticacionTest {

    private static final Instant INICIO = Instant.parse("2026-11-03T10:00:00Z");
    private static final String HASH_CROMERO =
            "891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4";
    private static final String HASH_SARMIENTO =
            "4503389bee2ee5ed2daf0125d825d7e364215989cce0f15804e67ac0fbce9adc";

    private Dobles.UsuarioDAOFalso usuarioDAO;
    private Dobles.LogSessionDAOFalso logSessionDAO;
    private TokenServicio tokenServicio;
    private ServicioAutenticacion servicio;

    @BeforeEach
    void preparar() {
        usuarioDAO = new Dobles.UsuarioDAOFalso()
                .conUsuario("cromero", "0001", HASH_CROMERO, "ACTIVO",
                        "Carlos Alberto Romero Castillo", "Procesos")
                .conUsuario("csarmiento", "0008", HASH_SARMIENTO, "ANULADO",
                        "Carlos Sarmiento Ruiz", "Consultas")
                .conUsuario("creyes", "0003", "32b3491336522e073489725b5daf298cd749007a",
                        "ANULADO", "Carlos Reyes Ruiz", "Util");
        logSessionDAO = new Dobles.LogSessionDAOFalso();
        tokenServicio = new TokenServicio(Duration.ofMinutes(30),
                Clock.fixed(INICIO, ZoneOffset.UTC));
        servicio = new ServicioAutenticacion(usuarioDAO, logSessionDAO, tokenServicio);
    }

@Test
    void unaCredencialValidaDevuelveElTokenYLosDatosDeLaSesion() {
        RespuestaAutenticacion respuesta = servicio.autenticar("cromero", "Cromero2026");

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Autenticación exitosa", respuesta.getMensaje());
        assertNotNull(respuesta.getToken());
        assertEquals("0001", respuesta.getEmpleado());
        assertEquals("Carlos Alberto Romero Castillo", respuesta.getNombreEmpleado());
        assertEquals("Procesos", respuesta.getRol());
    }

    @Test
    void laCredencialTambienSeAceptaEnviandoElHash() {
        // Decision D7 del contrato congelado: el cliente envia el SHA-256 de la clave.
        RespuestaAutenticacion respuesta = servicio.autenticar("cromero", HASH_CROMERO);

        assertEquals("OK", respuesta.getEstado());
        assertNotNull(respuesta.getToken());
    }

    @Test
    void laExpiracionTravelEnIso8601() {
        RespuestaAutenticacion respuesta = servicio.autenticar("cromero", "Cromero2026");

        assertTrue(respuesta.getExpiracion().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"),
                respuesta.getExpiracion());
        assertEquals("2026-11-03T10:30:00", respuesta.getExpiracion());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unUsuarioVacioDevuelveDat001(String usuario) {
        RespuestaAutenticacion respuesta = servicio.autenticar(usuario, "Cromero2026");

        assertEquals("ERROR", respuesta.getEstado());
        assertEquals("DAT-001", respuesta.getCodigo());
        assertNull(respuesta.getToken());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unaClaveVaciaDevuelveDat001(String clave) {
        RespuestaAutenticacion respuesta = servicio.autenticar("cromero", clave);

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void unUsuarioInexistenteDevuelveAuth001() {
        RespuestaAutenticacion respuesta = servicio.autenticar("nadie", "Cromero2026");

        assertEquals("AUTH-001", respuesta.getCodigo());
        assertEquals("El usuario o la contraseña son incorrectos", respuesta.getMensaje());
    }

    @Test
    void unaClaveIncorrectaDevuelveAuth001() {
        RespuestaAutenticacion respuesta = servicio.autenticar("cromero", "Cromero2027");

        assertEquals("AUTH-001", respuesta.getCodigo());
    }

    @Test
    void elErrorNoRevelaSiFalloElUsuarioOLaClave() {
        String porUsuario = servicio.autenticar("nadie", "Cromero2026").getMensaje();
        String porClave = servicio.autenticar("cromero", "Cromero2027").getMensaje();

        assertEquals(porUsuario, porClave);
    }

    @Test
    void unUsuarioInactivoDevuelveAuth002() {
        RespuestaAutenticacion respuesta = servicio.autenticar("csarmiento", "Sarmiento2026");

        assertEquals("AUTH-002", respuesta.getCodigo());
        assertEquals("El usuario se encuentra inactivo", respuesta.getMensaje());
    }

    @Test
    void unHashHeredadoDeSha1NoAutentica() {
        RespuestaAutenticacion respuesta = servicio.autenticar("creyes", "Cromero2026");

        assertEquals("AUTH-001", respuesta.getCodigo());
    }

    @Test
    void laAutenticacionRegistraLaSesionEnLaAuditoria() {
        servicio.autenticar("cromero", "Cromero2026");

        assertEquals(1, logSessionDAO.getSesiones().size());
        assertEquals("0001", logSessionDAO.getSesiones().values().iterator().next());
    }

    @Test
    void elTokenQuedaAsociadoALaFilaDeAuditoria() {
        RespuestaAutenticacion respuesta = servicio.autenticar("cromero", "Cromero2026");

        TokenServicio.Sesion sesion = tokenServicio.obtenerSesion(respuesta.getToken()).orElseThrow();
        assertNotNull(sesion.getIdSesionLog());
    }

    @Test
    void elNombreDeUsuarioSeBuscaSinEspaciosAlRededor() {
        RespuestaAutenticacion respuesta = servicio.autenticar("  cromero  ", "Cromero2026");

        assertEquals("OK", respuesta.getEstado());
    }

    @Test
    void cerrarUnaSesionValidaDevuelveElMensajeDeExito() {
        RespuestaAutenticacion autenticacion = servicio.autenticar("cromero", "Cromero2026");

        RespuestaBase respuesta = servicio.cerrarSesion(autenticacion.getToken());

        assertEquals("OK", respuesta.getEstado());
        assertEquals("200", respuesta.getCodigo());
        assertEquals("Sesión cerrada correctamente", respuesta.getMensaje());
    }

    @Test
    void cerrarUnaSesionMarcaLaSalidaEnLaAuditoria() {
        RespuestaAutenticacion autenticacion = servicio.autenticar("cromero", "Cromero2026");

        servicio.cerrarSesion(autenticacion.getToken());

        assertEquals(1, logSessionDAO.getCerradas().size());
    }

    @Test
    void cerrarUnaSesionDescartaElToken() {
        RespuestaAutenticacion autenticacion = servicio.autenticar("cromero", "Cromero2026");

        servicio.cerrarSesion(autenticacion.getToken());

        assertEquals(CodigoRespuesta.AUTH_003,
                tokenServicio.validarToken(autenticacion.getToken()));
    }

    @Test
    void cerrarSinTokenDevuelveDat001() {
        RespuestaBase respuesta = servicio.cerrarSesion("");

        assertEquals("DAT-001", respuesta.getCodigo());
    }

    @Test
    void cerrarConTokenDesconocidoDevuelveAuth003() {
        RespuestaBase respuesta = servicio.cerrarSesion("no-existe");

        assertEquals("AUTH-003", respuesta.getCodigo());
    }

    @Test
    void cerrarConTokenVencidoDevuelveAuth004YCierraLaSesion() {
        RelojManual reloj = new RelojManual(INICIO);
        TokenServicio conReloj = new TokenServicio(Duration.ofMinutes(30), reloj);
        ServicioAutenticacion servicioConReloj =
                new ServicioAutenticacion(usuarioDAO, logSessionDAO, conReloj);
        RespuestaAutenticacion autenticacion = servicioConReloj.autenticar("cromero", "Cromero2026");
        reloj.avanzar(Duration.ofMinutes(31));

        RespuestaBase respuesta = servicioConReloj.cerrarSesion(autenticacion.getToken());

        assertEquals("AUTH-004", respuesta.getCodigo());
        assertEquals(1, logSessionDAO.getCerradas().size());
    }

    @Test
    void elHashDeLaClaveSeCalculaConSha256() {
        assertEquals(HASH_CROMERO, HashClave.sha256("Cromero2026"));
        assertFalse(HashClave.coincide("Cromero2026", HASH_SARMIENTO));
    }
}
