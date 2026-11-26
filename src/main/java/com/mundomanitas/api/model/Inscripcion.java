package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Matrícula de una persona en un curso.
 *
 * <p>Una cuenta puede tener una sola matrícula por curso. La fecha de
 * finalización queda vacía hasta que la persona marca el curso como
 * terminado; con ella se habilita el certificado.</p>
 */
@Entity
@Table(name = "enrollments")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Curso curso;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime fechaInscripcion;

    @Column(name = "completed_at")
    private OffsetDateTime fechaFinalizacion;

    /** Constructor vacío que exige JPA. */
    protected Inscripcion() {
    }

    public Inscripcion(Usuario usuario, Curso curso) {
        this.usuario = usuario;
        this.curso = curso;
        this.fechaInscripcion = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Curso getCurso() {
        return curso;
    }

    public OffsetDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public OffsetDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public boolean isFinalizado() {
        return fechaFinalizacion != null;
    }

    /** Marca el curso como terminado; repetir la acción no cambia la fecha. */
    public void complete() {
        if (fechaFinalizacion == null) {
            fechaFinalizacion = OffsetDateTime.now();
        }
    }
}
