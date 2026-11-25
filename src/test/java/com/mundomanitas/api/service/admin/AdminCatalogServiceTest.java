package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Exercise;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catálogo administrativo")
class AdminCatalogServiceTest {

    private static final String OLD_COVER =
            "/uploads/covers/11111111-1111-1111-1111-111111111111.jpg";
    private static final String NEW_COVER =
            "/uploads/covers/22222222-2222-2222-2222-222222222222.webp";

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
    @DisplayName("crea el curso publicado sin lecciones escritas a mano")
    void createsPublishedCourse() {
        when(courses.findBySlug("python-desde-cero")).thenReturn(Optional.empty());

        service.createCourse(form(), UUID.randomUUID());

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PUBLICADO");
        assertThat(captor.getValue().getLessons()).isZero();
    }

    @Test
    @DisplayName("el checklist informa mejoras sin bloquear la publicación")
    void publishChecklist() {
        UUID id = UUID.randomUUID();
        Course course = course();
        when(courses.findById(id)).thenReturn(Optional.of(course));
        when(modules.findByCourseIdOrderByPositionAsc(id))
                .thenReturn(List.of(new CourseModule(id, "Módulo", 1)));

        AdminCatalogService.PublishChecklist check = service.publishChecklist(id);

        assertThat(check.missing()).contains("resumen", "portada", "video");
        assertThat(check.canPublish()).isTrue();
    }

    @Test
    @DisplayName("publica un curso aunque todavía no tenga portada ni videos")
    void publishesCourseWithoutExtras() {
        UUID id = UUID.randomUUID();
        Course course = course();
        when(courses.findById(id)).thenReturn(Optional.of(course));

        service.setPublished(id, true, UUID.randomUUID());

        assertThat(course.getStatus()).isEqualTo("PUBLICADO");
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
    @DisplayName("informa el estado real del curso")
    void courseStatus() {
        UUID id = UUID.randomUUID();
        Course course = course();
        when(courses.findById(id)).thenReturn(Optional.of(course));

        assertThat(service.courseStatus(id)).isEqualTo("BORRADOR");
        course.publish();
        assertThat(service.courseStatus(id)).isEqualTo("PUBLICADO");
    }

    @Test
    @DisplayName("cambia la portada local y devuelve la anterior")
    void updateCourseCover() {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Course course = course();
        course.update("curso", "Curso", "Inicial", false, "Programación", "Resumen",
                OLD_COVER, 1);
        when(courses.findById(id)).thenReturn(Optional.of(course));

        String previous = service.updateCourseCover(id, NEW_COVER, actor);

        assertThat(previous).isEqualTo(OLD_COVER);
        assertThat(course.getImage()).isEqualTo(NEW_COVER);
        verify(audit).record(actor, "UPDATE", "CURSO", id, "PORTADA");
    }

    @Test
    @DisplayName("rechaza una portada que no está en disco local")
    void rejectsRemoteCover() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateCourseCover(
                id, "https://ejemplo.com/portada.jpg", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("local");
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

    @Test
    @DisplayName("crea un módulo apoyado en un curso real y registra la auditoría")
    void createsModule() {
        UUID courseId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(courses.findById(courseId)).thenReturn(Optional.of(course()));

        service.createModule(new AdminModuleForm(courseId, "  Unidad 1  ", 2), actor);

        ArgumentCaptor<CourseModule> captor = ArgumentCaptor.forClass(CourseModule.class);
        verify(modules).save(captor.capture());
        assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
        assertThat(captor.getValue().getTitle()).isEqualTo("Unidad 1");
        assertThat(captor.getValue().getPosition()).isEqualTo(2);
        verify(audit).record(actor, "CREATE", "MODULO", captor.getValue().getId(), "Unidad 1");
    }

    @Test
    @DisplayName("crea un ejercicio con su consigna sin contarlo como lección")
    void createsExercise() {
        UUID moduleId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(modules.findById(moduleId))
                .thenReturn(Optional.of(new CourseModule(UUID.randomUUID(), "Unidad 1", 0)));

        service.createExercise(new AdminExerciseForm(
                moduleId, " Práctica 1 ", " Contá las señas ", 1), actor);

        ArgumentCaptor<Exercise> captor = ArgumentCaptor.forClass(Exercise.class);
        verify(exercises).save(captor.capture());
        assertThat(captor.getValue().getModuleId()).isEqualTo(moduleId);
        assertThat(captor.getValue().getTitle()).isEqualTo("Práctica 1");
        assertThat(captor.getValue().getPrompt()).isEqualTo("Contá las señas");
        verify(audit).record(actor, "CREATE", "EJERCICIO", captor.getValue().getId(), "Práctica 1");
        verify(videos, never()).save(any());
    }

    @Test
    @DisplayName("publica y despublica un curso completo con auditoría")
    void publishesAndUnpublishesCompleteCourse() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Course course = new Course("taller-lsp", "Taller de LSP", "Inicial", 1, true,
                "Comunicación", "Resumen real", "/uploads/covers/aaaa.jpg", 1);
        when(courses.findById(courseId)).thenReturn(Optional.of(course));

        service.setPublished(courseId, true, actor);

        assertThat(course.getStatus()).isEqualTo("PUBLICADO");
        verify(audit).record(actor, "UPDATE", "CURSO", courseId, "PUBLICADO");

        service.setPublished(courseId, false, actor);

        assertThat(course.getStatus()).isEqualTo("BORRADOR");
        verify(audit).record(actor, "UPDATE", "CURSO", courseId, "BORRADOR");
    }

    @Test
    @DisplayName("el contador de lecciones se recalcula desde los videos al crear uno")
    void refreshesLessonsWhenVideoIsCreated() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Course course = course();
        when(modules.findById(moduleId))
                .thenReturn(Optional.of(new CourseModule(courseId, "Unidad 1", 0)));
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(videos.countByCourseId(any())).thenReturn(3);

        service.createVideo(new AdminVideoForm(moduleId, "Clase 1", 0,
                "https://youtu.be/dQw4w9WgXcQ"), actor);

        assertThat(course.getLessons()).isEqualTo(3);
        verify(audit).record(eq(actor), eq("CREATE"), eq("VIDEO"), any(), eq("Clase 1"));
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
