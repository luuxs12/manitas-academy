package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.CourseResource;
import com.mundomanitas.api.model.CourseSection;
import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.service.CourseMaterialStorage;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.CourseResourceRepository;
import com.mundomanitas.api.repository.CourseSectionRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Carga administrativa de contenido")
class AdminCourseContentServiceTest {

    @Mock private CourseRepository courses;
    @Mock private CourseModuleRepository modules;
    @Mock private CourseSectionRepository sections;
    @Mock private ContentItemRepository items;
    @Mock private CourseResourceRepository resources;
    @Mock private VideoRepository videos;
    @Mock private ExerciseRepository exercises;
    @Mock private AdminAuditService audit;
    @Mock private CourseMaterialStorage materials;

    @InjectMocks private AdminCourseContentServiceImpl service;

    private final UUID courseId = UUID.randomUUID();
    private final UUID moduleId = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();

    @Test
    @DisplayName("crea un MATERIAL con enlace https validado")
    void createsMaterial() {
        when(modules.findById(moduleId)).thenReturn(Optional.of(module()));

        service.createItem(new AdminCourseContentService.ItemForm(moduleId, null, "MATERIAL",
                "Guía de estilo", "Lectura breve", "https://example.org/guia", null, null, 0, true),
                actor);

        ArgumentCaptor<ContentItem> captor = ArgumentCaptor.forClass(ContentItem.class);
        verify(items).save(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo("MATERIAL");
        assertThat(captor.getValue().getUrl()).isEqualTo("https://example.org/guia");
        verify(audit).record(eq(actor), eq("CREATE"), eq("ITEM"), any(), eq("Guía de estilo"));
    }

    @Test
    @DisplayName("rechaza enlaces inseguros y videos que no son de YouTube")
    void rejectsInvalidLinks() {
        AdminCourseContentService.ItemForm insecure = new AdminCourseContentService.ItemForm(
                moduleId, null, "MATERIAL", "Guía", "Lectura", "http://example.org/guia",
                null, null, 0, true);
        assertThatThrownBy(() -> service.createItem(insecure, actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("https");

        AdminCourseContentService.ItemForm fakeVideo = new AdminCourseContentService.ItemForm(
                moduleId, null, "VIDEO", "Clase", null, null, "https://example.org/video",
                null, 0, true);
        assertThatThrownBy(() -> service.createItem(fakeVideo, actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YouTube");
    }

    @Test
    @DisplayName("crea un VIDEO en el CRUD existente con el ID de YouTube validado")
    void createsVideoInExistingCrud() {
        when(modules.findById(moduleId)).thenReturn(Optional.of(module()));
        ContentItem created = material(UUID.randomUUID(), moduleId, 0);
        when(items.findBySourceTypeAndSourceId(eq("VIDEO"), any())).thenReturn(created);

        service.createItem(new AdminCourseContentService.ItemForm(moduleId, null, "VIDEO",
                "Clase 1", null, null, "https://youtu.be/dQw4w9WgXcQ", 600, 0, true), actor);

        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videos).save(captor.capture());
        assertThat(captor.getValue().getYoutubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(captor.getValue().getTitle()).isEqualTo("Clase 1");
    }

    @Test
    @DisplayName("guarda secciones y el sílabo con recursos complementarios")
    void savesSectionsAndSyllabus() {
        when(modules.findById(moduleId)).thenReturn(Optional.of(module()));
        Course course = course();
        when(courses.findById(courseId)).thenReturn(Optional.of(course));

        service.createSection(moduleId,
                new AdminCourseContentService.SectionForm("Lecturas", 0, true), actor);
        service.saveSyllabus(courseId, new AdminCourseContentService.SyllabusForm(
                "Objetivos reales", "Requisitos reales", List.of(
                        new AdminCourseContentService.ResourceForm(null, "Guía",
                                "https://example.org/guia", 0))), actor);

        ArgumentCaptor<CourseSection> sectionCaptor = ArgumentCaptor.forClass(CourseSection.class);
        verify(sections).save(sectionCaptor.capture());
        assertThat(sectionCaptor.getValue().getTitle()).isEqualTo("Lecturas");
        assertThat(course.getObjectives()).isEqualTo("Objetivos reales");
        assertThat(course.getRequirements()).isEqualTo("Requisitos reales");
        ArgumentCaptor<CourseResource> resourceCaptor = ArgumentCaptor.forClass(CourseResource.class);
        verify(resources).save(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getUrl()).isEqualTo("https://example.org/guia");
        verify(audit).record(eq(actor), eq("CREATE"), eq("SECCION"), any(), eq("Lecturas"));
        verify(audit).record(eq(actor), eq("UPDATE"), eq("SILABO"), eq(courseId), any());
    }

    @Test
    @DisplayName("sube y baja ítems con botones, sin arrastrar")
    void reordersItems() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        ContentItem first = material(firstId, moduleId, 0);
        ContentItem second = material(secondId, moduleId, 1);
        when(items.findById(firstId)).thenReturn(Optional.of(first));
        when(items.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(first, second));

        service.moveItem(firstId, false, actor);

        assertThat(first.getPosition()).isEqualTo(1);
        assertThat(second.getPosition()).isEqualTo(0);
        verify(items).save(first);
        verify(items).save(second);
        verify(audit).record(eq(actor), eq("UPDATE"), eq("ITEM"), eq(firstId), eq("BAJAR"));
    }

    @Test
    @DisplayName("sube un documento y guarda su ruta interna y su nombre original")
    void createsDocument() {
        when(modules.findById(moduleId)).thenReturn(Optional.of(module()));
        MockMultipartFile file = new MockMultipartFile(
                "file", "Guía de redes.pdf", "application/pdf", "contenido".getBytes());
        when(materials.store(file)).thenReturn("/uploads/materials/abc.pdf");

        service.createDocument(new AdminCourseContentService.DocumentForm(moduleId, null,
                "Guía descargable", "Material de la sesión", file, null, 0, true), actor);

        ArgumentCaptor<ContentItem> captor = ArgumentCaptor.forClass(ContentItem.class);
        verify(items).save(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo("MATERIAL");
        assertThat(captor.getValue().getUrl()).isEqualTo("/uploads/materials/abc.pdf");
        assertThat(captor.getValue().getFileName()).isEqualTo("Guía de redes.pdf");
        verify(audit).record(eq(actor), eq("CREATE"), eq("ITEM"), any(), eq("Guía descargable"));
    }

    @Test
    @DisplayName("borra el archivo subido cuando se elimina el documento")
    void deletesStoredFileWithItem() {
        UUID itemId = UUID.randomUUID();
        ContentItem document = material(itemId, moduleId, 0);
        ReflectionTestUtils.setField(document, "url", "/uploads/materials/abc.pdf");
        when(items.findById(itemId)).thenReturn(Optional.of(document));

        service.deleteItem(itemId, actor);

        verify(materials).delete("/uploads/materials/abc.pdf");
        verify(items).delete(document);
        verify(audit).record(eq(actor), eq("DELETE"), eq("ITEM"), eq(itemId), eq("Guía"));
    }

    private CourseModule module() {
        CourseModule module = new CourseModule(courseId, "Módulo 1", 0);
        ReflectionTestUtils.setField(module, "id", moduleId);
        return module;
    }

    private Course course() {
        Course course = new Course("python", "Python", "Inicial", 1, true,
                "Programación", "Resumen real", null, 0);
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }

    private ContentItem material(UUID id, UUID moduleId, int position) {
        ContentItem item = new ContentItem(moduleId, null, "MATERIAL", null, "Guía",
                "Lectura", "https://example.org/guia", null, null, position, true);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
