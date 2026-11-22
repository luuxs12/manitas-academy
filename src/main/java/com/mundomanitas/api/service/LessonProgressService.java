package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Avance real por lección.
 *
 * <p>Las lecciones no se inventan: se numeran del 1 al total declarado por
 * el curso en PostgreSQL. Solo se guarda lo que la persona marca.</p>
 */
@Service
public class LessonProgressService {

    private final LessonProgressRepository progress;
    private final EnrollmentRepository enrollments;

    public LessonProgressService(
            LessonProgressRepository progress,
            EnrollmentRepository enrollments
    ) {
        this.progress = progress;
        this.enrollments = enrollments;
    }

    @Transactional(readOnly = true)
    public List<LessonProgress> forEnrollment(UUID enrollmentId) {
        return progress.findByEnrollmentIdOrderByLessonNumberAsc(enrollmentId);
    }

    @Transactional(readOnly = true)
    public int completed(UUID enrollmentId) {
        return (int) progress.countByEnrollmentId(enrollmentId);
    }

    @Transactional(readOnly = true)
    public Set<Integer> completedLessons(UUID enrollmentId) {
        Set<Integer> result = new HashSet<>();
        for (LessonProgress item : forEnrollment(enrollmentId)) {
            result.add(item.getLessonNumber());
        }
        return result;
    }

    /**
     * Marca o desmarca una lección. Devuelve true si al hacerlo se terminó
     * el curso completo.
     */
    @Transactional
    public boolean toggle(User user, String slug, int lessonNumber) {
        Enrollment enrollment = enrollments.findByUserAndCourseSlug(user, slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No estás inscripto en ese curso"));

        int total = enrollment.getCourse().getLessons();
        if (lessonNumber < 1 || lessonNumber > total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lección fuera del curso");
        }

        LessonProgress.Key key = new LessonProgress.Key(enrollment.getId(), lessonNumber);
        progress.findById(key).ifPresentOrElse(
                progress::delete,
                () -> progress.save(new LessonProgress(enrollment.getId(), lessonNumber))
        );

        return progress.countByEnrollmentId(enrollment.getId()) == total;
    }

    /** Marca todas las lecciones cuando se usa el acceso directo "Ya terminé". */
    @Transactional
    public void completeAll(Enrollment enrollment) {
        Set<Integer> existing = completedLessons(enrollment.getId());
        for (int lesson = 1; lesson <= enrollment.getCourse().getLessons(); lesson++) {
            if (!existing.contains(lesson)) {
                progress.save(new LessonProgress(enrollment.getId(), lesson));
            }
        }
    }

    /** Total de lecciones terminadas por la cuenta, sobre sus matrículas. */
    @Transactional(readOnly = true)
    public LessonTotals totals(List<Enrollment> mine) {
        int completedLessons = 0;
        int totalLessons = 0;
        for (Enrollment enrollment : mine) {
            totalLessons += enrollment.getCourse().getLessons();
            completedLessons += completed(enrollment.getId());
        }
        int percent = totalLessons == 0
                ? 0
                : (int) Math.round(completedLessons * 100.0 / totalLessons);
        return new LessonTotals(completedLessons, totalLessons, percent);
    }

    public record LessonTotals(int completed, int total, int percent) {
    }
}
