package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Alternativa de una pregunta; una sola es correcta. */
@Entity
@Table(name = "quiz_alternatives")
public class CuestionarioAlternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID preguntaId;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected CuestionarioAlternativa() {
    }

    public CuestionarioAlternativa(UUID preguntaId, int posicion, String label, boolean correct) {
        this.preguntaId = preguntaId;
        this.posicion = posicion;
        this.label = label;
        this.correct = correct;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPreguntaId() {
        return preguntaId;
    }

    public int getPosicion() {
        return posicion;
    }

    public String getLabel() {
        return label;
    }

    public boolean isCorrect() {
        return correct;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
