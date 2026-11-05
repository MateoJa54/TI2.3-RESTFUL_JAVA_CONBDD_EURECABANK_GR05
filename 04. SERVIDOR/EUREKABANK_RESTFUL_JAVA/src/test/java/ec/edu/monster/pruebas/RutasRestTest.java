package ec.edu.monster.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.monster.api.rest.AuthResource;
import ec.edu.monster.api.rest.ClienteResource;
import ec.edu.monster.api.rest.CuentaResource;
import ec.edu.monster.api.rest.TransferenciaResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Comprueba las plantillas {@code @Path} de los recursos REST con una traduccion
 * sencilla a expresion regular. No sustituye el enrutado real del contenedor.
 */
class RutasRestTest {

    @Test
    void lasRutasDeAutenticacionSonExactas() {
        assertEquals(List.of("autenticar"), metodosQueCoinciden(AuthResource.class, "POST", "/api/auth/login"));
        assertEquals(List.of("cerrarSesion"), metodosQueCoinciden(AuthResource.class, "POST", "/api/auth/logout"));
        assertEquals(List.of(), metodosQueCoinciden(AuthResource.class, "GET", "/api/auth/login"));
        assertEquals(List.of(), metodosQueCoinciden(AuthResource.class, "POST", "/api/auth/log/out"));
    }

    @Test
    void laRutaDeClientesAceptaUnaCedulaYNoCruzaBarras() {
        assertEquals(List.of("listarCuentasPorCliente"),
                metodosQueCoinciden(ClienteResource.class, "GET", "/api/clientes/06914897/cuentas"));
        assertEquals(List.of("listarCuentasPorCliente"),
                metodosQueCoinciden(ClienteResource.class, "GET", "/api/clientes//cuentas"));
        assertEquals(List.of(), metodosQueCoinciden(ClienteResource.class, "GET", "/api/clientes/a/b/cuentas"));
        assertEquals(List.of(), metodosQueCoinciden(ClienteResource.class, "GET", "/api/clientes/a/cuentas/x"));
    }

    @Test
    void laRutaDeTransferenciasEsExacta() {
        assertEquals(List.of("realizarTransferencia"),
                metodosQueCoinciden(TransferenciaResource.class, "POST", "/api/transferencias"));
        assertEquals(List.of(), metodosQueCoinciden(TransferenciaResource.class, "POST", "/api/transferencias/x"));
        assertEquals(List.of(), metodosQueCoinciden(TransferenciaResource.class, "GET", "/api/transferencias"));
    }

    @Test
    void unaCuentaSinSufijoVaAConsultarCuenta() {
        assertEquals(List.of("consultarCuenta"),
                metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/00200002"));
    }

    @Test
    void lasRutasConSufijoNoSeConfundenConLaDeLaCuenta() {
        assertEquals(List.of("consultarMovimientos"),
                metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/00200002/movimientos"));
        assertEquals(List.of("consultarCostosMantenimiento"),
                metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/00200002/costos"));
        assertEquals(List.of("realizarDeposito"),
                metodosQueCoinciden(CuentaResource.class, "POST", "/api/cuentas/00200002/depositos"));
        assertEquals(List.of("realizarRetiro"),
                metodosQueCoinciden(CuentaResource.class, "POST", "/api/cuentas/00200002/retiros"));
    }

    @Test
    void unSegmentoVacioLlegaAlRecursoYNoDaUnError404() {
        assertEquals(List.of("consultarCuenta"), metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/"));
        assertEquals(List.of("consultarMovimientos"),
                metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas//movimientos"));
        assertEquals(List.of("realizarDeposito"),
                metodosQueCoinciden(CuentaResource.class, "POST", "/api/cuentas//depositos"));
        assertEquals(List.of("realizarRetiro"),
                metodosQueCoinciden(CuentaResource.class, "POST", "/api/cuentas//retiros"));
        assertEquals(List.of("consultarCostosMantenimiento"),
                metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas//costos"));
    }

    @Test
    void unSegmentoConBarraNoCoincideConLasPlantillasDeCuenta() {
        assertEquals(List.of(), metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/a/b"));
        assertEquals(List.of(), metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/a/b/movimientos"));
        assertEquals(List.of(), metodosQueCoinciden(CuentaResource.class, "POST", "/api/cuentas/a/b/depositos"));
        assertEquals(List.of(), metodosQueCoinciden(CuentaResource.class, "GET", "/api/cuentas/00200002/lista"));
        assertEquals(List.of(), metodosQueCoinciden(CuentaResource.class, "DELETE", "/api/cuentas/00200002"));
    }

    private static List<String> metodosQueCoinciden(Class<?> recurso, String verbo, String ruta) {
        String base = recurso.getAnnotation(Path.class).value();
        List<String> nombres = new ArrayList<>();
        for (Method metodo : recurso.getDeclaredMethods()) {
            if (!verboDe(metodo).equals(verbo)) {
                continue;
            }
            Path path = metodo.getAnnotation(Path.class);
            String sub = path == null ? "" : path.value();
            if (patron(base + sub).matcher(ruta).matches()) {
                nombres.add(metodo.getName());
            }
        }
        Collections.sort(nombres);
        return nombres;
    }

    private static String verboDe(Method metodo) {
        if (metodo.isAnnotationPresent(GET.class)) {
            return "GET";
        }
        if (metodo.isAnnotationPresent(POST.class)) {
            return "POST";
        }
        return "OTRO";
    }

    private static Pattern patron(String plantilla) {
        String regex = plantilla
                .replaceAll("\\{\\w+:([^}]*)\\}", "($1)")
                .replaceAll("\\{\\w+\\}", "([^/]+)");
        return Pattern.compile(regex);
    }
}
