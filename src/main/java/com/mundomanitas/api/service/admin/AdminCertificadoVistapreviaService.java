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
public interface AdminCertificadoVistapreviaService {

    record Vistaprevia(UUID id, String code, OffsetDateTime fechaEmision, String person,
                   String cursoTitulo, int videoContar, boolean activo) {
    }

    Optional<Vistaprevia> vistaprevia(UUID id);
}
