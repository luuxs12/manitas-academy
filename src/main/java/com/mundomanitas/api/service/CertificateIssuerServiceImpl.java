package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Certificate;
import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Emisión automática de certificados.
 *
 * <p>Se llama cuando una matrícula llega al 100%. Solo emite para cuentas
 * Premium, una vez por curso y sin sobrescribir un certificado revocado.</p>
 */
@Service
public class CertificateIssuerServiceImpl implements CertificateIssuerService {

    private static final Logger log = LoggerFactory.getLogger(CertificateIssuerServiceImpl.class);

    private final CertificateRepository certificates;
    private final NotificationService notifications;
    private final BusinessMetrics metrics;

    public CertificateIssuerServiceImpl(CertificateRepository certificates,
                                        NotificationService notifications,
                                        BusinessMetrics metrics) {
        this.certificates = certificates;
        this.notifications = notifications;
        this.metrics = metrics;
    }

    /** Devuelve true cuando se emitió un certificado nuevo. */
    @Override
    @Transactional
    public boolean issueFor(Enrollment enrollment) {
        User user = enrollment.getUser();
        if (user == null || !user.isPremium() || !enrollment.isCompleted()) {
            return false;
        }
        if (certificates.findByUserIdAndCourseIdAndRevokedAtIsNull(
                user.getId(), enrollment.getCourse().getId()).isPresent()) {
            return false;
        }

        String slug = enrollment.getCourse().getSlug();
        String prefix = slug.substring(0, Math.min(3, slug.length())).toUpperCase();
        String code = "MM-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        certificates.save(new Certificate(user.getId(), enrollment.getCourse().getId(), code));
        metrics.certificateIssued();
        notifications.notify(
                user,
                NotificationService.CERTIFICATE,
                "Certificado emitido",
                "Ya tenés el certificado de «" + enrollment.getCourse().getTitle() + "».",
                "/panel/certificados"
        );
        log.info("Certificado emitido para usuario {} en curso {}",
                user.getId(), enrollment.getCourse().getId());
        return true;
    }
}
