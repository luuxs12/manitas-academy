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
 * Un video terminado dentro de una matrícula.
 *
 * <p>El avance se apoya en videos reales del curso: nunca se guarda progreso
 * sobre una lección inventada ni sobre contenido de otro curso.</p>
 */
@Entity
@Table(name = "lesson_progress")
@IdClass(LessonProgress.Key.class)
public class LessonProgress {

    @Id
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Id
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    protected LessonProgress() {
    }

    public LessonProgress(UUID enrollmentId, UUID videoId) {
        this.enrollmentId = enrollmentId;
        this.videoId = videoId;
        this.completedAt = OffsetDateTime.now();
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    /** Clave compuesta, necesaria para que Hibernate valide la tabla. */
    public static class Key implements Serializable {
        private UUID enrollmentId;
        private UUID videoId;

        public Key() {
        }

        public Key(UUID enrollmentId, UUID videoId) {
            this.enrollmentId = enrollmentId;
            this.videoId = videoId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(videoId, key.videoId)
                    && Objects.equals(enrollmentId, key.enrollmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enrollmentId, videoId);
        }
    }
}
