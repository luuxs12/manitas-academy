package com.mundomanitas.api.exception;

/** Las dos copias de la contraseña nueva no coinciden. */
public class ContrasenaNoCoincideException extends RuntimeException {

    public ContrasenaNoCoincideException() {
        super("Las contraseñas nuevas no coinciden");
    }
}
