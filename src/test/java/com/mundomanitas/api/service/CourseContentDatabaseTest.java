package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCourseContentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Estructura de contenido contra PostgreSQL")
class CourseContentDatabaseTest {

    @Autowired private AdminCatalogService catalog;
    @Autowired private AdminCourseContentService adminContent;
    @Autowired private CourseContentService content;
    @Autowired private EnrollmentService enrollments;
    @Autowired private UserRepository users;
    @Autowired private ContentItemRepository items;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.local", "none",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("migra el contenido actual y lo deja navegable con avance real")
    void existingContentIsNavigable() {
        UUID actor = tempAdmin();
        User student = users.save(new User("alumno-" + UUID.randomUUID() + "@test.local",
                "hash-de-prueba", "Alumno de integración"));
        String slug = "curso-contenido-" + UUID.randomUUID();
        UUID courseId = catalog.createCourse(new AdminCourseForm(slug, "Contenido real",
                "Nivel inicial", true, "Pruebas", "Ficha para verificar la estructura.", "", 0), actor);
        UUID moduleId = catalog.createModule(
                new AdminModuleForm(courseId, "Unidad 1", 0), actor);
        UUID videoId = catalog.createVideo(new AdminVideoForm(moduleId, "Clase 1",
                0, "https://youtu.be/dQw4w9WgXcQ"), actor);
        UUID exerciseId = catalog.createExercise(new AdminExerciseForm(moduleId,
                "Práctica 1", "Consigna real.", 0), actor);

        UUID videoItemId = items.findBySourceTypeAndSourceId("VIDEO", videoId).getId();
        UUID exerciseItemId = items.findBySourceTypeAndSourceId("EJERCICIO", exerciseId).getId();
        assertThat(videoItemId).isNotNull();
        assertThat(exerciseItemId).isNotNull();

        UUID sectionId = adminContent.createSection(moduleId,
                new AdminCourseContentService.SectionForm("Lecturas", 0, true), actor);
        UUID materialId = adminContent.createItem(new AdminCourseContentService.ItemForm(
                moduleId, sectionId, "MATERIAL", "Guía de estilo", "Lectura breve",
                "https://example.org/guia", null, null, 0, true), actor);
        adminContent.saveSyllabus(courseId, new AdminCourseContentService.SyllabusForm(
                "Objetivos reales", "Requisitos reales",
                List.of(new AdminCourseContentService.ResourceForm(null, "Guía",
                        "https://example.org/guia", 0))), actor);

        Enrollment enrollment = enrollments.enroll(student, slug);
        content.markSeen(student, slug, videoItemId, true);

        CourseContentService.Outline outline = content.outline(courseId, enrollment.getId(),
                student, false);
        assertThat(outline.totalItems()).isEqualTo(3);
        assertThat(outline.doneItems()).isEqualTo(1);
        assertThat(outline.percent()).isEqualTo(33);
        assertThat(outline.nextPending()).isNotNull().isNotEqualTo(videoItemId);
        assertThat(outline.modules().get(0).sections())
                .anyMatch(section -> section.id().equals(sectionId)
                        && section.items().stream().anyMatch(item -> item.id().equals(materialId)));
        assertThat(content.syllabus(courseId).objectives()).isEqualTo("Objetivos reales");
        assertThat(content.progress(enrollment.getId(), courseId).percent()).isEqualTo(33);
    }

    private UUID tempAdmin() {
        User admin = new User("admin-" + UUID.randomUUID() + "@test.local", "hash",
                "Administrador de integración");
        admin.changeRole("ADMIN");
        return users.save(admin).getId();
    }
}
