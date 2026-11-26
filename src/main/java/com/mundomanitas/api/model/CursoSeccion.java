package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Agrupador opcional de ítems dentro de un módulo. */
@Entity
@Table(name = "course_sections")
public class CursoSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduloId;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected CursoSeccion() {
    }

    public CursoSeccion(UUID moduloId, String titulo, int posicion, boolean visible) {
        this.moduloId = moduloId;
        this.titulo = titulo;
        this.posicion = posicion;
        this.visible = visible;
    }

    public UUID getId() {
        return id;
    }

    public UUID getModuloId() {
        return moduloId;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getPosicion() {
        return posicion;
    }

    public boolean isVisible() {
        return visible;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void update(String titulo, int posicion, boolean visible) {
        this.titulo = titulo;
        this.posicion = posicion;
        this.visible = visible;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }
}
