package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.cliente.ClienteEUREKABANK;
import ec.edu.monster.escritorio.modelo.RespuestaBase;
import ec.edu.monster.escritorio.util.ExcepcionServicio;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * El sobre del banco viaja por HTTP tal cual: el código y el mensaje literal
 * del catálogo congelado llegan intactos, con acentos y sin variantes, y el
 * estado HTTP acompaña al código de negocio.
 */
class SobreRespuestaTest {

    private static final String BASE = "/EUREKABANK_RESTFUL_JAVA/api";

    private static ServidorDePrueba servidor;
    private static String url;

    @BeforeAll
    static void publicar() throws IOException {
        servidor = new ServidorDePrueba();
        url = servidor.url(BASE);
    }

    @AfterAll
    static void detener() {
        servidor.close();
    }

    /**
     * @return el catálogo congelado: estado, código, estado HTTP y mensaje literal
     */
    static Stream<Arguments> catalogo() {
        return Stream.of(
                Arguments.of("OK", "200", 200, "Cuenta consultada correctamente"),
                Arguments.of("ERROR", "DAT-001", 400,
                        "Los datos de la petición están incompletos o son incorrectos"),
                Arguments.of("ERROR", "AUTH-001", 401,
                        "El usuario o la contraseña son incorrectos"),
                Arguments.of("ERROR", "AUTH-002", 403,
                        "El usuario se encuentra inactivo"),
                Arguments.of("ERROR", "AUTH-003", 401,
                        "El token de sesión no es válido"),
                Arguments.of("ERROR", "AUTH-004", 401,
                        "El token de sesión ha expirado, inicie sesión nuevamente"),
                Arguments.of("ERROR", "CTA-001", 404,
                        "La cuenta solicitada no existe"),
                Arguments.of("ERROR", "CTA-002", 409,
                        "La cuenta no se encuentra en estado ACTIVO"),
                Arguments.of("ERROR", "CTA-003", 400,
                        "La cuenta origen y la cuenta destino no pueden ser iguales"),
                Arguments.of("ERROR", "CTA-004", 409,
                        "La cuenta de destino pertenece a una moneda distinta"),
                Arguments.of("ERROR", "CLI-001", 404,
                        "El cliente solicitado no existe"),
                Arguments.of("ERROR", "SAL-001", 409,
                        "El saldo de la cuenta es insuficiente para la operación"),
                Arguments.of("ERROR", "MON-001", 400,
                        "El monto debe ser mayor que cero y con máximo dos decimales"),
                Arguments.of("ERROR", "MON-002", 409,
                        "La moneda de la cuenta no está soportada por el sistema"),
                Arguments.of("ERROR", "LIM-001", 409,
                        "El monto supera el límite máximo de mantenimiento de la cuenta"),
                Arguments.of("ERROR", "ERR-001", 500,
                        "Ocurrió un error interno, intente nuevamente"));
    }

    @ParameterizedTest(name = "{1} ({2})")
    @MethodSource("catalogo")
    void elCodigoYMensajeLiteralesDelSobreLleganIntactosDesdeElHttp(
            String estado, String codigo, int estadoHttp, String mensaje) {
        servidor.registrar("GET", BASE + "/cuentas/" + codigo, estadoHttp,
                ServidorDePrueba.sobre(estado, codigo, mensaje));

        RespuestaBase respuesta = new ClienteEUREKABANK(url).consultarCuenta(codigo, "TOKEN-REST");

        assertEquals(estado, respuesta.getEstado());
        assertEquals(codigo, respuesta.getCodigo());
        assertEquals(mensaje, respuesta.getMensaje());
    }

    @Test
    void elCodigoDeTokenInvalidoLlegaIntactoConSuMensajeLiteral() {
        servidor.registrar("GET", BASE + "/cuentas/AUTH-003", 401,
                ServidorDePrueba.sobre("ERROR", "AUTH-003", "El token de sesión no es válido"));

        RespuestaBase respuesta = new ClienteEUREKABANK(url).consultarCuenta("AUTH-003", "TOKEN-REST");

        assertEquals("AUTH-003", respuesta.getCodigo());
        assertEquals("El token de sesión no es válido", respuesta.getMensaje());
    }

    @Test
    void elCodigoDeServicioNoDisponibleLlegaEnLaExcepcionDelCliente() {
        servidor.registrar("GET", BASE + "/cuentas/SER-001", 503,
                ServidorDePrueba.sobre("ERROR", "SER-001",
                        "El servicio no está disponible en este momento"));

        ExcepcionServicio error = assertThrows(ExcepcionServicio.class,
                () -> new ClienteEUREKABANK(url).consultarCuenta("SER-001", "TOKEN-REST"));

        assertEquals("SER-001", error.getCodigo());
        assertTrue(error.getMessage().contains("El servicio no está disponible en este momento"));
    }

    @Test
    void losMensajesConAcentosYEnieLleganIntactos() {
        servidor.registrar("GET", BASE + "/cuentas/LIM-001", 409,
                ServidorDePrueba.sobre("ERROR", "LIM-001",
                        "El monto supera el límite máximo de mantenimiento de la cuenta"));

        RespuestaBase respuesta = new ClienteEUREKABANK(url).consultarCuenta("LIM-001", "TOKEN-REST");

        assertEquals("El monto supera el límite máximo de mantenimiento de la cuenta",
                respuesta.getMensaje());
    }
}
