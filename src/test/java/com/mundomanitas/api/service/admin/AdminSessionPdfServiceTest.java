package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.CourseSection;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.CourseSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PDF de una sesión del curso")
class AdminSessionPdfServiceTest {

    @Mock private CourseRepository courses;
    @Mock private CourseModuleRepository modules;
    @Mock private CourseSectionRepository sections;
    @Mock private ContentItemRepository items;
    @Mock private AdminAuditService audit;

    private AdminSessionPdfService service;

    @BeforeEach
    void setUp() {
        service = new AdminSessionPdfServiceImpl(courses, modules, sections, items,
                audit, new PlainPdfWriter());
    }

    @Test
    @DisplayName("el PDF reúne el curso, la sesión y los ítems reales, incluso los ocultos")
    void sessionPdfUsesRealContent() {
        UUID actor = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        Course course = mock(Course.class);
        when(course.getTitle()).thenReturn("Python");
        when(courses.findById(courseId)).thenReturn(Optional.of(course));

        CourseModule module = mock(CourseModule.class);
        when(module.getCourseId()).thenReturn(courseId);
        when(module.getTitle()).thenReturn("Sesion 01");
        when(modules.findById(moduleId)).thenReturn(Optional.of(module));

        UUID sectionId = UUID.randomUUID();
        CourseSection section = mock(CourseSection.class);
        when(section.getId()).thenReturn(sectionId);
        when(section.getTitle()).thenReturn("Temas de la sesion");
        when(sections.findByModuleIdOrderByPositionAsc(moduleId)).thenReturn(List.of(section));

        ContentItem video = item("Video 1", "VIDEO", true, sectionId);
        ContentItem hidden = item("Guia 2", "MATERIAL", false, sectionId);
        ContentItem loose = item("Ejercicio 3", "EJERCICIO", true, null);
        when(items.findByModuleIdOrderByPositionAsc(moduleId))
                .thenReturn(List.of(video, hidden, loose));

        ExportResult result = service.modulePdf(moduleId, actor);

        String pdf = new String(result.bytes(), StandardCharsets.ISO_8859_1);
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.fileName()).startsWith("mundo-manitas-sesion-").endsWith(".pdf");
        assertThat(pdf).startsWith("%PDF-1.4")
                .contains("/Im0 Do")
                .contains("Sesión: Sesion 01")
                .contains("Python")
                .contains("Temas de la sesion")
                .contains("Video 1")
                .contains("Guia 2")
                .contains("Sin sección")
                .contains("Ítems:")
                .contains("Visible")
                .contains("Oculta")
                .contains("Página 1 / 1");
        verify(audit).record(actor, "EXPORT", "MODULO", moduleId, "Sesion 01");
    }

    @Test
    @DisplayName("un módulo inexistente no genera archivo ni auditoría")
    void missingModuleIsRejected() {
        UUID moduleId = UUID.randomUUID();
        when(modules.findById(moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modulePdf(moduleId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El módulo solicitado no existe.");

        verifyNoInteractions(audit);
    }

    private ContentItem item(String title, String type, boolean visible, UUID sectionId) {
        ContentItem item = mock(ContentItem.class);
        when(item.getTitle()).thenReturn(title);
        when(item.getSourceType()).thenReturn(type);
        if (sectionId != null) {
            when(item.getSectionId()).thenReturn(sectionId);
        }
        when(item.isVisible()).thenReturn(visible);
        return item;
    }
}
