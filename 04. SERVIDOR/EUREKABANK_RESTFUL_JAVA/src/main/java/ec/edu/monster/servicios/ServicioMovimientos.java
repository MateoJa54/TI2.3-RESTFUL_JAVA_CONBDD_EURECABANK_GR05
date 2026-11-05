package ec.edu.monster.servicios;

import ec.edu.monster.datos.ICostoDAO;
import ec.edu.monster.datos.ICuentaDAO;
import ec.edu.monster.datos.IMonedaDAO;
import ec.edu.monster.datos.IMovimientoDAO;
import ec.edu.monster.datos.IParametroDAO;
import ec.edu.monster.datos.ITipoMovimientoDAO;
import ec.edu.monster.datos.UnidadTrabajo;
import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import ec.edu.monster.modelos.RespuestaMovimiento;
import ec.edu.monster.modelos.RespuestaMovimientos;
import ec.edu.monster.modelos.RespuestaTransferencia;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.utilidades.FormatoDinero;
import ec.edu.monster.utilidades.Textos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de movimientos de las cuentas.
 *
 * <p>Cubre las operaciones 5, 6, 7 y 8 del contrato congelado: consulta de
 * movimientos, deposito, retiro y transferencia.
 *
 * <p>Reglas de negocio aplicadas:
 * <ul>
 * <li><b>RB-01</b>: todo importe con {@code BigDecimal} de escala 2 y redondeo
 * {@code HALF_UP}.</li>
 * <li><b>RB-02</b> y <b>RB-03</b>: cada movimiento es una transaccion unica que
 * bloquea la cuenta con {@code SELECT ... FOR UPDATE}; si algo falla se
 * deshace todo y el saldo nunca queda negativo.</li>
 * <li><b>RB-04</b>: el numero de movimiento es el contador anterior mas uno.</li>
 * <li><b>RB-05</b> a <b>RB-09</b>: deposito, retiro, transferencia con ITF y
 * cargo por movimiento.</li>
 * </ul>
 */
public class ServicioMovimientos {

    /** Tipo de movimiento del deposito. */
    public static final String TIPO_DEPOSITO = "003";

    /** Tipo de movimiento del retiro. */
    public static final String TIPO_RETIRO = "004";

    /** Tipo de movimiento del ITF. */
    public static final String TIPO_ITF = "007";

    /** Tipo de movimiento de la entrada de una transferencia. */
    public static final String TIPO_TRANSFERENCIA_INGRESO = "008";

    /** Tipo de movimiento de la salida de una transferencia. */
    public static final String TIPO_TRANSFERENCIA_SALIDA = "009";

    /** Tipo de movimiento del cargo por movimiento. */
    public static final String TIPO_COSTO_MOVIMIENTO = "010";

    /** Parametro con el porcentaje del ITF. */
    public static final String PARAMETRO_ITF = "001";

    /** Parametro con el numero de operaciones sin costo. */
    public static final String PARAMETRO_OPERACIONES_SIN_COSTO = "002";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Maximo importe admitido: el de la columna decimal(12,2) de la base de datos. */
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("9999999999.99");

    private final ICuentaDAO cuentaDAO;
    private final IMovimientoDAO movimientoDAO;
    private final ITipoMovimientoDAO tipoMovimientoDAO;
    private final IParametroDAO parametroDAO;
    private final ICostoDAO costoDAO;
    private final IMonedaDAO monedaDAO;
    private final UnidadTrabajo unidadTrabajo;
    private final TokenServicio tokenServicio;

