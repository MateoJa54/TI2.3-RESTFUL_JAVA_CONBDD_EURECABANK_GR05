package ec.edu.monster.servicios;

import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.MovimientoDTO;
import ec.edu.monster.modelos.RespuestaAutenticacion;
import ec.edu.monster.modelos.RespuestaBase;
import ec.edu.monster.modelos.RespuestaCostos;
import ec.edu.monster.modelos.RespuestaCuenta;
import ec.edu.monster.modelos.RespuestaCuentas;
import ec.edu.monster.modelos.RespuestaMovimiento;
import ec.edu.monster.modelos.RespuestaMovimientos;
import ec.edu.monster.modelos.RespuestaTransferencia;
import ec.edu.monster.utilidades.Codigos;
import ec.edu.monster.utilidades.FormatoDinero;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Fabrica de respuestas del servicio.
 *
 * <p>Es el unico lugar donde se arman los sobres de respuesta: asi el estado,
 * el codigo y el mensaje salen siempre del catalogo congelado y nunca se
 * escriben a mano dentro de la logica de negocio.
 */
public final class FabricaRespuestas {

    private FabricaRespuestas() {
    }

    /**
     * Respuesta de autenticacion correcta.
     *
     * @param token          token de sesion generado
     * @param empleado       codigo del empleado autenticado
     * @param nombreEmpleado nombre completo del empleado
     * @param expiracion     expiracion del token en ISO-8601
     * @param rol            rol o modulo de acceso
     * @return el sobre de exito
     */
    public static RespuestaAutenticacion autenticacionExitosa(String token, String empleado,
            String nombreEmpleado, String expiracion, String rol) {
        return new RespuestaAutenticacion(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_AUTENTICACION_OK, token, empleado, nombreEmpleado, expiracion, rol);
    }

    /**
     * Respuesta de autenticacion con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error
     */
    public static RespuestaAutenticacion autenticacionFallida(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaAutenticacion(codigo.getEstado(), codigo.getCodigo(),
                codigo.getMensaje(), null, null, null, null, null);
    }

    /**
     * Respuesta de cierre de sesion correcto.
     *
     * @return el sobre de exito
     */
    public static RespuestaBase cierreSesionExitoso() {
        return new RespuestaBase(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_CERRAR_SESION_OK);
    }

    /**
     * Respuesta de cierre de sesion con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error
     */
    public static RespuestaBase cierreSesionFallido(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaBase(codigo.getEstado(), codigo.getCodigo(), codigo.getMensaje());
    }

    /**
     * Respuesta de listado de cuentas correcto.
     *
     * @param cedula        cedula del cliente consultado
     * @param nombreCliente nombre completo del cliente
     * @param cuentas       cuentas del cliente
     * @return el sobre de exito
     */
    public static RespuestaCuentas cuentasExitosas(String cedula, String nombreCliente,
            List<CuentaDTO> cuentas) {
        return new RespuestaCuentas(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_CUENTAS_OK, cedula, nombreCliente, cuentas);
    }

    /**
     * Respuesta de listado de cuentas con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error con la lista de cuentas vacia
     */
    public static RespuestaCuentas cuentasFallidas(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaCuentas(codigo.getEstado(), codigo.getCodigo(), codigo.getMensaje(),
                null, null, List.of());
    }

    /**
     * Respuesta de consulta de cuenta correcta.
     *
     * @param cuenta datos de la cuenta
     * @return el sobre de exito
     */
    public static RespuestaCuenta cuentaExitosa(CuentaDTO cuenta) {
        return new RespuestaCuenta(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_CUENTA_OK, cuenta);
    }

    /**
     * Respuesta de consulta de cuenta con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error
     */
    public static RespuestaCuenta cuentaFallida(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaCuenta(codigo.getEstado(), codigo.getCodigo(), codigo.getMensaje(), null);
    }

    /**
     * Respuesta de consulta de movimientos correcta.
     *
     * @param cuenta      codigo de la cuenta consultada
     * @param movimientos movimientos de la cuenta
     * @return el sobre de exito
     */
    public static RespuestaMovimientos movimientosExitosos(String cuenta,
            List<MovimientoDTO> movimientos) {
        return new RespuestaMovimientos(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_MOVIMIENTOS_OK, cuenta, movimientos);
    }

    /**
     * Respuesta de consulta de movimientos con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error con la lista de movimientos vacia
     */
    public static RespuestaMovimientos movimientosFallidos(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaMovimientos(codigo.getEstado(), codigo.getCodigo(),
                codigo.getMensaje(), null, List.of());
    }

