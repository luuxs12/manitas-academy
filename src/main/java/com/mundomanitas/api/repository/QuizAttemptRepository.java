package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByEnrollmentIdAndExerciseId(UUID enrollmentId, UUID exerciseId);

    boolean existsByExerciseId(UUID exerciseId);

    List<QuizAttempt> findByEnrollmentIdIn(Collection<UUID> enrollmentIds);
}
