package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Fila de la tabla de gestión de cursos. */
public record AdminFilaCurso(
        UUID id, String slug, String titulo, String categoria, int lessons,
        String status, String price, int posicion, long inscripto, String imagen
) {
}
