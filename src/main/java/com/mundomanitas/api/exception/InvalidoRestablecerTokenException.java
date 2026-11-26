package com.mundomanitas.api.exception;

/** El enlace de restablecimiento no existe, ya se usó o venció. */
public class InvalidoRestablecerTokenException extends RuntimeException {

    public InvalidoRestablecerTokenException() {
        super("El enlace para restablecer la contraseña no es válido o venció.");
    }
}
