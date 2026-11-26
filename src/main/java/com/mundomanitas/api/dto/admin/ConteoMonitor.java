package com.mundomanitas.api.dto.admin;

/** Actividad de las últimas 24 horas, leída directamente de PostgreSQL. */
public record ConteoMonitor(
        long altas,
        long matriculas,
        long lecciones,
        long cuestionariosAprobados,
        long certificados,
        long pagos,
        long pagosFallidos
) {
}
