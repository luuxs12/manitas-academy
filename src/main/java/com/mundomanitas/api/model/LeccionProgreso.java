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
@IdClass(LeccionProgreso.Clave.class)
public class LeccionProgreso {

    @Id
    @Column(name = "enrollment_id", nullable = false)
    private UUID inscripcionId;

    @Id
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime fechaFinalizacion;

    protected LeccionProgreso() {
    }

    public LeccionProgreso(UUID inscripcionId, UUID videoId) {
        this.inscripcionId = inscripcionId;
        this.videoId = videoId;
        this.fechaFinalizacion = OffsetDateTime.now();
    }

    public UUID getInscripcionId() {
        return inscripcionId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public OffsetDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    /** Clave compuesta, necesaria para que Hibernate valide la tabla. */
    public static class Clave implements Serializable {
        @Column(name = "enrollment_id")
        private UUID inscripcionId;
        @Column(name = "video_id")
        private UUID videoId;

        public Clave() {
        }

        public Clave(UUID inscripcionId, UUID videoId) {
            this.inscripcionId = inscripcionId;
            this.videoId = videoId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Clave key)) return false;
            return Objects.equals(videoId, key.videoId)
                    && Objects.equals(inscripcionId, key.inscripcionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inscripcionId, videoId);
        }
    }
}
