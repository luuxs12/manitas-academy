package com.mundomanitas.api.dto.admin;

/** Indicadores de los reportes, calculados sobre datos reales. */
public record ReportSummary(
        long enrollments,
        long completed,
        int completionPercent,
        long revenueCents,
        long activeUsers,
        long totalUsers,
        int retentionPercent
) {
}
