package com.mundomanitas.api.dto;

import com.mundomanitas.api.model.Plan;

/**
 * Datos de la persona autenticada que necesitan las vistas.
 *
 * <p>Se arma desde PostgreSQL en cada petición para que el panel refleje
 * enseguida un cambio de nombre, correo, foto o plan.</p>
 */
public record UsuarioActual(String nombreCompleto, String email, Plan plan, String avatarPath,
                          String theme, String rol) {

    public String firstNombre() {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "";
        }
        return nombreCompleto.trim().split("\\s+")[0];
    }

    public String initials() {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "?";
        }
        String[] parts = nombreCompleto.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    public String planLabel() {
        return plan.getLabel();
    }

    public boolean premium() {
        return plan.isPremium();
    }

    public boolean hasAvatar() {
        return avatarPath != null && !avatarPath.isBlank();
    }

    public boolean admin() {
        return "ADMIN".equals(rol);
    }

    public String rolLabel() {
        return switch (rol == null ? "" : rol) {
            case "ADMIN" -> "Administrador";
            default -> "Estudiante";
        };
    }
}
