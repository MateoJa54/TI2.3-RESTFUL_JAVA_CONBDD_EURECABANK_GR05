package ec.edu.monster.vista;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import ec.edu.monster.modelo.RespuestaBanco;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Convierte cada respuesta REST en bloques visuales, evitando volcar el JSON como texto plano. */
public final class ResultadoUI {
    private static final int VERDE_SUAVE = Color.rgb(232, 246, 242);
    private static final int ROJO = Color.rgb(157, 55, 55);
    private static final List<String> CAMPOS_MONETARIOS = Arrays.asList(
            "saldo", "saldoActual", "saldoNuevo", "saldoOrigen", "saldoDestino", "importe",
            "costoAplicado", "costoMantenimiento", "costoMovimiento", "interesProyectado",
            "limiteMantenimiento", "itfAplicado");

    private ResultadoUI() { }

    public static LinearLayout crear(Context contexto, RespuestaBanco respuesta) {
        JSONObject datos = respuesta.getDatos();
        LinearLayout contenedor = Componentes.columna(contexto);
        contenedor.addView(resumen(contexto, respuesta), margen(contexto, 0, 0, 0, 12));

        if (datos.has("nombreCliente") || datos.has("cedula")) {
            contenedor.addView(resumenCliente(contexto, datos), margen(contexto, 0, 0, 0, 12));
        }

        if (datos.opt("cuenta") instanceof JSONObject) {
            contenedor.addView(cuenta(contexto, datos.optJSONObject("cuenta")), margen(contexto, 0, 0, 0, 12));
        }

        JSONArray cuentas = datos.optJSONArray("cuentas");
        if (cuentas != null) {
            contenedor.addView(tituloSeccion(contexto, "Cuentas", cuentas.length() + " encontradas"), margen(contexto, 0, 2, 0, 8));
            for (int i = 0; i < cuentas.length(); i++) {
                JSONObject item = cuentas.optJSONObject(i);
                if (item != null) contenedor.addView(cuenta(contexto, item), margen(contexto, 0, 0, 0, 10));
            }
        }

        JSONArray movimientos = datos.optJSONArray("movimientos");
        if (movimientos != null) {
            String codigoCuenta = datos.optString("cuenta", "");
            String detalle = movimientos.length() + (movimientos.length() == 1 ? " movimiento" : " movimientos");
            if (!codigoCuenta.isEmpty()) detalle += " · Cuenta " + codigoCuenta;
            contenedor.addView(tituloSeccion(contexto, "Historial", detalle), margen(contexto, 0, 2, 0, 8));
            agregarMovimientos(contexto, contenedor, movimientos);
        }

        JSONObject movimiento = datos.optJSONObject("movimiento");
        if (movimiento != null) {
            contenedor.addView(tituloSeccion(contexto, "Movimiento registrado", "Detalle de la operación"), margen(contexto, 0, 2, 0, 8));
            contenedor.addView(movimiento(contexto, movimiento), margen(contexto, 0, 0, 0, 10));
        }

        List<String> metricas = new ArrayList<>();
        java.util.Iterator<String> claves = datos.keys();
        while (claves.hasNext()) {
            String clave = claves.next();
            Object valor = datos.opt(clave);
            if (valor == null || valor == JSONObject.NULL || valor instanceof JSONObject || valor instanceof JSONArray) continue;
            if (Arrays.asList("estado", "codigo", "mensaje", "nombreCliente", "cedula", "cuenta", "movimiento", "movimientos", "cuentas", "token").contains(clave)) continue;
            metricas.add(clave);
        }
        if (!metricas.isEmpty()) {
            String titulo = datos.has("costoMantenimiento") ? "Proyección de costos"
                    : (datos.has("saldoOrigen") || datos.has("saldoDestino") ? "Resultado de transferencia" : "Resumen de la operación");
            contenedor.addView(tituloSeccion(contexto, titulo, "Valores informados por el servidor"), margen(contexto, 0, 2, 0, 8));
            contenedor.addView(cuadriculaMetricas(contexto, datos, metricas), margen(contexto, 0, 0, 0, 4));
        }
        return contenedor;
    }

    /** En historiales extensos muestra primero cinco registros y deja expandir el resto. */
    private static void agregarMovimientos(Context c, LinearLayout destino, JSONArray movimientos) {
        int total = movimientos.length();
        int iniciales = Math.min(total, 5);
        LinearLayout lista = Componentes.columna(c);
        destino.addView(lista);
        Button expandir = Componentes.boton(c, "Mostrar todos (" + total + ")", false);
        if (total > iniciales) destino.addView(expandir, margen(c, 0, 2, 0, 8));
        final boolean[] completo = {false};
        Runnable renderizar = () -> {
            lista.removeAllViews();
            int cantidad = completo[0] ? total : iniciales;
            // La API entrega los movimientos en orden ascendente: prioriza los más recientes.
            int inicio = completo[0] ? 0 : total - cantidad;
            for (int i = inicio; i < inicio + cantidad; i++) {
                JSONObject item = movimientos.optJSONObject(i);
                if (item != null) lista.addView(movimiento(c, item), margen(c, 0, 0, 0, 8));
            }
            if (total > iniciales) expandir.setText(completo[0] ? "Mostrar recientes" : "Mostrar todos (" + total + ")");
        };
        renderizar.run();
        if (total > iniciales) expandir.setOnClickListener(v -> { completo[0] = !completo[0]; renderizar.run(); });
    }

