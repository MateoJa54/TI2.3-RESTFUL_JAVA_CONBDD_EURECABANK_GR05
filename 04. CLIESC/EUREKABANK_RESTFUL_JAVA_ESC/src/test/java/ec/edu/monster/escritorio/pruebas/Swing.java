package ec.edu.monster.escritorio.pruebas;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;

/**
 * Ayudas para probar Swing sin abrir ventanas: ejecutar en el hilo de eventos,
 * esperar a que termine una tarea asíncrona y buscar componentes por nombre.
 */
final class Swing {

    private static final long ESPERA_MAXIMA_MS = 8_000;

    private Swing() {
    }

    /** Ejecuta en el hilo de eventos y espera a que termine. */
    static void enEdt(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(accion);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException r) {
                throw r;
            }
            if (causa instanceof Error er) {
                throw er;
            }
            throw new IllegalStateException(causa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** Espera, sin bloquear el hilo de eventos, a que se cumpla la condición. */
    static void esperar(BooleanSupplier condicion) {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;
        while (System.currentTimeMillis() < limite) {
            boolean[] cumplida = new boolean[1];
            enEdt(() -> cumplida[0] = condicion.getAsBoolean());
            if (cumplida[0]) {
                enEdt(() -> { });
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("La condición no se cumplió en " + ESPERA_MAXIMA_MS + " ms");
    }

    /** Busca un componente por su nombre y tipo. */
    static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : todos(raiz)) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
        }
        throw new AssertionError("No hay " + tipo.getSimpleName() + " llamado " + nombre);
    }

    /** Busca el primer componente de un tipo. */
    static <T extends Component> T primero(Container raiz, Class<T> tipo) {
        for (Component c : todos(raiz)) {
            if (tipo.isInstance(c)) {
                return tipo.cast(c);
            }
        }
        throw new AssertionError("No hay ningún " + tipo.getSimpleName());
    }

    /** Todos los componentes descendientes. */
    static List<Component> todos(Container raiz) {
        List<Component> lista = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            lista.add(c);
            if (c instanceof Container hijo) {
                lista.addAll(todos(hijo));
            }
        }
        return lista;
    }
}
