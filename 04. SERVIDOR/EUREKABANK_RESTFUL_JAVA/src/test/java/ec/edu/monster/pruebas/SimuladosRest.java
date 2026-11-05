package ec.edu.monster.pruebas;

import ec.edu.monster.api.rest.ServiciosEurekaBank;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.seguridad.ServicioAutenticacion;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.servicios.ServicioCostos;
import ec.edu.monster.servicios.ServicioCuentas;
import ec.edu.monster.servicios.ServicioMovimientos;
import ec.edu.monster.utilidades.HashClave;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dobles en memoria para probar los recursos REST sin contenedor ni base de datos.
 */
final class SimuladosRest {

    static final Instant INICIO = Instant.parse("2026-11-03T10:00:00Z");
    static final String CEDULA = "06914897";
    static final String CODIGO_CLIENTE = "00001";
    static final String ORIGEN = "00200002";
    static final String DESTINO = "00200001";
    static final String EXTRANJERA = "00200003";
    static final String INACTIVA = "00300001";
    static final String USUARIO = "cromero";
    static final String INACTIVO = "inactivo";
    static final String CLAVE = "Cromero2026";

    private SimuladosRest() {
    }

    static ServiciosEurekaBank servicios() {
        return servicios(Clock.fixed(INICIO, ZoneOffset.UTC));
    }

    static ServiciosEurekaBank servicios(Clock reloj) {
        TokenServicio tokens = new TokenServicio(Duration.ofMinutes(30), reloj);
        Dobles.ClienteDAOFalso clientes = new Dobles.ClienteDAOFalso()
                .conCliente(CEDULA, CODIGO_CLIENTE, "Eric Gustavo Coronel Castillo");
        Dobles.CuentaDAOFalso cuentas = new Dobles.CuentaDAOFalso()
                .conCuenta(cuenta(ORIGEN, "01", "ACTIVO", "6800.00", 7))
                .conCuenta(cuenta(DESTINO, "01", "ACTIVO", "7000.00", 20))
                .conCuenta(cuenta(EXTRANJERA, "02", "ACTIVO", "500.00", 1))
                .conCuenta(cuenta(INACTIVA, "01", "CANCELADO", "0.00", 3));
        Dobles.UsuarioDAOFalso usuarios = new Dobles.UsuarioDAOFalso()
                .conUsuario(USUARIO, "0001", HashClave.sha256(CLAVE), "ACTIVO", "Carlos Romero", "CAJERO")
                .conUsuario(INACTIVO, "0002", HashClave.sha256(CLAVE), "INACTIVO", "Ana Lopez", "CAJERO");
        Dobles.MovimientoDAOFalso movimientos = new Dobles.MovimientoDAOFalso();
        Dobles.ParametroDAOFalso parametros = new Dobles.ParametroDAOFalso()
                .conValor("001", new BigDecimal("0.08"))
                .conValor("002", new BigDecimal("15"));
        Dobles.CostoDAOFalso costos = new Dobles.CostoDAOFalso()
                .conMantenimiento("01", new BigDecimal("3500.00"), new BigDecimal("7.00"))
                .conCostoMovimiento("01", new BigDecimal("2.00"))
                .conInteres("01", new BigDecimal("0.70"));
        Dobles.MonedaDAOFalso monedas = new Dobles.MonedaDAOFalso()
                .conMoneda("01", "Soles", true)
                .conMoneda("02", "Dolares", true)
                .conMoneda("03", "Yenes", false);
        Dobles.LogSessionDAOFalso sesiones = new Dobles.LogSessionDAOFalso();
        return new ServiciosEurekaBank(
                tokens,
                new ServicioAutenticacion(usuarios, sesiones, tokens),
                new ServicioCuentas(clientes, cuentas, tokens),
                new ServicioMovimientos(cuentas, movimientos, new Dobles.TipoMovimientoDAOFalso(),
                        parametros, costos, monedas, new Dobles.UnidadTrabajoFalsa(), tokens),
                new ServicioCostos(cuentas, costos, monedas, parametros, tokens));
    }

    static String tokenValido(ServiciosEurekaBank servicios) {
        return servicios.tokens().generarToken("0001");
    }

    static CuentaDTO cuenta(String codigo, String moneda, String estado, String saldo, int operaciones) {
        return new CuentaDTO(codigo, moneda, "Soles", new BigDecimal(saldo), estado,
                CODIGO_CLIENTE, "Eric Gustavo Coronel Castillo", "002", "2008-01-09", "123456",
                operaciones);
    }

    /**
     * Petición JAX-RS simulada: guarda cabeceras y propiedades y registra si alguien aborta.
     */
    static final class PeticionFalsa {

        private final Map<String, String> cabeceras = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, Object> propiedades = new HashMap<>();
        private boolean abortada;
        private final ContainerRequestContext contexto;

        PeticionFalsa() {
            contexto = (ContainerRequestContext) Proxy.newProxyInstance(
                    ContainerRequestContext.class.getClassLoader(),
                    new Class<?>[] {ContainerRequestContext.class},
                    (proxy, metodo, args) -> switch (metodo.getName()) {
                        case "getHeaderString" -> cabeceras.get((String) args[0]);
                        case "setProperty" -> {
                            propiedades.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getProperty" -> propiedades.get((String) args[0]);
                        case "abortWith" -> {
                            abortada = true;
                            yield null;
                        }
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "PeticionFalsa";
                        default -> throw new UnsupportedOperationException(metodo.getName());
                    });
        }

        PeticionFalsa conCabecera(String nombre, String valor) {
            cabeceras.put(nombre, valor);
            return this;
        }

        ContainerRequestContext contexto() {
            return contexto;
        }

        Object propiedad(String nombre) {
            return propiedades.get(nombre);
        }

        boolean abortada() {
            return abortada;
        }
    }

    /**
     * Respuesta JAX-RS simulada para el filtro CORS: solo expone sus cabeceras.
     */
    static ContainerResponseContext respuestaFalsa(MultivaluedMap<String, Object> cabeceras) {
        return (ContainerResponseContext) Proxy.newProxyInstance(
                ContainerResponseContext.class.getClassLoader(),
                new Class<?>[] {ContainerResponseContext.class},
                (proxy, metodo, args) -> switch (metodo.getName()) {
                    case "getHeaders" -> cabeceras;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "RespuestaFalsa";
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
    }

    static MultivaluedMap<String, Object> cabecerasVacias() {
        return new MultivaluedHashMap<>();
    }
}
