package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.CourseResource;
import com.mundomanitas.api.model.CourseSection;
import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.ItemProgress;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.CourseResourceRepository;
import com.mundomanitas.api.repository.CourseSectionRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.ItemProgressRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Contenido del curso y avance real")
class CourseContentServiceTest {

    @Mock private CourseRepository courses;
    @Mock private CourseModuleRepository modules;
    @Mock private CourseSectionRepository sections;
    @Mock private ContentItemRepository items;
    @Mock private CourseResourceRepository resources;
    @Mock private EnrollmentRepository enrollments;
    @Mock private LessonProgressRepository lessonProgress;
    @Mock private ItemProgressRepository itemProgress;
    @Mock private VideoRepository videos;

    @InjectMocks private CourseContentServiceImpl service;

    private final UUID courseId = UUID.randomUUID();

    @Test
    @DisplayName("el sílabo sale de la base, con sus recursos complementarios")
    void syllabusFromDatabase() {
        Course course = course(true);
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(resources.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(
                new CourseResource(courseId, "Guía de estilo", "https://example.org/guia", 0)));

        CourseContentService.Syllabus syllabus = service.syllabus(courseId);

        assertThat(syllabus.summary()).isEqualTo("Resumen real");
        assertThat(syllabus.objectives()).isEqualTo("Objetivos reales");
        assertThat(syllabus.requirements()).isEqualTo("Requisitos reales");
        assertThat(syllabus.resources()).extracting(
                CourseContentService.ResourceView::url).containsExactly("https://example.org/guia");
    }

