package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Certificado emitido, con nombres legibles y estado real. */
public record FilaCertificado(
        UUID id, String code, String usuarioNombre, String cursoTitulo,
        String emitido, boolean activo
) {
}
