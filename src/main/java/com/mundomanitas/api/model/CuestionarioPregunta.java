package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Pregunta de dos puntos dentro del cuestionario de un ejercicio. */
@Entity
@Table(name = "quiz_questions")
public class CuestionarioPregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "exercise_id", nullable = false)
    private UUID ejercicioId;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected CuestionarioPregunta() {
    }

    public CuestionarioPregunta(UUID ejercicioId, int posicion, String prompt) {
        this.ejercicioId = ejercicioId;
        this.posicion = posicion;
        this.prompt = prompt;
        this.points = 2;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEjercicioId() {
        return ejercicioId;
    }

    public int getPosicion() {
        return posicion;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getPoints() {
        return points;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
