package com.mundomanitas.api.service;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Certificados del estudiante")
class StudentCertificateServiceTest {

    @Mock private CertificateRepository certificates;
    @Mock private User student;

    private StudentCertificateService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new StudentCertificateServiceImpl(certificates);
        userId = UUID.randomUUID();
        lenient().when(student.getId()).thenReturn(userId);
    }

    @Test
    @DisplayName("el listado usa el código, la fecha y los videos reales del certificado")
    void availableUsesIssuedData() {
        UUID certificateId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-11-20T10:15:00Z");
        CertificateRepository.IssuedRow row = issuedRow(
                certificateId, "MM-RED-ABCD1234", issuedAt, "Redes", "redes", 3);
        when(certificates.findActiveByUser(userId)).thenReturn(List.of(row));

        List<StudentCertificateService.Issued> result = service.available(student);

        assertThat(result).containsExactly(new StudentCertificateService.Issued(
                certificateId, "MM-RED-ABCD1234", issuedAt, "Redes", "redes", 3));
        verify(certificates).findActiveByUser(userId);
    }

    @Test
    @DisplayName("la ficha busca el certificado vigente del curso por slug")
    void findActiveUsesCourseSlug() {
        UUID certificateId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-11-21T10:15:00Z");
        CertificateRepository.IssuedRow row = issuedRow(
                certificateId, "MM-PYT-1234ABCD", issuedAt, "Python", "python", 5);
        when(certificates.findActiveByUserAndCourseSlug(userId, "python"))
                .thenReturn(Optional.of(row));

        Optional<StudentCertificateService.Issued> result = service.findActive(student, "python");

        assertThat(result).contains(new StudentCertificateService.Issued(
                certificateId, "MM-PYT-1234ABCD", issuedAt, "Python", "python", 5));
        verify(certificates).findActiveByUserAndCourseSlug(userId, "python");
    }

    @Test
    @DisplayName("un certificado revocado no abre la ficha")
    void revokedCertificateCannotBeOpened() {
        when(certificates.findActiveByUserAndCourseSlug(userId, "python"))
                .thenReturn(Optional.empty());

        assertThat(service.findActive(student, "python")).isEmpty();
    }

    private CertificateRepository.IssuedRow issuedRow(UUID certificateId, String code,
                                                      OffsetDateTime issuedAt, String title,
                                                      String slug, int videoCount) {
        CertificateRepository.IssuedRow row = mock(CertificateRepository.IssuedRow.class);
        when(row.getCertificateId()).thenReturn(certificateId);
        when(row.getCode()).thenReturn(code);
        when(row.getIssuedAt()).thenReturn(issuedAt);
        when(row.getCourseTitle()).thenReturn(title);
        when(row.getCourseSlug()).thenReturn(slug);
        when(row.getVideoCount()).thenReturn(videoCount);
        return row;
    }
}
