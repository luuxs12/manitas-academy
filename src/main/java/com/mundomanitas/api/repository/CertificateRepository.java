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
}
