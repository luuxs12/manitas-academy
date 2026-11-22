package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCertificateForm;
import com.mundomanitas.api.dto.admin.CertificateRow;
import com.mundomanitas.api.dto.admin.EligibleCertificate;
import com.mundomanitas.api.model.Certificate;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Emisión y revocación de certificados, sujeta a las reglas de negocio. */
@Service
public class AdminCertificateService {

    private final CertificateRepository certificates;
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final AdminAuditService audit;
    private final AdminFormat format;

    public AdminCertificateService(
            CertificateRepository certificates,
            UserRepository users,
            CourseRepository courses,
            EnrollmentRepository enrollments,
            AdminAuditService audit,
            AdminFormat format
    ) {
        this.certificates = certificates;
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.audit = audit;
        this.format = format;
    }

    @Transactional(readOnly = true)
    public Page<CertificateRow> certificates(boolean includeRevoked, UUID userId,
                                             UUID courseId, String term, Pageable pageable) {
        String clean = term == null || term.isBlank() ? null : term.trim();
        Map<UUID, String> userNames = userNames();
        Map<UUID, Course> courseMap = courses.findAll().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        return certificates.search(includeRevoked, userId, courseId, clean, pageable)
                .map(certificate -> new CertificateRow(
                        certificate.getId(),
                        certificate.getCode(),
                        userNames.getOrDefault(certificate.getUserId(), "Usuario eliminado"),
                        courseMap.containsKey(certificate.getCourseId())
                                ? courseMap.get(certificate.getCourseId()).getTitle()
                                : "Curso eliminado",
                        format.full(certificate.getIssuedAt()),
                        certificate.isActive()));
    }

    @Transactional(readOnly = true)
    public List<EligibleCertificate> eligible() {
        Map<UUID, Course> courseMap = courses.findAll().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        return enrollments.findAll().stream()
                .filter(Enrollment::isCompleted)
                .filter(enrollment -> enrollment.getUser().isPremium())
                .filter(enrollment -> certificates.findByUserIdAndCourseIdAndRevokedAtIsNull(
                        enrollment.getUser().getId(), enrollment.getCourse().getId()).isEmpty())
                .map(enrollment -> {
                    Course course = courseMap.get(enrollment.getCourse().getId());
                    return new EligibleCertificate(
                            enrollment.getUser().getId(),
                            enrollment.getUser().getFullName(),
                            enrollment.getCourse().getId(),
                            course == null ? enrollment.getCourse().getTitle() : course.getTitle());
                })
                .distinct()
                .toList();
    }

    @Transactional
    public String issue(AdminCertificateForm form, UUID actor) {
        User user = users.findById(form.userId())
                .orElseThrow(() -> new IllegalArgumentException("No existe esa cuenta."));
        Course course = courses.findById(form.courseId())
                .orElseThrow(() -> new IllegalArgumentException("No existe ese curso."));
        if (!user.isPremium()) {
            throw new IllegalArgumentException(
                    "Los certificados están incluidos en los planes Premium.");
        }
        Enrollment enrollment = enrollments.findByUserIdAndCourseId(user.getId(), course.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La persona no está inscripta a ese curso."));
        if (!enrollment.isCompleted()) {
            throw new IllegalArgumentException("El curso todavía no fue finalizado.");
        }
        certificates.findByUserIdAndCourseIdAndRevokedAtIsNull(user.getId(), course.getId())
                .ifPresent(certificate -> {
                    throw new IllegalArgumentException("Ese certificado ya está emitido.");
                });
        String code = "MM-" + course.getSlug().substring(0, Math.min(3, course.getSlug().length())).toUpperCase()
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Certificate certificate = new Certificate(user.getId(), course.getId(), code);
        certificates.save(certificate);
        audit.record(actor, "ISSUE", "CERTIFICADO", certificate.getId(), code);
        return code;
    }

    @Transactional
    public void revoke(UUID id, UUID actor) {
        Certificate certificate = certificates.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe ese certificado."));
        certificate.revoke();
        audit.record(actor, "REVOKE", "CERTIFICADO", id, certificate.getCode());
    }

    private Map<UUID, String> userNames() {
        return users.findAll().stream().collect(Collectors.toMap(
                User::getId, User::getFullName, (a, b) -> a, java.util.LinkedHashMap::new));
    }
}
