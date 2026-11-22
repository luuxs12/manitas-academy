package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Matrícula finalizada y Premium que todavía no tiene certificado activo. */
public record EligibleCertificate(
        UUID userId, String userName, UUID courseId, String courseTitle
) {
}
