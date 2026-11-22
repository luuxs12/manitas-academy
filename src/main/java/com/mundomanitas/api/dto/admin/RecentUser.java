package com.mundomanitas.api.dto.admin;

import java.util.UUID;

/** Fila compacta de la lista "Usuarios recientes". */
public record RecentUser(
        UUID id, String fullName, String email, String role, String initials, String when
) {
}
