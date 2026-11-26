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
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String prompt;

    @Column(nullable = false)
    private int points;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected QuizQuestion() {
    }

    public QuizQuestion(UUID exerciseId, int position, String prompt) {
        this.exerciseId = exerciseId;
        this.position = position;
        this.prompt = prompt;
        this.points = 2;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public int getPosition() {
        return position;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getPoints() {
        return points;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
