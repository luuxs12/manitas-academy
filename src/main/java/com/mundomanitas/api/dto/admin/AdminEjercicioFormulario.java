package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Alta y edición de un ejercicio. */
public record AdminEjercicioFormulario(
        @NotNull(message = "Elegí un módulo.") UUID moduloId,
        @NotBlank(message = "Ingresá el título del ejercicio.") String titulo,
        @NotBlank(message = "Ingresá la consigna.") String prompt,
        @Min(value = 0, message = "La posición no puede ser negativa.") int posicion,
        Boolean marcarAlRendir
) {
    /** Un checkbox ausente mantiene el comportamiento original: marcar al rendir. */
    public AdminEjercicioFormulario {
        if (marcarAlRendir == null) marcarAlRendir = true;
    }

    /** Conserva la firma anterior; por defecto rendir el cuestionario marca el ejercicio. */
    public AdminEjercicioFormulario(UUID moduloId, String titulo, String prompt, int posicion) {
        this(moduloId, titulo, prompt, posicion, true);
    }
}
