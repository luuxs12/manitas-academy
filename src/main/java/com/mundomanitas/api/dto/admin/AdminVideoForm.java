package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Alta y edición de un video. */
public record AdminVideoForm(
        @NotNull(message = "Elegí un módulo.") UUID moduleId,
        @NotBlank(message = "Ingresá el título del video.") String title,
        @NotBlank(message = "Ingresá la URL del video.")
        @Size(max = 300, message = "La URL del video no puede superar los 300 caracteres.")
        @Pattern(regexp = "(https?://.+)|(/video/[A-Za-z0-9/_-]+\\.mp4)",
                message = "Usá una URL http(s) o una ruta /video/… real del proyecto.")
        String url,
        @Min(value = 0, message = "La posición no puede ser negativa.") int position
) {
}
