package com.mundomanitas.api.dto;

/** Confirmación explícita para borrar la cuenta. */
public class CuentaDeleteFormulario {

    private String confirmation;

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }

    public boolean isConfirmed() {
        return "ELIMINAR".equals(confirmation);
    }
}
