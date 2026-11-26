package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Emisión de un certificado: solo para matrículas finalizadas y cuenta Premium. */
public record AdminCertificadoFormulario(
        @NotNull(message = "Elegí una persona.") UUID usuarioId,
        @NotNull(message = "Elegí un curso.") UUID cursoId
) {
}
