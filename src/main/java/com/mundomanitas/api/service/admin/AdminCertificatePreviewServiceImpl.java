package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Implementación de la vista previa administrativa de certificados. */
@Service
public class AdminCertificatePreviewServiceImpl implements AdminCertificatePreviewService {

    private static final Logger log = LoggerFactory.getLogger(AdminCertificatePreviewServiceImpl.class);

    private final CertificateRepository certificates;

    public AdminCertificatePreviewServiceImpl(CertificateRepository certificates) {
        this.certificates = certificates;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Preview> preview(UUID id) {
        Optional<Preview> result = certificates.findPreviewById(id).map(row -> new Preview(
                row.getCertificateId(),
                row.getCode(),
                row.getIssuedAt(),
                row.getPerson(),
                row.getCourseTitle(),
                row.getVideoCount(),
                row.isActive()));
        log.info("Vista previa de certificado {} resuelta: {}", id, result.isPresent());
        return result;
    }
}
