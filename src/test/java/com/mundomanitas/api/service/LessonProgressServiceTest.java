package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
@DisplayName("Avance por lección")
class LessonProgressServiceTest {

    @Mock
    private LessonProgressRepository progress;

    @Mock
    private EnrollmentRepository enrollments;

    @Mock
    private User user;

    @Mock
    private Enrollment enrollment;

    @Mock
    private Course course;

    private LessonProgressService service;

    @BeforeEach
    void setUp() {
        service = new LessonProgressServiceImpl(progress, enrollments);
    }

    @Test
    @DisplayName("marca una lección y avisa si el curso quedó completo")
    void marksLessonAndDetectsCompletion() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getLessons()).thenReturn(3);
        when(progress.findById(any(LessonProgress.Key.class))).thenReturn(Optional.empty());
        when(progress.countByEnrollmentId(enrollmentId)).thenReturn(3L);

        assertThat(service.toggle(user, "python", 3)).isTrue();

        verify(progress).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("desmarca una lección ya completada")
    void unmarksCompletedLesson() {
        UUID enrollmentId = UUID.randomUUID();
        LessonProgress completed = new LessonProgress(enrollmentId, 2);
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getLessons()).thenReturn(3);
        when(progress.findById(any(LessonProgress.Key.class))).thenReturn(Optional.of(completed));
        when(progress.countByEnrollmentId(enrollmentId)).thenReturn(1L);

        assertThat(service.toggle(user, "python", 2)).isFalse();

        verify(progress).delete(completed);
        verify(progress, never()).save(any());
    }

    @Test
    @DisplayName("rechaza una lección que no existe en el curso")
    void rejectsLessonOutsideTheCourse() {
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getLessons()).thenReturn(3);

        assertThatThrownBy(() -> service.toggle(user, "python", 4))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(progress, never()).save(any());
    }

    @Test
    @DisplayName("rechaza a quien no está inscripto")
    void rejectsWhenNotEnrolled() {
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle(user, "python", 1))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("completa todas las lecciones sin duplicar las ya marcadas")
    void completeAllOnlyAddsMissingLessons() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(course.getLessons()).thenReturn(4);
        when(progress.findByEnrollmentIdOrderByLessonNumberAsc(enrollmentId)).thenReturn(List.of(
                new LessonProgress(enrollmentId, 1),
                new LessonProgress(enrollmentId, 3)
        ));

        service.completeAll(enrollment);

        verify(progress, org.mockito.Mockito.times(2)).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("calcula el porcentaje total sobre todas las matrículas")
    void totalsUsesAllEnrollments() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Enrollment one = org.mockito.Mockito.mock(Enrollment.class);
        Enrollment two = org.mockito.Mockito.mock(Enrollment.class);
        Course firstCourse = org.mockito.Mockito.mock(Course.class);
        Course secondCourse = org.mockito.Mockito.mock(Course.class);
        when(one.getId()).thenReturn(first);
        when(two.getId()).thenReturn(second);
        when(one.getCourse()).thenReturn(firstCourse);
        when(two.getCourse()).thenReturn(secondCourse);
        when(firstCourse.getLessons()).thenReturn(2);
        when(secondCourse.getLessons()).thenReturn(4);
        when(progress.countByEnrollmentId(first)).thenReturn(1L);
        when(progress.countByEnrollmentId(second)).thenReturn(2L);

        LessonProgressService.LessonTotals totals = service.totals(List.of(one, two));

        assertThat(totals.completed()).isEqualTo(3);
        assertThat(totals.total()).isEqualTo(6);
        assertThat(totals.percent()).isEqualTo(50);
    }
}