    /**
     * Constructor con las dependencias explicitas.
     *
     * @param cuentaDAO          repositorio de cuentas
     * @param movimientoDAO      repositorio de movimientos
     * @param tipoMovimientoDAO  catalogo de tipos de movimiento
     * @param parametroDAO       repositorio de parametros de negocio
     * @param costoDAO           repositorio de costos e intereses
     * @param monedaDAO          repositorio de monedas
     * @param unidadTrabajo      unidad de trabajo transaccional
     * @param tokenServicio      registro de tokens
     */
    public ServicioMovimientos(ICuentaDAO cuentaDAO, IMovimientoDAO movimientoDAO,
            ITipoMovimientoDAO tipoMovimientoDAO, IParametroDAO parametroDAO,
            ICostoDAO costoDAO, IMonedaDAO monedaDAO, UnidadTrabajo unidadTrabajo,
            TokenServicio tokenServicio) {
        this.cuentaDAO = Objects.requireNonNull(cuentaDAO, "El repositorio de cuentas es obligatorio");
        this.movimientoDAO = Objects.requireNonNull(movimientoDAO,
                "El repositorio de movimientos es obligatorio");
        this.tipoMovimientoDAO = Objects.requireNonNull(tipoMovimientoDAO,
                "El catalogo de tipos de movimiento es obligatorio");
        this.parametroDAO = Objects.requireNonNull(parametroDAO,
                "El repositorio de parametros es obligatorio");
        this.costoDAO = Objects.requireNonNull(costoDAO, "El repositorio de costos es obligatorio");
        this.monedaDAO = Objects.requireNonNull(monedaDAO, "El repositorio de monedas es obligatorio");
        this.unidadTrabajo = Objects.requireNonNull(unidadTrabajo,
                "La unidad de trabajo transaccional es obligatoria");
        this.tokenServicio = Objects.requireNonNull(tokenServicio,
                "El registro de tokens es obligatorio");
    }

