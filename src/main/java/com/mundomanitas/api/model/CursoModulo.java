package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Bloque de contenido de un curso. */
@Entity
@Table(name = "course_modules")
public class CursoModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID cursoId;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "visible", nullable = false)
    private boolean visible = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected CursoModulo() {
    }

    public CursoModulo(UUID cursoId, String titulo, int posicion) {
        this.cursoId = cursoId;
        this.titulo = titulo;
        this.posicion = posicion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCursoId() {
        return cursoId;
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

    public void update(String titulo, int posicion) {
        this.titulo = titulo;
        this.posicion = posicion;
    }

    public void moveTo(UUID cursoId) {
        this.cursoId = cursoId;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
