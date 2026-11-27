package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.CursoRecurso;
import com.mundomanitas.api.model.CursoSeccion;
import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.service.CursoMaterialStorage;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.CursoRecursoRepository;
import com.mundomanitas.api.repository.CursoSeccionRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

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
@DisplayName("Carga administrativa de contenido")
class AdminCursoContenidoServiceTest {

    @Mock private CursoRepository courses;
    @Mock private CursoModuloRepository modules;
    @Mock private CursoSeccionRepository sections;
    @Mock private ContenidoItemRepository items;
    @Mock private CursoRecursoRepository resources;
    @Mock private VideoRepository videos;
    @Mock private EjercicioRepository exercises;
    @Mock private AdminAuditoriaService auditoria;
    @Mock private CursoMaterialStorage materials;

    @InjectMocks private AdminCursoContenidoServiceImpl service;

    private final UUID cursoId = UUID.randomUUID();
    private final UUID moduloId = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();

    @Test
    @DisplayName("crea un MATERIAL con enlace https validado")
    void createsMaterial() {
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo()));

        service.createItem(new AdminCursoContenidoService.ItemFormulario(moduloId, null, "MATERIAL",
                "Guía de estilo", "Lectura breve", "https://example.org/guia", null, null, 0, true),
                actor);

        ArgumentCaptor<ContenidoItem> captor = ArgumentCaptor.forClass(ContenidoItem.class);
        verify(items).save(captor.capture());
        assertThat(captor.getValue().getOrigenTipo()).isEqualTo("MATERIAL");
        assertThat(captor.getValue().getUrl()).isEqualTo("https://example.org/guia");
        verify(auditoria).record(eq(actor), eq("CREATE"), eq("ITEM"), any(), eq("Guía de estilo"));
    }

    @Test
    @DisplayName("rechaza enlaces inseguros y videos que no son de YouTube")
    void rejectsInvalidoEnlaces() {
        AdminCursoContenidoService.ItemFormulario insecure = new AdminCursoContenidoService.ItemFormulario(
                moduloId, null, "MATERIAL", "Guía", "Lectura", "http://example.org/guia",
                null, null, 0, true);
        assertThatThrownBy(() -> service.createItem(insecure, actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");

        AdminCursoContenidoService.ItemFormulario fakeVideo = new AdminCursoContenidoService.ItemFormulario(
                moduloId, null, "VIDEO", "Clase", null, null, "https://example.org/video",
                null, 0, true);
        assertThatThrownBy(() -> service.createItem(fakeVideo, actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YouTube");
    }

    @Test
    @DisplayName("crea un VIDEO en el CRUD existente con el ID de YouTube validado")
    void createsVideoInExistingCrud() {
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo()));
        ContenidoItem creado = material(UUID.randomUUID(), moduloId, 0);
        when(items.buscarPorOrigenTipoAndOrigenId(eq("VIDEO"), any())).thenReturn(creado);

        service.createItem(new AdminCursoContenidoService.ItemFormulario(moduloId, null, "VIDEO",
                "Clase 1", null, null, "https://youtu.be/dQw4w9WgXcQ", 600, 0, true), actor);

        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videos).save(captor.capture());
        assertThat(captor.getValue().getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(captor.getValue().getTitulo()).isEqualTo("Clase 1");
    }

    @Test
    @DisplayName("crea un VIDEO_LSP en content_items sin duplicar la fila de videos")
    void createsVideoLsp() {
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo()));

        service.createItem(new AdminCursoContenidoService.ItemFormulario(moduloId, null, "VIDEO_LSP",
                "Clase 1 en señas", "Versión en LSP", null, "https://youtu.be/dQw4w9WgXcQ",
                600, 0, true), actor);

        ArgumentCaptor<ContenidoItem> captor = ArgumentCaptor.forClass(ContenidoItem.class);
        verify(items).save(captor.capture());
        assertThat(captor.getValue().getOrigenTipo()).isEqualTo("VIDEO_LSP");
        assertThat(captor.getValue().getOrigenId()).isNull();
        assertThat(captor.getValue().getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
        verify(videos, never()).save(any());
    }

    @Test
    @DisplayName("edita y borra un VIDEO_LSP sin tocar la tabla de videos")
    void updatesAndDeletesVideoLsp() {
        UUID itemId = UUID.randomUUID();
        ContenidoItem lsp = new ContenidoItem(moduloId, null, "VIDEO_LSP", null,
                "Clase 1 en señas", "Versión en LSP", null, "abc", 600, 0, true);
        ReflectionTestUtils.setField(lsp, "id", itemId);
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo()));
        when(items.findById(itemId)).thenReturn(Optional.of(lsp));

        service.updateItem(itemId, new AdminCursoContenidoService.ItemFormulario(moduloId, null,
                "VIDEO_LSP", "Clase 2 en señas", "Versión actualizada", null,
                "https://youtu.be/dQw4w9WgXcQ", 700, 1, false), actor);
        assertThat(lsp.getTitulo()).isEqualTo("Clase 2 en señas");
        assertThat(lsp.getDescripcion()).isEqualTo("Versión actualizada");
        assertThat(lsp.getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(lsp.getPosicion()).isEqualTo(1);
        assertThat(lsp.isVisible()).isFalse();

        service.deleteItem(itemId, actor);
        verify(items).delete(lsp);
        verify(videos, never()).delete(any());
    }

    @Test
    @DisplayName("guarda secciones y el sílabo con recursos complementarios")
    void savesSectionsAndSilabo() {
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo()));
        Curso curso = curso();
        when(courses.findById(cursoId)).thenReturn(Optional.of(curso));

        service.createSeccion(moduloId,
                new AdminCursoContenidoService.SeccionFormulario("Lecturas", 0, true), actor);
        service.saveSilabo(cursoId, new AdminCursoContenidoService.SilaboFormulario(
                "Objetivos reales", "Requisitos reales", List.of(
                        new AdminCursoContenidoService.RecursoFormulario(null, "Guía",
                                "https://example.org/guia", 0))), actor);

        ArgumentCaptor<CursoSeccion> seccionCaptor = ArgumentCaptor.forClass(CursoSeccion.class);
        verify(sections).save(seccionCaptor.capture());
        assertThat(seccionCaptor.getValue().getTitulo()).isEqualTo("Lecturas");
        assertThat(curso.getObjectives()).isEqualTo("Objetivos reales");
        assertThat(curso.getRequirements()).isEqualTo("Requisitos reales");
        ArgumentCaptor<CursoRecurso> recursoCaptor = ArgumentCaptor.forClass(CursoRecurso.class);
        verify(resources).save(recursoCaptor.capture());
        assertThat(recursoCaptor.getValue().getUrl()).isEqualTo("https://example.org/guia");
        verify(auditoria).record(eq(actor), eq("CREATE"), eq("SECCION"), any(), eq("Lecturas"));
        verify(auditoria).record(eq(actor), eq("UPDATE"), eq("SILABO"), eq(cursoId), any());
    }

    @Test
    @DisplayName("sube y baja ítems con botones, sin arrastrar")
    void reordersItems() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        ContenidoItem first = material(firstId, moduloId, 0);
        ContenidoItem second = material(secondId, moduloId, 1);
        when(items.findById(firstId)).thenReturn(Optional.of(first));
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(first, second));

        service.moveItem(firstId, false, actor);

        assertThat(first.getPosicion()).isEqualTo(1);
        assertThat(second.getPosicion()).isEqualTo(0);
        verify(items).save(first);
        verify(items).save(second);
        verify(auditoria).record(eq(actor), eq("UPDATE"), eq("ITEM"), eq(firstId), eq("BAJAR"));
    }

    @Test
    @DisplayName("sube un documento y guarda su ruta interna y su nombre original")
    void createsDocument() {
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo()));
        MockMultipartFile file = new MockMultipartFile(
                "file", "Guía de redes.pdf", "application/pdf", "contenido".getBytes());
        when(materials.store(file)).thenReturn("/uploads/materials/abc.pdf");

        service.createDocument(new AdminCursoContenidoService.DocumentFormulario(moduloId, null,
                "Guía descargable", "Material de la sesión", file, null, 0, true), actor);

        ArgumentCaptor<ContenidoItem> captor = ArgumentCaptor.forClass(ContenidoItem.class);
        verify(items).save(captor.capture());
        assertThat(captor.getValue().getOrigenTipo()).isEqualTo("MATERIAL");
        assertThat(captor.getValue().getUrl()).isEqualTo("/uploads/materials/abc.pdf");
        assertThat(captor.getValue().getFileName()).isEqualTo("Guía de redes.pdf");
        verify(auditoria).record(eq(actor), eq("CREATE"), eq("ITEM"), any(), eq("Guía descargable"));
    }

    @Test
    @DisplayName("borra el archivo subido cuando se elimina el documento")
    void deletesStoredFileWithItem() {
        UUID itemId = UUID.randomUUID();
        ContenidoItem document = material(itemId, moduloId, 0);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.pdf");
        when(items.findById(itemId)).thenReturn(Optional.of(document));

        service.deleteItem(itemId, actor);

        verify(materials).delete("/uploads/materials/abc.pdf");
        verify(items).delete(document);
        verify(auditoria).record(eq(actor), eq("DELETE"), eq("ITEM"), eq(itemId), eq("Guía"));
    }

    @Test
    @DisplayName("alterna la visibilidad de módulos, secciones e ítems")
    void togglesVisibility() {
        CursoModulo modulo = modulo();
        UUID seccionId = UUID.randomUUID();
        CursoSeccion seccion = new CursoSeccion(moduloId, "Lecturas", 0, true);
        ReflectionTestUtils.setField(seccion, "id", seccionId);
        UUID itemId = UUID.randomUUID();
        ContenidoItem item = material(itemId, moduloId, 0);

        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo));
        when(sections.findById(seccionId)).thenReturn(Optional.of(seccion));
        when(items.findById(itemId)).thenReturn(Optional.of(item));

        service.toggleModuloVisible(cursoId, moduloId, actor);
        service.toggleSeccionVisible(cursoId, seccionId, actor);
        service.toggleItemVisible(cursoId, itemId, actor);

        assertThat(modulo.isVisible()).isFalse();
        assertThat(seccion.isVisible()).isFalse();
        assertThat(item.isVisible()).isFalse();
        verify(modules).save(modulo);
        verify(sections).save(seccion);
        verify(items).save(item);
        verify(auditoria).record(eq(actor), eq("UPDATE"), eq("MODULO"), eq(moduloId), eq("OCULTO"));
        verify(auditoria).record(eq(actor), eq("UPDATE"), eq("SECCION"), eq(seccionId), eq("OCULTA"));
        verify(auditoria).record(eq(actor), eq("UPDATE"), eq("ITEM"), eq(itemId), eq("OCULTO"));
    }

    private CursoModulo modulo() {
        CursoModulo modulo = new CursoModulo(cursoId, "Módulo 1", 0);
        ReflectionTestUtils.setField(modulo, "id", moduloId);
        return modulo;
    }

    private Curso curso() {
        Curso curso = new Curso("python", "Python", "Inicial", 1, true,
                "Programación", "Resumen real", null, 0);
        ReflectionTestUtils.setField(curso, "id", cursoId);
        return curso;
    }

    private ContenidoItem material(UUID id, UUID moduloId, int posicion) {
        ContenidoItem item = new ContenidoItem(moduloId, null, "MATERIAL", null, "Guía",
                "Lectura", "https://example.org/guia", null, null, posicion, true);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
