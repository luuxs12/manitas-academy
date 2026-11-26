package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro de una acción administrativa real.
 *
 * <p>La auditoría la escribe el servicio cuando la operación termina bien;
 * por eso cada fila corresponde a un cambio confirmado en PostgreSQL.</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditoriaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity", nullable = false)
    private String entity;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "detail", nullable = false)
    private String detail;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected AuditoriaRegistro() {
    }

    public AuditoriaRegistro(UUID actorId, String action, String entity, UUID entityId, String detail) {
        this.actorId = actorId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getEntity() {
        return entity;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getDetail() {
        return detail;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
