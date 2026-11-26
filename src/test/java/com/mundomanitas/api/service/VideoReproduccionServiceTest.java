package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
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
class VideoReproduccionServiceTest {

    @Mock private VideoRepository videos;
    @Mock private CursoModuloRepository modules;
    @Mock private ContenidoItemRepository items;

    private VideoReproduccionService service;
    private UUID videoId;
    private UUID cursoId;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        service = new VideoReproduccionService(videos, modules, items);
        videoId = UUID.randomUUID();
        cursoId = UUID.randomUUID();
        Curso curso = mock(Curso.class);
        lenient().when(curso.getId()).thenReturn(cursoId);
        inscripcion = mock(Inscripcion.class);
        lenient().when(inscripcion.getCurso()).thenReturn(curso);
    }

    @Test
    @DisplayName("arma el embed de YouTube desde el identificador guardado")
    void playsYouTube() {
        Video video = new Video(UUID.randomUUID(), "Clase 1", "dQw4w9WgXcQ", 1);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);

        VideoReproduccionService.Reproduccion reproduccion = service.play(videoId, inscripcion);

        assertThat(reproduccion.url())
                .isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?rel=0&enablejsapi=1");
    }

    @Test
    @DisplayName("rechaza un video sin identificador de YouTube")
    void rejectsVideoWithoutYouTubeId() {
        Video video = new Video(UUID.randomUUID(), "Clase 1", null, 1);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);

        assertThatThrownBy(() -> service.play(videoId, inscripcion))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("no reproduce un video cuyo ítem está oculto")
    void hiddenVideoIsNotPlayable() {
        Video video = new Video(UUID.randomUUID(), "Clase oculta", "dQw4w9WgXcQ", 1);
        ContenidoItem hidden = new ContenidoItem(UUID.randomUUID(), null, "VIDEO", videoId,
                "Clase oculta", "Borrador", null, "dQw4w9WgXcQ", 600, 0, false);
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);
        when(items.buscarPorOrigenTipoAndOrigenId("VIDEO", videoId)).thenReturn(hidden);

        assertThatThrownBy(() -> service.play(videoId, inscripcion))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("devuelve el temario ordenado sin direcciones de origen")
    void cursoVideos() {
        CursoModulo modulo = new CursoModulo(cursoId, "Unidad 1", 1);
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo));
        when(videos.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId()))
                .thenReturn(List.of(new Video(modulo.getId(), "Clase 1", "dQw4w9WgXcQ", 1)));

        List<VideoReproduccionService.CursoVideo> cards = service.cursoVideos(cursoId);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).titulo()).isEqualTo("Clase 1");
        assertThat(cards.get(0).moduloTitulo()).isEqualTo("Unidad 1");
    }
}
