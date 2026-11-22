package com.mundomanitas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña desde el perfil. */
public class PasswordChangeForm {

    @NotBlank(message = "Escribí tu contraseña actual.")
    private String currentPassword;

    @NotBlank(message = "Escribí la contraseña nueva.")
    @Size(min = 8, max = 72, message = "Usá entre 8 y 72 caracteres.")
    private String newPassword;

    @NotBlank(message = "Repetí la contraseña nueva.")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
