package com.mundomanitas.api.service;

import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.model.Certificate;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CertificateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateIssuerServiceTest {

    @Mock
    private CertificateRepository certificates;

    @Mock
    private NotificationService notifications;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private CertificateIssuerServiceImpl issuer;

    @Mock
    private Enrollment enrollment;

    @Mock
    private User user;

    @Mock
    private Course course;

    @Test
    void emiteCertificadoAlCompletarCursoPremium() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(enrollment.getUser()).thenReturn(user);
        when(user.isPremium()).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(enrollment.isCompleted()).thenReturn(true);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getId()).thenReturn(courseId);
        when(course.getSlug()).thenReturn("python-desde-cero");
        when(course.getTitle()).thenReturn("Python desde cero");
        when(certificates.findByUserIdAndCourseIdAndRevokedAtIsNull(userId, courseId))
                .thenReturn(Optional.empty());

        assertTrue(issuer.issueFor(enrollment));
        verify(certificates).save(any(Certificate.class));
        verify(notifications).notify(any(User.class), anyString(), anyString(), anyString(), anyString());
        verify(metrics).certificateIssued();
    }

    @Test
    void noEmiteParaCuentaGratuita() {
        when(enrollment.getUser()).thenReturn(user);
        when(user.isPremium()).thenReturn(false);

        assertFalse(issuer.issueFor(enrollment));
        verify(certificates, never()).save(any());
        verify(notifications, never()).notify(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void noEmiteDosCertificadosVigentesParaElMismoCurso() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(enrollment.getUser()).thenReturn(user);
        when(user.isPremium()).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(enrollment.isCompleted()).thenReturn(true);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getId()).thenReturn(courseId);
        when(certificates.findByUserIdAndCourseIdAndRevokedAtIsNull(userId, courseId))
                .thenReturn(Optional.of(new Certificate(userId, courseId, "MM-PYT-12345678")));

        assertFalse(issuer.issueFor(enrollment));
        verify(certificates, never()).save(any());
    }
}
