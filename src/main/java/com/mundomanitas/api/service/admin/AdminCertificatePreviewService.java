package com.mundomanitas.api.service.admin;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Vista previa administrativa de un certificado.
 *
 * <p>Incluye los revocados: el admin conserva el historial y puede revisar
 * el documento tal como se emitió.</p>
 */
public interface AdminCertificatePreviewService {

    record Preview(UUID id, String code, OffsetDateTime issuedAt, String person,
                   String courseTitle, int videoCount, boolean active) {
    }

    Optional<Preview> preview(UUID id);
}
