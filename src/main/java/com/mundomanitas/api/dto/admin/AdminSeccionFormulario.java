package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Alta de una sección dentro de un módulo. */
public record AdminSeccionFormulario(
        @NotNull(message = "Elegí un módulo.") UUID moduloId,

        @NotBlank(message = "Ingresá el título de la sección.")
        @Size(max = 140, message = "El título no puede superar los 140 caracteres.")
        String titulo,

        @Min(value = 0, message = "La posición no puede ser negativa.")
        @Max(value = 9999, message = "La posición no es válida.")
        int posicion,

        boolean visible
) {
}
