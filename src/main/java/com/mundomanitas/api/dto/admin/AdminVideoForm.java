package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Alta y edición de una clase; la fuente es siempre un enlace de YouTube. */
public record AdminVideoForm(
        @NotNull(message = "Elegí un módulo.") UUID moduleId,

        @NotBlank(message = "Ingresá el título de la clase.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String title,

        @Min(value = 0, message = "La posición no puede ser negativa.")
        @Max(value = 9999, message = "La posición no es válida.")
        int position,

        @NotBlank(message = "Pegá un enlace de YouTube.")
        @Size(max = 300, message = "El enlace no puede superar los 300 caracteres.")
        @Pattern(regexp = "^https?://(?:www\\.|m\\.)?(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/).+$",
                message = "Pegá un enlace válido de YouTube.")
        String youtubeUrl
) {
    /** Normaliza el enlace para no guardar espacios. */
    public static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
