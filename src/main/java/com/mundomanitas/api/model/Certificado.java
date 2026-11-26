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
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "course_id", nullable = false)
    private UUID cursoId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime fechaEmision;

    @Column(name = "revoked_at")
    private OffsetDateTime fechaRevocacion;

    protected Certificado() {
    }

    public Certificado(UUID usuarioId, UUID cursoId, String code) {
        this.usuarioId = usuarioId;
        this.cursoId = cursoId;
        this.code = code;
        this.fechaEmision = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getCursoId() {
        return cursoId;
    }

    public String getCode() {
        return code;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public OffsetDateTime getFechaRevocacion() {
        return fechaRevocacion;
    }

    public boolean isActivo() {
        return fechaRevocacion == null;
    }

    public void revoke() {
        if (isActivo()) {
            this.fechaRevocacion = OffsetDateTime.now();
        }
    }
}
