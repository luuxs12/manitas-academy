package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Intento único y definitivo del alumno en un ejercicio. */
@Entity
@Table(name = "quiz_attempts")
public class CuestionarioIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID inscripcionId;

    @Column(name = "exercise_id", nullable = false)
    private UUID ejercicioId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private OffsetDateTime submittedAt;

    protected CuestionarioIntento() {
    }

    public CuestionarioIntento(UUID inscripcionId, UUID ejercicioId, int score) {
        this.inscripcionId = inscripcionId;
        this.ejercicioId = ejercicioId;
        this.score = score;
        this.submittedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInscripcionId() {
        return inscripcionId;
    }

    public UUID getEjercicioId() {
        return ejercicioId;
    }

    public int getScore() {
        return score;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }
}
