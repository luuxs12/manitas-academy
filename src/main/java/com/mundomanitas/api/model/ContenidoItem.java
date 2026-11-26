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
 * Fila de contenido de un módulo: video, material o ejercicio.
 *
 * <p>Para videos y ejercicios, {@code origenId} apunta al registro existente;
 * el título y el orden los sincroniza la base. Los materiales viven solo acá.</p>
 */
@Entity
@Table(name = "content_items")
public class ContenidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduloId;

    @Column(name = "section_id")
    private UUID seccionId;

    @Column(name = "source_type", nullable = false)
    private String origenTipo;

    @Column(name = "source_id")
    private UUID origenId;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "description")
    private String descripcion;

    @Column(name = "url")
    private String url;

    @Column(name = "file_name")
    private String nombreArchivo;

    @Column(name = "youtube_id", length = 11)
    private String youtubeId;

    @Column(name = "duration_seconds")
    private Integer duracionSegundos;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected ContenidoItem() {
    }

    public ContenidoItem(UUID moduloId, UUID seccionId, String origenTipo, UUID origenId,
                       String titulo, String descripcion, String url, String youtubeId,
                       Integer duracionSegundos, int posicion, boolean visible) {
        this(moduloId, seccionId, origenTipo, origenId, titulo, descripcion, url, null,
                youtubeId, duracionSegundos, posicion, visible);
    }

    public ContenidoItem(UUID moduloId, UUID seccionId, String origenTipo, UUID origenId,
                       String titulo, String descripcion, String url, String nombreArchivo,
                       String youtubeId, Integer duracionSegundos, int posicion, boolean visible) {
        this.moduloId = moduloId;
        this.seccionId = seccionId;
        this.origenTipo = origenTipo;
        this.origenId = origenId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.url = url;
        this.nombreArchivo = nombreArchivo;
        this.youtubeId = youtubeId;
        this.duracionSegundos = duracionSegundos;
        this.posicion = posicion;
        this.visible = visible;
    }

    public UUID getId() {
        return id;
    }

    public UUID getModuloId() {
        return moduloId;
    }

    public UUID getSeccionId() {
        return seccionId;
    }

    public String getOrigenTipo() {
        return origenTipo;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return nombreArchivo;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public Integer getDuracionSegundos() {
        return duracionSegundos;
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

    public void updateMaterial(String titulo, String descripcion, String url) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.url = url;
    }

    public void updateDocument(String titulo, String descripcion, String url, String nombreArchivo) {
        updateMaterial(titulo, descripcion, url);
        this.nombreArchivo = nombreArchivo;
    }

    /** Actualiza la clase en lengua de señas guardada en el propio índice. */
    public void updateVideoLsp(String titulo, String descripcion, String youtubeId) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.youtubeId = youtubeId;
    }

    public void moveTo(UUID moduloId, UUID seccionId) {
        this.moduloId = moduloId;
        this.seccionId = seccionId;
    }

    public void setMetadata(Integer duracionSegundos, boolean visible, int posicion) {
        this.duracionSegundos = duracionSegundos;
        this.visible = visible;
        this.posicion = posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }
}
