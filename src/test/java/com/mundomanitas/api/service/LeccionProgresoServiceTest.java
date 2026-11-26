package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.LeccionProgreso;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.LeccionProgresoRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
class LeccionProgresoServiceTest {

    @Mock
    private LeccionProgresoRepository progreso;

    @Mock
    private InscripcionRepository enrollments;

    @Mock
    private VideoRepository videos;

    @Mock
    private ContenidoItemRepository items;

    @Mock
    private Usuario usuario;

    @Mock
    private Inscripcion inscripcion;

    @Mock
    private Curso curso;

    private LeccionProgresoService service;
    private UUID cursoId;
    private UUID videoId;

    @BeforeEach
    void setUp() {
        service = new LeccionProgresoServiceImpl(progreso, enrollments, videos, items);
        cursoId = UUID.randomUUID();
        videoId = UUID.randomUUID();
        lenient().when(curso.getId()).thenReturn(cursoId);
    }

    @Test
    @DisplayName("marca un video y avisa si el curso quedó completo")
    void marksVideoAndDetectsCompletion() {
        UUID inscripcionId = UUID.randomUUID();
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.of(inscripcion));
        when(inscripcion.getId()).thenReturn(inscripcionId);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);
        when(videos.contarPorCursoId(cursoId)).thenReturn(3);
        when(progreso.findById(any(LeccionProgreso.Clave.class))).thenReturn(Optional.empty());
        when(progreso.contarPorInscripcionId(inscripcionId)).thenReturn(3L);

        assertThat(service.toggle(usuario, "python", videoId)).isTrue();

        verify(progreso).save(any(LeccionProgreso.class));
    }

    @Test
    @DisplayName("no guarda avance de un video oculto")
    void hiddenVideoCannotBeToggled() {
        ContenidoItem hidden = new ContenidoItem(UUID.randomUUID(), null, "VIDEO", videoId,
                "Clase oculta", "Borrador", null, "dQw4w9WgXcQ", 600, 0, false);
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.of(inscripcion));
        when(inscripcion.getCurso()).thenReturn(curso);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);
        when(items.buscarPorOrigenTipoAndOrigenId("VIDEO", videoId)).thenReturn(hidden);

        assertThatThrownBy(() -> service.toggle(usuario, "python", videoId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);

        verify(progreso, never()).save(any(LeccionProgreso.class));
    }

    @Test
    @DisplayName("recorta espacios del identificador antes de marcar")
    void toggleTrimsSlug() {
        UUID inscripcionId = UUID.randomUUID();
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.of(inscripcion));
        when(inscripcion.getId()).thenReturn(inscripcionId);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);
        when(videos.contarPorCursoId(cursoId)).thenReturn(3);
        when(progreso.findById(any(LeccionProgreso.Clave.class))).thenReturn(Optional.empty());
        when(progreso.contarPorInscripcionId(inscripcionId)).thenReturn(1L);

        assertThat(service.toggle(usuario, "  python  ", videoId)).isFalse();

        verify(progreso).save(any(LeccionProgreso.class));
    }

    @Test
    @DisplayName("rechaza un identificador vacío con error de solicitud")
    void toggleRejectsBlankSlug() {
        assertThatThrownBy(() -> service.toggle(usuario, "   ", videoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("trae los videos vistos de varias matrículas en una sola consulta")
    void finalizadoVideosForSeveralEnrollments() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID watched = UUID.randomUUID();
        when(progreso.buscarPorInscripcionIdIn(Set.of(first, second))).thenReturn(List.of(
                new LeccionProgreso(first, watched),
                new LeccionProgreso(second, watched)));

        Map<UUID, Set<UUID>> resultado = service.finalizadoVideosFor(Set.of(first, second));

        assertThat(resultado).containsOnlyKeys(first, second);
        assertThat(resultado.get(first)).containsExactly(watched);
        assertThat(resultado.get(second)).containsExactly(watched);
        verify(progreso, times(1)).buscarPorInscripcionIdIn(any());
    }

    @Test
    @DisplayName("desmarca un video ya completado")
    void unmarksFinalizadoVideo() {
        UUID inscripcionId = UUID.randomUUID();
        LeccionProgreso finalizado = new LeccionProgreso(inscripcionId, videoId);
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.of(inscripcion));
        when(inscripcion.getId()).thenReturn(inscripcionId);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(true);
        when(videos.contarPorCursoId(cursoId)).thenReturn(3);
        when(progreso.findById(any(LeccionProgreso.Clave.class))).thenReturn(Optional.of(finalizado));
        when(progreso.contarPorInscripcionId(inscripcionId)).thenReturn(1L);

        assertThat(service.toggle(usuario, "python", videoId)).isFalse();

        verify(progreso).delete(finalizado);
        verify(progreso, never()).save(any());
    }

    @Test
    @DisplayName("no acepta un video de otro curso")
    void rejectsVideoOutsideTheCurso() {
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.of(inscripcion));
        when(inscripcion.getCurso()).thenReturn(curso);
        when(videos.findById(videoId)).thenReturn(Optional.of(mock(Video.class)));
        when(videos.existeInCurso(videoId, cursoId)).thenReturn(false);

        assertThatThrownBy(() -> service.toggle(usuario, "python", videoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));

        verify(progreso, never()).save(any());
    }

    @Test
    @DisplayName("rechaza a quien no está inscripto")
    void rejectsWhenNotInscripto() {
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle(usuario, "python", videoId))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("completa todos los videos sin duplicar los ya marcados")
    void completeAllOnlyAddsMissingVideos() {
        UUID inscripcionId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();
        when(inscripcion.getId()).thenReturn(inscripcionId);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(videos.buscarIdsPorCursoId(cursoId)).thenReturn(List.of(first, second, third, fourth));
        when(progreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of(
                new LeccionProgreso(inscripcionId, first),
                new LeccionProgreso(inscripcionId, third)
        ));

        service.completeAll(inscripcion);

        verify(progreso, times(2)).save(any(LeccionProgreso.class));
    }

    @Test
    @DisplayName("calcula el porcentaje total sobre los videos de todas las matrículas")
    void totalsUsesAllEnrollments() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Inscripcion one = mock(Inscripcion.class);
        Inscripcion two = mock(Inscripcion.class);
        Curso firstCurso = mock(Curso.class);
        Curso secondCurso = mock(Curso.class);
        UUID firstCursoId = UUID.randomUUID();
        UUID secondCursoId = UUID.randomUUID();
        when(one.getId()).thenReturn(first);
        when(two.getId()).thenReturn(second);
        when(one.getCurso()).thenReturn(firstCurso);
        when(two.getCurso()).thenReturn(secondCurso);
        when(firstCurso.getId()).thenReturn(firstCursoId);
        when(secondCurso.getId()).thenReturn(secondCursoId);
        when(videos.contarPorCursoId(firstCursoId)).thenReturn(2);
        when(videos.contarPorCursoId(secondCursoId)).thenReturn(4);
        when(progreso.contarPorInscripcionId(first)).thenReturn(1L);
        when(progreso.contarPorInscripcionId(second)).thenReturn(2L);

        LeccionProgresoService.LeccionTotals totals = service.totals(List.of(one, two));

        assertThat(totals.finalizado()).isEqualTo(3);
        assertThat(totals.total()).isEqualTo(6);
        assertThat(totals.percent()).isEqualTo(50);
    }
}