    /**
     * Operacion 5: consulta los movimientos de una cuenta.
     *
     * @param cuenta codigo de la cuenta
     * @param token  token de sesion
     * @return el sobre de respuesta
     */
    public RespuestaMovimientos consultarMovimientos(String cuenta, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.movimientosFallidos(acceso);
        }
        if (Textos.estaVacio(cuenta)) {
            return FabricaRespuestas.movimientosFallidos(CodigoRespuesta.DAT_001);
        }
        Optional<CuentaDTO> encontrada = cuentaDAO.buscarPorCodigo(cuenta.trim());
        if (encontrada.isEmpty()) {
            return FabricaRespuestas.movimientosFallidos(CodigoRespuesta.CTA_001);
        }
        if (!ServicioCuentas.ESTADO_ACTIVO.equals(encontrada.get().getEstado())) {
            return FabricaRespuestas.movimientosFallidos(CodigoRespuesta.CTA_002);
        }
        List<MovimientoDTO> movimientos = movimientoDAO.listarPorCuenta(cuenta.trim());
        return FabricaRespuestas.movimientosExitosos(cuenta.trim(), movimientos);
    }

    /**
     * Operacion 6: realiza un deposito.
     *
     * @param cuenta codigo de la cuenta que recibe el deposito
     * @param monto  importe del deposito
     * @param token  token de sesion
     * @return el sobre de respuesta
     */
    public RespuestaMovimiento realizarDeposito(String cuenta, BigDecimal monto, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.movimientoFallido(acceso);
        }
        if (Textos.estaVacio(cuenta)) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.DAT_001);
        }
        if (monto == null) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.DAT_001);
        }
        if (!montoAceptable(monto)) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.MON_001);
        }
        String codigoCuenta = cuenta.trim();
        Optional<CuentaDTO> encontrada = cuentaDAO.buscarPorCodigo(codigoCuenta);
        if (encontrada.isEmpty()) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.CTA_001);
        }
        if (!ServicioCuentas.ESTADO_ACTIVO.equals(encontrada.get().getEstado())) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.CTA_002);
        }
        if (!monedaDAO.estaSoportada(encontrada.get().getMoneda())) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.MON_002);
        }
        String empleado = empleadoDelToken(token);
        BigDecimal importe = FormatoDinero.aDosDecimales(monto);
        ResultadoOperacion resultado = unidadTrabajo.ejecutar(conexion -> {
            CuentaDTO bloqueada = cuentaDAO.bloquearPorCodigo(conexion, codigoCuenta)
                    .orElseThrow(() -> new IllegalStateException(
                            "La cuenta " + codigoCuenta + " desaparecio durante la operacion"));
            if (!ServicioCuentas.ESTADO_ACTIVO.equals(bloqueada.getEstado())) {
                return ResultadoOperacion.fallida(CodigoRespuesta.CTA_002);
            }
            int numero = bloqueada.getOperacionesRealizadas() + 1;
            BigDecimal saldo = FormatoDinero.sumar(bloqueada.getSaldo(), importe);
            MovimientoDTO movimiento = crearMovimiento(numero, TIPO_DEPOSITO, importe, null, empleado);
            movimientoDAO.insertar(conexion, codigoCuenta, movimiento);
            int contador = numero;
            BigDecimal costo = costoPorMovimiento(bloqueada, numero);
            if (costo.compareTo(FormatoDinero.CERO) > 0) {
                contador = contador + 1;
                movimientoDAO.insertar(conexion, codigoCuenta,
                        crearMovimiento(contador, TIPO_COSTO_MOVIMIENTO, costo, null, empleado));
                saldo = FormatoDinero.restar(saldo, costo);
            }
            cuentaDAO.actualizarSaldoYContador(conexion, codigoCuenta, saldo, contador);
            return ResultadoOperacion.exitosa(movimiento, saldo, costo);
        });
        if (!resultado.exitosa()) {
            return FabricaRespuestas.movimientoFallido(resultado.codigo());
        }
        return FabricaRespuestas.depositoExitoso(resultado.movimiento(), resultado.saldo(),
                resultado.costo(), FormatoDinero.CERO);
    }

    /**
     * Operacion 7: realiza un retiro.
     *
     * @param cuenta codigo de la cuenta de la que se retira el monto
     * @param monto  importe del retiro
     * @param token  token de sesion
     * @return el sobre de respuesta
     */
    public RespuestaMovimiento realizarRetiro(String cuenta, BigDecimal monto, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.movimientoFallido(acceso);
        }
        if (Textos.estaVacio(cuenta)) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.DAT_001);
        }
        if (monto == null) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.DAT_001);
        }
        if (!montoAceptable(monto)) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.MON_001);
        }
        String codigoCuenta = cuenta.trim();
        Optional<CuentaDTO> encontrada = cuentaDAO.buscarPorCodigo(codigoCuenta);
        if (encontrada.isEmpty()) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.CTA_001);
        }
        if (!ServicioCuentas.ESTADO_ACTIVO.equals(encontrada.get().getEstado())) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.CTA_002);
        }
        if (encontrada.get().getSaldo().compareTo(FormatoDinero.aDosDecimales(monto)) < 0) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.SAL_001);
        }
        if (!monedaDAO.estaSoportada(encontrada.get().getMoneda())) {
            return FabricaRespuestas.movimientoFallido(CodigoRespuesta.MON_002);
        }
        String empleado = empleadoDelToken(token);
        BigDecimal importe = FormatoDinero.aDosDecimales(monto);
        ResultadoOperacion resultado = unidadTrabajo.ejecutar(conexion -> {
            CuentaDTO bloqueada = cuentaDAO.bloquearPorCodigo(conexion, codigoCuenta)
                    .orElseThrow(() -> new IllegalStateException(
                            "La cuenta " + codigoCuenta + " desaparecio durante la operacion"));
            if (!ServicioCuentas.ESTADO_ACTIVO.equals(bloqueada.getEstado())) {
                return ResultadoOperacion.fallida(CodigoRespuesta.CTA_002);
            }
            BigDecimal saldo = bloqueada.getSaldo();
            if (saldo.compareTo(importe) < 0) {
                return ResultadoOperacion.fallida(CodigoRespuesta.SAL_001);
            }
            int numero = bloqueada.getOperacionesRealizadas() + 1;
            saldo = FormatoDinero.restar(saldo, importe);
            MovimientoDTO movimiento = crearMovimiento(numero, TIPO_RETIRO, importe, null, empleado);
            movimientoDAO.insertar(conexion, codigoCuenta, movimiento);
            int contador = numero;
            BigDecimal costo = costoPorMovimiento(bloqueada, numero);
            if (costo.compareTo(FormatoDinero.CERO) > 0) {
                contador = contador + 1;
                movimientoDAO.insertar(conexion, codigoCuenta,
                        crearMovimiento(contador, TIPO_COSTO_MOVIMIENTO, costo, null, empleado));
                saldo = FormatoDinero.restar(saldo, costo);
            }
            cuentaDAO.actualizarSaldoYContador(conexion, codigoCuenta, saldo, contador);
            return ResultadoOperacion.exitosa(movimiento, saldo, costo);
        });
        if (!resultado.exitosa()) {
            return FabricaRespuestas.movimientoFallido(resultado.codigo());
        }
        return FabricaRespuestas.retiroExitoso(resultado.movimiento(), resultado.saldo(),
                resultado.costo(), FormatoDinero.CERO);
    }

    /**
     * Operacion 8: realiza una transferencia entre dos cuentas.
     *
     * @param cuentaOrigen  codigo de la cuenta de la que sale el monto
     * @param cuentaDestino codigo de la cuenta que recibe el monto
     * @param monto         importe de la transferencia
     * @param token         token de sesion
     * @return el sobre de respuesta
     */
    public RespuestaTransferencia realizarTransferencia(String cuentaOrigen, String cuentaDestino,
            BigDecimal monto, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.transferenciaFallida(acceso);
        }
        if (Textos.estaVacio(cuentaOrigen) || Textos.estaVacio(cuentaDestino)) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.DAT_001);
        }
        if (monto == null) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.DAT_001);
        }
        if (!montoAceptable(monto)) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.MON_001);
        }
        String origen = cuentaOrigen.trim();
        String destino = cuentaDestino.trim();
        if (origen.equals(destino)) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.CTA_003);
        }
        Optional<CuentaDTO> datoOrigen = cuentaDAO.buscarPorCodigo(origen);
        if (datoOrigen.isEmpty()) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.CTA_001);
        }
        Optional<CuentaDTO> datoDestino = cuentaDAO.buscarPorCodigo(destino);
        if (datoDestino.isEmpty()) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.CTA_001);
        }
        if (!ServicioCuentas.ESTADO_ACTIVO.equals(datoOrigen.get().getEstado())
                || !ServicioCuentas.ESTADO_ACTIVO.equals(datoDestino.get().getEstado())) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.CTA_002);
        }
        if (!datoOrigen.get().getMoneda().equals(datoDestino.get().getMoneda())) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.CTA_004);
        }
        BigDecimal importe = FormatoDinero.aDosDecimales(monto);
        if (datoOrigen.get().getSaldo().compareTo(importe) < 0) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.SAL_001);
        }
        if (!monedaDAO.estaSoportada(datoOrigen.get().getMoneda())
                || !monedaDAO.estaSoportada(datoDestino.get().getMoneda())) {
            return FabricaRespuestas.transferenciaFallida(CodigoRespuesta.MON_002);
        }
        String empleado = empleadoDelToken(token);
        BigDecimal itf = calcularItf(importe);
        ResultadoTransferencia resultado = unidadTrabajo.ejecutar(conexion -> {
            // Se bloquean las dos cuentas siempre en el mismo orden para que dos
            // transferencias cruzadas no se bloqueen entre si.
            String primero = origen.compareTo(destino) < 0 ? origen : destino;
            String segundo = primero.equals(origen) ? destino : origen;
            CuentaDTO bloqueadaPrimero = cuentaDAO.bloquearPorCodigo(conexion, primero)
                    .orElseThrow(() -> new IllegalStateException(
                            "La cuenta " + primero + " desaparecio durante la operacion"));
            CuentaDTO bloqueadaSegundo = cuentaDAO.bloquearPorCodigo(conexion, segundo)
                    .orElseThrow(() -> new IllegalStateException(
                            "La cuenta " + segundo + " desaparecio durante la operacion"));
            CuentaDTO bloqueadaOrigen = primero.equals(origen) ? bloqueadaPrimero : bloqueadaSegundo;
            CuentaDTO bloqueadaDestino = primero.equals(origen) ? bloqueadaSegundo : bloqueadaPrimero;
            if (!ServicioCuentas.ESTADO_ACTIVO.equals(bloqueadaOrigen.getEstado())
                    || !ServicioCuentas.ESTADO_ACTIVO.equals(bloqueadaDestino.getEstado())) {
                return ResultadoTransferencia.fallida(CodigoRespuesta.CTA_002);
            }
            BigDecimal saldoOrigen = bloqueadaOrigen.getSaldo();
            if (saldoOrigen.compareTo(importe) < 0) {
                return ResultadoTransferencia.fallida(CodigoRespuesta.SAL_001);
            }
            int numeroOrigen = bloqueadaOrigen.getOperacionesRealizadas() + 1;
            saldoOrigen = FormatoDinero.restar(saldoOrigen, importe);
            movimientoDAO.insertar(conexion, origen,
                    crearMovimiento(numeroOrigen, TIPO_TRANSFERENCIA_SALIDA, importe, destino, empleado));
            int contadorOrigen = numeroOrigen;
            if (itf.compareTo(FormatoDinero.CERO) > 0) {
                contadorOrigen = contadorOrigen + 1;
                movimientoDAO.insertar(conexion, origen,
                        crearMovimiento(contadorOrigen, TIPO_ITF, itf, destino, empleado));
                saldoOrigen = FormatoDinero.restar(saldoOrigen, itf);
            }
            BigDecimal costoOrigen = costoPorMovimiento(bloqueadaOrigen, numeroOrigen);
            if (costoOrigen.compareTo(FormatoDinero.CERO) > 0) {
                contadorOrigen = contadorOrigen + 1;
                movimientoDAO.insertar(conexion, origen,
                        crearMovimiento(contadorOrigen, TIPO_COSTO_MOVIMIENTO, costoOrigen, null, empleado));
                saldoOrigen = FormatoDinero.restar(saldoOrigen, costoOrigen);
            }
            cuentaDAO.actualizarSaldoYContador(conexion, origen, saldoOrigen, contadorOrigen);

            int numeroDestino = bloqueadaDestino.getOperacionesRealizadas() + 1;
            BigDecimal saldoDestino = FormatoDinero.sumar(bloqueadaDestino.getSaldo(), importe);
            movimientoDAO.insertar(conexion, destino,
                    crearMovimiento(numeroDestino, TIPO_TRANSFERENCIA_INGRESO, importe, origen, empleado));
            int contadorDestino = numeroDestino;
            BigDecimal costoDestino = costoPorMovimiento(bloqueadaDestino, numeroDestino);
            if (costoDestino.compareTo(FormatoDinero.CERO) > 0) {
                contadorDestino = contadorDestino + 1;
                movimientoDAO.insertar(conexion, destino,
                        crearMovimiento(contadorDestino, TIPO_COSTO_MOVIMIENTO, costoDestino, null, empleado));
                saldoDestino = FormatoDinero.restar(saldoDestino, costoDestino);
            }
            cuentaDAO.actualizarSaldoYContador(conexion, destino, saldoDestino, contadorDestino);

            return ResultadoTransferencia.exitosa(numeroOrigen, numeroDestino,
                    saldoOrigen, saldoDestino, itf);
        });
        if (!resultado.exitosa()) {
            return FabricaRespuestas.transferenciaFallida(resultado.codigo());
        }
        return FabricaRespuestas.transferenciaExitosa(resultado.numeroOrigen(),
                resultado.numeroDestino(), resultado.saldoOrigen(), resultado.saldoDestino(),
                resultado.itf());
    }

    /**
     * Calcula el ITF de una transferencia (regla RB-08).
     *
     * <p>Solo aplica a las transferencias: {@code itf = redondeo(monto x
     * parametro '001' / 100, 2)}.
     *
     * @param importe importe de la transferencia
     * @return el ITF a cobrar
     */
    public BigDecimal calcularItf(BigDecimal importe) {
        return parametroDAO.valor(PARAMETRO_ITF)
                .map(porcentaje -> FormatoDinero.porcentaje(importe, porcentaje))
                .orElse(FormatoDinero.CERO);
    }

    /**
     * Calcula el cargo por movimiento (regla RB-09).
     *
     * <p>El movimiento numero N es sin costo si N no supera el parametro
     * {@code '002'}; si lo supera, se cobra el {@code costomovimiento} de la
     * moneda de la cuenta.
     *
     * @param cuenta          cuenta a la que pertenece el movimiento
     * @param numeroMovimiento numero del movimiento que se acaba de registrar
     * @return el importe a cobrar, {@code 0.00} si el movimiento es sin costo
     */
    public BigDecimal costoPorMovimiento(CuentaDTO cuenta, int numeroMovimiento) {
        int operacionesSinCosto = parametroDAO.valor(PARAMETRO_OPERACIONES_SIN_COSTO)
                .map(BigDecimal::intValue)
                .orElse(0);
        if (numeroMovimiento <= operacionesSinCosto) {
            return FormatoDinero.CERO;
        }
        return costoDAO.costoMovimiento(cuenta.getMoneda())
                .map(FormatoDinero::aDosDecimales)
                .orElse(FormatoDinero.CERO);
    }

    /**
     * Valida el monto de una operacion (MON-001): mayor que cero, con como
     * maximo dos decimales y no superior a {@link #MONTO_MAXIMO}.
     *
     * <p>El tope se comprueba primero: evita normalizar valores como
     * {@code 1e999} o {@code 12345678901234567890}, que pasan la comprobacion de
     * escala porque su escala es negativa.
     *
     * @param monto importe de la operacion, no nulo
     * @return {@code true} si el monto es aceptable
     */
    private static boolean montoAceptable(BigDecimal monto) {
        return monto.compareTo(MONTO_MAXIMO) <= 0 && FormatoDinero.esMontoValido(monto);
    }

    private MovimientoDTO crearMovimiento(int numero, String tipo, BigDecimal importe,
            String referencia, String empleado) {
        ITipoMovimientoDAO.TipoMovimiento tipoMovimiento = tipoMovimientoDAO
                .buscarPorCodigo(tipo)
                .orElseThrow(() -> new IllegalStateException(
                        "El tipo de movimiento " + tipo + " no existe en el catalogo"));
        return new MovimientoDTO(numero, LocalDate.now().format(FORMATO_FECHA), tipo,
                tipoMovimiento.descripcion(), tipoMovimiento.accion(),
                FormatoDinero.aDosDecimales(importe), referencia, empleado);
    }

    private String empleadoDelToken(String token) {
        return tokenServicio.obtenerSesion(token)
                .map(TokenServicio.Sesion::getEmpleado)
                .orElseThrow(() -> new IllegalStateException(
                        "El token no tiene una sesion asociada"));
    }

    /**
     * Resultado de un deposito o de un retiro.
     *
     * @param codigo    codigo de resultado de la operacion
     * @param movimiento movimiento registrado
     * @param saldo     saldo final de la cuenta
     * @param costo     cargo por movimiento aplicado
     */
    private record ResultadoOperacion(CodigoRespuesta codigo, MovimientoDTO movimiento,
            BigDecimal saldo, BigDecimal costo) {

        static ResultadoOperacion fallida(CodigoRespuesta codigo) {
            return new ResultadoOperacion(codigo, null, FormatoDinero.CERO, FormatoDinero.CERO);
        }

        static ResultadoOperacion exitosa(MovimientoDTO movimiento, BigDecimal saldo,
                BigDecimal costo) {
            return new ResultadoOperacion(CodigoRespuesta.OK_200, movimiento, saldo, costo);
        }

        boolean exitosa() {
            return codigo.esExitoso();
        }
    }

    /**
     * Resultado de una transferencia.
     *
     * @param codigo        codigo de resultado de la operacion
     * @param numeroOrigen  numero del movimiento de salida
     * @param numeroDestino numero del movimiento de ingreso
     * @param saldoOrigen   saldo final de la cuenta origen
     * @param saldoDestino  saldo final de la cuenta destino
     * @param itf           ITF descontado
     */
    private record ResultadoTransferencia(CodigoRespuesta codigo, int numeroOrigen,
            int numeroDestino, BigDecimal saldoOrigen, BigDecimal saldoDestino, BigDecimal itf) {

        static ResultadoTransferencia fallida(CodigoRespuesta codigo) {
            return new ResultadoTransferencia(codigo, 0, 0, FormatoDinero.CERO,
                    FormatoDinero.CERO, FormatoDinero.CERO);
        }

        static ResultadoTransferencia exitosa(int numeroOrigen, int numeroDestino,
                BigDecimal saldoOrigen, BigDecimal saldoDestino, BigDecimal itf) {
            return new ResultadoTransferencia(CodigoRespuesta.OK_200, numeroOrigen,
                    numeroDestino, saldoOrigen, saldoDestino, itf);
        }

        boolean exitosa() {
            return codigo.esExitoso();
        }
    }
}
