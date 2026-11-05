package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.seguridad.TokenServicio;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del registro de tokens (regla RB-14).
 */
class TokenServicioTest {

    private static final Instant INICIO = Instant.parse("2026-11-03T10:00:00Z");

    private RelojManual reloj;
    private TokenServicio servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual(INICIO);
        servicio = new TokenServicio(Duration.ofMinutes(30), reloj);
    }

    @Test
    void unTokenRecienGeneradoEsValido() {
        String token = servicio.generarToken("0001");

        assertEquals(CodigoRespuesta.OK_200, servicio.validarToken(token));
    }

    @Test
    void elTokenEsUnUuid() {
        String token = servicio.generarToken("0001");

        assertNotNull(UUID.fromString(token));
    }

    @Test
    void cadaTokenEsDistinto() {
        assertNotEquals(servicio.generarToken("0001"), servicio.generarToken("0001"));
    }

    @Test
    void unTokenVacioODesconocidoNoEsValido() {
        assertEquals(CodigoRespuesta.DAT_001, servicio.validarToken(null));
        assertEquals(CodigoRespuesta.DAT_001, servicio.validarToken(""));
        assertEquals(CodigoRespuesta.DAT_001, servicio.validarToken("  "));
        assertEquals(CodigoRespuesta.AUTH_003, servicio.validarToken("no-existe"));
    }

    @Test
    void elTokenVencePasadosLosTreintaMinutos() {
        String token = servicio.generarToken("0001");

        reloj.avanzar(Duration.ofMinutes(29).plusSeconds(59));
        assertEquals(CodigoRespuesta.OK_200, servicio.validarToken(token));

        reloj.avanzar(Duration.ofSeconds(1));
        assertEquals(CodigoRespuesta.AUTH_004, servicio.validarToken(token));
    }

    @Test
    void unTokenVencidoSigueDistinguiendoseDeUnoInexistente() {
        String token = servicio.generarToken("0001");
        reloj.avanzar(Duration.ofMinutes(31));

        assertEquals(CodigoRespuesta.AUTH_004, servicio.validarToken(token));
        assertEquals(CodigoRespuesta.AUTH_003, servicio.validarToken("otro-token"));
    }

    @Test
    void laExpiracionEsElInicioMasLaVigencia() {
        String token = servicio.generarToken("0001");

        assertEquals(INICIO.plus(Duration.ofMinutes(30)),
                servicio.obtenerExpiracion(token).orElseThrow());
        assertTrue(servicio.obtenerExpiracion("no-existe").isEmpty());
        assertTrue(servicio.obtenerExpiracion(null).isEmpty());
    }

    @Test
    void laSesionGuardaElEmpleadoYElIdentificadorDeAuditoria() {
        String token = servicio.generarToken("0001");
        servicio.asociarLogSession(token, 77);

        TokenServicio.Sesion sesion = servicio.obtenerSesion(token).orElseThrow();
        assertEquals("0001", sesion.getEmpleado());
        assertEquals(77, sesion.getIdSesionLog());
        assertEquals(token, sesion.getToken());
    }

    @Test
    void asociarLaAuditoriaDeUnTokenInexistenteNoFalla() {
        servicio.asociarLogSession("no-existe", 5);

        assertTrue(servicio.obtenerSesion("no-existe").isEmpty());
    }

    @Test
    void cerrarDescarcaElToken() {
        String token = servicio.generarToken("0001");

        servicio.cerrar(token);

        assertEquals(CodigoRespuesta.AUTH_003, servicio.validarToken(token));
        assertEquals(0, servicio.cantidadSesiones());
    }

    @Test
    void cerrarUnTokenNuloNoFalla() {
        servicio.cerrar(null);

        assertEquals(0, servicio.cantidadSesiones());
    }

    @Test
    void generarUnTokenDescartaLosTokensCaducadosHaceMucho() {
        String viejo = servicio.generarToken("0001");
        reloj.avanzar(Duration.ofMinutes(61));

        servicio.generarToken("0001");

        assertTrue(servicio.obtenerExpiracion(viejo).isEmpty());
    }

    @Test
    void unTokenCaducadoRecentlySeConservaParaPoderCerrarLaSesion() {
        String token = servicio.generarToken("0001");
        reloj.avanzar(Duration.ofMinutes(31));

        servicio.generarToken("0001");

        assertTrue(servicio.obtenerExpiracion(token).isPresent());
    }

    @Test
    void laVigenciaPorDefectoEsDeTreintaMinutos() {
        assertEquals(Duration.ofMinutes(30), TokenServicio.VIGENCIA_POR_DEFECTO);
        assertEquals(Duration.ofMinutes(30), new TokenServicio().getVigencia());
    }

    @Test
    void laVigenciaDebeSerPositiva() {
        assertThrows(IllegalArgumentException.class, () -> new TokenServicio(Duration.ZERO, reloj));
        assertThrows(IllegalArgumentException.class,
                () -> new TokenServicio(Duration.ofMinutes(-1), reloj));
        assertThrows(NullPointerException.class, () -> new TokenServicio(Duration.ofMinutes(1), null));
    }

    @Test
    void variosHilosGeneranTokensDistintosYValidos() throws Exception {
        int hilos = 8;
        int porHilo = 25;
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listo = new CountDownLatch(hilos);
        List<List<String>> generados = new ArrayList<>();

        for (int i = 0; i < hilos; i++) {
            ejecutor.submit(() -> {
                List<String> propios = new ArrayList<>();
                for (int j = 0; j < porHilo; j++) {
                    propios.add(servicio.generarToken("0001"));
                }
                synchronized (generados) {
                    generados.add(propios);
                }
                listo.countDown();
            });
        }
        assertTrue(listo.await(20, TimeUnit.SECONDS));

        List<String> todos = new ArrayList<>();
        for (List<String> lista : generados) {
            todos.addAll(lista);
        }
        assertEquals(hilos * porHilo, todos.size());
        assertEquals(todos.size(), todos.stream().distinct().count());
        for (String token : todos) {
            assertEquals(CodigoRespuesta.OK_200, servicio.validarToken(token));
        }
        ejecutor.shutdown();
    }
}
