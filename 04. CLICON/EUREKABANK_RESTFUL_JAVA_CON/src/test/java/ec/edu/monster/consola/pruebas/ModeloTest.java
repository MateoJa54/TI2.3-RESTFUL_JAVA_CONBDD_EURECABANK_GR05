package ec.edu.monster.consola.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.consola.modelo.CuentaDTO;
import ec.edu.monster.consola.modelo.MovimientoDTO;
import ec.edu.monster.consola.modelo.RespuestaAutenticacion;
import ec.edu.monster.consola.modelo.RespuestaBase;
import ec.edu.monster.consola.modelo.RespuestaCostos;
import ec.edu.monster.consola.modelo.RespuestaCuenta;
import ec.edu.monster.consola.modelo.RespuestaCuentas;
import ec.edu.monster.consola.modelo.RespuestaMovimiento;
import ec.edu.monster.consola.modelo.RespuestaMovimientos;
import ec.edu.monster.consola.modelo.RespuestaTransferencia;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El modelo son POJOs planos: sin anotaciones JAXB, con constructor sin
 * argumentos y con getter y setter para cada atributo, que es lo que necesita
 * JAXB para deserializar el sobre del servidor.
 */
class ModeloTest {

    /** Los once tipos del contrato congelado (el sobre y sus derivados). */
    private static final List<Class<?>> TIPOS = List.of(
            RespuestaBase.class,
            RespuestaAutenticacion.class,
            RespuestaCuentas.class,
            RespuestaCuenta.class,
            RespuestaMovimientos.class,
            RespuestaMovimiento.class,
            RespuestaTransferencia.class,
            RespuestaCostos.class,
            CuentaDTO.class,
            MovimientoDTO.class);

    @ParameterizedTest
    @ValueSource(classes = {
        RespuestaBase.class, RespuestaAutenticacion.class, RespuestaCuentas.class,
        RespuestaCuenta.class, RespuestaMovimientos.class, RespuestaMovimiento.class,
        RespuestaTransferencia.class, RespuestaCostos.class, CuentaDTO.class,
        MovimientoDTO.class})
    void cadaTipoTieneConstructorPublicoSinArgumentos(Class<?> tipo) throws Exception {
        assertNotNull(tipo.getConstructor().newInstance());
    }

