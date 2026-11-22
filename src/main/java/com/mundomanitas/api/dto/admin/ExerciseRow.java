package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Ejercicio con el nombre legible de su módulo y curso. */
public record ExerciseRow(UUID id, String moduleTitle, String courseTitle, String title, String prompt, int position) {
}
