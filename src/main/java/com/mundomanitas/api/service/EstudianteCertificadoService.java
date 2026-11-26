package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;

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
public interface EstudianteCertificadoService {

    record Emitido(UUID id, String code, OffsetDateTime fechaEmision, String cursoTitulo,
                  String cursoSlug, int videoContar) {
    }

    /** Certificados vigentes, del más reciente al más viejo. */
    List<Emitido> disponible(Usuario usuario);

    /** Certificado vigente de un curso, para abrir su ficha. */
    Optional<Emitido> buscarActivo(Usuario usuario, String cursoSlug);
}
