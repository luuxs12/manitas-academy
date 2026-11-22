package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByModuleIdOrderByPositionAsc(UUID moduleId);

    @Query("""
            select e from Exercise e
            where (:moduleId is null or e.moduleId = :moduleId)
              and (:term is null or lower(e.title) like lower(concat('%', cast(:term as string), '%')))
            order by e.position, e.title
            """)
    Page<Exercise> search(@Param("moduleId") UUID moduleId,
                          @Param("term") String term,
                          Pageable pageable);
}
