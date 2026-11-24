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
 * Video de un módulo. Los videos nuevos viven en un bucket privado y se sirven
 * por clave; {@code url} se conserva solo para los videos históricos.
 */
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

    /** URL histórica; los videos privados usan {@code objectKey}. */
    @Column
    private String url;

    @Column(nullable = false)
    private int position;

    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "subtitle_key")
    private String subtitleKey;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(length = 2000)
    private String transcript;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Video() {
    }

    public Video(UUID moduleId, String title, String url, int position) {
        this(moduleId, title, url, position, null, null, 0, null);
    }

    public Video(UUID moduleId, String title, String url, int position,
                 String objectKey, String subtitleKey, int durationSeconds, String transcript) {
        this.moduleId = moduleId;
        this.title = title;
        this.url = url;
        this.position = position;
        this.objectKey = objectKey;
        this.subtitleKey = subtitleKey;
        this.durationSeconds = durationSeconds;
        this.transcript = transcript;
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

    public String getUrl() {
        return url;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getSubtitleKey() {
        return subtitleKey;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getTranscript() {
        return transcript;
    }

    public int getPosition() {
        return position;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(String title, String url, int position) {
        update(title, url, position, objectKey, subtitleKey, durationSeconds, transcript);
    }

    public void update(String title, String url, int position, String objectKey,
                       String subtitleKey, int durationSeconds, String transcript) {
        this.title = title;
        this.url = url;
        this.position = position;
        this.objectKey = objectKey;
        this.subtitleKey = subtitleKey;
        this.durationSeconds = durationSeconds;
        this.transcript = transcript;
    }

    public void moveTo(UUID moduleId) {
        this.moduleId = moduleId;
    }
}
