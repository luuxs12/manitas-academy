package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Una lección terminada dentro de una matrícula.
 *
 * <p>El número de lección se valida contra {@code courses.lessons}; no hay
 * temario duplicado ni filas inventadas.</p>
 */
@Entity
@Table(name = "lesson_progress")
@IdClass(LessonProgress.Key.class)
public class LessonProgress {

    @Id
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Id
    @Column(name = "lesson_number", nullable = false)
    private int lessonNumber;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    protected LessonProgress() {
    }

    public LessonProgress(UUID enrollmentId, int lessonNumber) {
        this.enrollmentId = enrollmentId;
        this.lessonNumber = lessonNumber;
        this.completedAt = OffsetDateTime.now();
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    /** Clave compuesta, necesaria para que Hibernate valide la tabla. */
    public static class Key implements Serializable {
        private UUID enrollmentId;
        private int lessonNumber;

        public Key() {
        }

        public Key(UUID enrollmentId, int lessonNumber) {
            this.enrollmentId = enrollmentId;
            this.lessonNumber = lessonNumber;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return lessonNumber == key.lessonNumber
                    && Objects.equals(enrollmentId, key.enrollmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enrollmentId, lessonNumber);
        }
    }
}
