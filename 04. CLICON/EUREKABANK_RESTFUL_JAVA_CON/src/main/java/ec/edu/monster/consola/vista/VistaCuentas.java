package ec.edu.monster.consola.vista;

import ec.edu.monster.consola.controlador.ControladorCuentas;
import ec.edu.monster.consola.modelo.CuentaDTO;
import ec.edu.monster.consola.modelo.RespuestaCuenta;
import ec.edu.monster.consola.modelo.RespuestaCuentas;
import ec.edu.monster.consola.util.Consola;
import ec.edu.monster.consola.util.Formato;
import ec.edu.monster.consola.util.Teclado;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de cuentas (CU-03): listado por cédula y detalle de una cuenta.
 */
public class VistaCuentas {

    private final ControladorCuentas controlador;
    private final Teclado teclado;
    private final Consola consola;

    /**
     * @param controlador controlador de cuentas, obligatorio
     * @param teclado     teclado de la consola, obligatorio
     * @param consola     salida de la consola, obligatoria
     */
    public VistaCuentas(ControladorCuentas controlador, Teclado teclado, Consola consola) {
        this.controlador = controlador;
        this.teclado = teclado;
        this.consola = consola;
    }

    /**
     * Pide la cédula de un cliente y muestra sus cuentas.
     */
    public void listarCuentas() {
        String cedula = teclado.texto("Cédula del cliente");
        if (cedula.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        RespuestaCuentas respuesta = controlador.listarCuentas(cedula);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudieron listar las cuentas: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        consola.titulo("CUENTAS DE " + Formato.texto(respuesta.getNombreCliente())
                + " (" + Formato.texto(respuesta.getCedula()) + ")");
        List<CuentaDTO> cuentas = respuesta.getCuentas();
        if (cuentas == null || cuentas.isEmpty()) {
            consola.linea("El cliente no tiene cuentas registradas.");
            return;
        }
        List<String[]> filas = new ArrayList<>();
        for (CuentaDTO cuenta : cuentas) {
            filas.add(new String[] {
                    Formato.texto(cuenta.getCodigo()),
                    Formato.texto(cuenta.getDescripcionMoneda()),
                    Formato.monto(cuenta.getSaldo()),
                    Formato.texto(cuenta.getEstado()),
                    Formato.texto(cuenta.getNombreCliente()),
                    Formato.entero(cuenta.getOperacionesRealizadas())});
        }
        consola.tabla(new String[] {"CODIGO", "MONEDA", "SALDO", "ESTADO", "CLIENTE", "OPERACIONES"}, filas);
    }

    /**
     * Pide el código de una cuenta y muestra su detalle.
     */
    public void consultarCuenta() {
        String cuenta = teclado.texto("Código de la cuenta");
        if (cuenta.isEmpty()) {
            consola.linea("Operación cancelada.");
            return;
        }
        RespuestaCuenta respuesta = controlador.consultarCuenta(cuenta);
        if (!"OK".equals(respuesta.getEstado())) {
            consola.linea("No se pudo consultar la cuenta: "
                    + Formato.resultado(respuesta.getCodigo(), respuesta.getMensaje()));
            return;
        }
        CuentaDTO datos = respuesta.getCuenta();
        if (datos == null) {
            consola.linea("El servidor no devolvió los datos de la cuenta.");
            return;
        }
        consola.titulo("CUENTA " + Formato.texto(datos.getCodigo()));
        consola.linea("  Cliente:           " + Formato.texto(datos.getNombreCliente())
                + " (" + Formato.texto(datos.getCodigoCliente()) + ")");
        consola.linea("  Moneda:            " + Formato.texto(datos.getDescripcionMoneda())
                + " (" + Formato.texto(datos.getMoneda()) + ")");
        consola.linea("  Saldo:             " + Formato.monto(datos.getSaldo()));
        consola.linea("  Estado:            " + Formato.texto(datos.getEstado()));
        consola.linea("  Sucursal:          " + Formato.texto(datos.getCodigoSucursal()));
        consola.linea("  Fecha de creación: " + Formato.texto(datos.getFechaCreacion()));
        consola.linea("  Operaciones:       " + Formato.entero(datos.getOperacionesRealizadas()));
    }
}
