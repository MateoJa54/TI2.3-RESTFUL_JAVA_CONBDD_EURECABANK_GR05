package ec.edu.monster.escritorio.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.monster.escritorio.util.ExcepcionServicio;
import ec.edu.monster.escritorio.vista.TareaAsincrona;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** El trabajo corre fuera del hilo de eventos y el resultado llega dentro de él. */
class TareaAsincronaTest {

    @Test
    void elResultadoLlegaEnElHiloDeEventos() throws InterruptedException {
        AtomicReference<String> recibido = new AtomicReference<>();
        AtomicBoolean enEdt = new AtomicBoolean();
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.lanzar(() -> "hecho", r -> {
            recibido.set(r);
            enEdt.set(SwingUtilities.isEventDispatchThread());
            listo.countDown();
        }, f -> listo.countDown());
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        assertEquals("hecho", recibido.get());
        assertTrue(enEdt.get());
    }

    @Test
    void elTrabajoNoCorreEnElHiloDeEventos() throws InterruptedException {
        AtomicBoolean enEdt = new AtomicBoolean(true);
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.lanzar(() -> {
            enEdt.set(SwingUtilities.isEventDispatchThread());
            return 1;
        }, r -> listo.countDown(), f -> listo.countDown());
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        assertFalse(enEdt.get());
    }

    @Test
    void elFalloLlegaEnElHiloDeEventos() throws InterruptedException {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        AtomicBoolean enEdt = new AtomicBoolean();
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.<String>lanzar(() -> {
            throw new ExcepcionServicio("sin red", null);
        }, r -> listo.countDown(), f -> {
            fallo.set(f);
            enEdt.set(SwingUtilities.isEventDispatchThread());
            listo.countDown();
        });
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        assertInstanceOf(ExcepcionServicio.class, fallo.get());
        assertTrue(enEdt.get());
    }

    @Test
    void elFalloEsLaCausaOriginalYNoLaEnvoltura() throws InterruptedException {
        RuntimeException original = new IllegalStateException("original");
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.<String>lanzar(() -> {
            throw original;
        }, r -> listo.countDown(), f -> {
            fallo.set(f);
            listo.countDown();
        });
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        assertSame(original, fallo.get());
    }

    @Test
    void siHayExitoNoSeLlamaAlFallo() throws InterruptedException {
        AtomicBoolean fallo = new AtomicBoolean();
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.lanzar(() -> 1, r -> listo.countDown(), f -> fallo.set(true));
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        Swing.enEdt(() -> { });
        assertFalse(fallo.get());
    }

    @Test
    void siHayFalloNoSeLlamaAlExito() throws InterruptedException {
        AtomicBoolean exito = new AtomicBoolean();
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.<Integer>lanzar(() -> {
            throw new IllegalStateException();
        }, r -> exito.set(true), f -> listo.countDown());
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        Swing.enEdt(() -> { });
        assertFalse(exito.get());
    }

    @Test
    void unResultadoNuloSeEntregaTalCual() throws InterruptedException {
        AtomicReference<Object> recibido = new AtomicReference<>("sin tocar");
        CountDownLatch listo = new CountDownLatch(1);
        TareaAsincrona.lanzar(() -> null, r -> {
            recibido.set(r);
            listo.countDown();
        }, f -> listo.countDown());
        assertTrue(listo.await(5, TimeUnit.SECONDS));
        assertEquals(null, recibido.get());
    }

    @Test
    void sinManejadoresNoFalla() throws InterruptedException {
        TareaAsincrona.lanzar(() -> 1, null, null);
        TareaAsincrona.<Integer>lanzar(() -> {
            throw new IllegalStateException();
        }, null, null);
        Thread.sleep(100);
        Swing.enEdt(() -> { });
    }

    @Test
    void elLlamadorNoQuedaBloqueadoMientrasElTrabajoEspera() throws InterruptedException {
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch listo = new CountDownLatch(1);
        long inicio = System.nanoTime();
        TareaAsincrona.lanzar(() -> {
            try {
                liberar.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }, r -> listo.countDown(), f -> listo.countDown());
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(ms < 1_000, "lanzar tardó " + ms + " ms");
        liberar.countDown();
        assertTrue(listo.await(5, TimeUnit.SECONDS));
    }

    @Test
    void variasTareasSeEntreganTodas() throws InterruptedException {
        int total = 20;
        CountDownLatch listo = new CountDownLatch(total);
        AtomicReference<Integer> suma = new AtomicReference<>(0);
        for (int i = 1; i <= total; i++) {
            int valor = i;
            TareaAsincrona.lanzar(() -> valor, r -> {
                suma.updateAndGet(s -> s + r);
                listo.countDown();
            }, f -> listo.countDown());
        }
        assertTrue(listo.await(10, TimeUnit.SECONDS));
        assertEquals(210, suma.get());
        assertNotNull(suma.get());
    }
}
