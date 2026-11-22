package com.mundomanitas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos editables del perfil. */
public class ProfileForm {

    @NotBlank(message = "Escribí tu nombre completo.")
    @Size(max = 120, message = "Usá menos de 120 caracteres.")
    private String fullName;

    @NotBlank(message = "Escribí tu correo.")
    @Email(message = "Ese correo no parece válido.")
    @Size(max = 254, message = "Ese correo es demasiado largo.")
    private String email;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
