package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catálogo administrativo")
class AdminCatalogServiceTest {

    private static final String OLD_COVER =
            "/uploads/covers/11111111-1111-1111-1111-111111111111.jpg";
    private static final String NEW_COVER =
            "/uploads/covers/22222222-2222-2222-2222-222222222222.webp";

    @Mock private UsuarioRepository users;
    @Mock private CursoRepository courses;
    @Mock private CursoModuloRepository modules;
    @Mock private VideoRepository videos;
    @Mock private EjercicioRepository exercises;
    @Mock private InscripcionRepository enrollments;
    @Mock private CertificadoRepository certificates;
    @Mock private PasswordEncoder encoder;
    @Mock private AdminAuditoriaService auditoria;
    @Mock private AdminFormato format;

    private AdminCatalogService service;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogServiceImpl(
                users, courses, modules, videos, exercises, enrollments, certificates,
                encoder, auditoria, format);
    }

    @Test
    @DisplayName("genera el identificador desde el título")
    void slugFromTitulo() {
        assertThat(service.slugFromTitulo("  ¡Hola, Mundo LSP!  ")).isEqualTo("hola-mundo-lsp");
    }

    @Test
    @DisplayName("crea el curso publicado sin lecciones escritas a mano")
    void createsPublishedCurso() {
        when(courses.buscarPorSlug("python-desde-cero")).thenReturn(Optional.empty());

        service.createCurso(formulario(), UUID.randomUUID());

        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(courses).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("PUBLICADO");
        assertThat(captor.getValue().getLessons()).isZero();
    }

    @Test
    @DisplayName("el checklist informa mejoras sin bloquear la publicación")
    void publicarLista() {
        UUID id = UUID.randomUUID();
        Curso curso = curso();
        when(courses.findById(id)).thenReturn(Optional.of(curso));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(id))
                .thenReturn(List.of(new CursoModulo(id, "Módulo", 1)));

        AdminCatalogService.PublicarLista verificacion = service.publicarLista(id);

        assertThat(verificacion.missing()).contains("resumen", "portada", "video");
        assertThat(verificacion.canPublicar()).isTrue();
    }

    @Test
    @DisplayName("publica un curso aunque todavía no tenga portada ni videos")
    void publishesCursoWithoutExtras() {
        UUID id = UUID.randomUUID();
        Curso curso = curso();
        when(courses.findById(id)).thenReturn(Optional.of(curso));

        service.setPublished(id, true, UUID.randomUUID());

        assertThat(curso.getEstado()).isEqualTo("PUBLICADO");
    }

    @Test
    @DisplayName("informa el impacto real antes de borrar")
    void eliminacionImpacto() {
        UUID id = UUID.randomUUID();
        when(courses.findById(id)).thenReturn(Optional.of(curso()));
        CursoModulo modulo = new CursoModulo(id, "Módulo", 1);
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(id)).thenReturn(List.of(modulo));
        when(videos.contarPorCursoId(id)).thenReturn(4);
        when(exercises.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId())).thenReturn(List.of());
        when(enrollments.contarPorCursoId(id)).thenReturn(3L);
        when(certificates.contarPorCursoId(id)).thenReturn(2L);

        AdminCatalogService.EliminacionImpacto impacto = service.eliminacionImpacto(id);

        assertThat(impacto.videos()).isEqualTo(4);
        assertThat(impacto.enrollments()).isEqualTo(3L);
        assertThat(impacto.certificates()).isEqualTo(2L);
    }

    @Test
    @DisplayName("no borra un curso con inscritos")
    void refusesToDeleteCursoWithEnrollments() {
        UUID id = UUID.randomUUID();
        when(courses.findById(id)).thenReturn(Optional.of(curso()));
        when(enrollments.contarPorCursoId(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteCurso(id, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inscritos");
        verify(courses, never()).delete(any());
    }

    @Test
    @DisplayName("intercambia posiciones de módulos de forma transaccional")
    void movesModuloDown() {
        CursoModulo first = new CursoModulo(UUID.randomUUID(), "Uno", 1);
        CursoModulo second = new CursoModulo(UUID.randomUUID(), "Dos", 2);
        when(modules.findById(first.getId())).thenReturn(Optional.of(first));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(first.getCursoId()))
                .thenReturn(List.of(first, second));

        service.moveModulo(first.getId(), false, UUID.randomUUID());

        assertThat(first.getPosicion()).isEqualTo(2);
        assertThat(second.getPosicion()).isEqualTo(1);
    }

    @Test
    @DisplayName("cuenta las lecciones desde los videos de la base")
    void leccionContar() {
        UUID id = UUID.randomUUID();
        when(videos.contarPorCursoId(id)).thenReturn(6);
        assertThat(service.leccionContar(id)).isEqualTo(6);
    }

    @Test
    @DisplayName("informa el estado real del curso")
    void cursoEstado() {
        UUID id = UUID.randomUUID();
        Curso curso = curso();
        when(courses.findById(id)).thenReturn(Optional.of(curso));

        assertThat(service.cursoEstado(id)).isEqualTo("BORRADOR");
        curso.publicar();
        assertThat(service.cursoEstado(id)).isEqualTo("PUBLICADO");
    }

    @Test
    @DisplayName("cambia la portada local y devuelve la anterior")
    void updateCursoPortada() {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Curso curso = curso();
        curso.update("curso", "Curso", "Inicial", false, "Programación", "Resumen",
                OLD_COVER, 1);
        when(courses.findById(id)).thenReturn(Optional.of(curso));

        String previous = service.updateCursoPortada(id, NEW_COVER, actor);

        assertThat(previous).isEqualTo(OLD_COVER);
        assertThat(curso.getImagen()).isEqualTo(NEW_COVER);
        verify(auditoria).record(actor, "UPDATE", "CURSO", id, "PORTADA");
    }

    @Test
    @DisplayName("rechaza una portada que no está en disco local")
    void rejectsRemotePortada() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateCursoPortada(
                id, "https://ejemplo.com/portada.jpg", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("local");
    }

    @Test
    @DisplayName("guarda solo el identificador de YouTube")
    void storesOnlyYouTubeId() {
        UUID moduloId = UUID.randomUUID();
        when(modules.findById(moduloId)).thenReturn(Optional.of(mockModulo()));

        service.createVideo(new AdminVideoFormulario(
                moduloId, "Clase 1", 1, "https://youtu.be/dQw4w9WgXcQ?t=15"), UUID.randomUUID());

        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videos).save(captor.capture());
        assertThat(captor.getValue().getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("rechaza un enlace que no es de YouTube")
    void rejectsForeignEnlace() {
        UUID moduloId = UUID.randomUUID();
        when(modules.findById(moduloId)).thenReturn(Optional.of(mockModulo()));
        AdminVideoFormulario formulario = new AdminVideoFormulario(
                moduloId, "Clase", 1, "https://ejemplo.com/watch?v=dQw4w9WgXcQ");

        assertThatThrownBy(() -> service.createVideo(formulario, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YouTube");
        verify(videos, never()).save(any());
    }

    @Test
    @DisplayName("crea un módulo apoyado en un curso real y registra la auditoría")
    void createsModulo() {
        UUID cursoId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(courses.findById(cursoId)).thenReturn(Optional.of(curso()));

        service.createModulo(new AdminModuloFormulario(cursoId, "  Unidad 1  ", 2), actor);

        ArgumentCaptor<CursoModulo> captor = ArgumentCaptor.forClass(CursoModulo.class);
        verify(modules).save(captor.capture());
        assertThat(captor.getValue().getCursoId()).isEqualTo(cursoId);
        assertThat(captor.getValue().getTitulo()).isEqualTo("Unidad 1");
        assertThat(captor.getValue().getPosicion()).isEqualTo(2);
        verify(auditoria).record(actor, "CREATE", "MODULO", captor.getValue().getId(), "Unidad 1");
    }

    @Test
    @DisplayName("crea un ejercicio con su consigna sin contarlo como lección")
    void createsEjercicio() {
        UUID moduloId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(modules.findById(moduloId))
                .thenReturn(Optional.of(new CursoModulo(UUID.randomUUID(), "Unidad 1", 0)));

        service.createEjercicio(new AdminEjercicioFormulario(
                moduloId, " Práctica 1 ", " Contá las señas ", 1), actor);

        ArgumentCaptor<Ejercicio> captor = ArgumentCaptor.forClass(Ejercicio.class);
        verify(exercises).save(captor.capture());
        assertThat(captor.getValue().getModuloId()).isEqualTo(moduloId);
        assertThat(captor.getValue().getTitulo()).isEqualTo("Práctica 1");
        assertThat(captor.getValue().getPrompt()).isEqualTo("Contá las señas");
        verify(auditoria).record(actor, "CREATE", "EJERCICIO", captor.getValue().getId(), "Práctica 1");
        verify(videos, never()).save(any());
    }

    @Test
    @DisplayName("publica y despublica un curso completo con auditoría")
    void publishesAndUnpublishesCompleteCurso() {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Curso curso = new Curso("taller-lsp", "Taller de LSP", "Inicial", 1, true,
                "Comunicación", "Resumen real", "/uploads/covers/aaaa.jpg", 1);
        when(courses.findById(cursoId)).thenReturn(Optional.of(curso));

        service.setPublished(cursoId, true, actor);

        assertThat(curso.getEstado()).isEqualTo("PUBLICADO");
        verify(auditoria).record(actor, "UPDATE", "CURSO", cursoId, "PUBLICADO");

        service.setPublished(cursoId, false, actor);

        assertThat(curso.getEstado()).isEqualTo("BORRADOR");
        verify(auditoria).record(actor, "UPDATE", "CURSO", cursoId, "BORRADOR");
    }

    @Test
    @DisplayName("el contador de lecciones se recalcula desde los videos al crear uno")
    void refreshesLessonsWhenVideoIsCreado() {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Curso curso = curso();
        when(modules.findById(moduloId))
                .thenReturn(Optional.of(new CursoModulo(cursoId, "Unidad 1", 0)));
        when(courses.findById(cursoId)).thenReturn(Optional.of(curso));
        when(videos.contarPorCursoId(any())).thenReturn(3);

        service.createVideo(new AdminVideoFormulario(moduloId, "Clase 1", 0,
                "https://youtu.be/dQw4w9WgXcQ"), actor);

        assertThat(curso.getLessons()).isEqualTo(3);
        verify(auditoria).record(eq(actor), eq("CREATE"), eq("VIDEO"), any(), eq("Clase 1"));
    }

    private static CursoModulo mockModulo() {
        return new CursoModulo(UUID.randomUUID(), "Módulo", 1);
    }

    private static Curso curso() {
        return new Curso("curso", "Curso", "Inicial", 0, false,
                "Programación", "", "", 1);
    }

    private static AdminCursoFormulario formulario() {
        return new AdminCursoFormulario("python-desde-cero", "Python desde cero", "Inicial", true,
                "Programación", "Variables y ciclos", "/img/cursos/python.jpg", 1);
    }
}
