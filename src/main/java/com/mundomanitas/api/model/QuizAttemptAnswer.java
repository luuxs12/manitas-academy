package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Respuesta elegida por pregunta dentro de un intento. */
@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "alternative_id", nullable = false)
    private UUID alternativeId;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    protected QuizAttemptAnswer() {
    }

    public QuizAttemptAnswer(UUID attemptId, UUID questionId, UUID alternativeId,
                             int pointsEarned) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.alternativeId = alternativeId;
        this.pointsEarned = pointsEarned;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public UUID getAlternativeId() {
        return alternativeId;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }
}
