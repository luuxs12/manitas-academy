package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Enlace de un solo uso para restablecer la contraseña.
 *
 * <p>En la base vive el hash SHA-256, nunca el token que se envía por
 * correo. La tabla la crea Flyway en {@code V2__password_reset.sql}.</p>
 */
@Entity
@Table(name = "password_reset_tokens")
public class ContrasenaRestablecerToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    /** Constructor vacío que exige JPA. */
    protected ContrasenaRestablecerToken() {
    }

    public ContrasenaRestablecerToken(Usuario usuario, String tokenHash, OffsetDateTime fechaExpiracion) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.fechaExpiracion = fechaExpiracion;
    }

    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && fechaExpiracion.isAfter(now);
    }

    public void markUsed(OffsetDateTime now) {
        this.usedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
