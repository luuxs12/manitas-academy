package com.mundomanitas.api.dto.admin;

import java.time.LocalDate;
import java.util.List;

/** Ingresos cobrados por día, en centavos reales de los pagos confirmados. */
public record GraficoIngresos(
        LocalDate from,
        LocalDate to,
        List<String> etiquetas,
        List<String> points,
        long totalCents,
        long maxCents
) {
}
