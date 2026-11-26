package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Alta y edición de un módulo de contenido. */
public record AdminModuloFormulario(
        @NotNull(message = "Elegí un curso.") UUID cursoId,
        @NotBlank(message = "Ingresá el título del módulo.") String titulo,
        @Min(value = 0, message = "La posición no puede ser negativa.") int posicion
) {
}
