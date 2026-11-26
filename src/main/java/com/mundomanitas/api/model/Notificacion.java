package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Aviso generado por una acción real de la cuenta. */
@Entity
@Table(name = "notifications")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario usuario;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "target_url")
    private String targetUrl;

    @Column(name = "read_at")
    private OffsetDateTime fechaLectura;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected Notificacion() {
    }

    public Notificacion(Usuario usuario, String kind, String titulo, String message, String targetUrl) {
        this.usuario = usuario;
        this.kind = kind;
        this.titulo = titulo;
        this.message = message;
        this.targetUrl = targetUrl;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getKind() {
        return kind;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public OffsetDateTime getFechaLectura() {
        return fechaLectura;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public boolean isLeido() {
        return fechaLectura != null;
    }

    public void markLeido() {
        if (fechaLectura == null) {
            fechaLectura = OffsetDateTime.now();
        }
    }
}
