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
public class QuizAlternative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String label;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected QuizAlternative() {
    }

    public QuizAlternative(UUID questionId, int position, String label, boolean correct) {
        this.questionId = questionId;
        this.position = position;
        this.label = label;
        this.correct = correct;
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public int getPosition() {
        return position;
    }

    public String getLabel() {
        return label;
    }

    public boolean isCorrect() {
        return correct;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
