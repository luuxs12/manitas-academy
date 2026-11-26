package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Ejercicio de un módulo, con la consigna que ve el estudiante. */
@Entity
@Table(name = "exercises")
public class Ejercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduloId;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "position", nullable = false)
    private int posicion;

    /** Define si el cuestionario marca el ejercicio como visto (V17). */
    @Column(name = "mark_on_quiz", nullable = false)
    private boolean marcarAlRendir = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    protected Ejercicio() {
    }

    public Ejercicio(UUID moduloId, String titulo, String prompt, int posicion) {
        this(moduloId, titulo, prompt, posicion, true);
    }

    public Ejercicio(UUID moduloId, String titulo, String prompt, int posicion,
                     boolean marcarAlRendir) {
        this.moduloId = moduloId;
        this.titulo = titulo;
        this.prompt = prompt;
        this.posicion = posicion;
        this.marcarAlRendir = marcarAlRendir;
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

    public String getPrompt() {
        return prompt;
    }

    public int getPosicion() {
        return posicion;
    }

    public boolean isMarcarAlRendir() {
        return marcarAlRendir;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void update(String titulo, String prompt, int posicion) {
        update(titulo, prompt, posicion, this.marcarAlRendir);
    }

    public void update(String titulo, String prompt, int posicion, boolean marcarAlRendir) {
        this.titulo = titulo;
        this.prompt = prompt;
        this.posicion = posicion;
        this.marcarAlRendir = marcarAlRendir;
    }

    public void moveTo(UUID moduloId) {
        this.moduloId = moduloId;
    }
}
