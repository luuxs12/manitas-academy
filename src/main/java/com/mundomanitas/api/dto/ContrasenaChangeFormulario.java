package com.mundomanitas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña desde el perfil. */
public class ContrasenaChangeFormulario {

    @NotBlank(message = "Escribí tu contraseña actual.")
    private String actualContrasena;

    @NotBlank(message = "Escribí la contraseña nueva.")
    @Size(min = 8, max = 72, message = "Usá entre 8 y 72 caracteres.")
    private String newContrasena;

    @NotBlank(message = "Repetí la contraseña nueva.")
    private String confirmContrasena;

    public String getActualContrasena() {
        return actualContrasena;
    }

    public void setActualContrasena(String actualContrasena) {
        this.actualContrasena = actualContrasena;
    }

    public String getNewContrasena() {
        return newContrasena;
    }

    public void setNewContrasena(String newContrasena) {
        this.newContrasena = newContrasena;
    }

    public String getConfirmContrasena() {
        return confirmContrasena;
    }

    public void setConfirmContrasena(String confirmContrasena) {
        this.confirmContrasena = confirmContrasena;
    }
}
