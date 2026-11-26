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
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(nullable = false)
    private int score;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private OffsetDateTime submittedAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(UUID enrollmentId, UUID exerciseId, int score) {
        this.enrollmentId = enrollmentId;
        this.exerciseId = exerciseId;
        this.score = score;
        this.submittedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public int getScore() {
        return score;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }
}
