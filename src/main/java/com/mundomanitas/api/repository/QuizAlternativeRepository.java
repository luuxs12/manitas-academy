package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.QuizAlternative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuizAlternativeRepository extends JpaRepository<QuizAlternative, UUID> {

    List<QuizAlternative> findByQuestionIdInOrderByQuestionIdAscPositionAsc(
            Collection<UUID> questionIds);
}
