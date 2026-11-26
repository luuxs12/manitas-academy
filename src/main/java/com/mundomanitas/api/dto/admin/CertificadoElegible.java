package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Matrícula finalizada y Premium que todavía no tiene certificado activo. */
public record CertificadoElegible(
        UUID usuarioId, String usuarioNombre, UUID cursoId, String cursoTitulo
) {
}
