package ec.edu.monster.web.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.web.modelo.CuentaDTO;
import ec.edu.monster.web.modelo.MovimientoDTO;
import ec.edu.monster.web.modelo.RespuestaMovimientos;
import ec.edu.monster.web.servlet.CuentasServlet;
import ec.edu.monster.web.servlet.CostosServlet;
import ec.edu.monster.web.servlet.MovimientosServlet;
import ec.edu.monster.web.servlet.OperacionServlet;
import ec.edu.monster.web.util.SesionWeb;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Prefill de la cuenta por {@code ?cuenta=} en operaciones, resúmenes de movimientos y cuentas, y
 * datos de la tarjeta de resultado que se guardan con el mensaje de una sola vez.
 */
class PrefillYResumenTest {

    private static final String VISTA_OPERACION = "/WEB-INF/vistas/operacion.jsp";

    private final ClienteFalso servidor = new ClienteFalso();

    /** Petición GET con sesión vigente. Las llamadas de la autenticación no cuentan. */
    private ServletFalso get(String ruta, String cuenta) throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("GET").ruta(ruta);
        if (cuenta != null) {
            f.parametro("cuenta", cuenta);
        }
        servidor.llamadas.clear();
        return f;
    }

    // ------------------------------------------------------------------ prefill de operación

    @Test
    void operacionPrefillaUnaCuentaValidaSinLlamarAlServicio() throws Exception {
        ServletFalso f = get("/operacion", ClienteFalso.CUENTA);
        f.ejecutar(new OperacionServlet());
        assertEquals(VISTA_OPERACION, f.reenvio());
        assertEquals(ClienteFalso.CUENTA, f.atributo("consulta"));
        assertNull(f.atributo("mensaje"));
        assertTrue(servidor.llamadas.isEmpty(), "abrir la operación con cuenta no consulta nada");
    }

    @Test
    void operacionSinParametroNoPrefillaNada() throws Exception {
        ServletFalso f = get("/operacion", null);
        f.ejecutar(new OperacionServlet());
        assertEquals("", f.atributo("consulta"));
        assertNull(f.atributo("mensaje"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"  " + ClienteFalso.CUENTA + "  ", "\t" + ClienteFalso.CUENTA + "\r\n"})
    void operacionRecortaEspaciosAlPrefillar(String valor) throws Exception {
        ServletFalso f = get("/operacion", valor);
        f.ejecutar(new OperacionServlet());
        assertEquals(ClienteFalso.CUENTA, f.atributo("consulta"));
        assertNull(f.atributo("mensaje"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "\u0001", "\u001b", "\u007f", "\r", "\n"})
    void operacionQuitaLosCaracteresDeControlAntesDePrefillar(String control) throws Exception {
        String valor = ClienteFalso.CUENTA.substring(0, 4) + control + ClienteFalso.CUENTA.substring(4);
        ServletFalso f = get("/operacion", valor);
        f.ejecutar(new OperacionServlet());
        assertEquals(ClienteFalso.CUENTA, f.atributo("consulta"));
        assertNull(f.atributo("mensaje"));
    }

    @Test
    void operacionConCuentaDeSoloControlesPideElCodigo() throws Exception {
        ServletFalso f = get("/operacion", "\u0000\t");
        f.ejecutar(new OperacionServlet());
        assertEquals("", f.atributo("consulta"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("error", mensaje.get("tipo"));
        assertEquals("Ingrese el código de la cuenta", mensaje.get("texto"));
    }

    @Test
    void operacionConCuentaVaciaPideElCodigo() throws Exception {
        ServletFalso f = get("/operacion", "");
        f.ejecutar(new OperacionServlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("Ingrese el código de la cuenta", mensaje.get("texto"));
        assertEquals("", f.atributo("consulta"));
    }

    @Test
    void operacionConCuentaMuyLargaNoPrefillaYDaElMensajeDeFormato() throws Exception {
        ServletFalso f = get("/operacion", "9".repeat(65));
        f.ejecutar(new OperacionServlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.atributo("mensaje");
        assertEquals("error", mensaje.get("tipo"));
        assertEquals("El código de cuenta ingresado no tiene un formato válido", mensaje.get("texto"));
        assertEquals("9".repeat(65), f.atributo("consulta"), "el formulario conserva lo escrito, como en los demás módulos");
        assertTrue(servidor.llamadas.isEmpty(), "un valor fuera de formato no llega al servicio");
    }

    @Test
    void operacionConHtmlSeDejaIntactoParaQueLaVistaLoEscape() throws Exception {
        String hostil = "<script>alert(1)</script>";
        ServletFalso f = get("/operacion", hostil);
        f.ejecutar(new OperacionServlet());
        assertEquals(hostil, f.atributo("consulta"), "el servlet no altera el texto: operacion.jsp lo escapa con c:out");
        assertNull(f.atributo("mensaje"));
        assertTrue(servidor.llamadas.isEmpty());
    }

    @Test
    void laCuentaDelMensajePendienteTienePrioridadSobreElParametro() throws Exception {
        ServletFalso f = get("/operacion", ClienteFalso.CUENTA_DOLARES);
        SesionWeb.guardarMensaje(f.peticion(), mensajeConCuenta(ClienteFalso.CUENTA));
        f.ejecutar(new OperacionServlet());
        assertEquals(ClienteFalso.CUENTA, f.atributo("consulta"));
        assertNotNull(f.atributo("mensaje"));
    }

    private static Map<String, Object> mensajeConCuenta(String cuenta) {
        Map<String, Object> mensaje = SesionWeb.mensajeError("MON-001", "El monto debe ser mayor que cero");
        mensaje.put("cuenta", cuenta);
        mensaje.put("modo", "deposito");
        return mensaje;
    }

    // ------------------------------------------------------------------ resultado de la operación

    @Test
    void elDepositoGuardaElNumeroDeMovimientoConElMensajeDelServidor() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("GET").ruta("/operacion");
        f.ejecutar(new OperacionServlet());
        String envio = (String) f.atributo("envio");
        f.metodo("POST").ruta("/operacion").parametro("modo", "deposito")
                .parametro("cuenta", ClienteFalso.CUENTA).parametro("destino", "")
                .parametro("monto", "150.00").parametro("envio", envio)
                .parametro(SesionWeb.CAMPO_CSRF, f.csrf());
        f.ejecutar(new OperacionServlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> mensaje = (Map<String, Object>) f.flash();
        assertEquals("exito", mensaje.get("tipo"));
        assertEquals(8, mensaje.get("numero"));
        assertEquals("Depósito realizado correctamente", mensaje.get("texto"));
        assertEquals(new BigDecimal("6900.00"), mensaje.get("saldoNuevo"));
        assertEquals(new BigDecimal("0"), mensaje.get("costoAplicado"));
        assertEquals(new BigDecimal("0"), mensaje.get("itfAplicado"));
        assertNull(mensaje.get("saldoOrigen"), "un depósito no lleva datos de transferencia");
    }

    @Test
    void laTransferenciaGuardaLosDosNumerosDeMovimiento() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("GET").ruta("/operacion");
        f.ejecutar(new OperacionServlet());
        String envio = (String) f.atributo("envio");
        f.metodo("POST").ruta("/operacion").parametro("modo", "transferencia")
                .parametro("cuenta", ClienteFalso.CUENTA).parametro("destino", "00300003")
                .parametro("monto", "100.00").parametro("envio", envio)
                .parametro(SesionWeb.CAMPO_CSRF, f.csrf());
        f.ejecutar(new OperacionServlet());
        Map<String, Object> mensaje = f.flash();
        assertEquals(10, mensaje.get("numeroOrigen"));
        assertEquals(25, mensaje.get("numeroDestino"));
        assertNull(mensaje.get("numero"));
    }

    // ------------------------------------------------------------------ resumen de movimientos

    private static MovimientoDTO movimiento(String tipo, String importe) {
        MovimientoDTO m = new MovimientoDTO();
        m.setNumero(1);
        m.setFecha("2026-11-03");
        m.setTipoAccion(tipo);
        m.setImporte(new BigDecimal(importe));
        return m;
    }

    @Test
    void elResumenDeUnaListaVaciaEsCero() {
        Map<String, Object> resumen = MovimientosServlet.resumir(List.of());
        assertEquals(BigDecimal.ZERO, resumen.get("ingresos"));
        assertEquals(BigDecimal.ZERO, resumen.get("salidas"));
        assertEquals(0, resumen.get("cantidad"));
    }

    @Test
    void elResumenDeUnaListaNulaTambienEsCero() {
        Map<String, Object> resumen = MovimientosServlet.resumir(null);
        assertEquals(0, resumen.get("cantidad"));
        assertEquals(BigDecimal.ZERO, resumen.get("ingresos"));
    }

    @Test
    void elResumenSumaSoloIngresos() {
        Map<String, Object> resumen = MovimientosServlet.resumir(List.of(
                movimiento("INGRESO", "1000.00"), movimiento("INGRESO", "250.50")));
        assertEquals(new BigDecimal("1250.50"), resumen.get("ingresos"));
        assertEquals(BigDecimal.ZERO, resumen.get("salidas"));
        assertEquals(2, resumen.get("cantidad"));
    }

    @Test
    void elResumenSumaSoloSalidas() {
        Map<String, Object> resumen = MovimientosServlet.resumir(List.of(
                movimiento("SALIDA", "40.00"), movimiento("SALIDA", "0.25")));
        assertEquals(BigDecimal.ZERO, resumen.get("ingresos"));
        assertEquals(new BigDecimal("40.25"), resumen.get("salidas"));
        assertEquals(2, resumen.get("cantidad"));
    }

    @Test
    void elResumenSeparaIngresosYSalidasYCuentaTodo() {
        Map<String, Object> resumen = MovimientosServlet.resumir(List.of(
                movimiento("INGRESO", "100.00"), movimiento("SALIDA", "30.00"),
                movimiento("OTRO", "5.00")));
        assertEquals(new BigDecimal("100.00"), resumen.get("ingresos"));
        assertEquals(new BigDecimal("30.00"), resumen.get("salidas"));
        assertEquals(3, resumen.get("cantidad"), "los movimientos de otro tipo cuentan, pero no suman");
    }

    @Test
    void elResumenIgnoraMovimientosNulosYSinImporte() {
        MovimientoDTO sinImporte = new MovimientoDTO();
        sinImporte.setTipoAccion("INGRESO");
        Map<String, Object> resumen = MovimientosServlet.resumir(java.util.Arrays.asList(
                null, sinImporte, movimiento("INGRESO", "10.00")));
        assertEquals(new BigDecimal("10.00"), resumen.get("ingresos"));
        assertEquals(2, resumen.get("cantidad"));
    }

    @Test
    void laConsultaDeMovimientosDejaElResumenEnLaPeticion() throws Exception {
        ClienteFalso soloSalidas = new ClienteFalso() {
            @Override
            public RespuestaMovimientos consultarMovimientos(String cuenta, String token) {
                RespuestaMovimientos r = new RespuestaMovimientos("OK", "200", "Movimientos consultados correctamente");
                r.setCuenta(cuenta);
                r.setMovimientos(List.of(movimiento("SALIDA", "12.00")));
                return r;
            }
        };
        ServletFalso f = ServletFalso.crear().cliente(soloSalidas).conUsuarioConToken("TOKEN-X")
                .metodo("GET").ruta("/movimientos").parametro("cuenta", ClienteFalso.CUENTA);
        f.ejecutar(new MovimientosServlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> resumen = (Map<String, Object>) f.atributo("resumen");
        assertEquals(BigDecimal.ZERO, resumen.get("ingresos"));
        assertEquals(new BigDecimal("12.00"), resumen.get("salidas"));
        assertEquals(1, resumen.get("cantidad"));
    }

    @Test
    void unaConsultaConErrorNoDejaResumen() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("GET").ruta("/movimientos").parametro("cuenta", "99999999");
        f.ejecutar(new MovimientosServlet());
        assertNull(f.atributo("resumen"));
        assertNull(f.atributo("resultado"));
    }

    // ------------------------------------------------------------------ resumen de cuentas

    private static CuentaDTO cuenta(String moneda, String descripcion, String saldo) {
        CuentaDTO c = new CuentaDTO();
        c.setCodigo("00100001");
        c.setMoneda(moneda);
        c.setDescripcionMoneda(descripcion);
        c.setSaldo(saldo == null ? null : new BigDecimal(saldo));
        return c;
    }

    @Test
    void elResumenDeCuentasSinCuentasEsCeroYSinSaldos() {
        Map<String, Object> resumen = CuentasServlet.resumir(List.of());
        assertEquals(0, resumen.get("cantidad"));
        assertTrue(((Map<?, ?>) resumen.get("saldos")).isEmpty());
    }

    @Test
    void elResumenSumaElSaldoPorMoneda() {
        Map<String, Object> resumen = CuentasServlet.resumir(List.of(
                cuenta("01", "Soles", "100.00"), cuenta("01", "Soles", "50.25"),
                cuenta("02", "Dólares", "9.00")));
        assertEquals(3, resumen.get("cantidad"));
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> saldos = (Map<String, BigDecimal>) resumen.get("saldos");
        assertEquals(new BigDecimal("150.25"), saldos.get("Soles"));
        assertEquals(new BigDecimal("9.00"), saldos.get("Dólares"));
        assertEquals(List.of("Dólares", "Soles"), List.copyOf(saldos.keySet()), "orden alfabético");
    }

    @Test
    void unaMonedaSinDescripcionUsaSuCodigoYUnSaldoNuloNoSuma() {
        Map<String, Object> resumen = CuentasServlet.resumir(List.of(
                cuenta("03", null, "7.00"), cuenta("03", "", "1.00"), cuenta("04", "Euros", null)));
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> saldos = (Map<String, BigDecimal>) resumen.get("saldos");
        assertEquals(new BigDecimal("8.00"), saldos.get("03"));
        assertNull(saldos.get("Euros"), "una cuenta sin saldo no aparece en el total");
        assertEquals(3, resumen.get("cantidad"));
    }

    @Test
    void laConsultaDeCuentasDejaElResumenEnLaPeticion() throws Exception {
        ServletFalso f = ServletFalso.crear().cliente(servidor).conUsuarioAutenticado(servidor)
                .metodo("GET").ruta("/cuentas").parametro("cedula", ClienteFalso.CEDULA);
        f.ejecutar(new CuentasServlet());
        @SuppressWarnings("unchecked")
        Map<String, Object> resumen = (Map<String, Object>) f.atributo("resumen");
        assertEquals(1, resumen.get("cantidad"));
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> saldos = (Map<String, BigDecimal>) resumen.get("saldos");
        assertEquals(new BigDecimal("6800.00"), saldos.get("Soles"));
    }

    // ------------------------------------------------------------------ costos con cuenta

    @Test
    void costosPrefillaLaCuentaDeLaPeticion() throws Exception {
        ServletFalso f = get("/costos", ClienteFalso.CUENTA);
        f.ejecutar(new CostosServlet());
        assertEquals(ClienteFalso.CUENTA, f.atributo("consulta"));
        assertNotNull(f.atributo("resultado"));
    }
}
