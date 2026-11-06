package ec.edu.monster.util;

import org.json.JSONArray;
import org.json.JSONObject;

/** Presenta los sobres y DTO JSON con etiquetas legibles sin alterar mensajes del servidor. */
public final class FormatoRespuesta {
    private FormatoRespuesta() { }

    public static String presentar(JSONObject respuesta) {
        StringBuilder salida = new StringBuilder();
        salida.append("Código: ").append(respuesta.optString("codigo", "—"));
        salida.append("\n").append(respuesta.optString("mensaje", "Sin mensaje del servidor."));
        agregarObjeto(salida, respuesta, 0, true);
        return salida.toString();
    }

    private static void agregarObjeto(StringBuilder salida, JSONObject objeto, int nivel, boolean sobre) {
        java.util.Iterator<String> claves = objeto.keys();
        while (claves.hasNext()) {
            String clave = claves.next();
            if (sobre && ("estado".equals(clave) || "codigo".equals(clave) || "mensaje".equals(clave))) continue;
            Object valor = objeto.opt(clave);
            if (valor == null || valor == JSONObject.NULL) continue;
            if (valor instanceof JSONObject) {
                salto(salida, nivel).append(etiqueta(clave)).append(':');
                agregarObjeto(salida, (JSONObject) valor, nivel + 1, false);
            } else if (valor instanceof JSONArray) {
                JSONArray arreglo = (JSONArray) valor;
                salto(salida, nivel).append(etiqueta(clave)).append(" (").append(arreglo.length()).append("):");
                for (int i = 0; i < arreglo.length(); i++) {
                    Object item = arreglo.opt(i);
                    if (item instanceof JSONObject) {
                        salto(salida, nivel + 1).append("• Elemento ").append(i + 1);
                        agregarObjeto(salida, (JSONObject) item, nivel + 2, false);
                    } else salto(salida, nivel + 1).append("• ").append(String.valueOf(item));
                }
            } else salto(salida, nivel).append(etiqueta(clave)).append(": ").append(valor);
        }
    }

    private static StringBuilder salto(StringBuilder salida, int nivel) {
        salida.append('\n');
        for (int i = 0; i < nivel; i++) salida.append("  ");
        return salida;
    }

    private static String etiqueta(String clave) {
        switch (clave) {
            case "token": return "Token de sesión";
            case "empleado": return "Empleado";
            case "nombreEmpleado": return "Nombre del empleado";
            case "expiracion": return "Vence la sesión";
            case "nombreCliente": return "Cliente";
            case "cedula": return "Cédula";
            case "codigo": return "Código";
            case "cuentas": return "Cuentas";
            case "cuenta": return "Cuenta";
            case "codigoCliente": return "Código del cliente";
            case "codigoSucursal": return "Sucursal";
            case "descripcionMoneda": return "Moneda";
            case "saldo": case "saldoActual": return "Saldo actual";
            case "saldoNuevo": return "Saldo nuevo";
            case "saldoOrigen": return "Saldo de origen";
            case "saldoDestino": return "Saldo de destino";
            case "estado": return "Estado";
            case "fechaCreacion": return "Fecha de apertura";
            case "operacionesRealizadas": return "Operaciones realizadas";
            case "movimientos": return "Movimientos";
            case "movimiento": return "Movimiento";
            case "numero": return "Número";
            case "numeroOrigen": return "Movimiento de salida";
            case "numeroDestino": return "Movimiento de entrada";
            case "fecha": return "Fecha";
            case "tipoDescripcion": return "Tipo";
            case "tipoAccion": return "Acción";
            case "tipoCodigo": return "Código de movimiento";
            case "importe": return "Importe";
            case "referencia": return "Cuenta relacionada";
            case "costoAplicado": return "Costo aplicado";
            case "costoMantenimiento": return "Mantenimiento proyectado";
            case "costoMovimiento": return "Costo por movimiento";
            case "interesProyectado": return "Interés mensual proyectado";
            case "limiteMantenimiento": return "Límite de mantenimiento";
            case "operacionesSinCosto": return "Operaciones restantes sin costo";
            case "itfAplicado": return "ITF aplicado";
            case "clave": return "Clave";
            case "rol": return "Perfil";
            default: return separarCamelCase(clave);
        }
    }

    private static String separarCamelCase(String texto) {
        if (texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1).replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
