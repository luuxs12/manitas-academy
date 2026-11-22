package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ficha editable de un curso del catálogo. */
public record AdminCourseForm(
        @NotBlank(message = "Ingresá el identificador del curso.")
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "El identificador usa minúsculas y guiones, por ejemplo: python-desde-cero.")
        String slug,

        @NotBlank(message = "Ingresá el título.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String title,

        @NotBlank(message = "Ingresá el nivel.")
        String level,

        @Min(value = 1, message = "El curso debe tener al menos una lección.")
        @Max(value = 500, message = "El número de lecciones no es válido.")
        int lessons,

        boolean free,

        @NotBlank(message = "Ingresá la categoría.")
        String category,

        @NotBlank(message = "Ingresá el resumen.")
        @Size(max = 320, message = "El resumen no puede superar los 320 caracteres.")
        String summary,

        @NotBlank(message = "Ingresá la ruta de la imagen.")
        @Size(max = 300, message = "La URL de la imagen no puede superar los 300 caracteres.")
        @Pattern(regexp = "(https?://.+)|(/img/[A-Za-z0-9/_-]+\\.(jpg|jpeg|png|webp))",
                message = "Usá una ruta real bajo /img/ o una URL http(s) de R2.")
        String image,

        @Min(value = 0, message = "La posición no puede ser negativa.")
        int position
) {
}
