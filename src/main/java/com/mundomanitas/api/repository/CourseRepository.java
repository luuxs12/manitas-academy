package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso al catálogo guardado en PostgreSQL.
 */
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findAllByOrderByPositionAsc();

    List<Course> findAllByStatusOrderByPositionAsc(String status);

    Optional<Course> findBySlug(String slug);

    Optional<Course> findBySlugAndStatus(String slug, String status);

    Page<Course> findAllByOrderByPositionAsc(Pageable pageable);

    Page<Course> findByTitleIgnoreCaseContainingOrSlugIgnoreCaseContaining(
            String title, String slug, Pageable pageable);

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}
