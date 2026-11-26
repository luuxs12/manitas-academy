package com.mundomanitas.api.exception;

/**
 * La contraseña actual no coincide con la guardada en la cuenta.
 */
public class IncorrectaContrasenaException extends RuntimeException {

    public IncorrectaContrasenaException() {
        super("La contraseña actual no es correcta");
    }
}
