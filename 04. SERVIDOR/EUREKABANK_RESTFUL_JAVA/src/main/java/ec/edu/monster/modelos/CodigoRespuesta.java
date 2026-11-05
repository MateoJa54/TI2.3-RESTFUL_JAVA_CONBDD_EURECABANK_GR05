package ec.edu.monster.modelos;

/**
 * Catalogo de los 17 codigos de resultado del servicio con su mensaje literal.
 *
 * <p>Los mensajes son exactamente los de la seccion 5 del contrato congelado:
 * no se admiten variantes. El codigo {@code 200} es el unico cuyo mensaje
 * depende de la operacion, por eso el enum guarda un texto generico y es
 * {@code FabricaRespuestas} quien coloca el mensaje propio de cada operacion
 * (vease la tabla de mensajes de exito del contrato).
 */
public enum CodigoRespuesta {

    /** Operacion correcta. El mensaje concreto lo pone la fabrica de respuestas. */
    OK_200("OK", "200", "Operación realizada correctamente"),

    /** Los datos de la peticion estan incompletos o son incorrectos. */
    DAT_001("ERROR", "DAT-001", "Los datos de la petición están incompletos o son incorrectos"),

    /** El usuario o la contrasena son incorrectos. */
    AUTH_001("ERROR", "AUTH-001", "El usuario o la contraseña son incorrectos"),

    /** El usuario se encuentra inactivo. */
    AUTH_002("ERROR", "AUTH-002", "El usuario se encuentra inactivo"),

    /** El token de sesion no es valido. */
    AUTH_003("ERROR", "AUTH-003", "El token de sesión no es válido"),

    /** El token de sesion ha expirado. */
    AUTH_004("ERROR", "AUTH-004", "El token de sesión ha expirado, inicie sesión nuevamente"),

    /** La cuenta solicitada no existe. */
    CTA_001("ERROR", "CTA-001", "La cuenta solicitada no existe"),

    /** La cuenta no esta en estado ACTIVO. */
    CTA_002("ERROR", "CTA-002", "La cuenta no se encuentra en estado ACTIVO"),

    /** La cuenta origen y la cuenta destino son iguales. */
    CTA_003("ERROR", "CTA-003", "La cuenta origen y la cuenta destino no pueden ser iguales"),

    /** La cuenta destino pertenece a otra moneda. */
    CTA_004("ERROR", "CTA-004", "La cuenta de destino pertenece a una moneda distinta"),

    /** El cliente solicitado no existe. */
    CLI_001("ERROR", "CLI-001", "El cliente solicitado no existe"),

    /** El saldo de la cuenta es insuficiente. */
    SAL_001("ERROR", "SAL-001", "El saldo de la cuenta es insuficiente para la operación"),

    /** El monto es menor o igual que cero o tiene mas de dos decimales. */
    MON_001("ERROR", "MON-001", "El monto debe ser mayor que cero y con máximo dos decimales"),

    /** La moneda de la cuenta no esta soportada por el sistema. */
    MON_002("ERROR", "MON-002", "La moneda de la cuenta no está soportada por el sistema"),

    /** El monto supera el limite maximo de mantenimiento de la cuenta. */
    LIM_001("ERROR", "LIM-001", "El monto supera el límite máximo de mantenimiento de la cuenta"),

    /** El servicio no esta disponible. */
    SER_001("ERROR", "SER-001", "El servicio no está disponible en este momento"),

    /** Error interno no previsto. */
    ERR_001("ERROR", "ERR-001", "Ocurrió un error interno, intente nuevamente");

    private final String estado;
    private final String codigo;
    private final String mensaje;

    CodigoRespuesta(String estado, String codigo, String mensaje) {
        this.estado = estado;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    /**
     * @return {@code OK} o {@code ERROR}
     */
    public String getEstado() {
        return estado;
    }

    /**
     * @return el codigo de resultado, por ejemplo {@code AUTH-003}
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * @return el mensaje literal del catalogo
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Indica si este codigo representa una operacion correcta.
     *
     * @return {@code true} solo para {@link #OK_200}
     */
    public boolean esExitoso() {
        return this == OK_200;
    }
}
