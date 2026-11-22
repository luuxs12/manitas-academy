package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Módulo con el nombre de su curso y sus cantidades reales. */
public record ModuleRow(
        UUID id, String courseTitle, String title, int position, long videos, long exercises
) {
}
