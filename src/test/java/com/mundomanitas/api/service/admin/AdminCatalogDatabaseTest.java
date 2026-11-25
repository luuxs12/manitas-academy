package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
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

    @Test
    @DisplayName("crea curso sin portada y luego su contenido real")
    void createsCourseAndContent() {
        UUID actor = tempAdmin();
        UUID courseId = catalog.createCourse(new AdminCourseForm(
                "", "Curso de integración", "Nivel inicial", true, "Pruebas",
                "Ficha real para verificar el alta sin portada.", "", 0), actor);

        assertThat(catalog.courseForm(courseId).image()).isNull();
        assertThat(catalog.courseStatus(courseId)).isEqualTo("BORRADOR");

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
    }

    private UUID tempAdmin() {
        String suffix = UUID.randomUUID().toString();
        User admin = new User("admin-" + suffix + "@test.local", "hash-de-prueba",
                "Administrador de integración");
        admin.changeRole("ADMIN");
        return users.save(admin).getId();
    }
}
