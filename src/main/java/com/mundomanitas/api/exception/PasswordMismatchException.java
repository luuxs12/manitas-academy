package com.mundomanitas.api.exception;

/** Las dos copias de la contraseña nueva no coinciden. */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("Las contraseñas nuevas no coinciden");
    }
}
