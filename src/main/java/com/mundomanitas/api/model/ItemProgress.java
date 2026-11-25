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

/** Material o ejercicio marcado como visto dentro de una matrícula. */
@Entity
@Table(name = "item_progress")
@IdClass(ItemProgress.Key.class)
public class ItemProgress {

    @Id
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Id
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    protected ItemProgress() {
    }

    public ItemProgress(UUID enrollmentId, UUID itemId) {
        this.enrollmentId = enrollmentId;
        this.itemId = itemId;
        this.completedAt = OffsetDateTime.now();
    }

    public UUID getEnrollmentId() {
        return enrollmentId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    /** Clave compuesta, necesaria para que Hibernate valide la tabla. */
    public static class Key implements Serializable {
        private UUID enrollmentId;
        private UUID itemId;

        public Key() {
        }

        public Key(UUID enrollmentId, UUID itemId) {
            this.enrollmentId = enrollmentId;
            this.itemId = itemId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(itemId, key.itemId)
                    && Objects.equals(enrollmentId, key.enrollmentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enrollmentId, itemId);
        }
    }
}
