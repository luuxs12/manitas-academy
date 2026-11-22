package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findTop8ByOrderByCreatedAtDesc();

    List<AuditLog> findTop8ByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime since);

    long countByCreatedAtAfter(OffsetDateTime since);
}
