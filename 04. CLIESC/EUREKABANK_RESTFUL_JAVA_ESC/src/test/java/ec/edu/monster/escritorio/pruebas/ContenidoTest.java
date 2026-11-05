package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.controlador.ControladorAutenticacion;
import ec.edu.monster.escritorio.modelo.SesionUsuario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Reglas de contenido y de capas que se comprueban sobre los fuentes. */
class ContenidoTest {

    private static final Path FUENTES = Paths.get("src", "main", "java", "ec", "edu", "monster", "escritorio");
    private static final Path PRUEBAS = Paths.get("src", "test", "java");

    static Stream<Path> archivos() throws IOException {
        try (Stream<Path> s = Files.walk(Paths.get("src"))) {
            return s.filter(p -> p.toString().endsWith(".java")).toList().stream();
        }
    }

    static Stream<Path> vistas() throws IOException {
        try (Stream<Path> s = Files.walk(FUENTES.resolve("vista"))) {
            return s.filter(p -> p.toString().endsWith(".java")).toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("archivos")
    void ningunArchivoLlevaGuionLargo(Path archivo) throws IOException {
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        assertFalse(texto.indexOf((char) 0x2014) >= 0, archivo + " tiene un guion largo");
    }

    @ParameterizedTest
    @MethodSource("archivos")
    void ningunArchivoNombraLaTecnologiaDelOtroProyecto(Path archivo) throws IOException {
        String texto = Files.readString(archivo, StandardCharsets.UTF_8).toLowerCase();
        for (String palabra : List.of("so" + "ap", "ws" + "dl", "x" + "sd", "jax" + "-ws", "ws" + "eurekabank")) {
            assertFalse(texto.contains(palabra), archivo + " nombra " + palabra);
        }
    }

    @ParameterizedTest
    @MethodSource("vistas")
    void laVistaNuncaLlamaAlClienteHttp(Path archivo) throws IOException {
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        assertFalse(texto.contains("escritorio.cliente"), archivo + " importa el cliente HTTP");
        assertFalse(texto.contains("java.net.http"), archivo + " usa HTTP directamente");
        assertFalse(texto.contains("HttpURLConnection"), archivo + " usa HTTP directamente");
    }

    @Test
    void soloMainConoceElClienteHttp() throws IOException {
        try (Stream<Path> s = Files.walk(FUENTES)) {
            List<Path> conCliente = s.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p, StandardCharsets.UTF_8).contains("escritorio.cliente.ClienteEUREKABANK;");
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }).toList();
            assertEquals(1, conCliente.size(), conCliente.toString());
            assertEquals("Main.java", conCliente.get(0).getFileName().toString());
        }
    }

    @Test
    void laIlustracionDeMonsterEstaEnLosRecursos() {
        assertNotNullRecurso("/imagenes/Monster.png");
    }

    private static void assertNotNullRecurso(String ruta) {
        assertTrue(ContenidoTest.class.getResource(ruta) != null, "falta el recurso " + ruta);
    }

    @Test
    void laIlustracionSeLeeComoImagen() throws IOException {
        try (var entrada = ContenidoTest.class.getResourceAsStream("/imagenes/Monster.png")) {
            var imagen = javax.imageio.ImageIO.read(entrada);
            assertEquals(540, imagen.getWidth());
            assertEquals(866, imagen.getHeight());
        }
    }

    @Test
    void elDirectorioDePruebasExiste() {
        assertTrue(Files.isDirectory(PRUEBAS));
    }

    @Test
    void cerrarSesionDosVecesSeguidasNoLanzaExcepcion() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = new SesionUsuario();
        ControladorAutenticacion autenticacion = new ControladorAutenticacion(servidor, sesion);
        autenticacion.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        assertEquals("OK", autenticacion.cerrarSesion().getEstado());
        var segunda = autenticacion.cerrarSesion();
        assertEquals("ERROR", segunda.getEstado());
        assertEquals("AUTH-003", segunda.getCodigo());
        assertFalse(autenticacion.haySesionActiva());
        var tercera = autenticacion.cerrarSesion();
        assertEquals("AUTH-003", tercera.getCodigo());
        assertEquals(1, servidor.llamadas.stream().filter(l -> l.startsWith("cerrarSesion")).count());
    }

    @Test
    void cerrarSesionConElServidorApagadoLimpiaLaSesionLocal() {
        ClienteFalso servidor = new ClienteFalso();
        SesionUsuario sesion = new SesionUsuario();
        ControladorAutenticacion autenticacion = new ControladorAutenticacion(servidor, sesion);
        autenticacion.autenticar(ClienteFalso.USUARIO, ClienteFalso.CLAVE);
        servidor.servidorApagado = true;
        try {
            autenticacion.cerrarSesion();
        } catch (RuntimeException esperado) {
            // El fallo de red sube al llamador; la vista lo muestra como SER-001.
        }
        assertFalse(sesion.estaVigente());
    }
}
