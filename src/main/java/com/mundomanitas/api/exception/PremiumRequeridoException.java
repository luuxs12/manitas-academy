package com.mundomanitas.api.exception;

/**
 * La cuenta intentó inscribirse en un curso de paga sin un plan Premium.
 */
public class PremiumRequeridoException extends RuntimeException {

    private final String cursoTitulo;

    public PremiumRequeridoException(String cursoTitulo) {
        super("El curso «" + cursoTitulo + "» requiere un plan Premium");
        this.cursoTitulo = cursoTitulo;
    }

    public String getCursoTitulo() {
        return cursoTitulo;
    }
}