    @ParameterizedTest
    @ValueSource(classes = {
        RespuestaBase.class, RespuestaAutenticacion.class, RespuestaCuentas.class,
        RespuestaCuenta.class, RespuestaMovimientos.class, RespuestaMovimiento.class,
        RespuestaTransferencia.class, RespuestaCostos.class, CuentaDTO.class,
        MovimientoDTO.class})
    void ningunTipoLlevaAnotacionesJaxb(Class<?> tipo) {
        assertEquals(0, tipo.getAnnotations().length,
                tipo.getSimpleName() + " no debe llevar anotaciones JAXB");
        for (Field campo : tipo.getDeclaredFields()) {
            assertEquals(0, campo.getAnnotations().length,
                    campo.getName() + " no debe llevar anotaciones JAXB");
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {
        RespuestaBase.class, RespuestaAutenticacion.class, RespuestaCuentas.class,
        RespuestaCuenta.class, RespuestaMovimientos.class, RespuestaMovimiento.class,
        RespuestaTransferencia.class, RespuestaCostos.class, CuentaDTO.class,
        MovimientoDTO.class})
    void cadaAtributoTieneGetterYSetter(Class<?> tipo) throws Exception {
        for (Field campo : tipo.getDeclaredFields()) {
            if (campo.isSynthetic() || Modifier.isStatic(campo.getModifiers())) {
                continue;
            }
            String nombre = capitalizar(campo.getName());
            assertNotNull(tipo.getMethod("get" + nombre),
                    "Falta el getter de " + tipo.getSimpleName() + "." + campo.getName());
            assertNotNull(tipo.getMethod("set" + nombre, campo.getType()),
                    "Falta el setter de " + tipo.getSimpleName() + "." + campo.getName());
        }
    }

    @Test
    void losTiposDerivadosExtiendenElSobreComun() {
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaAutenticacion.class));
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaCuentas.class));
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaCuenta.class));
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaMovimientos.class));
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaMovimiento.class));
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaTransferencia.class));
        assertTrue(RespuestaBase.class.isAssignableFrom(RespuestaCostos.class));
    }

    @Test
    void elSobreComunGuardaEstadoCodigoYMensaje() {
        RespuestaBase sobre = new RespuestaBase();

        sobre.setEstado("OK");
        sobre.setCodigo("200");
        sobre.setMensaje("Autenticación exitosa");

        assertEquals("OK", sobre.getEstado());
        assertEquals("200", sobre.getCodigo());
        assertEquals("Autenticación exitosa", sobre.getMensaje());
    }

    @Test
    void laRespuestaDeAutenticacionGuardaLosDatosDeLaSesion() {
        RespuestaAutenticacion respuesta = new RespuestaAutenticacion("OK", "200", "Autenticación exitosa");

        respuesta.setToken("TOKEN");
        respuesta.setEmpleado("0001");
        respuesta.setNombreEmpleado("Carlos Romero");
        respuesta.setExpiracion("2026-11-03T12:15:00Z");
        respuesta.setRol("Consultas");

        assertEquals("TOKEN", respuesta.getToken());
        assertEquals("0001", respuesta.getEmpleado());
        assertEquals("Carlos Romero", respuesta.getNombreEmpleado());
        assertEquals("2026-11-03T12:15:00Z", respuesta.getExpiracion());
        assertEquals("Consultas", respuesta.getRol());
        assertEquals("OK", respuesta.getEstado());
    }

    @Test
    void laListaDeCuentasEmpiezaVaciaYNoEsNula() {
        RespuestaCuentas respuesta = new RespuestaCuentas();

        assertNotNull(respuesta.getCuentas());
        assertTrue(respuesta.getCuentas().isEmpty());
    }

    @Test
    void laListaDeMovimientosEmpiezaVaciaYNoEsNula() {
        RespuestaMovimientos respuesta = new RespuestaMovimientos();

        assertNotNull(respuesta.getMovimientos());
        assertTrue(respuesta.getMovimientos().isEmpty());
    }

    @Test
    void laCuentaGuardaTodosSusDatos() {
        CuentaDTO cuenta = new CuentaDTO();

        cuenta.setCodigo("00200002");
        cuenta.setMoneda("01");
        cuenta.setDescripcionMoneda("Soles");
        cuenta.setSaldo(new BigDecimal("6800.00"));
        cuenta.setEstado("ACTIVO");
        cuenta.setCodigoCliente("00001");
        cuenta.setNombreCliente("Cliente de Prueba");
        cuenta.setCodigoSucursal("001");
        cuenta.setFechaCreacion("2020-01-01");
        cuenta.setClave("ABC123");
        cuenta.setOperacionesRealizadas(7);

        assertEquals("00200002", cuenta.getCodigo());
        assertEquals("01", cuenta.getMoneda());
        assertEquals("Soles", cuenta.getDescripcionMoneda());
        assertEquals(new BigDecimal("6800.00"), cuenta.getSaldo());
        assertEquals("ACTIVO", cuenta.getEstado());
        assertEquals("00001", cuenta.getCodigoCliente());
        assertEquals("Cliente de Prueba", cuenta.getNombreCliente());
        assertEquals("001", cuenta.getCodigoSucursal());
        assertEquals("2020-01-01", cuenta.getFechaCreacion());
        assertEquals("ABC123", cuenta.getClave());
        assertEquals(7, cuenta.getOperacionesRealizadas());
    }

    @Test
    void elMovimientoGuardaTodosSusDatos() {
        MovimientoDTO movimiento = new MovimientoDTO();

        movimiento.setNumero(3);
        movimiento.setFecha("2026-01-05");
        movimiento.setTipoCodigo("003");
        movimiento.setTipoDescripcion("Deposito");
        movimiento.setTipoAccion("INGRESO");
        movimiento.setImporte(new BigDecimal("100.00"));
        movimiento.setReferencia("00100001");
        movimiento.setEmpleado("0001");

        assertEquals(3, movimiento.getNumero());
        assertEquals("2026-01-05", movimiento.getFecha());
        assertEquals("003", movimiento.getTipoCodigo());
        assertEquals("Deposito", movimiento.getTipoDescripcion());
        assertEquals("INGRESO", movimiento.getTipoAccion());
        assertEquals(new BigDecimal("100.00"), movimiento.getImporte());
        assertEquals("00100001", movimiento.getReferencia());
        assertEquals("0001", movimiento.getEmpleado());
    }

    @Test
    void laRespuestaDeMovimientoGuardaSaldoCostoItf() {
        RespuestaMovimiento respuesta = new RespuestaMovimiento();

        respuesta.setSaldoNuevo(new BigDecimal("6900.00"));
        respuesta.setCostoAplicado(new BigDecimal("0.50"));
        respuesta.setItfAplicado(new BigDecimal("0.08"));

        assertEquals(new BigDecimal("6900.00"), respuesta.getSaldoNuevo());
        assertEquals(new BigDecimal("0.50"), respuesta.getCostoAplicado());
        assertEquals(new BigDecimal("0.08"), respuesta.getItfAplicado());
    }

    @Test
    void laRespuestaDeTransferenciaGuardaNumerosYSaldos() {
        RespuestaTransferencia respuesta = new RespuestaTransferencia();

        respuesta.setNumeroOrigen(10);
        respuesta.setNumeroDestino(25);
        respuesta.setSaldoOrigen(new BigDecimal("6775.00"));
        respuesta.setSaldoDestino(new BigDecimal("7429.00"));
        respuesta.setItfAplicado(new BigDecimal("0.02"));

        assertEquals(10, respuesta.getNumeroOrigen());
        assertEquals(25, respuesta.getNumeroDestino());
        assertEquals(new BigDecimal("6775.00"), respuesta.getSaldoOrigen());
        assertEquals(new BigDecimal("7429.00"), respuesta.getSaldoDestino());
        assertEquals(new BigDecimal("0.02"), respuesta.getItfAplicado());
    }

    @Test
    void laRespuestaDeCostosGuardaLaProyeccion() {
        RespuestaCostos respuesta = new RespuestaCostos();

        respuesta.setSaldoActual(new BigDecimal("6800.00"));
        respuesta.setCostoMantenimiento(new BigDecimal("5.00"));
        respuesta.setCostoMovimiento(new BigDecimal("0.50"));
        respuesta.setInteresProyectado(new BigDecimal("54.40"));
        respuesta.setLimiteMantenimiento(new BigDecimal("100.00"));
        respuesta.setOperacionesSinCosto(15);

        assertEquals(new BigDecimal("6800.00"), respuesta.getSaldoActual());
        assertEquals(new BigDecimal("5.00"), respuesta.getCostoMantenimiento());
        assertEquals(new BigDecimal("0.50"), respuesta.getCostoMovimiento());
        assertEquals(new BigDecimal("54.40"), respuesta.getInteresProyectado());
        assertEquals(new BigDecimal("100.00"), respuesta.getLimiteMantenimiento());
        assertEquals(15, respuesta.getOperacionesSinCosto());
    }

    @Test
    void laRespuestaDeCuentaGuardaLaCuenta() {
        RespuestaCuenta respuesta = new RespuestaCuenta();
        CuentaDTO cuenta = new CuentaDTO();
        cuenta.setCodigo("00200002");

        respuesta.setCuenta(cuenta);

        assertEquals("00200002", respuesta.getCuenta().getCodigo());
    }

    @Test
    void laRespuestaDeCuentasGuardaLosDatosDelCliente() {
        RespuestaCuentas respuesta = new RespuestaCuentas();

        respuesta.setCedula("06914897");
        respuesta.setNombreCliente("Cliente de Prueba");

        assertEquals("06914897", respuesta.getCedula());
        assertEquals("Cliente de Prueba", respuesta.getNombreCliente());
    }

    @Test
    void losNombresDeLosAtributosSonLosDelEsquema() throws Exception {
        assertEquals("estado", RespuestaBase.class.getDeclaredField("estado").getName());
        assertEquals("codigo", RespuestaBase.class.getDeclaredField("codigo").getName());
        assertEquals("mensaje", RespuestaBase.class.getDeclaredField("mensaje").getName());
        assertEquals("nombreEmpleado",
                RespuestaAutenticacion.class.getDeclaredField("nombreEmpleado").getName());
        assertEquals("operacionesRealizadas",
                CuentaDTO.class.getDeclaredField("operacionesRealizadas").getName());
        assertEquals("tipoAccion", MovimientoDTO.class.getDeclaredField("tipoAccion").getName());
        assertFalse(RespuestaBase.class.getDeclaredFields().length == 0);
    }

    private static String capitalizar(String nombre) {
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
    }
}