    @Test
    @DisplayName("agrupa ítems por sección, marca lo visto y elige el siguiente pendiente")
    void outlineGroupsAndFindsNextPending() {
        UUID enrollmentId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID videoItemId = UUID.randomUUID();
        UUID materialItemId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();

        CourseModule module = module(moduleId);
        CourseSection section = section(sectionId, moduleId);
        ContentItem video = item(videoItemId, moduleId, sectionId, "VIDEO", videoId,
                "Clase 1", "dQw4w9WgXcQ", 0);
        ContentItem material = item(materialItemId, moduleId, null, "MATERIAL", null,
                "Lectura 1", null, 1);

        when(courses.findById(courseId)).thenReturn(Optional.of(course(true)));
        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module));
        when(sections.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(section));
        when(items.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(video, material));
        when(lessonProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId))
                .thenReturn(List.of(new LessonProgress(enrollmentId, videoId)));
        when(itemProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());

        CourseContentService.Outline outline = service.outline(courseId, enrollmentId, null, false);

        assertThat(outline.totalItems()).isEqualTo(2);
        assertThat(outline.doneItems()).isEqualTo(1);
        assertThat(outline.percent()).isEqualTo(50);
        assertThat(outline.modules().get(0).percent()).isEqualTo(50);
        assertThat(outline.modules().get(0).sections().get(0).items().get(0).seen()).isTrue();
        assertThat(outline.modules().get(0).items().get(0).seen()).isFalse();
        assertThat(outline.nextPending()).isEqualTo(materialItemId);
    }

    @Test
    @DisplayName("oculta los ítems invisibles salvo en la vista previa")
    void hiddenItemsOnlyInPreview() {
        UUID enrollmentId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ContentItem hidden = item(UUID.randomUUID(), moduleId, null, "MATERIAL", null,
                "Borrador", null, 0);
        ReflectionTestUtils.setField(hidden, "visible", false);

        when(courses.findById(courseId)).thenReturn(Optional.of(course(true)));
        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module(moduleId)));
        when(sections.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of());
        when(items.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(hidden));
        when(lessonProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());
        when(itemProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());

        CourseContentService.Outline student = service.outline(courseId, enrollmentId, null, false);
        CourseContentService.Outline preview = service.outline(courseId, enrollmentId, null, true);

        assertThat(student.totalItems()).isZero();
        assertThat(student.nextPending()).isNull();
        assertThat(preview.totalItems()).isEqualTo(1);
        assertThat(preview.nextPending()).isEqualTo(hidden.getId());
    }

    @Test
    @DisplayName("marca como bloqueado el contenido premium para una cuenta FREE")
    void premiumLockForFreeUser() {
        UUID enrollmentId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        ContentItem material = item(UUID.randomUUID(), moduleId, null, "MATERIAL", null,
                "Lectura premium", null, 0);

        when(courses.findById(courseId)).thenReturn(Optional.of(course(false)));
        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module(moduleId)));
        when(sections.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of());
        when(items.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(material));
        when(lessonProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());
        when(itemProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());

        CourseContentService.Outline outline = service.outline(courseId, enrollmentId, new User(
                "alumno@test.local", "hash", "Alumno"), false);

        assertThat(outline.nextPending()).isNull();
        assertThat(outline.modules().get(0).items().get(0).locked()).isTrue();
    }

    @Test
    @DisplayName("expone el id del video para abrir la clase desde el temario")
    void exposesVideoSource() {
        UUID enrollmentId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ContentItem video = item(itemId, moduleId, null, "VIDEO", videoId,
                "Clase 1", "dQw4w9WgXcQ", 0);

        when(courses.findById(courseId)).thenReturn(Optional.of(course(true)));
        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module(moduleId)));
        when(sections.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of());
        when(items.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(video));
        when(lessonProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());
        when(itemProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of());

        CourseContentService.Outline outline = service.outline(courseId, enrollmentId, null, false);

        assertThat(outline.modules().get(0).items().get(0).sourceId()).isEqualTo(videoId);
    }

    @Test
    @DisplayName("marca como visto un material sin tocar el progreso de videos")
    void marksMaterialAsSeen() {
        User student = new User("alumno@test.local", "hash", "Alumno");
        UUID moduleId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Enrollment enrollment = enrollment(student);
        ContentItem material = item(itemId, moduleId, null, "MATERIAL", null, "Guía", null, 0);

        when(enrollments.findByUserAndCourseSlug(student, "python")).thenReturn(Optional.of(enrollment));
        when(items.findById(itemId)).thenReturn(Optional.of(material));
        when(modules.findById(moduleId)).thenReturn(Optional.of(module(moduleId)));

        service.markSeen(student, "python", itemId, true);

        verify(itemProgress).save(any(ItemProgress.class));
    }

    @Test
    @DisplayName("la duración se expresa en minutos y horas, no en segundos crudos")
    void durationLabel() {
        assertThat(withDuration(null).durationLabel()).isEqualTo("Sin duración");
        assertThat(withDuration(0).durationLabel()).isEqualTo("0 s");
        assertThat(withDuration(90).durationLabel()).isEqualTo("1 min 30 s");
        assertThat(withDuration(600).durationLabel()).isEqualTo("10 min");
        assertThat(withDuration(3600).durationLabel()).isEqualTo("1 h");
        assertThat(withDuration(3725).durationLabel()).isEqualTo("1 h 2 min");
    }

    private CourseContentService.ItemView withDuration(Integer seconds) {
        return new CourseContentService.ItemView(UUID.randomUUID(), "VIDEO", "Clase", null,
                null, "abc", seconds, true, false, false);
    }

    private Course course(boolean free) {
        Course course = new Course("python", "Python", "Inicial", 1, free,
                "Programación", "Resumen real", null, 0);
        ReflectionTestUtils.setField(course, "id", courseId);
        course.updateSyllabus("Objetivos reales", "Requisitos reales");
        return course;
    }

    private CourseModule module(UUID moduleId) {
        CourseModule module = new CourseModule(courseId, "Módulo 1", 0);
        ReflectionTestUtils.setField(module, "id", moduleId);
        return module;
    }

    private CourseSection section(UUID sectionId, UUID moduleId) {
        CourseSection section = new CourseSection(moduleId, "Lecturas", 0, true);
        ReflectionTestUtils.setField(section, "id", sectionId);
        return section;
    }

    private ContentItem item(UUID itemId, UUID moduleId, UUID sectionId, String type,
                             UUID sourceId, String title, String youtubeId, int position) {
        ContentItem item = new ContentItem(moduleId, sectionId, type, sourceId, title,
                "Descripción real", "https://example.org/material", youtubeId, 600, position, true);
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private Enrollment enrollment(User student) {
        Course course = course(true);
        Enrollment enrollment = new Enrollment(student, course);
        ReflectionTestUtils.setField(enrollment, "id", UUID.randomUUID());
        return enrollment;
    }
}
