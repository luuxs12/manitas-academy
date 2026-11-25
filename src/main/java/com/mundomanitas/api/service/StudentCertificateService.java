package com.mundomanitas.api.service;

import com.mundomanitas.api.model.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Certificados que la cuenta puede ver.
 *
 * <p>Solo aparecen los que están vigentes en PostgreSQL: un certificado
 * revocado deja de ofrecerse aunque la matrícula siga completada.</p>
 */
public interface StudentCertificateService {

    record Issued(UUID id, String code, OffsetDateTime issuedAt, String courseTitle,
                  String courseSlug, int videoCount) {
    }

    /** Certificados vigentes, del más reciente al más viejo. */
    List<Issued> available(User user);

    /** Certificado vigente de un curso, para abrir su ficha. */
    Optional<Issued> findActive(User user, String courseSlug);
}
