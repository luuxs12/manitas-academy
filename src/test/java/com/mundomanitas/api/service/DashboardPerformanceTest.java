package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas reales: el avance no puede crecer con la cantidad de matrículas. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
@DisplayName("Rendimiento del avance del panel")
class DashboardPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(DashboardPerformanceTest.class);

    @Autowired private UserRepository users;
    @Autowired private CourseRepository courses;
    @Autowired private CourseModuleRepository modules;
    @Autowired private VideoRepository videos;
    @Autowired private EnrollmentRepository savedEnrollments;
    @Autowired private EnrollmentService enrollments;
    @Autowired private StudentContentService content;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("el avance general resuelve varias matrículas en consultas constantes")
    void totalsUseBoundedQueries() {
        User student = users.save(new User(
                "rendimiento-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba",
                "Prueba de rendimiento"));

        for (int index = 0; index < 2; index++) {
            Course course = new Course(
                    "rendimiento-" + UUID.randomUUID(),
                    "Curso de rendimiento " + index,
                    "Inicial", 2, true, "Pruebas",
                    "Curso creado para medir consultas.", "", index);
            course.publish();
            course = courses.save(course);

            CourseModule module = modules.save(new CourseModule(course.getId(), "Unidad 1", 0));
            videos.save(new Video(module.getId(), "Clase 1", "dQw4w9WgXcQ", 0));
            videos.save(new Video(module.getId(), "Clase 2", "dQw4w9WgXcQ", 1));
            savedEnrollments.save(new Enrollment(student, course));
        }
        entityManager.flush();
        entityManager.clear();

        List<Enrollment> mine = enrollments.mine(student);
        assertThat(mine).hasSize(2);

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        StudentContentService.Progress totals = content.totals(mine);
        long statements = stats.getPrepareStatementCount();

        log.info("Avance de {} matrículas resuelto en {} sentencias", mine.size(), statements);
        assertThat(totals.total()).isEqualTo(4);
        assertThat(statements).as("sentencias para %d matrículas", mine.size())
                .isLessThanOrEqualTo(2);
    }
}
