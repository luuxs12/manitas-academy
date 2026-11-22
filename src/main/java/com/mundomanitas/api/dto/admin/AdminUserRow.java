package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Fila de la tabla de gestión de cuentas. */
public record AdminUserRow(
        UUID id, String fullName, String email, String role, String plan,
        boolean active, String created, boolean current
) {
}
