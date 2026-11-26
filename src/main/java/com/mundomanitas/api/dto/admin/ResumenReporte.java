package com.mundomanitas.api.dto.admin;

/** Indicadores de los reportes, calculados sobre datos reales. */
public record ResumenReporte(
        long enrollments,
        long finalizado,
        int completionPercent,
        long revenueCents,
        long activoUsers,
        long totalUsers,
        int retentionPercent
) {
}
