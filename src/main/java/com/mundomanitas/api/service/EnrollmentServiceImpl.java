package com.mundomanitas.api.service;

import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.exception.PremiumRequiredException;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.EnrollmentRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reglas de inscripción y avance de los cursos de una cuenta.
 *
 * <p>Nadie nace inscripto: cada matrícula se crea con una acción explícita.
 * Los cursos de paga requieren plan Premium y la fecha de finalización
 * habilita el certificado. Cada acción genera un aviso real.</p>
 */
@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    private final EnrollmentRepository enrollments;
    private final CourseCatalog catalog;
    private final LessonProgressService lessons;
    private final NotificationService notifications;
    private final CertificateIssuerService certificates;
    private final BusinessMetrics metrics;

    public EnrollmentServiceImpl(
            EnrollmentRepository enrollments,
            CourseCatalog catalog,
            LessonProgressService lessons,
            NotificationService notifications,
            CertificateIssuerService certificates,
            BusinessMetrics metrics
    ) {
        this.enrollments = enrollments;
        this.catalog = catalog;
        this.lessons = lessons;
        this.notifications = notifications;
        this.certificates = certificates;
        this.metrics = metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> mine(User user) {
        return enrollments.findByUserOrderByEnrolledAtDesc(user).stream()
                .filter(EnrollmentServiceImpl::isPublished)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> completed(User user) {
        return enrollments.findByUserAndCompletedAtIsNotNullOrderByCompletedAtDesc(user).stream()
                .filter(EnrollmentServiceImpl::isPublished)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(UUID userId, String slug) {
        checkNotNull(userId, "userId");
        return enrollments.existsByUserIdAndCourseSlug(userId, requiredSlug(slug));
    }

    @Override
    @Transactional(readOnly = true)
    public Enrollment findMine(User user, String slug) {
        checkNotNull(user, "user");
        Enrollment enrollment = enrollments.findByUserAndCourseSlug(user, requiredSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No estás inscripto en ese curso"));
        if (!isPublished(enrollment)) {
            log.info("Acceso a curso en borrador denegado usuario={} curso={}",
                    user.getId(), slug);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El curso no está disponible");
        }
        return enrollment;
    }

    /**
     * Inscribe a la persona en un curso. Repetir la acción no crea otra
     * matrícula: devuelve la que ya existe.
     */
    @Override
    @Transactional
    public Enrollment enroll(User user, String slug) {
        checkNotNull(user, "user");
        String cleanSlug = requiredSlug(slug);
        Course course = catalog.findBySlug(cleanSlug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (!course.isFree() && !user.isPremium()) {
            throw new PremiumRequiredException(course.getTitle());
        }

        var existing = enrollments.findByUserAndCourseSlug(user, cleanSlug);
        if (existing.isPresent()) {
            return existing.get();
        }

        Enrollment enrollment = enrollments.save(new Enrollment(user, course));
        metrics.enrollmentCreated();
        notifications.notify(
                user,
                NotificationService.ENROLLMENT,
                "Te inscribiste en un curso",
                "«" + course.getTitle() + "» ya está en Mis cursos.",
                "/panel/cursos"
        );
        log.info("Inscripción creada usuario={} curso={}", user.getId(), cleanSlug);
        return enrollment;
    }

    /** Marca como terminado un curso en el que la persona está inscripta. */
    @Override
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
            log.info("Curso completado usuario={} curso={}", user.getId(), slug);
        }
        return enrollment;
    }

    @Override
    @Transactional
    public Enrollment toggleVideo(User user, String slug, UUID videoId) {
        Enrollment enrollment = findMine(user, slug);
        boolean justFinished = lessons.toggle(user, slug, videoId);
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
            log.info("Curso completado por videos usuario={} curso={}",
                    user.getId(), slug);
        }
        return enrollment;
    }

    /** Un curso en borrador no se sirve al alumnado aunque exista la matrícula. */
    private static boolean isPublished(Enrollment enrollment) {
        return enrollment.getCourse() != null
                && "PUBLICADO".equals(enrollment.getCourse().getStatus());
    }

    /** Primer curso inscripto todavía sin terminar, para "Continuar". */
    @Override
    @Transactional(readOnly = true)
    public Enrollment next(User user) {
        return mine(user).stream()
                .filter(enrollment -> !enrollment.isCompleted())
                .findFirst()
                .orElse(null);
    }

    private static String requiredSlug(String slug) {
        String clean = StringUtils.trimToNull(slug);
        if (clean == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no encontrado");
        }
        return clean;
    }
}
