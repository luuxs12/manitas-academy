package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.service.CourseCatalog;
import com.mundomanitas.api.service.EnrollmentService;
import com.mundomanitas.api.service.StudentContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Catálogo administrativo contra PostgreSQL")
class AdminCatalogDatabaseTest {

    @Autowired
    private AdminCatalogService catalog;

    @Autowired
    private UserRepository users;

    @Autowired
    private CourseCatalog studentCatalog;

    @Autowired
    private EnrollmentService enrollments;

    @Autowired
    private StudentContentService content;

    @Test
    @DisplayName("crea un curso visible y lo deja inscribible con su contenido real")
    void createsCourseAndContent() {
        UUID actor = tempAdmin();
        User student = users.save(new User("alumno-" + UUID.randomUUID() + "@test.local",
                "hash-de-prueba", "Alumno de integración"));
        String slug = "curso-integracion-" + UUID.randomUUID();
        UUID courseId = catalog.createCourse(new AdminCourseForm(
                slug, "Curso de integración", "Nivel inicial", true, "Pruebas",
                "Ficha real para verificar el alta sin portada.", "", 0), actor);

        assertThat(catalog.courseForm(courseId).image()).isNull();
        assertThat(catalog.courseStatus(courseId)).isEqualTo("PUBLICADO");
        assertThat(studentCatalog.all()).anyMatch(course -> course.getSlug().equals(slug));

        UUID moduleId = catalog.createModule(
                new AdminModuleForm(courseId, "Unidad de integración", 0), actor);
        UUID videoId = catalog.createVideo(new AdminVideoForm(moduleId, "Clase de integración",
                0, "https://youtu.be/dQw4w9WgXcQ"), actor);
        UUID exerciseId = catalog.createExercise(new AdminExerciseForm(moduleId,
                "Práctica de integración", "Consigna de integración.", 0), actor);

        assertThat(moduleId).isNotNull();
        assertThat(videoId).isNotNull();
        assertThat(exerciseId).isNotNull();
        assertThat(catalog.videos(null, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(row -> row.title().equals("Clase de integración"));
        assertThat(catalog.modules(courseId, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(row -> row.title().equals("Unidad de integración"));

        Enrollment enrollment = enrollments.enroll(student, slug);
        assertThat(enrollments.findMine(student, slug).getId()).isEqualTo(enrollment.getId());
        assertThat(content.outline(courseId).modules())
                .anyMatch(module -> module.title().equals("Unidad de integración")
                        && module.videos().stream().anyMatch(v -> v.id().equals(videoId)));
    }

    private UUID tempAdmin() {
        String suffix = UUID.randomUUID().toString();
        User admin = new User("admin-" + suffix + "@test.local", "hash-de-prueba",
                "Administrador de integración");
        admin.changeRole("ADMIN");
        return users.save(admin).getId();
    }
}
