package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Alta de un video, material o ejercicio dentro de un módulo. */
public record AdminItemForm(
        @NotNull(message = "Elegí un módulo.") UUID moduleId,

        UUID sectionId,

        @NotBlank(message = "Elegí el tipo de ítem.")
        @Pattern(regexp = "^(VIDEO|MATERIAL|EJERCICIO)$", message = "El tipo de ítem no es válido.")
        String type,

        @NotBlank(message = "Ingresá el título del ítem.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String title,

        @Size(max = 4000, message = "La descripción no puede superar los 4000 caracteres.")
        String description,

        @Size(max = 300, message = "El enlace no puede superar los 300 caracteres.")
        String url,

        @Size(max = 300, message = "El enlace no puede superar los 300 caracteres.")
        String youtubeUrl,

        @Min(value = 0, message = "La duración no puede ser negativa.")
        @Max(value = 86400, message = "La duración no es válida.")
        Integer durationSeconds,

        @Min(value = 0, message = "La posición no puede ser negativa.")
        @Max(value = 9999, message = "La posición no es válida.")
        int position,

        boolean visible
) {
}
