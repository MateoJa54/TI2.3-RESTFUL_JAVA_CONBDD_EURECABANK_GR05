package ec.edu.monster.api.rest;

import ec.edu.monster.datos.ClienteDAO;
import ec.edu.monster.datos.CostoDAO;
import ec.edu.monster.datos.CuentaDAO;
import ec.edu.monster.datos.LogSessionDAO;
import ec.edu.monster.datos.MonedaDAO;
import ec.edu.monster.datos.MovimientoDAO;
import ec.edu.monster.datos.ParametroDAO;
import ec.edu.monster.datos.ProveedorConexion;
import ec.edu.monster.datos.TipoMovimientoDAO;
import ec.edu.monster.datos.UnidadTrabajoJdbc;
import ec.edu.monster.datos.UsuarioDAO;
import ec.edu.monster.seguridad.ServicioAutenticacion;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.servicios.ServicioCostos;
import ec.edu.monster.servicios.ServicioCuentas;
import ec.edu.monster.servicios.ServicioMovimientos;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Ensamblaje único de los servicios. Todos comparten el mismo {@link TokenServicio}:
 * el token que emite la autenticación es el que validan las demás operaciones.
 */
public final class ServiciosEurekaBank {

    static final String VARIABLE_TOKEN_MINUTOS = "EUREKABANK_TOKEN_MINUTOS";
    static final int MINUTOS_POR_DEFECTO = 30;

    private static ServiciosEurekaBank porDefecto;

    private final TokenServicio tokens;
    private final ServicioAutenticacion autenticacion;
    private final ServicioCuentas cuentas;
    private final ServicioMovimientos movimientos;
    private final ServicioCostos costos;

    public ServiciosEurekaBank(TokenServicio tokens, ServicioAutenticacion autenticacion,
            ServicioCuentas cuentas, ServicioMovimientos movimientos, ServicioCostos costos) {
        this.tokens = Objects.requireNonNull(tokens, "El registro de tokens es obligatorio");
        this.autenticacion = Objects.requireNonNull(autenticacion, "La autenticacion es obligatoria");
        this.cuentas = Objects.requireNonNull(cuentas, "El servicio de cuentas es obligatorio");
        this.movimientos = Objects.requireNonNull(movimientos, "El servicio de movimientos es obligatorio");
        this.costos = Objects.requireNonNull(costos, "El servicio de costos es obligatorio");
    }

    /**
     * @return los servicios conectados a la base de datos según la configuración del entorno
     */
    public static synchronized ServiciosEurekaBank porDefecto() {
        if (porDefecto == null) {
            porDefecto = ensamblar(new ProveedorConexion());
        }
        return porDefecto;
    }

    private static ServiciosEurekaBank ensamblar(ProveedorConexion proveedor) {
        int minutos = leerMinutos(System.getenv(VARIABLE_TOKEN_MINUTOS));
        TokenServicio tokens = new TokenServicio(Duration.ofMinutes(minutos), Clock.systemUTC());
        return new ServiciosEurekaBank(
                tokens,
                new ServicioAutenticacion(new UsuarioDAO(proveedor), new LogSessionDAO(proveedor), tokens),
                new ServicioCuentas(new ClienteDAO(proveedor), new CuentaDAO(proveedor), tokens),
                new ServicioMovimientos(new CuentaDAO(proveedor), new MovimientoDAO(proveedor),
                        new TipoMovimientoDAO(proveedor), new ParametroDAO(proveedor),
                        new CostoDAO(proveedor), new MonedaDAO(proveedor),
                        new UnidadTrabajoJdbc(proveedor), tokens),
                new ServicioCostos(new CuentaDAO(proveedor), new CostoDAO(proveedor),
                        new MonedaDAO(proveedor), new ParametroDAO(proveedor), tokens));
    }

    /**
     * @param valor valor de {@code EUREKABANK_TOKEN_MINUTOS}, puede ser {@code null}
     * @return los minutos de vigencia del token; 30 si el valor falta, no es un entero
     *         positivo
     */
    public static int leerMinutos(String valor) {
        if (valor == null) {
            return MINUTOS_POR_DEFECTO;
        }
        try {
            int minutos = Integer.parseInt(valor.strip());
            return minutos > 0 ? minutos : MINUTOS_POR_DEFECTO;
        } catch (NumberFormatException error) {
            return MINUTOS_POR_DEFECTO;
        }
    }

    public TokenServicio tokens() {
        return tokens;
    }

    public ServicioAutenticacion autenticacion() {
        return autenticacion;
    }

    public ServicioCuentas cuentas() {
        return cuentas;
    }

    public ServicioMovimientos movimientos() {
        return movimientos;
    }

    public ServicioCostos costos() {
        return costos;
    }
}
