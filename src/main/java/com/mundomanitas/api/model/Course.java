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
 * {@code position} conserva el orden en el que se muestran las tarjetas.</p>
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private int lessons;

    @Column(nullable = false)
    private String status = "BORRADOR";

    @Column(nullable = false)
    private boolean free;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Constructor vacío que exige JPA. */
    protected Course() {
    }

    public Course(
            String slug,
            String title,
            String level,
            int lessons,
            boolean free,
            String category,
            String summary,
            String image,
            int position
    ) {
        this.slug = slug;
        this.title = title;
        this.level = level;
        this.lessons = lessons;
        this.free = free;
        this.category = category;
        this.summary = summary;
        this.image = image;
        this.position = position;
        this.status = "BORRADOR";
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getLevel() {
        return level;
    }

    public int getLessons() {
        return lessons;
    }

    public String getStatus() {
        return status;
    }

    public void publish() {
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

    public String getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public String getImage() {
        return image;
    }

    public int getPosition() {
        return position;
    }

    /** Texto de acceso que se muestra en la tarjeta y en el detalle. */
    public String priceLabel() {
        return free ? "Gratis" : "Premium";
    }

    /** Actualiza la ficha desde el panel administrativo. */
    public void update(
            String slug,
            String title,
            String level,
            boolean free,
            String category,
            String summary,
            String image,
            int position
    ) {
        this.slug = slug;
        this.title = title;
        this.level = level;
        this.free = free;
        this.category = category;
        this.summary = summary;
        this.image = image;
        this.position = position;
    }
}
