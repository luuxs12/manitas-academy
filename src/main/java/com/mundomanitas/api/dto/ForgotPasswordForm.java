package com.mundomanitas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulario para pedir el enlace de restablecimiento. */
public class ForgotPasswordForm {

    @NotBlank(message = "Escribí tu correo.")
    @Email(message = "Ese correo no parece válido.")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
