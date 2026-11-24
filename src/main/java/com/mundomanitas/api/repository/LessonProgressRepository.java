package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LessonProgressRepository
        extends JpaRepository<LessonProgress, LessonProgress.Key> {

    List<LessonProgress> findByEnrollmentIdOrderByCompletedAtAsc(UUID enrollmentId);

    long countByEnrollmentId(UUID enrollmentId);

    long countByCompletedAtBetween(OffsetDateTime from, OffsetDateTime to);
}