    public static LinearLayout mensaje(Context contexto, String mensaje, boolean error) {
        LinearLayout tarjeta = Componentes.tarjeta(contexto);
        tarjeta.addView(Componentes.texto(contexto, error ? "No se pudo completar" : "Información", 16,
                error ? ROJO : Componentes.NAVY, true));
        tarjeta.addView(Componentes.texto(contexto, mensaje, 14, Componentes.MUTED, false), margen(contexto, 0, 7, 0, 0));
        return tarjeta;
    }

    private static LinearLayout resumen(Context c, RespuestaBanco respuesta) {
        boolean ok = respuesta.esCorrecta();
        LinearLayout tarjeta = Componentes.tarjeta(c);
        LinearLayout fila = Componentes.fila(c); fila.setGravity(Gravity.CENTER_VERTICAL);
        TextView estado = Componentes.texto(c, ok ? "COMPLETADA" : "REVISAR", 10, ok ? Componentes.TEAL : ROJO, true);
        estado.setPadding(dp(c, 10), dp(c, 7), dp(c, 10), dp(c, 7));
        estado.setBackground(Componentes.fondo(ok ? VERDE_SUAVE : Color.rgb(252, 239, 239), 12, Color.TRANSPARENT));
        fila.addView(estado, new LinearLayout.LayoutParams(0, -2, 1));
        TextView http = Componentes.texto(c, "HTTP " + respuesta.getEstadoHttp(), 11, Componentes.MUTED, true);
        fila.addView(http); tarjeta.addView(fila);
        tarjeta.addView(Componentes.texto(c, "Código " + respuesta.getCodigo(), 12, Componentes.MUTED, true), margen(c, 0, 11, 0, 4));
        tarjeta.addView(Componentes.texto(c, respuesta.getMensaje(), 16, Componentes.NAVY, true));
        return tarjeta;
    }

    private static LinearLayout resumenCliente(Context c, JSONObject datos) {
        LinearLayout tarjeta = Componentes.tarjeta(c);
        tarjeta.addView(Componentes.texto(c, datos.optString("nombreCliente", "Cliente"), 17, Componentes.NAVY, true));
        String cedula = datos.optString("cedula", "");
        if (!cedula.isEmpty()) tarjeta.addView(Componentes.texto(c, "Cédula " + cedula, 13, Componentes.MUTED, false), margen(c, 0, 5, 0, 0));
        return tarjeta;
    }

