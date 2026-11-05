package ec.edu.monster.servicios;

import ec.edu.monster.datos.IClienteDAO;
import ec.edu.monster.datos.ICuentaDAO;
import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.RespuestaCuenta;
import ec.edu.monster.modelos.RespuestaCuentas;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.utilidades.Textos;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de consulta de cuentas.
 *
 * <p>Cubre las operaciones 3 y 4 del contrato congelado:
 * {@code listarCuentasPorCliente} y {@code consultarCuenta}.
 *
 * <p>Respeta el orden de validacion de la seccion 6: primero el token
 * ({@code DAT-001}, {@code AUTH-003}, {@code AUTH-004}), despues los datos de
 * la operacion ({@code DAT-001}) y por ultimo los datos del banco
 * ({@code CLI-001}, {@code CTA-001}, {@code CTA-002}).
 */
public class ServicioCuentas {

    /** Estado de una cuenta operable. */
    public static final String ESTADO_ACTIVO = "ACTIVO";

    private final IClienteDAO clienteDAO;
    private final ICuentaDAO cuentaDAO;
    private final TokenServicio tokenServicio;

    /**
     * Constructor con las dependencias explicitas.
     *
     * @param clienteDAO    repositorio de clientes
     * @param cuentaDAO     repositorio de cuentas
     * @param tokenServicio registro de tokens
     */
    public ServicioCuentas(IClienteDAO clienteDAO, ICuentaDAO cuentaDAO,
            TokenServicio tokenServicio) {
        this.clienteDAO = Objects.requireNonNull(clienteDAO, "El repositorio de clientes es obligatorio");
        this.cuentaDAO = Objects.requireNonNull(cuentaDAO, "El repositorio de cuentas es obligatorio");
        this.tokenServicio = Objects.requireNonNull(tokenServicio,
                "El registro de tokens es obligatorio");
    }

    /**
     * Operacion 3: lista las cuentas del cliente con la cedula indicada,
     * incluidas las que no estan ACTIVO (su estado queda visible).
     *
     * @param cedula cedula del cliente
     * @param token  token de sesion
     * @return el sobre de respuesta
     */
    public RespuestaCuentas listarCuentasPorCliente(String cedula, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.cuentasFallidas(acceso);
        }
        if (Textos.estaVacio(cedula)) {
            return FabricaRespuestas.cuentasFallidas(CodigoRespuesta.DAT_001);
        }
        Optional<IClienteDAO.DatosCliente> cliente = clienteDAO.buscarPorDni(cedula.trim());
        if (cliente.isEmpty()) {
            return FabricaRespuestas.cuentasFallidas(CodigoRespuesta.CLI_001);
        }
        List<CuentaDTO> cuentas = cuentaDAO.listarPorCliente(cliente.get().codigo());
        return FabricaRespuestas.cuentasExitosas(cedula.trim(), cliente.get().nombre(), cuentas);
    }

    /**
     * Operacion 4: consulta una cuenta por su codigo.
     *
     * @param cuenta codigo de la cuenta
     * @param token  token de sesion
     * @return el sobre de respuesta
     */
    public RespuestaCuenta consultarCuenta(String cuenta, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.cuentaFallida(acceso);
        }
        if (Textos.estaVacio(cuenta)) {
            return FabricaRespuestas.cuentaFallida(CodigoRespuesta.DAT_001);
        }
        Optional<CuentaDTO> encontrada = cuentaDAO.buscarPorCodigo(cuenta.trim());
        if (encontrada.isEmpty()) {
            return FabricaRespuestas.cuentaFallida(CodigoRespuesta.CTA_001);
        }
        if (!ESTADO_ACTIVO.equals(encontrada.get().getEstado())) {
            return FabricaRespuestas.cuentaFallida(CodigoRespuesta.CTA_002);
        }
        return FabricaRespuestas.cuentaExitosa(encontrada.get());
    }
}
