package ec.edu.monster.servicios;

import ec.edu.monster.datos.ICostoDAO;
import ec.edu.monster.datos.ICuentaDAO;
import ec.edu.monster.datos.IMonedaDAO;
import ec.edu.monster.datos.IParametroDAO;
import ec.edu.monster.modelos.CodigoRespuesta;
import ec.edu.monster.modelos.CuentaDTO;
import ec.edu.monster.modelos.RespuestaCostos;
import ec.edu.monster.seguridad.TokenServicio;
import ec.edu.monster.utilidades.FormatoDinero;
import ec.edu.monster.utilidades.Textos;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de proyeccion de costos de mantenimiento.
 *
 * <p>Cubre la operacion 9 del contrato congelado:
 * {@code consultarCostosMantenimiento}.
 *
 * <p>Reglas RB-10, RB-11 y RB-12: el cargo por mantenimiento se proyecta
 * cuando el saldo no supera el umbral de la moneda, el interes mensual se
 * proyecta como {@code saldo x interes / 100} y el cobro real lo hace un
 * proceso batch que queda fuera del alcance de este servicio.
 */
public class ServicioCostos {

    private static final String PARAMETRO_OPERACIONES_SIN_COSTO = "002";

    private final ICuentaDAO cuentaDAO;
    private final ICostoDAO costoDAO;
    private final IMonedaDAO monedaDAO;
    private final IParametroDAO parametroDAO;
    private final TokenServicio tokenServicio;

    /**
     * Constructor con las dependencias explicitas.
     *
     * @param cuentaDAO     repositorio de cuentas
     * @param costoDAO      repositorio de costos e intereses
     * @param monedaDAO     repositorio de monedas
     * @param parametroDAO  repositorio de parametros de negocio
     * @param tokenServicio registro de tokens
     */
    public ServicioCostos(ICuentaDAO cuentaDAO, ICostoDAO costoDAO,
            IMonedaDAO monedaDAO, IParametroDAO parametroDAO, TokenServicio tokenServicio) {
        this.cuentaDAO = Objects.requireNonNull(cuentaDAO, "El repositorio de cuentas es obligatorio");
        this.costoDAO = Objects.requireNonNull(costoDAO, "El repositorio de costos es obligatorio");
        this.monedaDAO = Objects.requireNonNull(monedaDAO, "El repositorio de monedas es obligatorio");
        this.parametroDAO = Objects.requireNonNull(parametroDAO,
                "El repositorio de parametros es obligatorio");
        this.tokenServicio = Objects.requireNonNull(tokenServicio,
                "El registro de tokens es obligatorio");
    }

    /**
     * Operacion 9: proyecta el cargo por mantenimiento, el costo por movimiento
     * y el interes mensual de una cuenta.
     *
     * @param cuenta codigo de la cuenta
     * @param token  token de sesion
     * @return el sobre de respuesta con los seis valores de la proyeccion
     */
    public RespuestaCostos consultarCostosMantenimiento(String cuenta, String token) {
        CodigoRespuesta acceso = tokenServicio.validarToken(token);
        if (acceso != CodigoRespuesta.OK_200) {
            return FabricaRespuestas.costosFallidos(acceso);
        }
        if (Textos.estaVacio(cuenta)) {
            return FabricaRespuestas.costosFallidos(CodigoRespuesta.DAT_001);
        }
        Optional<CuentaDTO> encontrada = cuentaDAO.buscarPorCodigo(cuenta.trim());
        if (encontrada.isEmpty()) {
            return FabricaRespuestas.costosFallidos(CodigoRespuesta.CTA_001);
        }
        CuentaDTO datos = encontrada.get();
        if (!ServicioCuentas.ESTADO_ACTIVO.equals(datos.getEstado())) {
            return FabricaRespuestas.costosFallidos(CodigoRespuesta.CTA_002);
        }
        if (!monedaDAO.estaSoportada(datos.getMoneda())) {
            return FabricaRespuestas.costosFallidos(CodigoRespuesta.MON_002);
        }

        BigDecimal saldo = FormatoDinero.aDosDecimales(datos.getSaldo());
        BigDecimal limite = FormatoDinero.CERO;
        BigDecimal costoMantenimiento = FormatoDinero.CERO;
        Optional<ICostoDAO.CostoMantenimiento> mantenimiento =
                costoDAO.costoMantenimiento(datos.getMoneda());
        if (mantenimiento.isPresent()) {
            limite = FormatoDinero.aDosDecimales(mantenimiento.get().montoMaximo());
            if (saldo.compareTo(limite) <= 0) {
                costoMantenimiento = FormatoDinero.aDosDecimales(mantenimiento.get().importe());
            }
        }
        BigDecimal costoMovimiento = costoDAO.costoMovimiento(datos.getMoneda())
                .map(FormatoDinero::aDosDecimales)
                .orElse(FormatoDinero.CERO);
        BigDecimal interes = costoDAO.interesMensual(datos.getMoneda())
                .map(porcentaje -> FormatoDinero.porcentaje(saldo, porcentaje))
                .orElse(FormatoDinero.CERO);
        int operacionesSinCosto = parametroDAO.valor(PARAMETRO_OPERACIONES_SIN_COSTO)
                .map(BigDecimal::intValue)
                .orElse(0);
        return FabricaRespuestas.costosExitosos(saldo, costoMantenimiento, costoMovimiento,
                interes, limite, operacionesSinCosto);
    }
}
