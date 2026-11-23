package com.mundomanitas.api.service;

import com.mundomanitas.api.exception.PremiumRequiredException;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inscripciones y finalización de cursos")
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollments;

    @Mock
    private CourseCatalog catalog;

    @Mock
    private LessonProgressService lessons;

    @Mock
    private NotificationService notifications;

    @Mock
    private CertificateIssuerService certificates;

    @Mock
    private User user;

    @Mock
    private Course course;

    @Mock
    private Enrollment enrollment;

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentServiceImpl(
                enrollments, catalog, lessons, notifications, certificates);
    }

    @Test
    @DisplayName("inscribe en un curso gratuito y avisa")
    void enrollsInFreeCourse() {
        when(catalog.findBySlug("python")).thenReturn(Optional.of(course));
        when(course.isFree()).thenReturn(true);
        when(course.getTitle()).thenReturn("Python desde cero");
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.empty());
        when(enrollments.save(any(Enrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment created = service.enroll(user, "python");

        assertThat(created.getCourse()).isSameAs(course);
        verify(enrollments).save(created);
        verify(notifications).notify(any(User.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("exige Premium para un curso de paga")
    void rejectsPaidCourseWithoutPremium() {
        when(catalog.findBySlug("redes")).thenReturn(Optional.of(course));
        when(course.isFree()).thenReturn(false);
        when(course.getTitle()).thenReturn("Redes y soporte");
        when(user.isPremium()).thenReturn(false);

        assertThatThrownBy(() -> service.enroll(user, "redes"))
                .isInstanceOf(PremiumRequiredException.class)
                .hasMessageContaining("Redes y soporte");

        verify(enrollments, never()).save(any());
        verify(notifications, never()).notify(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("repetir la inscripción devuelve la matrícula existente")
    void returnsExistingEnrollment() {
        when(catalog.findBySlug("python")).thenReturn(Optional.of(course));
        when(course.isFree()).thenReturn(true);
        when(enrollments.findByUserAndCourseSlug(user, "python"))
                .thenReturn(Optional.of(enrollment));

        assertThat(service.enroll(user, "python")).isSameAs(enrollment);

        verify(enrollments, never()).save(any());
    }

    @Test
    @DisplayName("al terminar completa lecciones, guarda y emite certificado")
    void completeFinishesCourseAndIssuesCertificate() {
        when(enrollments.findByUserAndCourseSlug(user, "python"))
                .thenReturn(Optional.of(enrollment));
        when(enrollment.isCompleted()).thenReturn(false);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getTitle()).thenReturn("Python desde cero");

        Enrollment result = service.complete(user, "python");

        assertThat(result).isSameAs(enrollment);
        verify(lessons).completeAll(enrollment);
        verify(enrollment).complete();
        verify(enrollments).save(enrollment);
        verify(notifications).notify(any(User.class), anyString(), anyString(), anyString(), anyString());
        verify(certificates).issueFor(enrollment);
    }

    @Test
    @DisplayName("marcar la última lección cierra el curso una sola vez")
    void lastLessonCompletesCourse() {
        when(enrollments.findByUserAndCourseSlug(user, "python"))
                .thenReturn(Optional.of(enrollment));
        when(lessons.toggle(user, "python", 8)).thenReturn(true);
        when(enrollment.isCompleted()).thenReturn(false);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getTitle()).thenReturn("Python desde cero");

        Enrollment result = service.toggleLesson(user, "python", 8);

        assertThat(result).isSameAs(enrollment);
        verify(enrollment).complete();
        verify(enrollments).save(enrollment);
        verify(certificates).issueFor(enrollment);
    }
}
