package com.mundomanitas.api.service;

import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.exception.PremiumRequeridoException;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.InscripcionRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inscripciones y finalización de cursos")
class InscripcionServiceTest {

    @Mock
    private InscripcionRepository enrollments;

    @Mock
    private CursoCatalog catalog;

    @Mock
    private LeccionProgresoService lessons;

    @Mock
    private NotificacionService notifications;

    @Mock
    private CertificadoEmisorService certificates;

    @Mock
    private BusinessMetrics metrics;

    @Mock
    private Usuario usuario;

    @Mock
    private Curso curso;

    @Mock
    private Inscripcion inscripcion;

    private InscripcionService service;

    @BeforeEach
    void setUp() {
        service = new InscripcionServiceImpl(
                enrollments, catalog, lessons, notifications, certificates, metrics);
    }

    @Test
    @DisplayName("inscribe en un curso gratuito y avisa")
    void enrollsInFreeCurso() {
        when(catalog.buscarPorSlug("python")).thenReturn(Optional.of(curso));
        when(curso.isFree()).thenReturn(true);
        when(curso.getTitulo()).thenReturn("Python desde cero");
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.empty());
        when(enrollments.save(any(Inscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inscripcion creado = service.enroll(usuario, "python");

        assertThat(creado.getCurso()).isSameAs(curso);
        verify(enrollments).save(creado);
        verify(notifications).notify(any(Usuario.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("recorta espacios del identificador antes de inscribir")
    void enrollTrimsSlug() {
        when(catalog.buscarPorSlug("python")).thenReturn(Optional.of(curso));
        when(curso.isFree()).thenReturn(true);
        when(curso.getTitulo()).thenReturn("Python desde cero");
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python")).thenReturn(Optional.empty());
        when(enrollments.save(any(Inscripcion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Inscripcion creado = service.enroll(usuario, "  python  ");

        assertThat(creado.getCurso()).isSameAs(curso);
    }

    @Test
    @DisplayName("rechaza un identificador vacío con error de solicitud")
    void rejectsBlankSlug() {
        assertThatThrownBy(() -> service.enroll(usuario, "   "))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("exige Premium para un curso de paga")
    void rejectsPagadoCursoWithoutPremium() {
        when(catalog.buscarPorSlug("redes")).thenReturn(Optional.of(curso));
        when(curso.isFree()).thenReturn(false);
        when(curso.getTitulo()).thenReturn("Redes y soporte");
        when(usuario.isPremium()).thenReturn(false);

        assertThatThrownBy(() -> service.enroll(usuario, "redes"))
                .isInstanceOf(PremiumRequeridoException.class)
                .hasMessageContaining("Redes y soporte");

        verify(enrollments, never()).save(any());
        verify(notifications, never()).notify(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("repetir la inscripción devuelve la matrícula existente")
    void returnsExistingInscripcion() {
        when(catalog.buscarPorSlug("python")).thenReturn(Optional.of(curso));
        when(curso.isFree()).thenReturn(true);
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python"))
                .thenReturn(Optional.of(inscripcion));

        assertThat(service.enroll(usuario, "python")).isSameAs(inscripcion);

        verify(enrollments, never()).save(any());
    }

    @Test
    @DisplayName("al terminar completa lecciones, guarda y emite certificado")
    void completeFinishesCursoAndIssuesCertificado() {
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python"))
                .thenReturn(Optional.of(inscripcion));
        when(inscripcion.isFinalizado()).thenReturn(false);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(curso.getEstado()).thenReturn("PUBLICADO");
        when(curso.getTitulo()).thenReturn("Python desde cero");

        Inscripcion resultado = service.complete(usuario, "python");

        assertThat(resultado).isSameAs(inscripcion);
        verify(lessons).completeAll(inscripcion);
        verify(inscripcion).complete();
        verify(enrollments).save(inscripcion);
        verify(notifications).notify(any(Usuario.class), anyString(), anyString(), anyString(), anyString());
        verify(certificates).issueFor(inscripcion);
    }

    @Test
    @DisplayName("marcar el último video cierra el curso una sola vez")
    void lastVideoCompletesCurso() {
        UUID videoId = UUID.randomUUID();
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python"))
                .thenReturn(Optional.of(inscripcion));
        when(lessons.toggle(usuario, "python", videoId)).thenReturn(true);
        when(inscripcion.isFinalizado()).thenReturn(false);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(curso.getEstado()).thenReturn("PUBLICADO");
        when(curso.getTitulo()).thenReturn("Python desde cero");

        Inscripcion resultado = service.toggleVideo(usuario, "python", videoId);

        assertThat(resultado).isSameAs(inscripcion);
        verify(inscripcion).complete();
        verify(enrollments).save(inscripcion);
        verify(certificates).issueFor(inscripcion);
    }

    @Test
    @DisplayName("no sirve una matrícula cuyo curso pasó a borrador")
    void buscarMineRejectsDraft() {
        when(enrollments.buscarPorUsuarioAndCursoSlug(usuario, "python"))
                .thenReturn(Optional.of(inscripcion));
        when(inscripcion.getCurso()).thenReturn(curso);
        when(curso.getEstado()).thenReturn("BORRADOR");

        assertThatThrownBy(() -> service.buscarMine(usuario, "python"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("mis cursos deja afuera los borradores")
    void mineOnlyPublished() {
        Inscripcion published = mock(Inscripcion.class);
        Inscripcion draft = mock(Inscripcion.class);
        Curso draftCurso = mock(Curso.class);
        when(published.getCurso()).thenReturn(curso);
        when(curso.getEstado()).thenReturn("PUBLICADO");
        when(draft.getCurso()).thenReturn(draftCurso);
        when(draftCurso.getEstado()).thenReturn("BORRADOR");
        when(enrollments.buscarPorUsuarioOrdenarPorFechaInscripcionDesc(usuario))
                .thenReturn(List.of(published, draft));

        assertThat(service.mine(usuario)).containsExactly(published);
    }

    @Test
    @DisplayName("los certificados tampoco incluyen borradores")
    void finalizadoOnlyPublished() {
        Inscripcion published = mock(Inscripcion.class);
        Inscripcion draft = mock(Inscripcion.class);
        Curso draftCurso = mock(Curso.class);
        when(published.getCurso()).thenReturn(curso);
        when(curso.getEstado()).thenReturn("PUBLICADO");
        when(draft.getCurso()).thenReturn(draftCurso);
        when(draftCurso.getEstado()).thenReturn("BORRADOR");
        when(enrollments.buscarPorUsuarioAndFechaFinalizacionNoEsNuloOrdenarPorFechaFinalizacionDesc(usuario))
                .thenReturn(List.of(published, draft));

        assertThat(service.finalizado(usuario)).containsExactly(published);
    }
}
