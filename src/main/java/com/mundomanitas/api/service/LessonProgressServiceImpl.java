package com.mundomanitas.api.service;

import com.google.common.collect.ImmutableSet;
import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Avance real por video.
 *
 * <p>Solo se marcan videos que existen en PostgreSQL y pertenecen al curso
 * inscripto. El total sale de los videos cargados, no del número declarado
 * en el catálogo.</p>
 */
@Service
public class LessonProgressServiceImpl implements LessonProgressService {

    private static final Logger log = LoggerFactory.getLogger(LessonProgressServiceImpl.class);

    private final LessonProgressRepository progress;
    private final EnrollmentRepository enrollments;
    private final VideoRepository videos;
    private final ContentItemRepository items;

    public LessonProgressServiceImpl(
            LessonProgressRepository progress,
            EnrollmentRepository enrollments,
            VideoRepository videos,
            ContentItemRepository items
    ) {
        this.progress = progress;
        this.enrollments = enrollments;
        this.videos = videos;
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonProgress> forEnrollment(UUID enrollmentId) {
        return progress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public int completed(UUID enrollmentId) {
        return (int) progress.countByEnrollmentId(enrollmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> completedVideos(UUID enrollmentId) {
        ImmutableSet.Builder<UUID> result = ImmutableSet.builder();
        for (LessonProgress item : forEnrollment(enrollmentId)) {
            result.add(item.getVideoId());
        }
        return result.build();
    }

    /**
     * Marca o desmarca un video. Devuelve true si al hacerlo se terminó
     * el curso completo.
     */
    @Override
    @Transactional
    public boolean toggle(User user, String slug, UUID videoId) {
        checkNotNull(user, "user");
        checkNotNull(videoId, "videoId");
        Enrollment enrollment = enrollments.findByUserAndCourseSlug(user, requiredSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No estás inscripto en ese curso"));
        UUID courseId = enrollment.getCourse().getId();

        if (!videos.findById(videoId).isPresent()
                || !videos.existsInCourse(videoId, courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El video no pertenece a este curso");
        }
        ContentItem source = items.findBySourceTypeAndSourceId("VIDEO", videoId);
        if (source != null && !source.isVisible()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El video no está disponible");
        }
        int total = videos.countByCourseId(courseId);
        if (total == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Este curso todavía no tiene videos");
        }

        LessonProgress.Key key = new LessonProgress.Key(enrollment.getId(), videoId);
        progress.findById(key).ifPresentOrElse(
                progress::delete,
                () -> progress.save(new LessonProgress(enrollment.getId(), videoId))
        );

        boolean completed = progress.countByEnrollmentId(enrollment.getId()) == total;
        log.info("Video actualizado en matrícula {}; curso completo={}",
                enrollment.getId(), completed);
        return completed;
    }

    /** Marca todos los videos cuando se usa el acceso directo "Ya terminé". */
    @Override
    @Transactional
    public void completeAll(Enrollment enrollment) {
        checkNotNull(enrollment, "enrollment");
        Set<UUID> existing = completedVideos(enrollment.getId());
        List<UUID> courseVideos = videos.findIdsByCourseId(enrollment.getCourse().getId());
        for (UUID videoId : courseVideos) {
            if (!existing.contains(videoId)) {
                progress.save(new LessonProgress(enrollment.getId(), videoId));
            }
        }
        log.debug("Matrícula {} completada con {} videos",
                enrollment.getId(), courseVideos.size());
    }

    /** Total de videos terminados por la cuenta, sobre sus matrículas. */
    @Override
    @Transactional(readOnly = true)
    public LessonTotals totals(List<Enrollment> mine) {
        int completedVideos = 0;
        int totalVideos = 0;
        for (Enrollment enrollment : mine) {
            totalVideos += videos.countByCourseId(enrollment.getCourse().getId());
            completedVideos += completed(enrollment.getId());
        }
        int percent = totalVideos == 0
                ? 0
                : (int) Math.round(completedVideos * 100.0 / totalVideos);
        return new LessonTotals(completedVideos, totalVideos, percent);
    }

    private static String requiredSlug(String slug) {
        String clean = StringUtils.trimToNull(slug);
        if (clean == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no encontrado");
        }
        return clean;
    }
}
