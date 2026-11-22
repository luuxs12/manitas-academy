package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Alta y edición de un ejercicio. */
public record AdminExerciseForm(
        @NotNull(message = "Elegí un módulo.") UUID moduleId,
        @NotBlank(message = "Ingresá el título del ejercicio.") String title,
        @NotBlank(message = "Ingresá la consigna.") String prompt,
        @Min(value = 0, message = "La posición no puede ser negativa.") int position
) {
}
