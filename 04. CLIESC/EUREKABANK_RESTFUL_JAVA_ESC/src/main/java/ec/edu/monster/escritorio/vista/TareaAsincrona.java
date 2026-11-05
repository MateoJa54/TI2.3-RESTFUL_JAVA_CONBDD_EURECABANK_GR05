package ec.edu.monster.escritorio.vista;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

/**
 * SwingWorker genérico: el trabajo (una llamada de red) corre en un hilo de
 * fondo y el resultado, o el fallo, se entrega siempre en el hilo de eventos.
 * Así ninguna llamada al servicio bloquea la interfaz.
 *
 * @param <T> tipo del resultado
 */
public final class TareaAsincrona<T> extends SwingWorker<T, Void> {

    private final Supplier<T> trabajo;
    private final Consumer<T> alTerminar;
    private final Consumer<Throwable> alFallar;

    private TareaAsincrona(Supplier<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        this.trabajo = trabajo;
        this.alTerminar = alTerminar;
        this.alFallar = alFallar;
    }

    @Override
    protected T doInBackground() {
        return trabajo.get();
    }

    @Override
    protected void done() {
        try {
            T resultado = get();
            if (alTerminar != null) {
                alTerminar.accept(resultado);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fallar(e);
        } catch (ExecutionException e) {
            fallar(e.getCause() != null ? e.getCause() : e);
        } catch (CancellationException e) {
            fallar(e);
        }
    }

    private void fallar(Throwable causa) {
        if (alFallar != null) {
            alFallar.accept(causa);
        }
    }

    /**
     * Ejecuta el trabajo en segundo plano.
     *
     * @param <T>        tipo del resultado
     * @param trabajo    lo que hay que hacer fuera del hilo de eventos
     * @param alTerminar recibe el resultado, en el hilo de eventos
     * @param alFallar   recibe el fallo, en el hilo de eventos
     */
    public static <T> void lanzar(Supplier<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        new TareaAsincrona<>(trabajo, alTerminar, alFallar).execute();
    }
}
