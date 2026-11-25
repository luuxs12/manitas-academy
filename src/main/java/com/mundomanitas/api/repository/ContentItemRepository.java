package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    List<ContentItem> findByModuleIdOrderByPositionAsc(UUID moduleId);

    List<ContentItem> findBySectionIdOrderByPositionAsc(UUID sectionId);

    ContentItem findBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    @Query("""
            select count(i) from ContentItem i, CourseModule m
            where i.moduleId = m.id and m.courseId = :courseId
            """)
    int countByCourseId(@Param("courseId") UUID courseId);
}
