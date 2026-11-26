package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.CursoRecurso;
import com.mundomanitas.api.model.CursoSeccion;
import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.ItemProgreso;
import com.mundomanitas.api.model.LeccionProgreso;
import com.mundomanitas.api.model.CuestionarioIntento;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.CursoRecursoRepository;
import com.mundomanitas.api.repository.CursoSeccionRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.ItemProgresoRepository;
import com.mundomanitas.api.repository.LeccionProgresoRepository;
import com.mundomanitas.api.repository.CuestionarioIntentoRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Contenido del curso y avance real")
class CursoContenidoServiceTest {

    @Mock private CursoRepository courses;
    @Mock private CursoModuloRepository modules;
    @Mock private CursoSeccionRepository sections;
    @Mock private ContenidoItemRepository items;
    @Mock private CursoRecursoRepository resources;
    @Mock private InscripcionRepository enrollments;
    @Mock private LeccionProgresoRepository leccionProgreso;
    @Mock private ItemProgresoRepository itemProgreso;
    @Mock private CuestionarioIntentoRepository cuestionarioAttempts;
    @Mock private EjercicioRepository exercises;
    @Mock private VideoRepository videos;
    @Mock private CursoMaterialStorage materials;

    @InjectMocks private CursoContenidoServiceImpl service;

    private final UUID cursoId = UUID.randomUUID();

    @Test
    @DisplayName("el sílabo sale de la base, con sus recursos complementarios")
    void silaboFromBase() {
        Curso curso = curso(true);
        when(courses.findById(cursoId)).thenReturn(Optional.of(curso));
        when(resources.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(
                new CursoRecurso(cursoId, "Guía de estilo", "https://example.org/guia", 0)));

        CursoContenidoService.Silabo silabo = service.silabo(cursoId);

        assertThat(silabo.resumen()).isEqualTo("Resumen real");
        assertThat(silabo.objectives()).isEqualTo("Objetivos reales");
        assertThat(silabo.requirements()).isEqualTo("Requisitos reales");
        assertThat(silabo.resources()).extracting(
                CursoContenidoService.RecursoVista::url).containsExactly("https://example.org/guia");
    }

    @Test
    @DisplayName("agrupa ítems por sección, marca lo visto y elige el siguiente pendiente")
    void esquemaGroupsAndFindsNextPending() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID seccionId = UUID.randomUUID();
        UUID videoItemId = UUID.randomUUID();
        UUID materialItemId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();

