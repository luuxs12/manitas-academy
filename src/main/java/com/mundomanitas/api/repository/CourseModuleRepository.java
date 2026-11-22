package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CourseModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findAllByOrderByPositionAsc();

    List<CourseModule> findByCourseIdOrderByPositionAsc(UUID courseId);

    @Query("""
            select m from CourseModule m
            where (:courseId is null or m.courseId = :courseId)
              and (:term is null or lower(m.title) like lower(concat('%', cast(:term as string), '%')))
            order by m.position, m.title
            """)
    Page<CourseModule> search(@Param("courseId") UUID courseId,
                              @Param("term") String term,
                              Pageable pageable);
}
