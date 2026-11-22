package com.mundomanitas.api.exception;

/**
 * La contraseña actual no coincide con la guardada en la cuenta.
 */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("La contraseña actual no es correcta");
    }
}
