package com.mundomanitas.api.security;

import java.time.Instant;

/** Decide si una clave todavía puede intentar una acción sensible. */
public interface RateLimiter {

    /**
     * Registra un intento y devuelve si se permite. Cuando se rechaza,
     * informa los segundos que faltan para reintentar.
     */
    Decision check(String key, Instant now);

    record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
