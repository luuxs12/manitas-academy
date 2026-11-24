package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CourseModuleRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoPlaybackServiceTest {

    @Mock
    private VideoRepository videos;

    @Mock
    private R2StorageService storage;

    @Mock
    private CourseModuleRepository modules;

    private VideoPlaybackService service;
    private UUID videoId;
    private UUID courseId;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new VideoPlaybackService(videos, storage, modules);
        videoId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        Course course = mock(Course.class);
        lenient().when(course.getId()).thenReturn(courseId);
        enrollment = mock(Enrollment.class);
        lenient().when(enrollment.getCourse()).thenReturn(course);
    }

    @Test
    @DisplayName("reproduce un video privado con URL firmada, subtítulo y transcripción")
    void playsPrivateVideo() {
        Video video = new Video(UUID.randomUUID(), "Clase 1", null, 1,
                "videos/clase.mp4", "subtitles/clase.vtt", 620, "Texto de la clase");
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);
        when(storage.presignedUrl("videos/clase.mp4"))
                .thenReturn("https://firmado.example/video?firma=1");
        when(storage.presignedUrl("subtitles/clase.vtt"))
                .thenReturn("https://firmado.example/vtt?firma=2");

        VideoPlaybackService.Playback playback = service.play(videoId, enrollment);

        assertThat(playback.title()).isEqualTo("Clase 1");
        assertThat(playback.url()).isEqualTo("https://firmado.example/video?firma=1");
        assertThat(playback.subtitleUrl()).isEqualTo("https://firmado.example/vtt?firma=2");
        assertThat(playback.durationSeconds()).isEqualTo(620);
        assertThat(playback.transcript()).isEqualTo("Texto de la clase");
        assertThat(playback.legacy()).isFalse();
        assertThat(playback.youtube()).isFalse();
        assertThat(playback.available()).isTrue();
    }

    @Test
    @DisplayName("reproduce un video de YouTube sin firmar URLs ni usar el bucket")
    void playsYouTubeVideo() {
        Video video = new Video(UUID.randomUUID(), "Clase de YouTube", null, 1,
                null, null, 480, "Texto de la clase", "dQw4w9WgXcQ");
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);

        VideoPlaybackService.Playback playback = service.play(videoId, enrollment);

        assertThat(playback.title()).isEqualTo("Clase de YouTube");
        assertThat(playback.youtube()).isTrue();
        assertThat(playback.url())
                .isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(playback.durationSeconds()).isEqualTo(480);
        assertThat(playback.transcript()).isEqualTo("Texto de la clase");
        assertThat(playback.available()).isTrue();
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("usa la URL histórica cuando el video viejo todavía no tiene clave privada")
    void fallsBackToLegacyUrl() {
        Video video = new Video(UUID.randomUUID(), "Clase vieja",
                "https://antiguo.example/clase.mp4", 1, null, null, 0, null);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);

        VideoPlaybackService.Playback playback = service.play(videoId, enrollment);

        assertThat(playback.url()).isEqualTo("https://antiguo.example/clase.mp4");
        assertThat(playback.subtitleUrl()).isNull();
        assertThat(playback.legacy()).isTrue();
        assertThat(playback.youtube()).isFalse();
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("no sirve un video que pertenece a otro curso")
    void rejectsVideoFromAnotherCourse() {
        Video video = new Video(UUID.randomUUID(), "Ajeno", null, 1,
                "videos/ajeno.mp4", null, 30, null);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(false);

        assertThatThrownBy(() -> service.play(videoId, enrollment))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("devuelve estado vacío si el curso todavía no cargó el video")
    void returnsEmptyState() {
        Video video = new Video(UUID.randomUUID(), "Sin archivo", null, 1,
                null, null, 0, null);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);

        VideoPlaybackService.Playback playback = service.play(videoId, enrollment);

        assertThat(playback.available()).isFalse();
        assertThat(playback.url()).isNull();
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("lista los videos del curso agrupados por módulo")
    void listsCourseVideos() {
        UUID moduleId = UUID.randomUUID();
        CourseModule module = mock(CourseModule.class);
        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module));
        when(module.getId()).thenReturn(moduleId);
        when(module.getTitle()).thenReturn("Módulo 1");
        when(videos.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(
                new Video(moduleId, "Clase 1", null, 1, "videos/clase.mp4", null, 300, null)));

        var cards = service.courseVideos(courseId);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).moduleTitle()).isEqualTo("Módulo 1");
        assertThat(cards.get(0).title()).isEqualTo("Clase 1");
        assertThat(cards.get(0).durationSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("cuenta los videos del curso sin listarlos")
    void countsCourseVideos() {
        when(videos.countByCourseId(courseId)).thenReturn(4);

        assertThat(service.videoCount(courseId)).isEqualTo(4);
    }

    @Test
    @DisplayName("avisa cuando el video no existe")
    void rejectsMissingVideo() {
        when(videos.findById(videoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.play(videoId, enrollment))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(storage);
    }
}