        CursoModulo modulo = modulo(moduloId);
        CursoSeccion seccion = seccion(seccionId, moduloId);
        ContenidoItem video = item(videoItemId, moduloId, seccionId, "VIDEO", videoId,
                "Clase 1", "dQw4w9WgXcQ", 0);
        ContenidoItem material = item(materialItemId, moduloId, null, "MATERIAL", null,
                "Lectura 1", null, 1);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(seccion));
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(video, material));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId))
                .thenReturn(List.of(new LeccionProgreso(inscripcionId, videoId)));
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());

        CursoContenidoService.Esquema esquema = service.esquema(cursoId, inscripcionId, null, false);

        assertThat(esquema.totalItems()).isEqualTo(2);
        assertThat(esquema.doneItems()).isEqualTo(1);
        assertThat(esquema.percent()).isEqualTo(50);
        assertThat(esquema.modules().get(0).percent()).isEqualTo(50);
        assertThat(esquema.modules().get(0).sections().get(0).items().get(0).visto()).isTrue();
        assertThat(esquema.modules().get(0).items().get(0).visto()).isFalse();
        assertThat(esquema.nextPending()).isEqualTo(materialItemId);
    }

    @Test
    @DisplayName("oculta los ítems invisibles salvo en la vista previa")
    void hiddenItemsOnlyInVistaprevia() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem hidden = item(UUID.randomUUID(), moduloId, null, "MATERIAL", null,
                "Borrador", null, 0);
        ReflectionTestUtils.setField(hidden, "visible", false);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo(moduloId)));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(hidden));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());

        CursoContenidoService.Esquema estudiante = service.esquema(cursoId, inscripcionId, null, false);
        CursoContenidoService.Esquema vistaprevia = service.esquema(cursoId, inscripcionId, null, true);

        assertThat(estudiante.totalItems()).isZero();
        assertThat(estudiante.nextPending()).isNull();
        assertThat(vistaprevia.totalItems()).isEqualTo(1);
        assertThat(vistaprevia.nextPending()).isEqualTo(hidden.getId());
    }

    @Test
    @DisplayName("oculta los módulos invisibles salvo en la vista previa")
    void hiddenModulesOnlyInVistaprevia() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        CursoModulo hidden = modulo(moduloId);
        ReflectionTestUtils.setField(hidden, "visible", false);
        ContenidoItem item = item(UUID.randomUUID(), moduloId, null, "MATERIAL", null,
                "Borrador", null, 0);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(hidden));
        lenient().when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(item));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());

        CursoContenidoService.Esquema estudiante = service.esquema(cursoId, inscripcionId, null, false);
        CursoContenidoService.Esquema vistaprevia = service.esquema(cursoId, inscripcionId, null, true);

        assertThat(estudiante.modules()).isEmpty();
        assertThat(estudiante.totalItems()).isZero();
        assertThat(vistaprevia.modules()).hasSize(1);
        assertThat(vistaprevia.totalItems()).isEqualTo(1);
    }

    @Test
    @DisplayName("no guarda avance de un ítem oculto para el estudiante")
    void hiddenItemCannotBeMarkedAsVisto() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        UUID moduloId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Inscripcion inscripcion = inscripcion(estudiante);
        ContenidoItem hidden = item(itemId, moduloId, null, "MATERIAL", null, "Borrador", null, 0);
        ReflectionTestUtils.setField(hidden, "visible", false);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> service.markVisto(estudiante, "python", itemId, true))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);

        verify(itemProgreso, never()).save(any(ItemProgreso.class));
    }

    @Test
    @DisplayName("marca como bloqueado el contenido premium para una cuenta FREE")
    void premiumLockForFreeUsuario() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem material = item(UUID.randomUUID(), moduloId, null, "MATERIAL", null,
                "Lectura premium", null, 0);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(false)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo(moduloId)));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(material));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());

        CursoContenidoService.Esquema esquema = service.esquema(cursoId, inscripcionId, new Usuario(
                "alumno@test.local", "hash", "Alumno"), false);

        assertThat(esquema.nextPending()).isNull();
        assertThat(esquema.modules().get(0).items().get(0).bloqueado()).isTrue();
    }

    @Test
    @DisplayName("expone el id del video para abrir la clase desde el temario")
    void exposesVideoOrigen() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ContenidoItem video = item(itemId, moduloId, null, "VIDEO", videoId,
                "Clase 1", "dQw4w9WgXcQ", 0);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo(moduloId)));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(video));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());

        CursoContenidoService.Esquema esquema = service.esquema(cursoId, inscripcionId, null, false);

        assertThat(esquema.modules().get(0).items().get(0).origenId()).isEqualTo(videoId);
    }

    @Test
    @DisplayName("marca como visto un material sin tocar el progreso de videos")
    void marksMaterialAsVisto() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        UUID moduloId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Inscripcion inscripcion = inscripcion(estudiante);
        ContenidoItem material = item(itemId, moduloId, null, "MATERIAL", null, "Guía", null, 0);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(material));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo(moduloId)));

        service.markVisto(estudiante, "python", itemId, true);

        verify(itemProgreso).save(any(ItemProgreso.class));
    }

    @Test
    @DisplayName("la duración se expresa en minutos y horas, no en segundos crudos")
    void duracionLabel() {
        assertThat(withDuracion(null).duracionLabel()).isEqualTo("Sin duración");
        assertThat(withDuracion(0).duracionLabel()).isEqualTo("0 s");
        assertThat(withDuracion(90).duracionLabel()).isEqualTo("1 min 30 s");
        assertThat(withDuracion(600).duracionLabel()).isEqualTo("10 min");
        assertThat(withDuracion(3600).duracionLabel()).isEqualTo("1 h");
        assertThat(withDuracion(3725).duracionLabel()).isEqualTo("1 h 2 min");
    }

    @Test
    @DisplayName("descarga un documento subido para una cuenta inscripta en el curso")
    void downloadsUploadedMaterial() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        Inscripcion inscripcion = inscripcion(estudiante);
        UUID itemId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem document = item(itemId, moduloId, null, "MATERIAL", null,
                "Guía descargable", null, 0);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.pdf");
        ReflectionTestUtils.setField(document, "nombreArchivo", "guia-de-redes.pdf");
        byte[] bytes = "contenido".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(document));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo(moduloId)));
        when(materials.read("/uploads/materials/abc.pdf")).thenReturn(bytes);
        when(materials.contenidoTipo("/uploads/materials/abc.pdf")).thenReturn("application/pdf");

        CursoContenidoService.MaterialDescarga descarga =
                service.descargaMaterial(estudiante, "python", itemId);

        assertThat(descarga.nombreArchivo()).isEqualTo("guia-de-redes.pdf");
        assertThat(descarga.contenidoTipo()).isEqualTo("application/pdf");
        assertThat(descarga.bytes()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("prepara la ficha de un PDF subido para abrirlo en el visor")
    void viewsUploadedPdf() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        Inscripcion inscripcion = inscripcion(estudiante);
        UUID itemId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem document = item(itemId, moduloId, null, "MATERIAL", null,
                "Guía de la clase 01", null, 0);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.pdf");
        ReflectionTestUtils.setField(document, "nombreArchivo", "guia-de-clase-01.pdf");

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(document));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo(moduloId)));
        when(materials.contenidoTipo("/uploads/materials/abc.pdf")).thenReturn("application/pdf");

        CursoContenidoService.MaterialVista view = service.vistaMaterial(estudiante, "python", itemId);

        assertThat(view.id()).isEqualTo(itemId);
        assertThat(view.titulo()).isEqualTo("Guía de la clase 01");
        assertThat(view.nombreArchivo()).isEqualTo("guia-de-clase-01.pdf");
        assertThat(view.contenidoTipo()).isEqualTo("application/pdf");
        verify(materials, never()).read(any());
    }

    @Test
    @DisplayName("entrega al visor los bytes del PDF con su nombre seguro")
    void readsPdfForViewer() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        Inscripcion inscripcion = inscripcion(estudiante);
        UUID itemId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem document = item(itemId, moduloId, null, "MATERIAL", null,
                "Guía de la clase 01", null, 0);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.pdf");
        ReflectionTestUtils.setField(document, "nombreArchivo", "guia de clase 01.pdf");
        byte[] bytes = "%PDF-1.7 contenido".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(document));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo(moduloId)));
        when(materials.contenidoTipo("/uploads/materials/abc.pdf")).thenReturn("application/pdf");
        when(materials.read("/uploads/materials/abc.pdf")).thenReturn(bytes);

        CursoContenidoService.MaterialDescarga pdf = service.leidoPdf(estudiante, "python", itemId);

        assertThat(pdf.nombreArchivo()).isEqualTo("guia de clase 01.pdf");
        assertThat(pdf.contenidoTipo()).isEqualTo("application/pdf");
        assertThat(pdf.bytes()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("el visor rechaza documentos que no son PDF u ocultos")
    void refusesNonPdfForViewer() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        Inscripcion inscripcion = inscripcion(estudiante);
        UUID itemId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem document = item(itemId, moduloId, null, "MATERIAL", null,
                "Borrador", null, 0);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.doc");

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(document));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo(moduloId)));
        when(materials.contenidoTipo("/uploads/materials/abc.doc")).thenReturn("application/msword");

        assertThatThrownBy(() -> service.vistaMaterial(estudiante, "python", itemId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> service.leidoPdf(estudiante, "python", itemId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
        verify(materials, never()).read(any());
    }

    @Test
    @DisplayName("no descarga documentos ocultos ni de otro curso")
    void refusesHiddenMaterial() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        Inscripcion inscripcion = inscripcion(estudiante);
        UUID itemId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        ContenidoItem document = item(itemId, moduloId, null, "MATERIAL", null,
                "Borrador", null, 0);
        ReflectionTestUtils.setField(document, "visible", false);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.pdf");

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.descargaMaterial(estudiante, "python", itemId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
        verify(materials, never()).read(any());
    }

    @Test
    @DisplayName("el ejercicio cuenta como visto cuando existe su intento de cuestionario")
    void ejercicioVistoComesFromCuestionarioIntento() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID ejercicioId = UUID.randomUUID();
        ContenidoItem ejercicio = item(UUID.randomUUID(), moduloId, null, "EJERCICIO", ejercicioId,
                "Practica", null, 0);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo(moduloId)));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(ejercicio));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(cuestionarioAttempts.buscarPorInscripcionIdIn(Set.of(inscripcionId)))
                .thenReturn(List.of(new CuestionarioIntento(inscripcionId, ejercicioId, 20)));

        CursoContenidoService.Esquema esquema = service.esquema(cursoId, inscripcionId, null, false);

        CursoContenidoService.ItemVista view = esquema.modules().get(0).items().get(0);
        assertThat(view.visto()).isTrue();
        assertThat(view.cuestionarioDone()).isTrue();
        assertThat(esquema.doneItems()).isEqualTo(1);
    }

    @Test
    @DisplayName("una cuenta con LSP ve la clase en señas y no el video habitual")
    void esquemaFiltraVideoSegunLsp() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID videoItemId = UUID.randomUUID();
        UUID lspItemId = UUID.randomUUID();
        ContenidoItem video = item(videoItemId, moduloId, null, "VIDEO", videoId,
                "Clase 1", "dQw4w9WgXcQ", 0);
        ContenidoItem lsp = item(lspItemId, moduloId, null, "VIDEO_LSP", null,
                "Clase 1 en señas", "dQw4w9WgXcQ", 1);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo(moduloId)));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(video, lsp));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId))
                .thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId))
                .thenReturn(List.of());

        Usuario conLsp = new Usuario("lsp@example.com", "hash", "Ana Lsp");
        conLsp.changeUsaLsp(true);
        CursoContenidoService.Esquema paraLsp = service.esquema(cursoId, inscripcionId, conLsp, false);
        assertThat(paraLsp.modules().get(0).items())
                .extracting(CursoContenidoService.ItemVista::id).containsExactly(lspItemId);

        Usuario sinLsp = new Usuario("oyente@example.com", "hash", "Ana Oyente");
        CursoContenidoService.Esquema sinIdentificador =
                service.esquema(cursoId, inscripcionId, sinLsp, false);
        assertThat(sinIdentificador.modules().get(0).items())
                .extracting(CursoContenidoService.ItemVista::id).containsExactly(videoItemId);
    }

    @Test
    @DisplayName("un intento fallido no marca el ejercicio como visto")
    void ejercicioNoVistoConIntentoFallido() {
        UUID inscripcionId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID ejercicioId = UUID.randomUUID();
        ContenidoItem ejercicio = item(UUID.randomUUID(), moduloId, null, "EJERCICIO", ejercicioId,
                "Practica", null, 0);

        when(courses.findById(cursoId)).thenReturn(Optional.of(curso(true)));
        when(modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)).thenReturn(List.of(modulo(moduloId)));
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of());
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(ejercicio));
        when(leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId)).thenReturn(List.of());
        when(cuestionarioAttempts.buscarPorInscripcionIdIn(Set.of(inscripcionId)))
                .thenReturn(List.of(new CuestionarioIntento(inscripcionId, ejercicioId, 14)));

        CursoContenidoService.Esquema esquema = service.esquema(cursoId, inscripcionId, null, false);

        CursoContenidoService.ItemVista view = esquema.modules().get(0).items().get(0);
        assertThat(view.visto()).isFalse();
        assertThat(view.cuestionarioDone()).isFalse();
        assertThat(esquema.doneItems()).isZero();
    }

    @Test
    @DisplayName("un ejercicio no se puede marcar como visto a mano")
    void ejercicioCannotBeMarkedAsVistoManually() {
        Usuario estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        UUID moduloId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Inscripcion inscripcion = inscripcion(estudiante);
        ContenidoItem ejercicio = item(itemId, moduloId, null, "EJERCICIO", UUID.randomUUID(),
                "Practica", null, 0);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python")).thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(ejercicio));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo(moduloId)));

        assertThatThrownBy(() -> service.markVisto(estudiante, "python", itemId, true))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(itemProgreso, never()).save(any(ItemProgreso.class));
    }

    private CursoContenidoService.ItemVista withDuracion(Integer segundos) {
        return new CursoContenidoService.ItemVista(UUID.randomUUID(), "VIDEO", "Clase", null,
                null, "abc", segundos, true, false, false);
    }

    private Curso curso(boolean free) {
        Curso curso = new Curso("python", "Python", "Inicial", 1, free,
                "Programación", "Resumen real", null, 0);
        ReflectionTestUtils.setField(curso, "id", cursoId);
        curso.updateSilabo("Objetivos reales", "Requisitos reales");
        return curso;
    }

    private CursoModulo modulo(UUID moduloId) {
        CursoModulo modulo = new CursoModulo(cursoId, "Módulo 1", 0);
        ReflectionTestUtils.setField(modulo, "id", moduloId);
        return modulo;
    }

    private CursoSeccion seccion(UUID seccionId, UUID moduloId) {
        CursoSeccion seccion = new CursoSeccion(moduloId, "Lecturas", 0, true);
        ReflectionTestUtils.setField(seccion, "id", seccionId);
        return seccion;
    }

    private ContenidoItem item(UUID itemId, UUID moduloId, UUID seccionId, String type,
                             UUID origenId, String titulo, String youtubeId, int posicion) {
        ContenidoItem item = new ContenidoItem(moduloId, seccionId, type, origenId, titulo,
                "Descripción real", "https://example.org/material", youtubeId, 600, posicion, true);
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private Inscripcion inscripcion(Usuario estudiante) {
        Curso curso = curso(true);
        Inscripcion inscripcion = new Inscripcion(estudiante, curso);
        ReflectionTestUtils.setField(inscripcion, "id", UUID.randomUUID());
        return inscripcion;
    }
}
