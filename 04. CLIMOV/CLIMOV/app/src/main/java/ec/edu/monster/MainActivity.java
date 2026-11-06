package ec.edu.monster;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import ec.edu.monster.controlador.ControladorBanco;
import ec.edu.monster.modelo.RespuestaBanco;
import ec.edu.monster.util.FormatoRespuesta;
import ec.edu.monster.vista.Componentes;
import ec.edu.monster.vista.ResultadoUI;
import ec.edu.monster.ws.ClienteEurekaBank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Vista principal Android. Delega autenticación y operaciones en ControladorBanco. */
public class MainActivity extends Activity {
    private final ExecutorService tareas = Executors.newSingleThreadExecutor();
    private LinearLayout contenido;
    private ScrollView scrollVista;
    private ProgressBar progreso;
    private Button accionPrincipal;
    private Button botonActual;
    private String etiquetaBotonActual = "Iniciar sesión";
    private EditText usuario, clave, servidor;
    private TextView estadoAcceso;
    private LinearLayout resultado;
    private ControladorBanco controlador;
    private LinearLayout panelOperacion;
    private LinearLayout cuadriculaServicios;
    private final List<EditText> entradasOperacion = new ArrayList<>();
    private int operacionSeleccionada = -1;
    private String empleadoActual = "Bienvenido";

    private static final String[] TITULOS = {
            "Cuentas de cliente", "Consultar cuenta", "Movimientos", "Realizar depósito",
            "Realizar retiro", "Transferir", "Costos de mantenimiento"
    };
    private static final String[] SUBTITULOS = {
            "Buscar por cédula", "Saldo y datos de cuenta", "Historial de operaciones",
            "Agregar fondos", "Retirar fondos", "Entre cuentas bancarias", "Proyección de cargos"
    };
    private static final int[] ICONOS_SERVICIO = {
            R.drawable.ic_people, R.drawable.ic_wallet, R.drawable.ic_receipt,
            R.drawable.ic_add_circle, R.drawable.ic_remove_circle, R.drawable.ic_transfer,
            R.drawable.ic_price_check
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Componentes.NAVY_DARK);
        getWindow().setNavigationBarColor(Componentes.CANVAS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        mostrarAcceso();
    }

