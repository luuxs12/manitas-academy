package com.mundomanitas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulario para elegir la contraseña nueva desde el enlace del correo. */
public class RestablecerContrasenaFormulario {

    @NotBlank(message = "Falta el enlace de restablecimiento.")
    private String token;

    @NotBlank(message = "Elegí una contraseña.")
    @Size(min = 8, max = 72, message = "Usá entre 8 y 72 caracteres.")
    private String contrasena;

    @NotBlank(message = "Repetí la contraseña.")
    private String confirmContrasena;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getPassword() {
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
}
