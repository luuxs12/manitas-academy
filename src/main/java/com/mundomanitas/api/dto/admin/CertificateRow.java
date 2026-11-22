package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Certificado emitido, con nombres legibles y estado real. */
public record CertificateRow(
        UUID id, String code, String userName, String courseTitle,
        String issued, boolean active
) {
}
