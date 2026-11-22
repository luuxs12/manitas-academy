package com.mundomanitas.api.exception;

/** El cobro no está configurado o no se pudo iniciar. */
public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(String message) {
        super(message);
    }
}
