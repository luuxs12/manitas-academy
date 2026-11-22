package com.mundomanitas.api.dto.admin;

import java.time.LocalDate;

/** Conteo real de un día para el gráfico de actividad. */
public record DayCount(LocalDate day, long total) {
}
