package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Curso con más matrículas y su avance real de lecciones. */
public record PopularCourse(
        UUID id, String slug, String title, String image, long enrolled, int percent
) {
}
