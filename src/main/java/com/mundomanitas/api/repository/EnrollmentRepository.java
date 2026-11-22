package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Matrículas de las cuentas.
 *
 * <p>Se cargan junto con el curso para que el panel no haga una consulta
 * extra por cada tarjeta.</p>
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserOrderByEnrolledAtDesc(User user);

    @EntityGraph(attributePaths = "course")
    Optional<Enrollment> findByUserAndCourseSlug(User user, String slug);

    boolean existsByUserAndCourseSlug(User user, String slug);

    boolean existsByUserIdAndCourseSlug(UUID userId, String slug);

    @EntityGraph(attributePaths = "course")
    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserAndCompletedAtIsNotNullOrderByCompletedAtDesc(User user);

    @EntityGraph(attributePaths = {"user", "course"})
    List<Enrollment> findTop5ByOrderByEnrolledAtDesc();

    long countByEnrolledAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByCompletedAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByCompletedAtIsNotNull();

    @Query("select e.course.id as courseId, count(e) as total from Enrollment e group by e.course.id")
    List<Object[]> countByCourse();
}
