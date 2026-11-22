package com.mundomanitas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulario para elegir la contraseña nueva desde el enlace del correo. */
public class ResetPasswordForm {

    @NotBlank(message = "Falta el enlace de restablecimiento.")
    private String token;

    @NotBlank(message = "Elegí una contraseña.")
    @Size(min = 8, max = 72, message = "Usá entre 8 y 72 caracteres.")
    private String password;

    @NotBlank(message = "Repetí la contraseña.")
    private String confirmPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
