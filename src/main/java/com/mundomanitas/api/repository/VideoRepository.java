package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findByModuleIdOrderByPositionAsc(UUID moduleId);

    /** Evita servir un video de un curso ajeno a la matrícula. */
    @Query("""
            select case when count(v) > 0 then true else false end
            from Video v, CourseModule m
            where v.id = :videoId and v.moduleId = m.id and m.courseId = :courseId
            """)
    boolean existsInCourse(@Param("videoId") UUID videoId, @Param("courseId") UUID courseId);

    @Query("""
            select v from Video v
            where (:moduleId is null or v.moduleId = :moduleId)
              and (:term is null or lower(v.title) like lower(concat('%', cast(:term as string), '%')))
            order by v.position, v.title
            """)
    Page<Video> search(@Param("moduleId") UUID moduleId,
                       @Param("term") String term,
                       Pageable pageable);
}
