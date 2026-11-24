package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Entrega de videos al área de estudiante.
 *
 * <p>Un video solo se reproduce dentro de la matrícula de su curso. Las claves
 * privadas se firman al momento; las URLs históricas se respetan para no dejar
 * videos antiguos sin reproducir. Nunca se registran URLs firmadas en logs.</p>
 */
@Service
public class VideoPlaybackService {

    private final VideoRepository videos;
    private final R2StorageService storage;

    public VideoPlaybackService(VideoRepository videos, R2StorageService storage) {
        this.videos = videos;
        this.storage = storage;
    }

    /** Datos mínimos para el reproductor; {@code available} es falso si aún no hay archivo. */
    public record Playback(String title, String url, String subtitleUrl, int durationSeconds,
                           String transcript, boolean legacy, boolean available) {
    }

    @Transactional(readOnly = true)
    public Playback play(UUID videoId, Enrollment enrollment) {
        if (enrollment == null || enrollment.getCourse() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada");
        }
        Video video = videos.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Video no encontrado"));
        if (!videos.existsInCourse(videoId, enrollment.getCourse().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El video no pertenece a este curso");
        }

        if (video.getObjectKey() != null && !video.getObjectKey().isBlank()) {
            String subtitle = video.getSubtitleKey() == null || video.getSubtitleKey().isBlank()
                    ? null
                    : storage.presignedUrl(video.getSubtitleKey());
            String url = storage.presignedUrl(video.getObjectKey());
            return new Playback(video.getTitle(), url, subtitle, video.getDurationSeconds(),
                    video.getTranscript(), false, true);
        }

        String legacyUrl = video.getUrl();
        boolean available = legacyUrl != null && !legacyUrl.isBlank();
        return new Playback(video.getTitle(), available ? legacyUrl : null, null,
                video.getDurationSeconds(), video.getTranscript(), true, available);
    }
}
