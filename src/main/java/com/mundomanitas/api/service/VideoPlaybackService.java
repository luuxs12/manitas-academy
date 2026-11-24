package com.mundomanitas.api.service;

import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
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
    private final CourseModuleRepository modules;

    public VideoPlaybackService(VideoRepository videos, R2StorageService storage,
                                CourseModuleRepository modules) {
        this.videos = videos;
        this.storage = storage;
        this.modules = modules;
    }

    /** Datos mínimos para el reproductor; {@code available} es falso si aún no hay archivo. */
    public record Playback(String title, String url, String subtitleUrl, int durationSeconds,
                           String transcript, boolean legacy, boolean available) {
    }

    /** Tarjeta del temario: no incluye URLs firmadas para no firmar todo el curso. */
    public record CourseVideo(UUID id, UUID moduleId, String moduleTitle, String title,
                              int durationSeconds, String durationLabel,
                              boolean hasSubtitle, boolean hasTranscript) {
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

        if (StringUtils.hasText(video.getObjectKey())) {
            String subtitle = StringUtils.hasText(video.getSubtitleKey())
                    ? storage.presignedUrl(video.getSubtitleKey())
                    : null;
            String url = storage.presignedUrl(video.getObjectKey());
            return new Playback(video.getTitle(), url, subtitle, video.getDurationSeconds(),
                    video.getTranscript(), false, true);
        }

        String legacyUrl = video.getUrl();
        boolean available = StringUtils.hasText(legacyUrl);
        return new Playback(video.getTitle(), available ? legacyUrl : null, null,
                video.getDurationSeconds(), video.getTranscript(), true, available);
    }

    /** Temario del curso en orden de módulos, sin exponer claves privadas. */
    @Transactional(readOnly = true)
    public List<CourseVideo> courseVideos(UUID courseId) {
        List<CourseVideo> cards = new ArrayList<>();
        for (CourseModule module : modules.findByCourseIdOrderByPositionAsc(courseId)) {
            for (Video video : videos.findByModuleIdOrderByPositionAsc(module.getId())) {
                cards.add(new CourseVideo(
                        video.getId(),
                        module.getId(),
                        module.getTitle(),
                        video.getTitle(),
                        video.getDurationSeconds(),
                        durationLabel(video.getDurationSeconds()),
                        StringUtils.hasText(video.getSubtitleKey()),
                        StringUtils.hasText(video.getTranscript())
                ));
            }
        }
        return cards;
    }

    @Transactional(readOnly = true)
    public int videoCount(UUID courseId) {
        return videos.countByCourseId(courseId);
    }

    private static String durationLabel(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format("%d:%02d", safe / 60, safe % 60);
    }
}
