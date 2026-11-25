package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Alta de un documento descargable dentro de una sección del temario. */
public record AdminDocumentForm(
        @NotNull(message = "Elegí un módulo.") UUID moduleId,

        UUID sectionId,

        @NotBlank(message = "Ingresá el título del documento.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String title,

        @Size(max = 4000, message = "La descripción no puede superar los 4000 caracteres.")
        String description,

        @NotNull(message = "Elegí un archivo.")
        MultipartFile file,

        @Min(value = 0, message = "La posición no puede ser negativa.")
        @Max(value = 9999, message = "La posición no es válida.")
        int position,

        boolean visible
) {
}
