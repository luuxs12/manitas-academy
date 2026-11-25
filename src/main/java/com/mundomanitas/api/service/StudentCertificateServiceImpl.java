package com.mundomanitas.api.service;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Implementación de los certificados visibles para el estudiante. */
@Service
public class StudentCertificateServiceImpl implements StudentCertificateService {

    private static final Logger log = LoggerFactory.getLogger(StudentCertificateServiceImpl.class);

    private final CertificateRepository certificates;

    public StudentCertificateServiceImpl(CertificateRepository certificates) {
        this.certificates = certificates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Issued> available(User user) {
        List<Issued> result = certificates.findActiveByUser(user.getId()).stream()
                .map(this::view)
                .toList();
        log.info("Certificados vigentes recuperados para usuario {}: {}", user.getId(), result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Issued> findActive(User user, String courseSlug) {
        return certificates.findActiveByUserAndCourseSlug(user.getId(), courseSlug)
                .map(this::view);
    }

    private Issued view(CertificateRepository.IssuedRow row) {
        return new Issued(
                row.getCertificateId(),
                row.getCode(),
                row.getIssuedAt(),
                row.getCourseTitle(),
                row.getCourseSlug(),
                row.getVideoCount());
    }
}
