package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    List<Certificate> findAllByOrderByIssuedAtDesc();

    Optional<Certificate> findByUserIdAndCourseIdAndRevokedAtIsNull(UUID userId, UUID courseId);

    long countByRevokedAtIsNull();

    long countByCourseId(UUID courseId);

    long countByIssuedAtBetween(OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select c from Certificate c
            where (:includeRevoked = true or c.revokedAt is null)
              and (:userId is null or c.userId = :userId)
              and (:courseId is null or c.courseId = :courseId)
              and (:term is null or lower(c.code) like lower(concat('%', cast(:term as string), '%')))
            order by c.issuedAt desc
            """)
    Page<Certificate> search(@Param("includeRevoked") boolean includeRevoked,
                             @Param("userId") UUID userId,
                             @Param("courseId") UUID courseId,
                             @Param("term") String term,
                             Pageable pageable);

    /** Certificados vigentes de una cuenta con el curso y los videos reales. */
    @Query("""
            select c.id as certificateId, c.code as code, c.issuedAt as issuedAt,
                   co.title as courseTitle, co.slug as courseSlug,
                   (select count(v) from Video v, CourseModule m
                     where v.moduleId = m.id and m.courseId = co.id) as videoCount
            from Certificate c, Course co
            where c.courseId = co.id and c.userId = :userId and c.revokedAt is null
            order by c.issuedAt desc
            """)
    List<IssuedRow> findActiveByUser(@Param("userId") UUID userId);

    @Query("""
            select c.id as certificateId, c.code as code, c.issuedAt as issuedAt,
                   co.title as courseTitle, co.slug as courseSlug,
                   (select count(v) from Video v, CourseModule m
                     where v.moduleId = m.id and m.courseId = co.id) as videoCount
            from Certificate c, Course co
            where c.courseId = co.id and c.userId = :userId
              and c.revokedAt is null and co.slug = :courseSlug
            """)
    Optional<IssuedRow> findActiveByUserAndCourseSlug(@Param("userId") UUID userId,
                                                      @Param("courseSlug") String courseSlug);

    interface IssuedRow {
        UUID getCertificateId();

        String getCode();

        OffsetDateTime getIssuedAt();

        String getCourseTitle();

        String getCourseSlug();

        int getVideoCount();
    }

    /** Vista previa del admin: incluye revocados y trae los datos reales de persona y curso. */
    @Query("""
            select c.id as certificateId, c.code as code, c.issuedAt as issuedAt,
                   u.fullName as person, co.title as courseTitle,
                   (select count(v) from Video v, CourseModule m
                     where v.moduleId = m.id and m.courseId = co.id) as videoCount,
                   case when c.revokedAt is null then true else false end as active
            from Certificate c, User u, Course co
            where c.id = :id and c.userId = u.id and c.courseId = co.id
            """)
    Optional<PreviewRow> findPreviewById(@Param("id") UUID id);

    interface PreviewRow {
        UUID getCertificateId();

        String getCode();

        OffsetDateTime getIssuedAt();

        String getPerson();

        String getCourseTitle();

        int getVideoCount();

        boolean isActive();
    }
}
