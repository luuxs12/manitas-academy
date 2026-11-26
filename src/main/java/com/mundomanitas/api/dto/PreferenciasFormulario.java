package com.mundomanitas.api.dto;

import jakarta.validation.constraints.Pattern;

/** Preferencias generales de la cuenta. */
public class PreferenciasFormulario {

    @Pattern(regexp = "SYSTEM|LIGHT|DARK")
    private String theme = "SYSTEM";

    private boolean notifyEmail = true;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(boolean notifyEmail) {
        this.notifyEmail = notifyEmail;
    }
}
