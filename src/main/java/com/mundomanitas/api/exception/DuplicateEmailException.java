package com.mundomanitas.api.exception;

/**
 * Se lanza cuando alguien intenta registrarse con un correo que ya existe.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Ya existe una cuenta con el correo " + email);
    }
}
