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
    private final Duration window;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxAttempts, Duration window) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("El límite de intentos debe ser positivo");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("La ventana de intentos debe ser positiva");
        }
        this.maxAttempts = maxAttempts;
        this.window = window;
        log.info("Limitador de intentos configurado max={} ventana={}s",
                maxAttempts, window.toSeconds());
    }

    @Override
    public Decision check(String key, Instant now) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(now, "now");
        purge(now);

        Window current = windows.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (current) {
            if (!now.isBefore(current.start.plus(window))) {
                current.start = now;
                current.count = 0;
            }
            if (current.count < maxAttempts) {
                current.count++;
                return new Decision(true, 0);
            }
            long elapsed = Math.max(0, Duration.between(current.start, now).getSeconds());
            long retry = Math.max(1, window.toSeconds() - elapsed);
            return new Decision(false, retry);
        }
    }

    private void purge(Instant now) {
        if (windows.size() < CLEANUP_FROM) return;
        windows.entrySet().removeIf(entry -> {
            Window candidate = entry.getValue();
            synchronized (candidate) {
                return !now.isBefore(candidate.start.plus(window));
            }
        });
    }

    private static final class Window {
        private Instant start;
        private int count;

        private Window(Instant start) {
            this.start = start;
        }
    }
}
