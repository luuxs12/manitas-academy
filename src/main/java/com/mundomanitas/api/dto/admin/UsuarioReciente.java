package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Fila compacta de la lista "Usuarios recientes". */
public record UsuarioReciente(
        UUID id, String nombreCompleto, String email, String rol, String initials, String when
) {
}
