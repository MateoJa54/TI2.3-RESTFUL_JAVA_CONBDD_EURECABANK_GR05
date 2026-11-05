package ec.edu.monster.pruebas;

import ec.edu.monster.datos.ICostoDAO;
import ec.edu.monster.datos.IClienteDAO;
import ec.edu.monster.datos.ICuentaDAO;
import ec.edu.monster.datos.ILogSessionDAO;
import ec.edu.monster.datos.IMonedaDAO;
import ec.edu.monster.datos.IMovimientoDAO;
import ec.edu.monster.datos.IParametroDAO;
import ec.edu.monster.datos.ITipoMovimientoDAO;
import ec.edu.monster.datos.IUsuarioDAO;
import ec.edu.monster.datos.UnidadTrabajo;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dobles de prueba de los repositorios.
 *
 * <p>Cada repositorio del paquete {@code datos} es un puerto (interfaz): las
 * pruebas unitarias usan estos dobles en memoria y no tocan la base de datos.
 */
public final class Dobles {

    private Dobles() {
    }

    /** Cliente en memoria. */
    public static final class ClienteDAOFalso implements IClienteDAO {

        private final Map<String, DatosCliente> porDni = new HashMap<>();

        public ClienteDAOFalso conCliente(String dni, String codigo, String nombre) {
            porDni.put(dni, new DatosCliente(codigo, nombre));
            return this;
        }

        @Override
        public Optional<DatosCliente> buscarPorDni(String dni) {
            return Optional.ofNullable(porDni.get(dni));
        }
    }

    /** Cuentas en memoria, con el mismo comportamiento de bloqueo y saldo. */
    public static final class CuentaDAOFalso implements ICuentaDAO {

        private final Map<String, CuentaDTO> cuentas = new LinkedHashMap<>();
        private final List<String> bloqueadas = new ArrayList<>();

        public CuentaDAOFalso conCuenta(CuentaDTO cuenta) {
            cuentas.put(cuenta.getCodigo(), cuenta);
            return this;
        }

        public List<String> getBloqueadas() {
            return bloqueadas;
        }

        @Override
        public List<CuentaDTO> listarPorCliente(String codigoCliente) {
            List<CuentaDTO> resultado = new ArrayList<>();
            for (CuentaDTO cuenta : cuentas.values()) {
                if (cuenta.getCodigoCliente().equals(codigoCliente)) {
                    resultado.add(cuenta);
                }
            }
            return resultado;
        }

        @Override
        public Optional<CuentaDTO> buscarPorCodigo(String codigo) {
            return Optional.ofNullable(cuentas.get(codigo));
        }

        @Override
        public Optional<CuentaDTO> bloquearPorCodigo(Connection conexion, String codigo) {
            bloqueadas.add(codigo);
            return Optional.ofNullable(cuentas.get(codigo));
        }

        @Override
        public void actualizarSaldoYContador(Connection conexion, String codigo,
                BigDecimal saldo, int contador) {
            CuentaDTO cuenta = cuentas.get(codigo);
            if (cuenta == null) {
                throw new IllegalStateException("La cuenta " + codigo + " no existe");
            }
            cuenta.setSaldo(saldo);
            cuenta.setOperacionesRealizadas(contador);
        }
    }

    /** Movimientos en memoria; puede fallar a proposito al insertar. */
    public static final class MovimientoDAOFalso implements IMovimientoDAO {

        private final List<MovimientoDTO> movimientos = new ArrayList<>();
        private final Map<String, List<MovimientoDTO>> porCuenta = new LinkedHashMap<>();
        private boolean fallarAlInsertar;

        public MovimientoDAOFalso fallarAlInsertar(boolean fallar) {
            this.fallarAlInsertar = fallar;
            return this;
        }

        public List<MovimientoDTO> getMovimientos() {
            return movimientos;
        }

        public List<MovimientoDTO> getMovimientosDe(String codigoCuenta) {
            return porCuenta.getOrDefault(codigoCuenta, List.of());
        }

        @Override
        public List<MovimientoDTO> listarPorCuenta(String codigoCuenta) {
            return new ArrayList<>(porCuenta.getOrDefault(codigoCuenta, List.of()));
        }

        @Override
        public void insertar(Connection conexion, String codigoCuenta, MovimientoDTO movimiento) {
            if (fallarAlInsertar) {
                throw new IllegalStateException("Fallo simulado al insertar el movimiento");
            }
            movimientos.add(movimiento);
            porCuenta.computeIfAbsent(codigoCuenta, clave -> new ArrayList<>()).add(movimiento);
        }
    }

    /** Usuarios en memoria. */
    public static final class UsuarioDAOFalso implements IUsuarioDAO {

        private final Map<String, Usuario> porNombre = new HashMap<>();

        public UsuarioDAOFalso conUsuario(String nombre, String codigo, String hash,
                String estado, String nombreEmpleado, String rol) {
            porNombre.put(nombre, new Usuario(codigo, hash, estado, nombreEmpleado, rol));
            return this;
        }

        @Override
        public Optional<Usuario> buscarPorNombre(String nombreUsuario) {
            return Optional.ofNullable(porNombre.get(nombreUsuario));
        }
    }

