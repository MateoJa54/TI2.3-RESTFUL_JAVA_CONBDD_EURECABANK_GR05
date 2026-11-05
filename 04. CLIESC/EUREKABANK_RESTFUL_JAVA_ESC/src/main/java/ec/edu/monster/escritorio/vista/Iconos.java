package ec.edu.monster.escritorio.vista;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Iconos de la aplicación, dibujados con Java2D. Todos usan el mismo trazo
 * ({@link #TRAZO} píxeles a tamaño {@link #TAMANO}) y se escalan de forma
 * proporcional: la geometría se diseña en una rejilla de 20 unidades.
 */
public final class Iconos {

    /** Grosor del trazo, en píxeles, a tamaño normal. */
    public static final float TRAZO = 1.75f;
    /** Tamaño normal de un icono, en píxeles. */
    public static final int TAMANO = 20;

    /** Figuras disponibles. */
    public enum Tipo {
        /** Tarjeta de cuenta (destino Cuentas). */
        CUENTAS,
        /** Flechas arriba y abajo (destino Movimientos). */
        MOVIMIENTOS,
        /** Dos flechas opuestas (destino Operaciones). */
        OPERACIONES,
        /** Calculadora (destino Costos). */
        COSTOS,
        /** Ojo abierto: mostrar la clave. */
        MOSTRAR,
        /** Ojo tachado: ocultar la clave. */
        OCULTAR,
        /** Círculo con exclamación: error. */
        ERROR,
        /** Círculo con visto: éxito. */
        EXITO,
        /** Puerta con flecha: cerrar sesión. */
        SALIR,
        /** Equis: cerrar un aviso. */
        CERRAR
    }

    private Iconos() {
    }

    /**
     * @param tipo  figura
     * @param color color del trazo
     * @return el icono de tamaño {@link #TAMANO}
     */
    public static Icon de(Tipo tipo, Color color) {
        return de(tipo, TAMANO, color);
    }

    /**
     * @param tipo    figura
     * @param tamano  lado del icono, en píxeles
     * @param color   color del trazo
     * @return el icono; su grosor de trazo siempre es {@link #TRAZO} píxeles
     */
    public static Icon de(Tipo tipo, int tamano, Color color) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de icono es obligatorio");
        }
        if (tamano <= 0) {
            throw new IllegalArgumentException("El tamaño debe ser mayor que cero");
        }
        return new Figura(tipo, tamano, color);
    }

    /**
     * Dibuja la figura en el área indicada con su trazo.
     */
    static void dibujar(Graphics2D g, Tipo tipo, double tamano, Color color) {
        double k = tamano / TAMANO;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke((float) (TRAZO / k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.scale(k, k);
        switch (tipo) {
            case CUENTAS -> {
                g.draw(new RoundRectangle2D.Double(2, 4.5, 16, 11, 3, 3));
                g.draw(new Line2D.Double(2, 8.5, 18, 8.5));
                g.draw(new Line2D.Double(5, 12.5, 8.5, 12.5));
            }
            case MOVIMIENTOS -> {
                g.draw(new Line2D.Double(6.5, 16, 6.5, 4));
                g.draw(polilinea(3, 7.5, 6.5, 4, 10, 7.5));
                g.draw(new Line2D.Double(13.5, 4, 13.5, 16));
                g.draw(polilinea(10, 12.5, 13.5, 16, 17, 12.5));
            }
            case OPERACIONES -> {
                g.draw(new Line2D.Double(3, 7, 17, 7));
                g.draw(polilinea(13.5, 3.5, 17, 7, 13.5, 10.5));
                g.draw(new Line2D.Double(17, 13, 3, 13));
                g.draw(polilinea(6.5, 9.5, 3, 13, 6.5, 16.5));
            }
            case COSTOS -> {
                g.draw(new RoundRectangle2D.Double(4, 2.5, 12, 15, 2.5, 2.5));
                g.draw(new Line2D.Double(7, 6, 13, 6));
                g.draw(new Line2D.Double(7, 10, 7, 10));
                g.draw(new Line2D.Double(10, 10, 10, 10));
                g.draw(new Line2D.Double(13, 10, 13, 10));
                g.draw(new Line2D.Double(7, 14, 7, 14));
                g.draw(new Line2D.Double(10, 14, 10, 14));
                g.draw(new Line2D.Double(13, 14, 13, 14));
            }
            case MOSTRAR, OCULTAR -> {
                Path2D ojo = new Path2D.Double();
                ojo.moveTo(2, 10);
                ojo.quadTo(10, 2, 18, 10);
                ojo.quadTo(10, 18, 2, 10);
                g.draw(ojo);
                g.draw(new Ellipse2D.Double(7.2, 7.2, 5.6, 5.6));
                if (tipo == Tipo.OCULTAR) {
                    g.draw(new Line2D.Double(3, 3, 17, 17));
                }
            }
            case ERROR -> {
                g.draw(new Ellipse2D.Double(2, 2, 16, 16));
                g.draw(new Line2D.Double(10, 6, 10, 11));
                g.draw(new Line2D.Double(10, 14, 10, 14));
            }
            case EXITO -> {
                g.draw(new Ellipse2D.Double(2, 2, 16, 16));
                g.draw(polilinea(6.5, 10.5, 9, 13, 13.5, 7.5));
            }
            case SALIR -> {
                g.draw(polilinea(8, 4, 4, 4, 4, 16, 8, 16));
                g.draw(new Line2D.Double(9, 10, 16, 10));
                g.draw(polilinea(13, 7, 16, 10, 13, 13));
            }
            case CERRAR -> {
                g.draw(new Line2D.Double(6, 6, 14, 14));
                g.draw(new Line2D.Double(14, 6, 6, 14));
            }
            default -> throw new IllegalStateException("Figura sin dibujo: " + tipo);
        }
    }

    private static Path2D polilinea(double... puntos) {
        Path2D camino = new Path2D.Double();
        camino.moveTo(puntos[0], puntos[1]);
        for (int i = 2; i < puntos.length; i += 2) {
            camino.lineTo(puntos[i], puntos[i + 1]);
        }
        return camino;
    }

    /** Icono cuadrado que dibuja una figura con su trazo. */
    private static final class Figura implements Icon {

        private final Tipo tipo;
        private final int tamano;
        private final Color color;

        Figura(Tipo tipo, int tamano, Color color) {
            this.tipo = tipo;
            this.tamano = tamano;
            this.color = color == null ? Color.BLACK : color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(x, y);
                dibujar(g2, tipo, tamano, color);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }
    }
}
