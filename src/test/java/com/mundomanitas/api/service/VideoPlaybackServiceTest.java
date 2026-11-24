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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reproducción de clases")
class VideoPlaybackServiceTest {

    @Mock private VideoRepository videos;
    @Mock private CourseModuleRepository modules;

    private VideoPlaybackService service;
    private UUID videoId;
    private UUID courseId;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new VideoPlaybackService(videos, modules);
        videoId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        Course course = mock(Course.class);
        lenient().when(course.getId()).thenReturn(courseId);
        enrollment = mock(Enrollment.class);
        lenient().when(enrollment.getCourse()).thenReturn(course);
    }

    @Test
    @DisplayName("arma el embed de YouTube desde el identificador guardado")
    void playsYouTube() {
        Video video = new Video(UUID.randomUUID(), "Clase 1", "dQw4w9WgXcQ", 1);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);

        VideoPlaybackService.Playback playback = service.play(videoId, enrollment);

        assertThat(playback.url())
                .isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("rechaza un video sin identificador de YouTube")
    void rejectsVideoWithoutYouTubeId() {
        Video video = new Video(UUID.randomUUID(), "Clase 1", null, 1);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existsInCourse(videoId, courseId)).thenReturn(true);

        assertThatThrownBy(() -> service.play(videoId, enrollment))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("devuelve el temario ordenado sin direcciones de origen")
    void courseVideos() {
        CourseModule module = new CourseModule(courseId, "Unidad 1", 1);
        when(modules.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module));
        when(videos.findByModuleIdOrderByPositionAsc(module.getId()))
                .thenReturn(List.of(new Video(module.getId(), "Clase 1", "dQw4w9WgXcQ", 1)));

        List<VideoPlaybackService.CourseVideo> cards = service.courseVideos(courseId);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).title()).isEqualTo("Clase 1");
        assertThat(cards.get(0).moduleTitle()).isEqualTo("Unidad 1");
    }
}
