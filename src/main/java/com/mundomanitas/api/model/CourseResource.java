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
public class CourseResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int position;

    protected CourseResource() {
    }

    public CourseResource(UUID courseId, String title, String url, int position) {
        this.courseId = courseId;
        this.title = title;
        this.url = url;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public int getPosition() {
        return position;
    }

    public void update(String title, String url, int position) {
        this.title = title;
        this.url = url;
        this.position = position;
    }
}
