package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Curso con más matrículas y su avance real de lecciones. */
public record CursoPopular(
        UUID id, String slug, String titulo, String imagen, long inscripto, int percent
) {
}
