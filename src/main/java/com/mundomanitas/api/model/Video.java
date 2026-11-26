package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una clase del curso; su única fuente es el identificador de YouTube. */
@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduloId;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "youtube_id", length = 11)
    private String youtubeId;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected Video() {
    }

    public Video(UUID moduloId, String titulo, String youtubeId, int posicion) {
        this.moduloId = moduloId;
        this.titulo = titulo;
        this.youtubeId = youtubeId;
        this.posicion = posicion;
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

    public String getYoutubeId() {
        return youtubeId;
    }

    public int getPosicion() {
        return posicion;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void update(String titulo, String youtubeId, int posicion) {
        this.titulo = titulo;
        this.youtubeId = youtubeId;
        this.posicion = posicion;
    }

    public void moveTo(UUID moduloId) {
        this.moduloId = moduloId;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }
}