    /** Parametros de negocio en memoria. */
    public static final class ParametroDAOFalso implements IParametroDAO {

        private final Map<String, BigDecimal> valores = new HashMap<>();

        public ParametroDAOFalso conValor(String codigo, BigDecimal valor) {
            valores.put(codigo, valor);
            return this;
        }

        @Override
        public Optional<BigDecimal> valor(String codigo) {
            return Optional.ofNullable(valores.get(codigo));
        }
    }

    /** Catalogo de tipos de movimiento en memoria. */
    public static final class TipoMovimientoDAOFalso implements ITipoMovimientoDAO {

        private final Map<String, TipoMovimiento> tipos = new HashMap<>();

        public TipoMovimientoDAOFalso() {
            tipos.put("003", new TipoMovimiento("003", "Deposito", "INGRESO"));
            tipos.put("004", new TipoMovimiento("004", "Retiro", "SALIDA"));
            tipos.put("007", new TipoMovimiento("007", "ITF", "SALIDA"));
            tipos.put("008", new TipoMovimiento("008", "Transferencia", "INGRESO"));
            tipos.put("009", new TipoMovimiento("009", "Transferencia", "SALIDA"));
            tipos.put("010", new TipoMovimiento("010", "Cargo por Movimiento", "SALIDA"));
        }

        @Override
        public Optional<TipoMovimiento> buscarPorCodigo(String codigo) {
            return Optional.ofNullable(tipos.get(codigo));
        }
    }

    /** Monedas en memoria. */
    public static final class MonedaDAOFalso implements IMonedaDAO {

        private final Map<String, String> descripciones = new HashMap<>();
        private final List<String> soportadas = new ArrayList<>();

        public MonedaDAOFalso conMoneda(String codigo, String descripcion, boolean soportada) {
            descripciones.put(codigo, descripcion);
            soportadas.remove(codigo);
            if (soportada) {
                soportadas.add(codigo);
            }
            return this;
        }

        @Override
        public Optional<String> descripcion(String codigo) {
            return Optional.ofNullable(descripciones.get(codigo));
        }

        @Override
        public boolean estaSoportada(String codigo) {
            return soportadas.contains(codigo);
        }
    }

    /** Costos e intereses en memoria. */
    public static final class CostoDAOFalso implements ICostoDAO {

        private final Map<String, CostoMantenimiento> mantenimiento = new HashMap<>();
        private final Map<String, BigDecimal> costoMovimiento = new HashMap<>();
        private final Map<String, BigDecimal> interes = new HashMap<>();

        public CostoDAOFalso conMantenimiento(String moneda, BigDecimal maximo, BigDecimal importe) {
            mantenimiento.put(moneda, new CostoMantenimiento(maximo, importe));
            return this;
        }

        public CostoDAOFalso conCostoMovimiento(String moneda, BigDecimal importe) {
            costoMovimiento.put(moneda, importe);
            return this;
        }

        public CostoDAOFalso conInteres(String moneda, BigDecimal porcentaje) {
            interes.put(moneda, porcentaje);
            return this;
        }

        @Override
        public Optional<CostoMantenimiento> costoMantenimiento(String moneda) {
            return Optional.ofNullable(mantenimiento.get(moneda));
        }

        @Override
        public Optional<BigDecimal> costoMovimiento(String moneda) {
            return Optional.ofNullable(costoMovimiento.get(moneda));
        }

        @Override
        public Optional<BigDecimal> interesMensual(String moneda) {
            return Optional.ofNullable(interes.get(moneda));
        }
    }

    /** Auditoria de sesiones en memoria. */
    public static final class LogSessionDAOFalso implements ILogSessionDAO {

        private final Map<Integer, String> sesiones = new LinkedHashMap<>();
        private final List<Integer> cerradas = new ArrayList<>();
        private int siguiente = 1;

        @Override
        public Integer registrarIngreso(String empleado, String ip, String hostname) {
            int id = siguiente++;
            sesiones.put(id, empleado);
            return id;
        }

        @Override
        public boolean registrarSalida(Integer idSesion) {
            if (idSesion == null || !sesiones.containsKey(idSesion)) {
                return false;
            }
            cerradas.add(idSesion);
            return true;
        }

        public List<Integer> getCerradas() {
            return cerradas;
        }

        public Map<Integer, String> getSesiones() {
            return sesiones;
        }
    }

    /**
     * Unidad de trabajo que ejecuta el bloque sin base de datos: la conexion
     * que se le pasa es {@code null} y ningun doble la usa.
     */
    public static final class UnidadTrabajoFalsa implements UnidadTrabajo {

        @Override
        public <T> T ejecutar(Cuerpo<T> cuerpo) {
            try {
                return cuerpo.aplicar(null);
            } catch (SQLException e) {
                throw new IllegalStateException("Fallo simulado en la unidad de trabajo", e);
            }
        }
    }

    /**
     * Unidad de trabajo que falla al ejecutar el bloque, para comprobar que la
     * fachada responde {@code ERR-001}.
     */
    public static final class UnidadTrabajoQueFalla implements UnidadTrabajo {

        @Override
        public <T> T ejecutar(Cuerpo<T> cuerpo) {
            throw new IllegalStateException("Fallo simulado de la base de datos");
        }
    }
}
