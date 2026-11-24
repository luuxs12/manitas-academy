package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.Exercise;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Contenido y avance del estudiante")
class StudentContentServiceTest {

    @Mock private CourseModuleRepository modules;
    @Mock private VideoRepository videos;
    @Mock private ExerciseRepository exercises;
    @Mock private LessonProgressService lessons;

    private StudentContentService service;

    @BeforeEach
    void setUp() {
        service = new StudentContentServiceImpl(modules, videos, exercises, lessons);
    }

    @Test
    @DisplayName("el temario agrupa videos y ejercicios reales en orden")
    void outlineGroupsContent() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        CourseModule module = mock(CourseModule.class);
        when(module.getId()).thenReturn(moduleId);
        when(module.getTitle()).thenReturn("Unidad 1");

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Video one = mock(Video.class);
        when(one.getId()).thenReturn(first);
        when(one.getTitle()).thenReturn("Clase 1");
        Video two = mock(Video.class);
        when(two.getId()).thenReturn(second);
        when(two.getTitle()).thenReturn("Clase 2");

        Exercise exercise = mock(Exercise.class);
        when(exercise.getId()).thenReturn(UUID.randomUUID());
        when(exercise.getTitle()).thenReturn("Práctica 1");
        when(exercise.getPrompt()).thenReturn("Escribí una función.");

        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module));
        when(videos.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(one, two));
        when(exercises.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(exercise));

        StudentContentService.Outline outline = service.outline(courseId);

        assertThat(outline.videoCount()).isEqualTo(2);
        assertThat(outline.modules()).hasSize(1);
        assertThat(outline.modules().get(0).videos())
                .extracting(StudentContentService.VideoOutline::title)
                .containsExactly("Clase 1", "Clase 2");
        assertThat(outline.modules().get(0).exercises())
                .extracting(StudentContentService.ExerciseOutline::prompt)
                .containsExactly("Escribí una función.");
    }

    @Test
    @DisplayName("el avance descarta videos viejos que ya no están en el curso")
    void progressUsesCurrentVideos() {
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        when(videos.findIdsByCourseId(courseId))
                .thenReturn(List.of(current, UUID.randomUUID()));
        when(lessons.completedVideos(enrollmentId))
                .thenReturn(Set.of(current, UUID.randomUUID()));

        StudentContentService.Progress progress = service.progress(enrollmentId, courseId);

        assertThat(progress.done()).isEqualTo(1);
        assertThat(progress.total()).isEqualTo(2);
        assertThat(progress.percent()).isEqualTo(50);
    }

    @Test
    @DisplayName("el total general suma el avance vigente de cada matrícula")
    void totalsAcrossEnrollments() {
        UUID firstCourse = UUID.randomUUID();
        UUID secondCourse = UUID.randomUUID();
        UUID firstEnrollment = UUID.randomUUID();
        UUID secondEnrollment = UUID.randomUUID();
        UUID watched = UUID.randomUUID();

        Enrollment one = mock(Enrollment.class);
        Enrollment two = mock(Enrollment.class);
        Course first = mock(Course.class);
        Course second = mock(Course.class);
        when(one.getId()).thenReturn(firstEnrollment);
        when(two.getId()).thenReturn(secondEnrollment);
        when(one.getCourse()).thenReturn(first);
        when(two.getCourse()).thenReturn(second);
        when(first.getId()).thenReturn(firstCourse);
        when(second.getId()).thenReturn(secondCourse);
        when(videos.findIdsByCourseId(firstCourse)).thenReturn(List.of(watched));
        when(videos.findIdsByCourseId(secondCourse))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(lessons.completedVideos(firstEnrollment)).thenReturn(Set.of(watched));
        when(lessons.completedVideos(secondEnrollment)).thenReturn(Set.of());

        StudentContentService.Progress totals = service.totals(List.of(one, two));

        assertThat(totals.done()).isEqualTo(1);
        assertThat(totals.total()).isEqualTo(3);
        assertThat(totals.percent()).isEqualTo(33);
    }

    @Test
    @DisplayName("un curso sin videos arranca con avance cero")
    void emptyProgress() {
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        when(videos.findIdsByCourseId(courseId)).thenReturn(List.of());
        when(lessons.completedVideos(enrollmentId)).thenReturn(Set.of());

        StudentContentService.Progress progress = service.progress(enrollmentId, courseId);

        assertThat(progress.done()).isZero();
        assertThat(progress.total()).isZero();
        assertThat(progress.percent()).isZero();
    }
}
