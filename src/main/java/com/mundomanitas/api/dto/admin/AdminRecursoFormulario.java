package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Enlace complementario del sílabo; solo se acepta https. */
public record AdminRecursoFormulario(
        @NotBlank(message = "Ingresá el título del recurso.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String titulo,

        @NotBlank(message = "Ingresá el enlace del recurso.")
        @Size(max = 300, message = "El enlace no puede superar los 300 caracteres.")
        @Pattern(regexp = "^https://.+$", message = "El enlace debe usar https.")
        String url,

        @NotNull(message = "La posición es obligatoria.")
        @Min(value = 0, message = "La posición no puede ser negativa.")
        @Max(value = 9999, message = "La posición no es válida.")
        Integer posicion
) {
}
