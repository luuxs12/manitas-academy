package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Contenido y avance del estudiante")
class EstudianteContenidoServiceTest {

    @Mock private CursoModuloRepository modules;
    @Mock private VideoRepository videos;
    @Mock private EjercicioRepository exercises;
    @Mock private LeccionProgresoService lessons;

    private EstudianteContenidoService service;

    @BeforeEach
    void setUp() {
        service = new EstudianteContenidoServiceImpl(modules, videos, exercises, lessons);
    }

    @Test
    @DisplayName("el temario agrupa videos y ejercicios reales en orden")
    void esquemaGroupsContenido() {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        CursoModulo modulo = mock(CursoModulo.class);
        when(modulo.getId()).thenReturn(moduloId);
        when(modulo.getTitulo()).thenReturn("Unidad 1");

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Video one = mock(Video.class);
        when(one.getId()).thenReturn(first);
        when(one.getTitulo()).thenReturn("Clase 1");
        Video two = mock(Video.class);
        when(two.getId()).thenReturn(second);
        when(two.getTitulo()).thenReturn("Clase 2");

        Ejercicio ejercicio = mock(Ejercicio.class);
        when(ejercicio.getId()).thenReturn(UUID.randomUUID());
        when(ejercicio.getTitulo()).thenReturn("Práctica 1");
        when(ejercicio.getPrompt()).thenReturn("Escribí una función.");

        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo));
        when(videos.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(one, two));
        when(exercises.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(ejercicio));

        EstudianteContenidoService.Esquema esquema = service.esquema(cursoId);

        assertThat(esquema.videoContar()).isEqualTo(2);
        assertThat(esquema.modules()).hasSize(1);
        assertThat(esquema.modules().get(0).videos())
                .extracting(EstudianteContenidoService.VideoEsquema::titulo)
                .containsExactly("Clase 1", "Clase 2");
        assertThat(esquema.modules().get(0).exercises())
                .extracting(EstudianteContenidoService.EjercicioEsquema::prompt)
                .containsExactly("Escribí una función.");
    }

    @Test
    @DisplayName("el avance descarta videos viejos que ya no están en el curso")
    void progresoUsesActualVideos() {
        UUID cursoId = UUID.randomUUID();
        UUID inscripcionId = UUID.randomUUID();
        UUID actual = UUID.randomUUID();
        when(videos.buscarIdsPorCursoId(cursoId))
                .thenReturn(List.of(actual, UUID.randomUUID()));
        when(lessons.finalizadoVideos(inscripcionId))
                .thenReturn(Set.of(actual, UUID.randomUUID()));

        EstudianteContenidoService.Progreso progreso = service.progreso(inscripcionId, cursoId);

        assertThat(progreso.done()).isEqualTo(1);
        assertThat(progreso.total()).isEqualTo(2);
        assertThat(progreso.percent()).isEqualTo(50);
    }

    @Test
    @DisplayName("el avance en lote consulta una sola vez videos y progreso de todas las matrículas")
    void progresoForUsesOneBatchPerOrigen() {
        UUID firstCurso = UUID.randomUUID();
        UUID secondCurso = UUID.randomUUID();
        UUID firstInscripcion = UUID.randomUUID();
        UUID secondInscripcion = UUID.randomUUID();
        UUID watched = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID extra = UUID.randomUUID();

        Inscripcion one = mock(Inscripcion.class);
        Inscripcion two = mock(Inscripcion.class);
        Curso first = mock(Curso.class);
        Curso second = mock(Curso.class);
        when(one.getId()).thenReturn(firstInscripcion);
        when(two.getId()).thenReturn(secondInscripcion);
        when(one.getCurso()).thenReturn(first);
        when(two.getCurso()).thenReturn(second);
        when(first.getId()).thenReturn(firstCurso);
        when(second.getId()).thenReturn(secondCurso);

        List<VideoRepository.CursoVideo> cursoVideos = List.of(
                cursoVideo(firstCurso, watched),
                cursoVideo(secondCurso, other),
                cursoVideo(secondCurso, extra));
        when(videos.buscarCursoVideos(Set.of(firstCurso, secondCurso))).thenReturn(cursoVideos);
        when(lessons.finalizadoVideosFor(Set.of(firstInscripcion, secondInscripcion)))
                .thenReturn(Map.of(
                        firstInscripcion, Set.of(watched),
                        secondInscripcion, Set.of(other)));

        Map<UUID, EstudianteContenidoService.Progreso> resultado = service.progresoFor(List.of(one, two));

        assertThat(resultado).containsOnlyKeys(firstInscripcion, secondInscripcion);
        assertThat(resultado.get(firstInscripcion))
                .isEqualTo(new EstudianteContenidoService.Progreso(1, 1, 100));
        assertThat(resultado.get(secondInscripcion))
                .isEqualTo(new EstudianteContenidoService.Progreso(1, 2, 50));
        verify(videos, times(1)).buscarCursoVideos(any());
        verify(lessons, times(1)).finalizadoVideosFor(any());
    }

    @Test
    @DisplayName("el total general suma el avance vigente de cada matrícula")
    void totalsAcrossEnrollments() {
        UUID firstCurso = UUID.randomUUID();
        UUID secondCurso = UUID.randomUUID();
        UUID firstInscripcion = UUID.randomUUID();
        UUID secondInscripcion = UUID.randomUUID();
        UUID watched = UUID.randomUUID();

        Inscripcion one = mock(Inscripcion.class);
        Inscripcion two = mock(Inscripcion.class);
        Curso first = mock(Curso.class);
        Curso second = mock(Curso.class);
        when(one.getId()).thenReturn(firstInscripcion);
        when(two.getId()).thenReturn(secondInscripcion);
        when(one.getCurso()).thenReturn(first);
        when(two.getCurso()).thenReturn(second);
        when(first.getId()).thenReturn(firstCurso);
        when(second.getId()).thenReturn(secondCurso);
        List<VideoRepository.CursoVideo> cursoVideos = List.of(
                cursoVideo(firstCurso, watched),
                cursoVideo(secondCurso, UUID.randomUUID()),
                cursoVideo(secondCurso, UUID.randomUUID()));
        when(videos.buscarCursoVideos(Set.of(firstCurso, secondCurso))).thenReturn(cursoVideos);
        when(lessons.finalizadoVideosFor(Set.of(firstInscripcion, secondInscripcion)))
                .thenReturn(Map.of(firstInscripcion, Set.of(watched), secondInscripcion, Set.of()));

        EstudianteContenidoService.Progreso totals = service.totals(List.of(one, two));

        assertThat(totals.done()).isEqualTo(1);
        assertThat(totals.total()).isEqualTo(3);
        assertThat(totals.percent()).isEqualTo(33);
    }

    private VideoRepository.CursoVideo cursoVideo(UUID cursoId, UUID videoId) {
        var row = mock(VideoRepository.CursoVideo.class);
        when(row.getCursoId()).thenReturn(cursoId);
        when(row.getVideoId()).thenReturn(videoId);
        return row;
    }

    @Test
    @DisplayName("un curso sin videos arranca con avance cero")
    void emptyProgreso() {
        UUID cursoId = UUID.randomUUID();
        UUID inscripcionId = UUID.randomUUID();
        when(videos.buscarIdsPorCursoId(cursoId)).thenReturn(List.of());
        when(lessons.finalizadoVideos(inscripcionId)).thenReturn(Set.of());

        EstudianteContenidoService.Progreso progreso = service.progreso(inscripcionId, cursoId);

        assertThat(progreso.done()).isZero();
        assertThat(progreso.total()).isZero();
        assertThat(progreso.percent()).isZero();
    }
}
