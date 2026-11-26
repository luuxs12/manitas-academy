package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Preferencias de accesibilidad de una cuenta (tabla creada en V1).
 *
 * <p>No guarda discapacidades ni datos médicos: solo cómo quiere ver el
 * contenido la persona.</p>
 */
@Entity
@Table(name = "accessibility_preferences")
public class AccesibilidadPreferencias {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "sign_language_enabled", nullable = false)
    private boolean signLanguageEnabled;

    @Column(name = "sign_language_variant")
    private String signLanguageVariant;

    @Column(name = "captions_enabled", nullable = false)
    private boolean captionsEnabled = true;

    @Column(name = "caption_size", nullable = false)
    private String captionSize = "MEDIUM";

    @Column(name = "playback_speed", nullable = false, precision = 3, scale = 2)
    private BigDecimal reproduccionSpeed = BigDecimal.ONE;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    protected AccesibilidadPreferencias() {
    }

    public AccesibilidadPreferencias(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public boolean isSignLanguageEnabled() {
        return signLanguageEnabled;
    }

    public String getSignLanguageVariant() {
        return signLanguageVariant;
    }

    public boolean isCaptionsEnabled() {
        return captionsEnabled;
    }

    public String getCaptionSize() {
        return captionSize;
    }

    public BigDecimal getReproduccionSpeed() {
        return reproduccionSpeed;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void apply(boolean signLanguageEnabled, String signLanguageVariant,
                      boolean captionsEnabled, String captionSize, BigDecimal reproduccionSpeed) {
        this.signLanguageEnabled = signLanguageEnabled;
        this.signLanguageVariant = signLanguageVariant;
        this.captionsEnabled = captionsEnabled;
        this.captionSize = captionSize;
        this.reproduccionSpeed = reproduccionSpeed;
    }
}
