package com.mundomanitas.api.dto.admin;

import java.time.LocalDate;
import java.util.List;

/** Serie temporal para el gráfico "Actividad general". */
public record GraficoActividad(
        int days,
        LocalDate from,
        LocalDate to,
        List<String> etiquetas,
        List<String> usersPoints,
        List<String> coursesPoints,
        int max,
        long usersTotal,
        long coursesTotal
) {
}
