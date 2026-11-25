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
 * <p>Para videos y ejercicios, {@code sourceId} apunta al registro existente;
 * el título y el orden los sincroniza la base. Los materiales viven solo acá.</p>
 */
@Entity
@Table(name = "content_items")
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String url;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "youtube_id", length = 11)
    private String youtubeId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean visible;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ContentItem() {
    }

    public ContentItem(UUID moduleId, UUID sectionId, String sourceType, UUID sourceId,
                       String title, String description, String url, String youtubeId,
                       Integer durationSeconds, int position, boolean visible) {
        this(moduleId, sectionId, sourceType, sourceId, title, description, url, null,
                youtubeId, durationSeconds, position, visible);
    }

    public ContentItem(UUID moduleId, UUID sectionId, String sourceType, UUID sourceId,
                       String title, String description, String url, String fileName,
                       String youtubeId, Integer durationSeconds, int position, boolean visible) {
        this.moduleId = moduleId;
        this.sectionId = sectionId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.description = description;
        this.url = url;
        this.fileName = fileName;
        this.youtubeId = youtubeId;
        this.durationSeconds = durationSeconds;
        this.position = position;
        this.visible = visible;
    }

    public UUID getId() {
        return id;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public int getPosition() {
        return position;
    }

    public boolean isVisible() {
        return visible;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateMaterial(String title, String description, String url) {
        this.title = title;
        this.description = description;
        this.url = url;
    }

    public void updateDocument(String title, String description, String url, String fileName) {
        updateMaterial(title, description, url);
        this.fileName = fileName;
    }

    public void moveTo(UUID moduleId, UUID sectionId) {
        this.moduleId = moduleId;
        this.sectionId = sectionId;
    }

    public void setMetadata(Integer durationSeconds, boolean visible, int position) {
        this.durationSeconds = durationSeconds;
        this.visible = visible;
        this.position = position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
