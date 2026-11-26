package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Opción legible para los selectores del panel; los módulos guardan su curso. */
public record AdminOpcion(UUID id, String label, UUID cursoId, String nombre) {

    public AdminOpcion(UUID id, String label) {
        this(id, label, null, label);
    }
}
