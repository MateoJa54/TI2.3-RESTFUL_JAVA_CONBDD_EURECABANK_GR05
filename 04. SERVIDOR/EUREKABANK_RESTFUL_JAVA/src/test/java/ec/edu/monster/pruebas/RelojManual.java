package ec.edu.monster.pruebas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj de prueba que solo avanza cuando la prueba lo pide.
 *
 * <p>Permite comprobar que un token vence a los 30 minutos sin esperar 30
 * minutos reales.
 */
final class RelojManual extends Clock {

    private Instant instante;

    RelojManual(Instant inicio) {
        this.instante = inicio;
    }

    void avanzar(Duration duracion) {
        instante = instante.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
