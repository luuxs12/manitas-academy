package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catálogo administrativo")
class AdminCatalogServiceTest {

    @Mock private UserRepository users;
    @Mock private CourseRepository courses;
    @Mock private CourseModuleRepository modules;
    @Mock private VideoRepository videos;
    @Mock private ExerciseRepository exercises;
    @Mock private EnrollmentRepository enrollments;
    @Mock private CertificateRepository certificates;
    @Mock private PasswordEncoder encoder;
    @Mock private AdminAuditService audit;
    @Mock private AdminFormat format;

    private AdminCatalogService service;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogServiceImpl(
                users, courses, modules, videos, exercises, enrollments, certificates,
                encoder, audit, format);
    }

    @Test
    @DisplayName("genera el identificador desde el título")
    void slugFromTitle() {
        assertThat(service.slugFromTitle("  ¡Hola, Mundo LSP!  ")).isEqualTo("hola-mundo-lsp");
    }

    @Test
    @DisplayName("crea el curso en borrador sin lecciones escritas a mano")
    void createsDraftCourse() {
        when(courses.findBySlug("python-desde-cero")).thenReturn(Optional.empty());

        service.createCourse(form(), UUID.randomUUID());

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("BORRADOR");
        assertThat(captor.getValue().getLessons()).isZero();
    }

    @Test
    @DisplayName("el checklist lista lo que falta para publicar")
    void publishChecklist() {
        UUID id = UUID.randomUUID();
        Course course = course();
        when(courses.findById(id)).thenReturn(Optional.of(course));
        when(modules.findByCourseIdOrderByPositionAsc(id))
                .thenReturn(List.of(new CourseModule(id, "Módulo", 1)));

        AdminCatalogService.PublishChecklist check = service.publishChecklist(id);

        assertThat(check.missing()).contains("resumen", "portada", "video");
        assertThat(check.canPublish()).isFalse();
    }

    @Test
    @DisplayName("no publica un curso incompleto")
    void rejectsPublishingIncompleteCourse() {
        UUID id = UUID.randomUUID();
        when(courses.findById(id)).thenReturn(Optional.of(course()));
        when(modules.findByCourseIdOrderByPositionAsc(id)).thenReturn(List.of());

        assertThatThrownBy(() -> service.setPublished(id, true, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Falta");
    }

    @Test
    @DisplayName("informa el impacto real antes de borrar")
    void deletionImpact() {
        UUID id = UUID.randomUUID();
        when(courses.findById(id)).thenReturn(Optional.of(course()));
        CourseModule module = new CourseModule(id, "Módulo", 1);
        when(modules.findByCourseIdOrderByPositionAsc(id)).thenReturn(List.of(module));
        when(videos.countByCourseId(id)).thenReturn(4);
        when(exercises.findByModuleIdOrderByPositionAsc(module.getId())).thenReturn(List.of());
        when(enrollments.countByCourseId(id)).thenReturn(3L);
        when(certificates.countByCourseId(id)).thenReturn(2L);

        AdminCatalogService.DeletionImpact impact = service.deletionImpact(id);

        assertThat(impact.videos()).isEqualTo(4);
        assertThat(impact.enrollments()).isEqualTo(3L);
        assertThat(impact.certificates()).isEqualTo(2L);
    }

    @Test
    @DisplayName("no borra un curso con inscritos")
    void refusesToDeleteCourseWithEnrollments() {
        UUID id = UUID.randomUUID();
        when(courses.findById(id)).thenReturn(Optional.of(course()));
        when(enrollments.countByCourseId(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteCourse(id, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inscritos");
        verify(courses, never()).delete(any());
    }

    @Test
    @DisplayName("intercambia posiciones de módulos de forma transaccional")
    void movesModuleDown() {
        CourseModule first = new CourseModule(UUID.randomUUID(), "Uno", 1);
        CourseModule second = new CourseModule(UUID.randomUUID(), "Dos", 2);
        when(modules.findById(first.getId())).thenReturn(Optional.of(first));
        when(modules.findByCourseIdOrderByPositionAsc(first.getCourseId()))
                .thenReturn(List.of(first, second));

        service.moveModule(first.getId(), false, UUID.randomUUID());

        assertThat(first.getPosition()).isEqualTo(2);
        assertThat(second.getPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("cuenta las lecciones desde los videos de la base")
    void lessonCount() {
        UUID id = UUID.randomUUID();
        when(videos.countByCourseId(id)).thenReturn(6);
        assertThat(service.lessonCount(id)).isEqualTo(6);
    }

    @Test
    @DisplayName("guarda solo el identificador de YouTube")
    void storesOnlyYouTubeId() {
        UUID moduleId = UUID.randomUUID();
        when(modules.findById(moduleId)).thenReturn(Optional.of(mockModule()));

        service.createVideo(new AdminVideoForm(
                moduleId, "Clase 1", 1, "https://youtu.be/dQw4w9WgXcQ?t=15"), UUID.randomUUID());

        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videos).save(captor.capture());
        assertThat(captor.getValue().getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("rechaza un enlace que no es de YouTube")
    void rejectsForeignLink() {
        UUID moduleId = UUID.randomUUID();
        when(modules.findById(moduleId)).thenReturn(Optional.of(mockModule()));
        AdminVideoForm form = new AdminVideoForm(
                moduleId, "Clase", 1, "https://ejemplo.com/watch?v=dQw4w9WgXcQ");

        assertThatThrownBy(() -> service.createVideo(form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YouTube");
        verify(videos, never()).save(any());
    }

    private static CourseModule mockModule() {
        return new CourseModule(UUID.randomUUID(), "Módulo", 1);
    }

    private static Course course() {
        return new Course("curso", "Curso", "Inicial", 0, false,
                "Programación", "", "", 1);
    }

    private static AdminCourseForm form() {
        return new AdminCourseForm("python-desde-cero", "Python desde cero", "Inicial", true,
                "Programación", "Variables y ciclos", "/img/cursos/python.jpg", 1);
    }
}
