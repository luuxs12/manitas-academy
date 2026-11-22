package com.mundomanitas.api.service;

import com.mundomanitas.api.exception.PremiumRequiredException;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.EnrollmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Reglas de inscripción y avance de los cursos de una cuenta.
 *
 * <p>Nadie nace inscripto: cada matrícula se crea con una acción explícita.
 * Los cursos de paga requieren plan Premium y la fecha de finalización
 * habilita el certificado. Cada acción genera un aviso real.</p>
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final CourseCatalog catalog;
    private final LessonProgressService lessons;
    private final NotificationService notifications;
    private final CertificateIssuerService certificates;

    public EnrollmentService(
            EnrollmentRepository enrollments,
            CourseCatalog catalog,
            LessonProgressService lessons,
            NotificationService notifications,
            CertificateIssuerService certificates
    ) {
        this.enrollments = enrollments;
        this.catalog = catalog;
        this.lessons = lessons;
        this.notifications = notifications;
        this.certificates = certificates;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> mine(User user) {
        return enrollments.findByUserOrderByEnrolledAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> completed(User user) {
        return enrollments.findByUserAndCompletedAtIsNotNullOrderByCompletedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(UUID userId, String slug) {
        return enrollments.existsByUserIdAndCourseSlug(userId, slug);
    }

    @Transactional(readOnly = true)
    public Enrollment findMine(User user, String slug) {
        return enrollments.findByUserAndCourseSlug(user, slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No estás inscripto en ese curso"));
    }

    /**
     * Inscribe a la persona en un curso. Repetir la acción no crea otra
     * matrícula: devuelve la que ya existe.
     */
    @Transactional
    public Enrollment enroll(User user, String slug) {
        Course course = catalog.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (!course.isFree() && !user.isPremium()) {
            throw new PremiumRequiredException(course.getTitle());
        }

        var existing = enrollments.findByUserAndCourseSlug(user, slug);
        if (existing.isPresent()) {
            return existing.get();
        }

        Enrollment enrollment = enrollments.save(new Enrollment(user, course));
        notifications.notify(
                user,
                NotificationService.ENROLLMENT,
                "Te inscribiste en un curso",
                "«" + course.getTitle() + "» ya está en Mis cursos.",
                "/panel/cursos"
        );
        return enrollment;
    }

    /** Marca como terminado un curso en el que la persona está inscripta. */
    @Transactional
    public Enrollment complete(User user, String slug) {
        Enrollment enrollment = findMine(user, slug);
        lessons.completeAll(enrollment);
        if (!enrollment.isCompleted()) {
            enrollment.complete();
            enrollments.save(enrollment);
            notifications.notify(
                    user,
                    NotificationService.COMPLETION,
                    "Terminaste un curso",
                    "«" + enrollment.getCourse().getTitle() + "» quedó marcado como terminado.",
                    "/panel/certificados"
            );
            certificates.issueFor(enrollment);
        }
        return enrollment;
    }

    @Transactional
    public Enrollment toggleLesson(User user, String slug, int lessonNumber) {
        Enrollment enrollment = findMine(user, slug);
        boolean justFinished = lessons.toggle(user, slug, lessonNumber);
        if (justFinished && !enrollment.isCompleted()) {
            enrollment.complete();
            enrollments.save(enrollment);
            notifications.notify(
                    user,
                    NotificationService.COMPLETION,
                    "Terminaste un curso",
                    "Completaste todas las lecciones de «"
                            + enrollment.getCourse().getTitle() + "».",
                    "/panel/certificados"
            );
            certificates.issueFor(enrollment);
        }
        return enrollment;
    }

    /** Primer curso inscripto todavía sin terminar, para "Continuar". */
    @Transactional(readOnly = true)
    public Enrollment next(User user) {
        return mine(user).stream()
                .filter(enrollment -> !enrollment.isCompleted())
                .findFirst()
                .orElse(null);
    }
}