    private static LinearLayout cuenta(Context c, JSONObject datos) {
        LinearLayout tarjeta = Componentes.tarjeta(c);
        LinearLayout encabezado = Componentes.fila(c); encabezado.setGravity(Gravity.CENTER_VERTICAL);
        encabezado.addView(Componentes.texto(c, "Cuenta " + datos.optString("codigo", ""), 16, Componentes.NAVY, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView estado = Componentes.texto(c, datos.optString("estado", ""), 10, Componentes.TEAL, true);
        estado.setPadding(dp(c, 9), dp(c, 6), dp(c, 9), dp(c, 6)); estado.setBackground(Componentes.fondo(VERDE_SUAVE, 12, Color.TRANSPARENT));
        encabezado.addView(estado); tarjeta.addView(encabezado);
        String moneda = datos.optString("descripcionMoneda", "");
        String saldo = valor("saldo", datos.opt("saldo"));
        tarjeta.addView(Componentes.texto(c, saldo + (moneda.isEmpty() ? "" : " " + moneda), 24, Componentes.TEAL, true), margen(c, 0, 14, 0, 12));
        String[][] filas = {
                {"Titular", datos.optString("nombreCliente", "—")},
                {"Código del cliente", datos.optString("codigoCliente", "—")},
                {"Sucursal", datos.optString("codigoSucursal", "—")},
                {"Apertura", datos.optString("fechaCreacion", "—")},
                {"Operaciones", datos.has("operacionesRealizadas") ? datos.optString("operacionesRealizadas") : "—"}
        };
        tarjeta.addView(listaFilas(c, filas));
        return tarjeta;
    }

    private static LinearLayout movimiento(Context c, JSONObject datos) {
        LinearLayout tarjeta = Componentes.columna(c); tarjeta.setPadding(dp(c, 15), dp(c, 13), dp(c, 15), dp(c, 13));
        tarjeta.setBackground(Componentes.fondo(Color.WHITE, 16, Componentes.LINE));
        LinearLayout encabezado = Componentes.fila(c); encabezado.setGravity(Gravity.CENTER_VERTICAL);
        encabezado.addView(Componentes.texto(c, "N.º " + datos.optString("numero", "—"), 13, Componentes.NAVY, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        encabezado.addView(Componentes.texto(c, datos.optString("fecha", ""), 12, Componentes.MUTED, false));
        tarjeta.addView(encabezado);
        tarjeta.addView(Componentes.texto(c, datos.optString("tipoDescripcion", "Movimiento"), 15, Componentes.INK, true), margen(c, 0, 8, 0, 3));
        String accion = datos.optString("tipoAccion", "");
        String detalle = accion;
        String referencia = datos.optString("referencia", "");
        if (!referencia.isEmpty() && !"null".equalsIgnoreCase(referencia)) detalle += (detalle.isEmpty() ? "" : " · ") + "Cuenta " + referencia;
        if (!detalle.isEmpty()) tarjeta.addView(Componentes.texto(c, detalle, 12, Componentes.MUTED, false));
        tarjeta.addView(Componentes.texto(c, valor("importe", datos.opt("importe")), 18,
                "SALIDA".equalsIgnoreCase(accion) ? ROJO : Componentes.TEAL, true), margen(c, 0, 8, 0, 0));
        return tarjeta;
    }

    private static LinearLayout cuadriculaMetricas(Context c, JSONObject datos, List<String> claves) {
        LinearLayout grid = Componentes.columna(c);
        for (int i = 0; i < claves.size(); i += 2) {
            LinearLayout fila = Componentes.fila(c);
            fila.addView(metrica(c, etiqueta(claves.get(i)), valor(claves.get(i), datos.opt(claves.get(i)))),
                    new LinearLayout.LayoutParams(0, -2, 1));
            if (i + 1 < claves.size()) {
                fila.addView(new View(c), new LinearLayout.LayoutParams(dp(c, 9), 1));
                fila.addView(metrica(c, etiqueta(claves.get(i + 1)), valor(claves.get(i + 1), datos.opt(claves.get(i + 1)))),
                        new LinearLayout.LayoutParams(0, -2, 1));
            }
            grid.addView(fila, margen(c, 0, i == 0 ? 0 : 8, 0, 0));
        }
        return grid;
    }

    private static LinearLayout metrica(Context c, String titulo, String dato) {
        LinearLayout tarjeta = Componentes.columna(c); tarjeta.setPadding(dp(c, 13), dp(c, 12), dp(c, 13), dp(c, 12));
        tarjeta.setBackground(Componentes.fondo(Color.rgb(248, 250, 252), 13, Componentes.LINE));
        tarjeta.addView(Componentes.texto(c, titulo, 11, Componentes.MUTED, false));
        tarjeta.addView(Componentes.texto(c, dato, 16, Componentes.NAVY, true), margen(c, 0, 5, 0, 0));
        return tarjeta;
    }

    private static LinearLayout listaFilas(Context c, String[][] datos) {
        LinearLayout columna = Componentes.columna(c);
        for (int i = 0; i < datos.length; i++) {
            if (datos[i][1] == null || datos[i][1].isEmpty() || "—".equals(datos[i][1])) continue;
            LinearLayout fila = Componentes.fila(c); fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.addView(Componentes.texto(c, datos[i][0], 12, Componentes.MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
            fila.addView(Componentes.texto(c, datos[i][1], 13, Componentes.INK, true));
            columna.addView(fila, margen(c, 0, i == 0 ? 0 : 9, 0, 0));
        }
        return columna;
    }

    private static LinearLayout tituloSeccion(Context c, String titulo, String detalle) {
        LinearLayout fila = Componentes.fila(c); fila.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout textos = Componentes.columna(c);
        textos.addView(Componentes.texto(c, titulo, 17, Componentes.NAVY, true));
        textos.addView(Componentes.texto(c, detalle, 12, Componentes.MUTED, false), margen(c, 0, 3, 0, 0));
        fila.addView(textos); return fila;
    }

    private static String etiqueta(String clave) {
        switch (clave) {
            case "saldoNuevo": return "Saldo nuevo";
            case "saldoOrigen": return "Saldo de origen";
            case "saldoDestino": return "Saldo de destino";
            case "saldoActual": return "Saldo actual";
            case "costoAplicado": return "Costo aplicado";
            case "costoMantenimiento": return "Mantenimiento";
            case "costoMovimiento": return "Costo por movimiento";
            case "interesProyectado": return "Interés proyectado";
            case "limiteMantenimiento": return "Límite de mantenimiento";
            case "operacionesSinCosto": return "Operaciones sin costo";
            case "itfAplicado": return "ITF aplicado";
            case "numeroOrigen": return "N.º salida";
            case "numeroDestino": return "N.º entrada";
            default:
                String texto = clave.replaceAll("([a-z])([A-Z])", "$1 $2");
                return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        }
    }

    private static String valor(String clave, Object valor) {
        if (valor == null || valor == JSONObject.NULL) return "—";
        if (CAMPOS_MONETARIOS.contains(clave)) {
            try { return new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP).toPlainString(); }
            catch (NumberFormatException ignored) { return valor.toString(); }
        }
        return valor.toString();
    }

    private static LinearLayout.LayoutParams margen(Context c, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b)); return p;
    }
    private static int dp(Context c, int n) { return Componentes.dp(c, n); }
}
