package com.mundomanitas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos editables del perfil. */
public class PerfilFormulario {

    @NotBlank(message = "Escribí tu nombre completo.")
    @Size(max = 120, message = "Usá menos de 120 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "Escribí tu correo.")
    @Email(message = "Ese correo no parece válido.")
    @Size(max = 254, message = "Ese correo es demasiado largo.")
    private String email;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
