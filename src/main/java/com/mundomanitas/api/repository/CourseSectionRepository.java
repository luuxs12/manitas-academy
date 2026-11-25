package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseSectionRepository extends JpaRepository<CourseSection, UUID> {

    List<CourseSection> findByModuleIdOrderByPositionAsc(UUID moduleId);
}
