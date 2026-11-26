package com.mundomanitas.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario público de registro.
 */
public class RegistroFormulario {

    @NotBlank(message = "Escribí tu nombre completo.")
    @Size(max = 120, message = "Usá menos de 120 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "Escribí tu correo.")
    @Email(message = "Ese correo no parece válido.")
    @Size(max = 254, message = "Ese correo es demasiado largo.")
    private String email;

    @NotBlank(message = "Elegí una contraseña.")
    @Size(min = 8, max = 72, message = "Usá entre 8 y 72 caracteres.")
    private String contrasena;

    @NotBlank(message = "Repetí la contraseña.")
    private String confirmContrasena;

    @AssertTrue(message = "Tenés que aceptar los términos para continuar.")
    private boolean acceptTerms;

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

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getConfirmContrasena() {
        return confirmContrasena;
    }

    public void setConfirmContrasena(String confirmContrasena) {
        this.confirmContrasena = confirmContrasena;
    }

    public boolean isAcceptTerms() {
        return acceptTerms;
    }

    public void setAcceptTerms(boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }
}