    /**
     * Respuesta de deposito correcta.
     *
     * @param movimiento    movimiento registrado
     * @param saldoNuevo    saldo posterior a la operacion
     * @param costoAplicado costo por movimiento aplicado
     * @param itfAplicado   impuesto aplicado
     * @return el sobre de exito
     */
    public static RespuestaMovimiento depositoExitoso(MovimientoDTO movimiento,
            BigDecimal saldoNuevo, BigDecimal costoAplicado, BigDecimal itfAplicado) {
        return new RespuestaMovimiento(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_DEPOSITO_OK, movimiento,
                FormatoDinero.aDosDecimales(saldoNuevo),
                FormatoDinero.aDosDecimales(costoAplicado),
                FormatoDinero.aDosDecimales(itfAplicado));
    }

    /**
     * Respuesta de deposito o retiro con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error
     */
    public static RespuestaMovimiento movimientoFallido(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaMovimiento(codigo.getEstado(), codigo.getCodigo(),
                codigo.getMensaje(), null, FormatoDinero.CERO, FormatoDinero.CERO,
                FormatoDinero.CERO);
    }

    /**
     * Respuesta de retiro correcta (mismo sobre que el deposito, con su propio
     * mensaje de exito).
     *
     * @param movimiento    movimiento registrado
     * @param saldoNuevo    saldo posterior a la operacion
     * @param costoAplicado costo por movimiento aplicado
     * @param itfAplicado   impuesto aplicado
     * @return el sobre de exito
     */
    public static RespuestaMovimiento retiroExitoso(MovimientoDTO movimiento,
            BigDecimal saldoNuevo, BigDecimal costoAplicado, BigDecimal itfAplicado) {
        return new RespuestaMovimiento(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_RETIRO_OK, movimiento,
                FormatoDinero.aDosDecimales(saldoNuevo),
                FormatoDinero.aDosDecimales(costoAplicado),
                FormatoDinero.aDosDecimales(itfAplicado));
    }

    /**
     * Respuesta de transferencia correcta.
     *
     * @param numeroOrigen  numero del movimiento de salida
     * @param numeroDestino numero del movimiento de ingreso
     * @param saldoOrigen   saldo final de la cuenta origen
     * @param saldoDestino  saldo final de la cuenta destino
     * @param itfAplicado   impuesto descontado
     * @return el sobre de exito
     */
    public static RespuestaTransferencia transferenciaExitosa(int numeroOrigen,
            int numeroDestino, BigDecimal saldoOrigen, BigDecimal saldoDestino,
            BigDecimal itfAplicado) {
        return new RespuestaTransferencia(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_TRANSFERENCIA_OK, numeroOrigen, numeroDestino,
                FormatoDinero.aDosDecimales(saldoOrigen),
                FormatoDinero.aDosDecimales(saldoDestino),
                FormatoDinero.aDosDecimales(itfAplicado));
    }

    /**
     * Respuesta de transferencia con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error
     */
    public static RespuestaTransferencia transferenciaFallida(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaTransferencia(codigo.getEstado(), codigo.getCodigo(),
                codigo.getMensaje(), 0, 0, FormatoDinero.CERO, FormatoDinero.CERO,
                FormatoDinero.CERO);
    }

    /**
     * Respuesta de consulta de costos correcta.
     *
     * @param saldoActual         saldo actual de la cuenta
     * @param costoMantenimiento  costo de mantenimiento proyectado
     * @param costoMovimiento     costo por movimiento
     * @param interesProyectado   interes mensual proyectado
     * @param limiteMantenimiento umbral de cobro de mantenimiento
     * @param operacionesSinCosto operaciones sin costo permitidas
     * @return el sobre de exito
     */
    public static RespuestaCostos costosExitosos(BigDecimal saldoActual,
            BigDecimal costoMantenimiento, BigDecimal costoMovimiento,
            BigDecimal interesProyectado, BigDecimal limiteMantenimiento,
            int operacionesSinCosto) {
        return new RespuestaCostos(Codigos.ESTADO_OK, Codigos.CODIGO_OK,
                Codigos.MENSAJE_COSTOS_OK,
                FormatoDinero.aDosDecimales(saldoActual),
                FormatoDinero.aDosDecimales(costoMantenimiento),
                FormatoDinero.aDosDecimales(costoMovimiento),
                FormatoDinero.aDosDecimales(interesProyectado),
                FormatoDinero.aDosDecimales(limiteMantenimiento),
                operacionesSinCosto);
    }

    /**
     * Respuesta de consulta de costos con error.
     *
     * @param codigo codigo de error del catalogo
     * @return el sobre de error
     */
    public static RespuestaCostos costosFallidos(CodigoRespuesta codigo) {
        exigirError(codigo);
        return new RespuestaCostos(codigo.getEstado(), codigo.getCodigo(), codigo.getMensaje(),
                FormatoDinero.CERO, FormatoDinero.CERO, FormatoDinero.CERO, FormatoDinero.CERO,
                FormatoDinero.CERO, 0);
    }

    private static void exigirError(CodigoRespuesta codigo) {
        Objects.requireNonNull(codigo, "El codigo de error es obligatorio");
        if (codigo.esExitoso()) {
            throw new IllegalArgumentException("El codigo " + codigo + " no es un codigo de error");
        }
    }
}
