package com.mundomanitas.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario público de registro.
 */
public class RegisterForm {

    @NotBlank(message = "Escribí tu nombre completo.")
    @Size(max = 120, message = "Usá menos de 120 caracteres.")
    private String fullName;

    @NotBlank(message = "Escribí tu correo.")
    @Email(message = "Ese correo no parece válido.")
    @Size(max = 254, message = "Ese correo es demasiado largo.")
    private String email;

    @NotBlank(message = "Elegí una contraseña.")
    @Size(min = 8, max = 72, message = "Usá entre 8 y 72 caracteres.")
    private String password;

    @NotBlank(message = "Repetí la contraseña.")
    private String confirmPassword;

    @AssertTrue(message = "Tenés que aceptar los términos para continuar.")
    private boolean acceptTerms;

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

    public boolean isAcceptTerms() {
        return acceptTerms;
    }

    public void setAcceptTerms(boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }
}
