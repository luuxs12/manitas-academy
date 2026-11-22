package com.mundomanitas.api.dto;

import com.mundomanitas.api.model.Plan;

/**
 * Datos de la persona autenticada que necesitan las vistas.
 *
 * <p>Se arma desde PostgreSQL en cada petición para que el panel refleje
 * enseguida un cambio de nombre, correo, foto o plan.</p>
 */
public record CurrentUser(String fullName, String email, Plan plan, String avatarPath,
                          String theme, String role) {

    public String firstName() {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.trim().split("\\s+")[0];
    }

    public String initials() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
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
        return "ADMIN".equals(role);
    }

    public String roleLabel() {
        return switch (role == null ? "" : role) {
            case "ADMIN" -> "Administrador";
            case "TEACHER" -> "Docente";
            default -> "Estudiante";
        };
    }
}
