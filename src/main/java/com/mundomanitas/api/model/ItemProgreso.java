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
@IdClass(ItemProgreso.Clave.class)
public class ItemProgreso {

    @Id
    @Column(name = "enrollment_id", nullable = false)
    private UUID inscripcionId;

    @Id
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime fechaFinalizacion;

    protected ItemProgreso() {
    }

    public ItemProgreso(UUID inscripcionId, UUID itemId) {
        this.inscripcionId = inscripcionId;
        this.itemId = itemId;
        this.fechaFinalizacion = OffsetDateTime.now();
    }

    public UUID getInscripcionId() {
        return inscripcionId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public OffsetDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    /** Clave compuesta, necesaria para que Hibernate valide la tabla. */
    public static class Clave implements Serializable {
        @Column(name = "enrollment_id")
        private UUID inscripcionId;
        @Column(name = "item_id")
        private UUID itemId;

        public Clave() {
        }

        public Clave(UUID inscripcionId, UUID itemId) {
            this.inscripcionId = inscripcionId;
            this.itemId = itemId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Clave key)) return false;
            return Objects.equals(itemId, key.itemId)
                    && Objects.equals(inscripcionId, key.inscripcionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inscripcionId, itemId);
        }
    }
}
