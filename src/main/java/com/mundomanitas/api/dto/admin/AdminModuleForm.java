package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Alta y edición de un módulo de contenido. */
public record AdminModuleForm(
        @NotNull(message = "Elegí un curso.") UUID courseId,
        @NotBlank(message = "Ingresá el título del módulo.") String title,
        @Min(value = 0, message = "La posición no puede ser negativa.") int position
) {
}
