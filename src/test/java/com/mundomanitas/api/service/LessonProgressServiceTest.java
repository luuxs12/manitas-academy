package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import com.mundomanitas.api.repository.VideoRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Avance por video")
class LessonProgressServiceTest {

    @Mock
    private LessonProgressRepository progress;

    @Mock
    private EnrollmentRepository enrollments;

    @Mock
    private VideoRepository videos;

    @Mock
    private User user;

    @Mock
    private Enrollment enrollment;

    @Mock
    private Course course;

    private LessonProgressService service;
    private UUID courseId;
    private UUID videoId;

    @BeforeEach
    void setUp() {
        service = new LessonProgressServiceImpl(progress, enrollments, videos);
        courseId = UUID.randomUUID();
        videoId = UUID.randomUUID();
        lenient().when(course.getId()).thenReturn(courseId);
    }

    @Test
    @DisplayName("marca un video y avisa si el curso quedó completo")
    void marksVideoAndDetectsCompletion() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);
        when(videos.countByCourseId(courseId)).thenReturn(3);
        when(progress.findById(any(LessonProgress.Key.class))).thenReturn(Optional.empty());
        when(progress.countByEnrollmentId(enrollmentId)).thenReturn(3L);

        assertThat(service.toggle(user, "python", videoId)).isTrue();

        verify(progress).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("recorta espacios del identificador antes de marcar")
    void toggleTrimsSlug() {
        UUID enrollmentId = UUID.randomUUID();
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);
        when(videos.countByCourseId(courseId)).thenReturn(3);
        when(progress.findById(any(LessonProgress.Key.class))).thenReturn(Optional.empty());
        when(progress.countByEnrollmentId(enrollmentId)).thenReturn(1L);

        assertThat(service.toggle(user, "  python  ", videoId)).isFalse();

        verify(progress).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("rechaza un identificador vacío con error de solicitud")
    void toggleRejectsBlankSlug() {
        assertThatThrownBy(() -> service.toggle(user, "   ", videoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("desmarca un video ya completado")
    void unmarksCompletedVideo() {
        UUID enrollmentId = UUID.randomUUID();
        LessonProgress completed = new LessonProgress(enrollmentId, videoId);
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);
        when(videos.countByCourseId(courseId)).thenReturn(3);
        when(progress.findById(any(LessonProgress.Key.class))).thenReturn(Optional.of(completed));
        when(progress.countByEnrollmentId(enrollmentId)).thenReturn(1L);

        assertThat(service.toggle(user, "python", videoId)).isFalse();

        verify(progress).delete(completed);
        verify(progress, never()).save(any());
    }

    @Test
    @DisplayName("no acepta un video de otro curso")
    void rejectsVideoOutsideTheCourse() {
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.of(enrollment));
        when(enrollment.getCourse()).thenReturn(course);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> service.toggle(user, "python", videoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));

        verify(progress, never()).save(any());
    }

    @Test
    @DisplayName("rechaza a quien no está inscripto")
    void rejectsWhenNotEnrolled() {
        when(enrollments.findByUserAndCourseSlug(user, "python")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle(user, "python", videoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("completa todos los videos sin duplicar los ya marcados")
    void completeAllOnlyAddsMissingVideos() {
        UUID enrollmentId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);
        when(videos.findIdsByCourseId(courseId)).thenReturn(List.of(first, second, third, fourth));
        when(progress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId)).thenReturn(List.of(
                new LessonProgress(enrollmentId, first),
                new LessonProgress(enrollmentId, third)
        ));

        service.completeAll(enrollment);

        verify(progress, times(2)).save(any(LessonProgress.class));
    }

    @Test
    @DisplayName("calcula el porcentaje total sobre los videos de todas las matrículas")
    void totalsUsesAllEnrollments() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Enrollment one = mock(Enrollment.class);
        Enrollment two = mock(Enrollment.class);
        Course firstCourse = mock(Course.class);
        Course secondCourse = mock(Course.class);
        UUID firstCourseId = UUID.randomUUID();
        UUID secondCourseId = UUID.randomUUID();
        when(one.getId()).thenReturn(first);
        when(two.getId()).thenReturn(second);
        when(one.getCourse()).thenReturn(firstCourse);
        when(two.getCourse()).thenReturn(secondCourse);
        when(firstCourse.getId()).thenReturn(firstCourseId);
        when(secondCourse.getId()).thenReturn(secondCourseId);
        when(videos.countByCourseId(firstCourseId)).thenReturn(2);
        when(videos.countByCourseId(secondCourseId)).thenReturn(4);
        when(progress.countByEnrollmentId(first)).thenReturn(1L);
        when(progress.countByEnrollmentId(second)).thenReturn(2L);

        LessonProgressService.LessonTotals totals = service.totals(List.of(one, two));

        assertThat(totals.completed()).isEqualTo(3);
        assertThat(totals.total()).isEqualTo(6);
        assertThat(totals.percent()).isEqualTo(50);
    }
}
