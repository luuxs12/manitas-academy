package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Certificado emitido a una persona por un curso finalizado.
 *
 * <p>Solo se emite si la cuenta es Premium y la matrícula está completada.
 * La revocación es lógica para conservar el historial.</p>
 */
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected Certificate() {
    }

    public Certificate(UUID userId, UUID courseId, String code) {
        this.userId = userId;
        this.courseId = courseId;
        this.code = code;
        this.issuedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getCode() {
        return code;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public void revoke() {
        if (isActive()) {
            this.revokedAt = OffsetDateTime.now();
        }
    }
}
