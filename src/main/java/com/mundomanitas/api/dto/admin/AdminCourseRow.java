package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Fila de la tabla de gestión de cursos. */
public record AdminCourseRow(
        UUID id, String slug, String title, String category, int lessons,
        String price, int position, long enrolled, String image
) {
}
