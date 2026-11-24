package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ficha editable de un curso; las lecciones se calculan, no se escriben. */
public record AdminCourseForm(
        @Pattern(regexp = "^$|[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "El identificador usa minúsculas y guiones.")
        String slug,

        @NotBlank(message = "Ingresá el título.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String title,

        @NotBlank(message = "Ingresá el nivel.")
        @Size(max = 60, message = "El nivel no puede superar los 60 caracteres.")
        String level,

        boolean free,

        @NotBlank(message = "Ingresá la categoría.")
        @Size(max = 60, message = "La categoría no puede superar los 60 caracteres.")
        String category,

        @NotBlank(message = "Ingresá el resumen.")
        @Size(max = 320, message = "El resumen no puede superar los 320 caracteres.")
        String summary,

        @Pattern(regexp = "^$|(/img/[A-Za-z0-9/_-]+\\.(jpg|jpeg|png|webp))"
                        + "|(/uploads/covers/[0-9a-f-]+\\.(jpg|png|webp))",
                message = "Usá una portada local real.")
        String image,

        @Min(value = 0, message = "La posición no puede ser negativa.")
        @Max(value = 9999, message = "La posición no es válida.")
        int position
) {
}
