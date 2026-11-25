package com.mundomanitas.api.service;

import com.mundomanitas.api.service.admin.AdminCertificatePreviewService;
import com.mundomanitas.api.service.admin.AdminCertificatePreviewServiceImpl;
import com.mundomanitas.api.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vista previa administrativa de certificados")
class AdminCertificatePreviewServiceTest {

    @Mock private CertificateRepository certificates;

    private AdminCertificatePreviewService service;

    @BeforeEach
    void setUp() {
        service = new AdminCertificatePreviewServiceImpl(certificates);
    }

    @Test
    @DisplayName("la vista previa trae persona, curso, videos y estado reales, incluso si está revocada")
    void previewIncludesRealDataAndState() {
        UUID id = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.parse("2026-11-20T10:15:00Z");
        CertificateRepository.PreviewRow row = previewRow(
                id, "MM-RED-ABCD1234", issuedAt, "Ana Quispe", "Redes", 3, false);
        when(certificates.findPreviewById(id)).thenReturn(Optional.of(row));

        Optional<AdminCertificatePreviewService.Preview> result = service.preview(id);

        assertThat(result).contains(new AdminCertificatePreviewService.Preview(
                id, "MM-RED-ABCD1234", issuedAt, "Ana Quispe", "Redes", 3, false));
        verify(certificates).findPreviewById(id);
    }

    @Test
    @DisplayName("un id que no existe no abre ninguna vista previa")
    void missingCertificateHasNoPreview() {
        UUID id = UUID.randomUUID();
        when(certificates.findPreviewById(id)).thenReturn(Optional.empty());

        assertThat(service.preview(id)).isEmpty();
    }

    private CertificateRepository.PreviewRow previewRow(UUID id, String code,
                                                        OffsetDateTime issuedAt, String person,
                                                        String courseTitle, int videoCount,
                                                        boolean active) {
        CertificateRepository.PreviewRow row = mock(CertificateRepository.PreviewRow.class);
        when(row.getCertificateId()).thenReturn(id);
        when(row.getCode()).thenReturn(code);
        when(row.getIssuedAt()).thenReturn(issuedAt);
        when(row.getPerson()).thenReturn(person);
        when(row.getCourseTitle()).thenReturn(courseTitle);
        when(row.getVideoCount()).thenReturn(videoCount);
        when(row.isActive()).thenReturn(active);
        return row;
    }
}
