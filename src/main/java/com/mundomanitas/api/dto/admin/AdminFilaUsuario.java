package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Fila de la tabla de gestión de cuentas. */
public record AdminFilaUsuario(
        UUID id, String nombreCompleto, String email, String rol, String plan,
        boolean activo, String creado, boolean actual
) {
}
