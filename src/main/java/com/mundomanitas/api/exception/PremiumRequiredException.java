package com.mundomanitas.api.exception;

/**
 * La cuenta intentó inscribirse en un curso de paga sin un plan Premium.
 */
public class PremiumRequiredException extends RuntimeException {

    private final String courseTitle;

    public PremiumRequiredException(String courseTitle) {
        super("El curso «" + courseTitle + "» requiere un plan Premium");
        this.courseTitle = courseTitle;
    }

    public String getCourseTitle() {
        return courseTitle;
    }
}
