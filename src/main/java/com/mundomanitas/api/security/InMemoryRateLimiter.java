package com.mundomanitas.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Limitador en memoria por clave y ventana fija. No usa dependencias externas:
 * cada proceso cuenta sus propios intentos y se reinicia al arrancar.
 */
public final class InMemoryRateLimiter implements RateLimiter {

    private static final int CLEANUP_FROM = 1000;
    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiter.class);

    private final int maxAttempts;
    private final Duration ventana;
    private final ConcurrentMap<String, Ventana> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxAttempts, Duration ventana) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("El límite de intentos debe ser positivo");
        }
        if (ventana == null || ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("La ventana de intentos debe ser positiva");
        }
        this.maxAttempts = maxAttempts;
        this.ventana = ventana;
        log.info("Limitador de intentos configurado max={} ventana={}s",
                maxAttempts, ventana.toSeconds());
    }

    @Override
    public Decision verificacion(String key, Instant now) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(now, "now");
        purge(now);

        Ventana actual = windows.computeIfAbsent(key, ignored -> new Ventana(now));
        synchronized (actual) {
            if (!now.isBefore(actual.start.plus(ventana))) {
                actual.start = now;
                actual.count = 0;
            }
            if (actual.count < maxAttempts) {
                actual.count++;
                return new Decision(true, 0);
            }
            long elapsed = Math.max(0, Duration.between(actual.start, now).getSeconds());
            long retry = Math.max(1, ventana.toSeconds() - elapsed);
            return new Decision(false, retry);
        }
    }

    private void purge(Instant now) {
        if (windows.size() < CLEANUP_FROM) return;
        windows.entrySet().removeIf(entry -> {
            Ventana candidate = entry.getValue();
            synchronized (candidate) {
                return !now.isBefore(candidate.start.plus(ventana));
            }
        });
    }

    private static final class Ventana {
        private Instant start;
        private int count;

        private Ventana(Instant start) {
            this.start = start;
        }
    }
}
