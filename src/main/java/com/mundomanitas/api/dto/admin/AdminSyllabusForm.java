package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Size;

/** Objetivos y requisitos del sílabo de un curso. */
public record AdminSyllabusForm(
        @Size(max = 4000, message = "Los objetivos no pueden superar los 4000 caracteres.")
        String objectives,

        @Size(max = 4000, message = "Los requisitos no pueden superar los 4000 caracteres.")
        String requirements
) {
}
