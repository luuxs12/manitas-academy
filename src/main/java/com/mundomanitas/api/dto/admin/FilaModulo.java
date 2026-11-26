package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Módulo con el nombre de su curso y sus cantidades reales. */
public record FilaModulo(
        UUID id, String cursoTitulo, String titulo, int posicion, long videos, long exercises
) {
}
