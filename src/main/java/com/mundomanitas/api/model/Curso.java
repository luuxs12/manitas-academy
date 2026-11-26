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
 * Curso del catálogo, guardado en PostgreSQL.
 *
 * <p>La tabla la crea Flyway en {@code V3__courses_and_enrollments.sql}.
 * {@code posicion} conserva el orden en el que se muestran las tarjetas.</p>
 */
@Entity
@Table(name = "courses")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "title", nullable = false)
    private String titulo;

    @Column(name = "level", nullable = false)
    private String nivel;

    @Column(name = "lessons", nullable = false)
    private int lessons;

    @Column(name = "status", nullable = false)
    private String status = "BORRADOR";

    @Column(name = "free", nullable = false)
    private boolean free;

    @Column(name = "category", nullable = false)
    private String categoria;

    @Column(name = "summary", nullable = false)
    private String resumen;

    @Column(name = "image")
    private String imagen;

    @Column(name = "objectives")
    private String objectives;

    @Column(name = "requirements")
    private String requirements;

    @Column(name = "position", nullable = false)
    private int posicion;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    /** Constructor vacío que exige JPA. */
    protected Curso() {
    }

    public Curso(
            String slug,
            String titulo,
            String nivel,
            int lessons,
            boolean free,
            String categoria,
            String resumen,
            String imagen,
            int posicion
    ) {
        this.slug = slug;
        this.titulo = titulo;
        this.nivel = nivel;
        this.lessons = lessons;
        this.free = free;
        this.categoria = categoria;
        this.resumen = resumen;
        this.imagen = imagen;
        this.posicion = posicion;
        this.status = "BORRADOR";
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getNivel() {
        return nivel;
    }

    public int getLessons() {
        return lessons;
    }

    public String getEstado() {
        return status;
    }

    public void publicar() {
        this.status = "PUBLICADO";
    }

    public void unpublish() {
        this.status = "BORRADOR";
    }

    /** Actualiza el contador con el contenido real; nadie lo escribe a mano. */
    public void refreshLessons(int count) {
        this.lessons = Math.max(0, count);
    }

    public boolean isFree() {
        return free;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getResumen() {
        return resumen;
    }

    public String getImagen() {
        return imagen;
    }

    public String getObjectives() {
        return objectives;
    }

    public String getRequirements() {
        return requirements;
    }

    /** Datos del sílabo cargados desde el panel administrativo. */
    public void updateSilabo(String objectives, String requirements) {
        this.objectives = objectives;
        this.requirements = requirements;
    }

    /** Cambia la portada sin reescribir el resto de la ficha. */
    public void changeImagen(String imagen) {
        this.imagen = imagen;
    }

    public int getPosicion() {
        return posicion;
    }

    /** Texto de acceso que se muestra en la tarjeta y en el detalle. */
    public String priceLabel() {
        return free ? "Gratis" : "Premium";
    }

    /** Actualiza la ficha desde el panel administrativo. */
    public void update(
            String slug,
            String titulo,
            String nivel,
            boolean free,
            String categoria,
            String resumen,
            String imagen,
            int posicion
    ) {
        this.slug = slug;
        this.titulo = titulo;
        this.nivel = nivel;
        this.free = free;
        this.categoria = categoria;
        this.resumen = resumen;
        this.imagen = imagen;
        this.posicion = posicion;
    }
}
