package com.mundomanitas.api.exception;

/** El cobro no está configurado o no se pudo iniciar. */
public class PagoNoDisponibleException extends RuntimeException {

    public PagoNoDisponibleException(String message) {
        super(message);
    }
}
