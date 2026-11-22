package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Emisión de un certificado: solo para matrículas finalizadas y cuenta Premium. */
public record AdminCertificateForm(
        @NotNull(message = "Elegí una persona.") UUID userId,
        @NotNull(message = "Elegí un curso.") UUID courseId
) {
}
