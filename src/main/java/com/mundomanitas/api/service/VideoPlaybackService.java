package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Entrega de clases de YouTube al área de estudiante. */
@Service
public class VideoPlaybackService {

    private final VideoRepository videos;
    private final CourseModuleRepository modules;
    private final ContentItemRepository items;

    public VideoPlaybackService(VideoRepository videos, CourseModuleRepository modules,
                                ContentItemRepository items) {
        this.videos = videos;
        this.modules = modules;
        this.items = items;
    }

    /** Dirección de incrustación construida en el servidor desde el ID guardado. */
    public record Playback(String title, String url) {
    }

    /** Tarjeta del temario en orden de módulos y lecciones. */
    public record CourseVideo(UUID id, UUID moduleId, String moduleTitle, String title) {
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
        ContentItem source = items.findBySourceTypeAndSourceId("VIDEO", videoId);
        if (source != null && !source.isVisible()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "La clase no está disponible");
        }
        String url = YouTubeLink.embedUrl(video.getYoutubeId());
        if (url == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "La clase todavía no tiene un video de YouTube");
        }
        return new Playback(video.getTitle(), url);
    }

    @Transactional(readOnly = true)
    public List<CourseVideo> courseVideos(UUID courseId) {
        List<CourseVideo> cards = new ArrayList<>();
        for (CourseModule module : modules.findByCourseIdOrderByPositionAsc(courseId)) {
            for (Video video : videos.findByModuleIdOrderByPositionAsc(module.getId())) {
                cards.add(new CourseVideo(video.getId(), module.getId(),
                        module.getTitle(), video.getTitle()));
            }
        }
        return cards;
    }

    @Transactional(readOnly = true)
    public int videoCount(UUID courseId) {
        return videos.countByCourseId(courseId);
    }
}
