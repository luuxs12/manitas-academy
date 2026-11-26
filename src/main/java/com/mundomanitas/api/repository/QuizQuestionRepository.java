package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByExerciseIdOrderByPositionAsc(UUID exerciseId);
}