    private void base() {
        ScrollView scroll = new ScrollView(this); scrollVista=scroll; scroll.setFillViewport(true); scroll.setBackgroundColor(Componentes.CANVAS);
        contenido = Componentes.columna(this); contenido.setPadding(dp(22), dp(16), dp(22), dp(30));
        scroll.addView(contenido); setContentView(scroll);
        progreso = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progreso.setIndeterminate(true); progreso.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Componentes.TEAL));
        progreso.setVisibility(View.GONE); contenido.addView(progreso, new LinearLayout.LayoutParams(-1, dp(3)));
    }

    private void mostrarAcceso() {
        controlador = null; operacionSeleccionada = -1; panelOperacion=null; botonActual=null; base();
        contenido.addView(hero("EurekaBank", "BANCA MÓVIL", "Tus servicios bancarios,\na un solo toque",
                "Conéctate al servidor de EURECABANK desde tu emulador o teléfono."), lp(-1, -2, 0, 0, 0, 20));

        LinearLayout tarjeta = Componentes.tarjeta(this);
        tarjeta.addView(tituloSeccion(R.drawable.ic_lock,"Iniciar sesión", "Ingresa tus credenciales para continuar"), lp(-1,-2,0,0,0,15));
        usuario = agregarCampo(tarjeta, "USUARIO", "Usuario del empleado", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        clave = agregarCampo(tarjeta, "CONTRASEÑA", "Contraseña", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        servidor = agregarCampo(tarjeta, "SERVIDOR · URL BASE DE LA API", "http://10.0.2.2:8080/EUREKABANK_RESTFUL_JAVA/api", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        servidor.setText(getPreferences(MODE_PRIVATE).getString("servidor", ClienteEurekaBank.URL_EMULADOR));
        servidor.setTextSize(12); servidor.setHorizontallyScrolling(true);
        accionPrincipal = Componentes.boton(this, "Iniciar sesión", true);
        botonActual=accionPrincipal; etiquetaBotonActual="Iniciar sesión";
        tarjeta.addView(accionPrincipal, lp(-1,dp(54),0,8,0,0)); accionPrincipal.setOnClickListener(v -> iniciarSesion());
        estadoAcceso = Componentes.texto(this, "", 13, Color.rgb(174,54,54), false);
        tarjeta.addView(estadoAcceso, lp(-1,-2,0,10,0,0));
        contenido.addView(tarjeta);

        TextView ayuda = Componentes.texto(this,
                "Emulador: usa 10.0.2.2. En un teléfono, usa la IP local del equipo con Payara. Permite el puerto 8080 en el firewall.",
                12, Componentes.MUTED, false);
        ayuda.setPadding(dp(7), dp(16), dp(7), 0); contenido.addView(ayuda);
    }

    private void iniciarSesion() {
        String nombre = usuario.getText().toString().trim(), contrasena = clave.getText().toString();
        String baseUrl = servidor.getText().toString().trim();
        if (nombre.isEmpty()) { usuario.setError("Ingresa tu usuario."); return; }
        if (contrasena.isEmpty()) { clave.setError("Ingresa tu contraseña."); return; }
        try { controlador = new ControladorBanco(baseUrl); }
        catch (IllegalArgumentException e) { servidor.setError(e.getMessage()); return; }
        getPreferences(MODE_PRIVATE).edit().putString("servidor", controladorUrl(baseUrl)).apply();
        estadoAcceso.setText(""); setOcupado(true, "Conectando…");
        tareas.execute(() -> {
            try {
                RespuestaBanco respuesta = controlador.autenticar(nombre, contrasena);
                runOnUiThread(() -> {
                    setOcupado(false, null);
                    if (respuesta.esCorrecta() && controlador.haySesion()) mostrarInicio(respuesta);
                    else estadoAcceso.setText(FormatoRespuesta.presentar(respuesta.getDatos()));
                });
            } catch (Exception e) { runOnUiThread(() -> { setOcupado(false, null); estadoAcceso.setText(mensajeError(e)); }); }
        });
    }

    private String controladorUrl(String valor) {
        try { return new ClienteEurekaBank(valor).getBaseUrl(); }
        catch (RuntimeException e) { return valor; }
    }

    private void mostrarInicio(RespuestaBanco autenticacion) {
        base();
        panelOperacion=null; botonActual=null; operacionSeleccionada=-1;
        empleadoActual = autenticacion.getDatos().optString("nombreEmpleado", empleadoActual);
        String nombre = empleadoActual;
        LinearLayout saludo = Componentes.fila(this); saludo.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout nombres = Componentes.columna(this);
        nombres.addView(Componentes.texto(this,"Hola,",14,Componentes.MUTED,false));
        nombres.addView(Componentes.texto(this,nombre,21,Componentes.NAVY,true));
        saludo.addView(nombres,new LinearLayout.LayoutParams(0,-2,1));
        TextView seguro = Componentes.texto(this,"● Sesión segura",11,Componentes.TEAL,true);
        seguro.setPadding(dp(10),dp(8),dp(10),dp(8)); seguro.setBackground(Componentes.fondo(Color.rgb(226,243,239),14,Color.TRANSPARENT));
        saludo.addView(seguro); contenido.addView(saludo,lp(-1,-2,0,12,0,18));

        LinearLayout servicios = Componentes.tarjeta(this);
        servicios.addView(tituloSeccion(R.drawable.ic_grid,"¿Qué necesitas hacer?", "Elige una operación bancaria"),lp(-1,-2,0,0,0,12));
        cuadriculaServicios=Componentes.columna(this); servicios.addView(cuadriculaServicios); construirCuadriculaServicios();
        contenido.addView(servicios);
        resultado=Componentes.columna(this); resultado.setVisibility(View.GONE);
        contenido.addView(resultado,lp(-1,-2,0,15,0,0));
        Button salir=Componentes.boton(this,"Cerrar sesión",false); contenido.addView(salir,lp(-1,dp(50),0,12,0,0));
        salir.setOnClickListener(v -> cerrarSesion(salir));
    }

    /** Construye tarjetas en dos columnas, siguiendo la guía del cliente móvil SOAP. */
    private void construirCuadriculaServicios() {
        if(cuadriculaServicios==null) return;
        cuadriculaServicios.removeAllViews();
        for(int indice=0;indice<TITULOS.length;indice+=2) {
            LinearLayout fila=Componentes.fila(this);
            fila.addView(tarjetaServicio(indice),new LinearLayout.LayoutParams(0,dp(136),1));
            int siguiente=indice+1;
            if(siguiente<TITULOS.length) {
                fila.addView(new View(this),new LinearLayout.LayoutParams(dp(10),1));
                fila.addView(tarjetaServicio(siguiente),new LinearLayout.LayoutParams(0,dp(136),1));
            }
            cuadriculaServicios.addView(fila,lp(-1,-2,0,indice==0?0:dp(10),0,0));
        }
    }

    private View tarjetaServicio(int indice) {
        boolean seleccionada=indice==operacionSeleccionada;
        LinearLayout tarjeta=Componentes.columna(this); tarjeta.setGravity(Gravity.CENTER_VERTICAL);
        tarjeta.setPadding(dp(13),dp(12),dp(11),dp(12));
        tarjeta.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(30,0,125,112)),
                Componentes.fondo(seleccionada?Color.rgb(238,248,246):Color.WHITE,16,
                        seleccionada?Componentes.TEAL:Componentes.LINE),null));
        tarjeta.setClickable(true); tarjeta.setFocusable(true);
        tarjeta.setContentDescription(TITULOS[indice]+". "+SUBTITULOS[indice]);
        LinearLayout filaIconos=Componentes.fila(this); filaIconos.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout cajaIcono=Componentes.fila(this); cajaIcono.setGravity(Gravity.CENTER);
        cajaIcono.setBackground(Componentes.fondo(seleccionada?Color.rgb(216,239,234):Color.rgb(232,243,245),12,Color.TRANSPARENT));
        ImageView icono=Componentes.icono(this,ICONOS_SERVICIO[indice],Componentes.TEAL);
        cajaIcono.addView(icono,new LinearLayout.LayoutParams(dp(38),dp(38))); filaIconos.addView(cajaIcono,new LinearLayout.LayoutParams(dp(54),dp(54)));
        if(seleccionada){ImageView check=Componentes.icono(this,R.drawable.ic_check,Componentes.TEAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(20),dp(20));p.leftMargin=dp(7);filaIconos.addView(check,p);}
        tarjeta.addView(filaIconos);
        TextView titulo=Componentes.texto(this,TITULOS[indice],14,Componentes.NAVY,true);titulo.setMaxLines(2);titulo.setPadding(0,dp(9),0,dp(3));tarjeta.addView(titulo);
        TextView subtitulo=Componentes.texto(this,SUBTITULOS[indice],11,Componentes.MUTED,false);subtitulo.setMaxLines(1);tarjeta.addView(subtitulo);
        tarjeta.setOnClickListener(v->mostrarOperacion(indice));return tarjeta;
    }

    private void mostrarOperacion(int indice) {
        operacionSeleccionada=indice; entradasOperacion.clear();
        construirCuadriculaServicios();
        // Reemplaza el panel de resultado por un formulario corto junto a los servicios.
        if(panelOperacion!=null) contenido.removeView(panelOperacion);
        LinearLayout panel=Componentes.tarjeta(this); panelOperacion=panel;
        panel.addView(tituloSeccion(ICONOS_SERVICIO[indice],TITULOS[indice], "Completa los datos para continuar"),lp(-1,-2,0,0,0,12));
        switch(indice) {
            case 0: campoOperacion(panel,"CÉDULA DEL CLIENTE","Ej.: 06914897",InputType.TYPE_CLASS_NUMBER); break;
            case 1: case 2: case 6: campoOperacion(panel,"NÚMERO DE CUENTA","Ej.: 00200002",InputType.TYPE_CLASS_TEXT); break;
            case 3: case 4:
                campoOperacion(panel,"NÚMERO DE CUENTA","Ej.: 00200002",InputType.TYPE_CLASS_TEXT);
                campoOperacion(panel,"MONTO","Ej.: 125.50",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); break;
            case 5:
                campoOperacion(panel,"CUENTA DE ORIGEN","Ej.: 00200002",InputType.TYPE_CLASS_TEXT);
                campoOperacion(panel,"CUENTA DE DESTINO","Ej.: 00100002",InputType.TYPE_CLASS_TEXT);
                campoOperacion(panel,"MONTO","Ej.: 125.50",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); break;
        }
        Button ejecutar=Componentes.boton(this,"Ejecutar operación",true);
        botonActual=ejecutar; etiquetaBotonActual="Ejecutar operación";
        panel.addView(ejecutar,lp(-1,dp(52),0,6,0,0)); ejecutar.setOnClickListener(v -> ejecutarOperacion());
        Button volver=Componentes.boton(this,"Volver a servicios",false);
        panel.addView(volver,lp(-1,dp(48),0,8,0,0)); volver.setOnClickListener(v -> mostrarInicioDesdeSesion());
        // El panel se coloca antes del texto de resultado, al final del contenido.
        int position=contenido.indexOfChild(resultado);
        contenido.addView(panel,position,lp(-1,-2,0,14,0,0));
        scrollVista.post(() -> scrollVista.smoothScrollTo(0,panel.getTop()));
        resultado.removeAllViews(); resultado.setVisibility(View.GONE);
    }

    private void mostrarInicioDesdeSesion() {
        // Reconstruir el menú no requiere volver a autenticar; conserva el token en el controlador.
        panelOperacion=null;
        RespuestaBanco sesion = new RespuestaBanco(200, ClienteEurekaBank.json(
                "estado","OK","codigo","200","mensaje","Sesión activa","nombreEmpleado",empleadoActual));
        mostrarInicio(sesion);
    }

    private void campoOperacion(LinearLayout contenedor,String etiqueta,String pista,int tipo) {
        contenedor.addView(Componentes.etiqueta(this,etiqueta),lp(-1,-2,0,5,0,6));
        EditText campo=Componentes.campo(this,pista,tipo); entradasOperacion.add(campo);
        contenedor.addView(campo,lp(-1,dp(52),0,0,0,13));
    }

    private void ejecutarOperacion() {
        for(EditText campo:entradasOperacion) if(campo.getText().toString().trim().isEmpty()) { campo.setError("Completa este campo."); campo.requestFocus(); return; }
        final String[] datos=new String[entradasOperacion.size()];
        for(int i=0;i<datos.length;i++) datos[i]=entradasOperacion.get(i).getText().toString().trim();
        final int op=operacionSeleccionada;
        ocultarTeclado();
        mostrarMensajeResultado("Consultando el servidor…",false);
        setOcupado(true,"Procesando…");
        tareas.execute(() -> {
            try {
                RespuestaBanco r;
                switch(op) {
                    case 0:r=controlador.listarCuentas(datos[0]);break;
                    case 1:r=controlador.consultarCuenta(datos[0]);break;
                    case 2:r=controlador.consultarMovimientos(datos[0]);break;
                    case 3:r=controlador.depositar(datos[0],datos[1]);break;
                    case 4:r=controlador.retirar(datos[0],datos[1]);break;
                    case 5:r=controlador.transferir(datos[0],datos[1],datos[2]);break;
                    case 6:r=controlador.consultarCostos(datos[0]);break;
                    default:throw new IllegalArgumentException("Selecciona una operación válida.");
                }
                final boolean sesionVencida="AUTH-003".equals(r.getCodigo())||"AUTH-004".equals(r.getCodigo());
                runOnUiThread(() -> {
                    setOcupado(false,null);
                    if(sesionVencida){mostrarAcceso();estadoAcceso.setText(FormatoRespuesta.presentar(r.getDatos()));}
                    else{mostrarRespuesta(r);desplazarAResultado();}
                });
            } catch(Exception e) { runOnUiThread(() -> {setOcupado(false,null);mostrarMensajeResultado(mensajeError(e),true);desplazarAResultado();}); }
        });
    }

    private void mostrarRespuesta(RespuestaBanco respuesta) {
        resultado.removeAllViews(); resultado.addView(ResultadoUI.crear(this,respuesta)); resultado.setVisibility(View.VISIBLE);
    }

    private void mostrarMensajeResultado(String mensaje,boolean error) {
        resultado.removeAllViews(); resultado.addView(ResultadoUI.mensaje(this,mensaje,error)); resultado.setVisibility(View.VISIBLE);
    }

    private void desplazarAResultado() {
        scrollVista.post(() -> scrollVista.smoothScrollTo(0,resultado.getTop()));
    }

    /** La respuesta debe ocupar la pantalla completa, no quedar tapada por el teclado numérico. */
    private void ocultarTeclado() {
        View enfocado=getCurrentFocus();
        if(enfocado!=null) {
            InputMethodManager teclado=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(teclado!=null) teclado.hideSoftInputFromWindow(enfocado.getWindowToken(),0);
            enfocado.clearFocus();
        }
        if(scrollVista!=null) scrollVista.clearFocus();
    }

    private void cerrarSesion(Button boton) {
        boton.setEnabled(false);botonActual=boton;etiquetaBotonActual="Cerrar sesión";setOcupado(true,"Cerrando sesión…");
        tareas.execute(() -> {
            try {
                RespuestaBanco r=controlador.cerrarSesion();
                boolean sesionCerrada=r.esCorrecta()||"AUTH-003".equals(r.getCodigo())||"AUTH-004".equals(r.getCodigo());
                runOnUiThread(() -> {
                    setOcupado(false,null);
                    if(sesionCerrada){mostrarAcceso();estadoAcceso.setText(FormatoRespuesta.presentar(r.getDatos()));}
                    else{mostrarRespuesta(r);desplazarAResultado();}
                });
            } catch(Exception e) { runOnUiThread(() -> {setOcupado(false,null);boton.setEnabled(true);mostrarMensajeResultado(mensajeError(e),true);desplazarAResultado();}); }
        });
    }

    private LinearLayout hero(String marca,String subtitulo,String titular,String texto) {
        LinearLayout box=Componentes.columna(this);box.setPadding(dp(22),dp(24),dp(22),dp(23));
        box.setBackground(Componentes.fondo(Componentes.NAVY,24,Color.TRANSPARENT));
        LinearLayout fila=Componentes.fila(this);fila.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icono=Componentes.icono(this,R.drawable.ic_bank,Color.WHITE);
        icono.setPadding(dp(13),dp(13),dp(13),dp(13));icono.setBackground(Componentes.fondo(Color.rgb(42,75,103),15,Color.TRANSPARENT));
        fila.addView(icono,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout marcaBox=Componentes.columna(this);marcaBox.setPadding(dp(14),0,0,0);marcaBox.setGravity(Gravity.CENTER_VERTICAL);
        marcaBox.addView(Componentes.texto(this,marca,23,Color.WHITE,true));
        TextView tag=Componentes.texto(this,subtitulo,10,Color.rgb(194,216,231),true);tag.setLetterSpacing(.14f);marcaBox.addView(tag);
        fila.addView(marcaBox);box.addView(fila);
        box.addView(Componentes.texto(this,titular,23,Color.WHITE,true),lp(-1,-2,0,22,0,7));
        box.addView(Componentes.texto(this,texto,14,Color.rgb(216,228,238),false));return box;
    }

    private LinearLayout tituloSeccion(String titulo,String detalle) {
        LinearLayout b=Componentes.columna(this);b.addView(Componentes.texto(this,titulo,18,Componentes.NAVY,true));
        b.addView(Componentes.texto(this,detalle,12,Componentes.MUTED,false),lp(-1,-2,0,5,0,0));return b;
    }

    private LinearLayout tituloSeccion(int recurso,String titulo,String detalle) {
        LinearLayout fila=Componentes.fila(this);fila.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout caja=Componentes.fila(this);caja.setGravity(Gravity.CENTER);
        caja.setBackground(Componentes.fondo(Color.rgb(232,243,245),12,Color.TRANSPARENT));
        ImageView imagen=Componentes.icono(this,recurso,Componentes.TEAL);caja.addView(imagen,new LinearLayout.LayoutParams(dp(38),dp(38)));
        fila.addView(caja,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout textos=Componentes.columna(this);textos.setPadding(dp(12),0,0,0);
        textos.addView(Componentes.texto(this,titulo,18,Componentes.NAVY,true));
        textos.addView(Componentes.texto(this,detalle,12,Componentes.MUTED,false),lp(-1,-2,0,5,0,0));
        fila.addView(textos,new LinearLayout.LayoutParams(0,-2,1));return fila;
    }

    private EditText agregarCampo(LinearLayout panel,String nombre,String pista,int tipo) {
        panel.addView(Componentes.etiqueta(this,nombre),lp(-1,-2,0,4,0,6));
        EditText campo=Componentes.campo(this,pista,tipo);panel.addView(campo,lp(-1,dp(52),0,0,0,13));return campo;
    }

    private void setOcupado(boolean busy,String texto) {
        if(progreso!=null)progreso.setVisibility(busy?View.VISIBLE:View.GONE);
        if(botonActual!=null){botonActual.setEnabled(!busy);botonActual.setText(busy&&texto!=null?texto:etiquetaBotonActual);}
    }
    private String mensajeError(Exception e) {
        if(e instanceof IOException) return "No se pudo completar la comunicación con el servidor. Revisa la IP, el puerto y que Payara esté accesible desde este dispositivo.\nDetalle: "+e.getMessage();
        return e.getMessage()==null?"Ocurrió un error inesperado.":e.getMessage();
    }
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private int dp(int n){return Componentes.dp(this,n);}

    @Override protected void onDestroy(){super.onDestroy();tareas.shutdownNow();}
}
