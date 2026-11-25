package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CourseResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseResourceRepository extends JpaRepository<CourseResource, UUID> {

    List<CourseResource> findByCourseIdOrderByPositionAsc(UUID courseId);
}
