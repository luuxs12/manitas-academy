package com.mundomanitas.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Preferencias de LSP, subtítulos y velocidad que elige la persona. */
public class AccesibilidadFormulario {

    private boolean signLanguageEnabled;

    @Size(max = 60)
    private String signLanguageVariant;

    private boolean captionsEnabled;

    @NotBlank
    @Pattern(regexp = "SMALL|MEDIUM|LARGE")
    private String captionSize = "MEDIUM";

    @NotNull
    @DecimalMin("0.50")
    @DecimalMax("2.00")
    private BigDecimal reproduccionSpeed = BigDecimal.ONE;

    public boolean isSignLanguageEnabled() {
        return signLanguageEnabled;
    }

    public void setSignLanguageEnabled(boolean signLanguageEnabled) {
        this.signLanguageEnabled = signLanguageEnabled;
    }

    public String getSignLanguageVariant() {
        return signLanguageVariant;
    }

    public void setSignLanguageVariant(String signLanguageVariant) {
        this.signLanguageVariant = signLanguageVariant;
    }

    public boolean isCaptionsEnabled() {
        return captionsEnabled;
    }

    public void setCaptionsEnabled(boolean captionsEnabled) {
        this.captionsEnabled = captionsEnabled;
    }

    public String getCaptionSize() {
        return captionSize;
    }

    public void setCaptionSize(String captionSize) {
        this.captionSize = captionSize;
    }

    public BigDecimal getReproduccionSpeed() {
        return reproduccionSpeed;
    }

    public void setReproduccionSpeed(BigDecimal reproduccionSpeed) {
        this.reproduccionSpeed = reproduccionSpeed;
    }
}
