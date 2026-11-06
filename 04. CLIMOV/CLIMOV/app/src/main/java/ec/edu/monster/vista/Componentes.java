package ec.edu.monster.vista;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Elementos visuales reutilizables para conservar una interfaz uniforme. */
public final class Componentes {
    public static final int NAVY = Color.rgb(17, 48, 78);
    public static final int NAVY_DARK = Color.rgb(12, 37, 63);
    public static final int TEAL = Color.rgb(0, 125, 112);
    public static final int CANVAS = Color.rgb(246, 248, 251);
    public static final int INK = Color.rgb(34, 48, 63);
    public static final int MUTED = Color.rgb(103, 117, 132);
    public static final int LINE = Color.rgb(222, 229, 236);

    private Componentes() { }

    public static LinearLayout columna(Context c) {
        LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    public static LinearLayout fila(Context c) {
        LinearLayout v = new LinearLayout(c); v.setOrientation(LinearLayout.HORIZONTAL); return v;
    }
    public static TextView texto(Context c, String valor, int tamano, int color, boolean medio) {
        TextView v = new TextView(c); v.setText(valor); v.setTextSize(tamano); v.setTextColor(color);
        v.setTypeface(Typeface.create(medio ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        v.setIncludeFontPadding(false); return v;
    }
    public static EditText campo(Context c, String pista, int tipo) {
        EditText v = new EditText(c); v.setSingleLine(true); v.setHint(pista); v.setTextSize(15);
        v.setTextColor(INK); v.setHintTextColor(Color.rgb(145,157,169)); v.setInputType(tipo);
        v.setPadding(dp(c, 14), 0, dp(c, 14), 0); v.setBackground(fondo(Color.rgb(248,250,252), 12, LINE));
        v.setMinHeight(dp(c, 52)); return v;
    }
    public static TextView etiqueta(Context c, String valor) {
        TextView v = texto(c, valor.toUpperCase(), 11, MUTED, true); v.setLetterSpacing(.08f); return v;
    }
    public static ImageView icono(Context c, int recurso, int color) {
        ImageView v = new ImageView(c); v.setImageResource(recurso);
        v.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        v.setContentDescription(null); return v;
    }
    public static Button boton(Context c, String valor, boolean primario) {
        Button v = new Button(c); v.setAllCaps(false); v.setText(valor); v.setTextSize(15);
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setTextColor(primario ? Color.WHITE : NAVY);
        v.setBackground(fondo(primario ? TEAL : Color.WHITE, 14, primario ? TEAL : LINE));
        v.setElevation(primario ? dp(c, 2) : 0); v.setMinimumHeight(dp(c, 50));
        v.setGravity(Gravity.CENTER); return v;
    }
    public static LinearLayout tarjeta(Context c) {
        LinearLayout v = columna(c); v.setPadding(dp(c, 18), dp(c, 19), dp(c, 18), dp(c, 18));
        v.setBackground(fondo(Color.WHITE, 20, LINE)); return v;
    }
    public static GradientDrawable fondo(int color, int radio, int borde) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radio * 3f);
        if (borde != Color.TRANSPARENT) d.setStroke(1, borde); return d;
    }
    public static int dp(Context c, int n) { return (int) (n * c.getResources().getDisplayMetrics().density + .5f); }
    public static void margen(View view, int start, int top, int end, int bottom) {
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) view.getLayoutParams();
        p.setMargins(start, top, end, bottom); view.setLayoutParams(p);
    }
}
