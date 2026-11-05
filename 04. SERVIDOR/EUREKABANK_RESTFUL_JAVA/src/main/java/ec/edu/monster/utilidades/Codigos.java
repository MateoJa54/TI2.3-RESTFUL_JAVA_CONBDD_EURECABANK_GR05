package ec.edu.monster.utilidades;

/**
 * Constantes de los mensajes de resultado del servicio.
 *
 * <p>Los mensajes de error viven en el enum {@code CodigoRespuesta}; aqui
 * viven los mensajes de exito, que dependen de la operacion (seccion 5 del
 * contrato congelado) y por eso no pueden estar en el enum.
 */
public final class Codigos {

    /** Estado de una operacion correcta. */
    public static final String ESTADO_OK = "OK";

    /** Estado de una operacion con error. */
    public static final String ESTADO_ERROR = "ERROR";

    /** Codigo de exito del catalogo. */
    public static final String CODIGO_OK = "200";

    /** Mensaje de exito de {@code autenticar}. */
    public static final String MENSAJE_AUTENTICACION_OK = "Autenticación exitosa";

    /** Mensaje de exito de {@code cerrarSesion}. */
    public static final String MENSAJE_CERRAR_SESION_OK = "Sesión cerrada correctamente";

    /** Mensaje de exito de {@code listarCuentasPorCliente}. */
    public static final String MENSAJE_CUENTAS_OK = "Cuentas consultadas correctamente";

    /** Mensaje de exito de {@code consultarCuenta}. */
    public static final String MENSAJE_CUENTA_OK = "Cuenta consultada correctamente";

    /** Mensaje de exito de {@code consultarMovimientos}. */
    public static final String MENSAJE_MOVIMIENTOS_OK = "Movimientos consultados correctamente";

    /** Mensaje de exito de {@code realizarDeposito}. */
    public static final String MENSAJE_DEPOSITO_OK = "Depósito realizado correctamente";

    /** Mensaje de exito de {@code realizarRetiro}. */
    public static final String MENSAJE_RETIRO_OK = "Retiro realizado correctamente";

    /** Mensaje de exito de {@code realizarTransferencia}. */
    public static final String MENSAJE_TRANSFERENCIA_OK = "Transferencia realizada correctamente";

    /** Mensaje de exito de {@code consultarCostosMantenimiento}. */
    public static final String MENSAJE_COSTOS_OK = "Costos de mantenimiento consultados correctamente";

    /** Formato de fecha (sin hora) del contrato. */
    public static final String FORMATO_FECHA = "yyyy-MM-dd";

    /** Formato de fecha y hora del contrato. */
    public static final String FORMATO_FECHA_HORA = "yyyy-MM-dd'T'HH:mm:ss";

    private Codigos() {
    }
}
