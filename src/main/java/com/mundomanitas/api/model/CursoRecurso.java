package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Enlace de la bibliografía complementaria del sílabo. */
@Entity
@Table(name = "course_resources")
public class CursoRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID cursoId;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "position", nullable = false)
    private int posicion;

    protected CursoRecurso() {
    }

    public CursoRecurso(UUID cursoId, String titulo, String url, int posicion) {
        this.cursoId = cursoId;
        this.titulo = titulo;
        this.url = url;
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

    public String getUrl() {
        return url;
    }

    public int getPosicion() {
        return posicion;
    }

    public void update(String titulo, String url, int posicion) {
        this.titulo = titulo;
        this.url = url;
        this.posicion = posicion;
    }
}
