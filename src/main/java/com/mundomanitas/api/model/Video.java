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
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(nullable = false)
    private String title;

    @Column(name = "youtube_id", length = 11)
    private String youtubeId;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Video() {
    }

    public Video(UUID moduleId, String title, String youtubeId, int position) {
        this.moduleId = moduleId;
        this.title = title;
        this.youtubeId = youtubeId;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public String getTitle() {
        return title;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public int getPosition() {
        return position;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(String title, String youtubeId, int position) {
        this.title = title;
        this.youtubeId = youtubeId;
        this.position = position;
    }

    public void moveTo(UUID moduleId) {
        this.moduleId = moduleId;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
