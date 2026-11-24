package com.mundomanitas.api.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta y edición de cuentas desde el panel administrativo. */
public record AdminUserForm(
        @NotBlank(message = "Ingresá el nombre completo.")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
        String fullName,

        @NotBlank(message = "Ingresá el correo electrónico.")
        @Email(message = "Ingresá un correo válido.")
        String email,

        @NotBlank(message = "Elegí un rol.")
        @Pattern(regexp = "STUDENT|ADMIN", message = "El rol no es válido.")
        String role,

        boolean active,

        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
        String password
) {
    public AdminUserForm {
        // En la edición, dejar la contraseña vacía significa mantener la actual.
        if (password != null && password.isBlank()) password = null;
    }

    public String trimmedEmail() {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public String trimmedName() {
        return fullName == null ? "" : fullName.trim();
    }
}
