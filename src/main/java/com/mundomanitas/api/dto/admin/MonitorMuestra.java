package com.mundomanitas.api.dto.admin;

import java.time.Instant;

/** Una petición real ya terminada; alimenta las series del monitor. */
public record MonitorMuestra(
        Instant tiempo,
        String metodo,
        String ruta,
        int estado,
        long duracionMs,
        String requestId
) {
}
