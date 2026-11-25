package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminCourseStructureController;
import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.CourseContentService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCourseContentService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("Estructura de contenido en el panel administrativo")
class AdminCourseStructureFlowTest {

    private AdminCatalogService catalog;
    private AdminCourseContentService adminContent;
    private CourseContentService content;
    private MockMvc mvc;
    private UUID actor;
    private UUID courseId;
    private UUID moduleId;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        courseId = UUID.randomUUID();
        moduleId = UUID.randomUUID();
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRole()).thenReturn("ADMIN");
        when(admin.getEmail()).thenReturn("admin@prueba.test");
        when(admin.getPasswordHash()).thenReturn("hash");
        when(admin.getFullName()).thenReturn("Administradora");
        AuthenticatedUser principal = new AuthenticatedUser(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        catalog = mock(AdminCatalogService.class);
        adminContent = mock(AdminCourseContentService.class);
        content = mock(CourseContentService.class);
        AdminNotificationService notifications = mock(AdminNotificationService.class);
        when(notifications.recent(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);

        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminCourseStructureController(notifications, catalog, adminContent, content))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("muestra módulos, secciones, ítems y sílabo leídos de PostgreSQL")
    void rendersStructure() throws Exception {
        UUID sectionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CourseContentService.ItemView item = new CourseContentService.ItemView(itemId, "VIDEO",
                "Clase 1", "Introducción", null, "dQw4w9WgXcQ", 600, true, false, false);
        CourseContentService.Outline outline = new CourseContentService.Outline(
                List.of(new CourseContentService.ModuleView(moduleId, "Unidad 1", true,
                        List.of(new CourseContentService.SectionView(sectionId, "Lecturas", true,
                                List.of(item))), List.of(), 0)),
                1, 0, 0, itemId);
        when(catalog.courseForm(courseId)).thenReturn(new AdminCourseForm("python", "Python",
                "Inicial", true, "Programación", "Resumen real", "", 0));
        when(content.outline(courseId, null, null, true)).thenReturn(outline);
        when(content.syllabus(courseId)).thenReturn(new CourseContentService.Syllabus(
                "Resumen real", "Objetivos reales", "Requisitos reales",
                List.of(new CourseContentService.ResourceView(UUID.randomUUID(), "Guía",
                        "https://example.org/guia"))));

        mvc.perform(get("/admin/cursos/{id}/contenido", courseId))
                .andExpect(view().name("admin/contenido"))
                .andExpect(model().attribute("courseTitle", "Python"))
                .andExpect(model().attribute("outline", outline))
                .andExpect(model().attributeExists("syllabus", "sectionForm", "itemForm",
                        "syllabusForm", "resourceForm"));
    }

    @Test
    @DisplayName("carga secciones e ítems con el actor de la sesión")
    void createsSectionsAndItems() throws Exception {
        UUID sectionId = UUID.randomUUID();
        when(adminContent.createSection(eq(moduleId), any(AdminCourseContentService.SectionForm.class),
                eq(actor))).thenReturn(sectionId);
        when(adminContent.createItem(any(AdminCourseContentService.ItemForm.class), eq(actor)))
                .thenReturn(UUID.randomUUID());

        mvc.perform(post("/admin/cursos/{id}/contenido/secciones", courseId)
                        .param("moduleId", moduleId.toString())
                        .param("title", "Lecturas")
                        .param("position", "0")
                        .param("visible", "true"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido#modulo-" + moduleId))
                .andExpect(flash().attribute("panelOk", "La sección se creó correctamente."));

        mvc.perform(post("/admin/cursos/{id}/contenido/items", courseId)
                        .param("moduleId", moduleId.toString())
                        .param("sectionId", sectionId.toString())
                        .param("type", "VIDEO")
                        .param("title", "Clase 1")
                        .param("description", "Introducción")
                        .param("youtubeUrl", "https://youtu.be/dQw4w9WgXcQ")
                        .param("durationSeconds", "600")
                        .param("position", "0")
                        .param("visible", "true"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido#modulo-" + moduleId))
                .andExpect(flash().attribute("panelOk", "El ítem se creó correctamente."));

        ArgumentCaptor<AdminCourseContentService.SectionForm> section =
                ArgumentCaptor.forClass(AdminCourseContentService.SectionForm.class);
        ArgumentCaptor<AdminCourseContentService.ItemForm> item =
                ArgumentCaptor.forClass(AdminCourseContentService.ItemForm.class);
        verify(adminContent).createSection(eq(moduleId), section.capture(), eq(actor));
        verify(adminContent).createItem(item.capture(), eq(actor));
        assertThat(section.getValue().title()).isEqualTo("Lecturas");
        assertThat(section.getValue().visible()).isTrue();
        assertThat(item.getValue().type()).isEqualTo("VIDEO");
        assertThat(item.getValue().youtubeUrl()).isEqualTo("https://youtu.be/dQw4w9WgXcQ");
        assertThat(item.getValue().durationSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("guarda el sílabo sin perder recursos y reordena o borra ítems")
    void savesSyllabusAndReorders() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(content.syllabus(courseId)).thenReturn(new CourseContentService.Syllabus("resumen",
                "Objetivos viejos", "Requisitos viejos", List.of(
                        new CourseContentService.ResourceView(resourceId, "Guía",
                                "https://example.org/guia"))));

        mvc.perform(post("/admin/cursos/{id}/contenido/syllabus", courseId)
                        .param("objectives", "Objetivos nuevos")
                        .param("requirements", "Requisitos nuevos"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido#syllabus"));

        ArgumentCaptor<AdminCourseContentService.SyllabusForm> form =
                ArgumentCaptor.forClass(AdminCourseContentService.SyllabusForm.class);
        verify(adminContent).saveSyllabus(eq(courseId), form.capture(), eq(actor));
        assertThat(form.getValue().objectives()).isEqualTo("Objetivos nuevos");
        assertThat(form.getValue().resources()).extracting(
                AdminCourseContentService.ResourceForm::id).containsExactly(resourceId);

        mvc.perform(post("/admin/cursos/{id}/contenido/items/{itemId}/mover", courseId, itemId)
                        .param("up", "true"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido"));
        mvc.perform(post("/admin/cursos/{id}/contenido/secciones/{sectionId}/mover", courseId, sectionId)
                        .param("up", "false"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido"));
        mvc.perform(post("/admin/cursos/{id}/contenido/items/{itemId}/eliminar", courseId, itemId))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido"));

        verify(adminContent).moveItem(itemId, true, actor);
        verify(adminContent).moveSection(sectionId, false, actor);
        verify(adminContent).deleteItem(itemId, actor);
    }

    @Test
    @DisplayName("agrega y quita recursos complementarios del sílabo")
    void managesResources() throws Exception {
        UUID resourceId = UUID.randomUUID();
        when(adminContent.createResource(eq(courseId), any(AdminCourseContentService.ResourceForm.class),
                eq(actor))).thenReturn(resourceId);

        mvc.perform(post("/admin/cursos/{id}/contenido/recursos", courseId)
                        .param("title", "Guía")
                        .param("url", "https://example.org/guia")
                        .param("position", "0"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido#syllabus"));
        mvc.perform(post("/admin/cursos/{id}/contenido/recursos/{resourceId}/eliminar",
                        courseId, resourceId))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/contenido#syllabus"));

        ArgumentCaptor<AdminCourseContentService.ResourceForm> resource =
                ArgumentCaptor.forClass(AdminCourseContentService.ResourceForm.class);
        verify(adminContent).createResource(eq(courseId), resource.capture(), eq(actor));
        verify(adminContent).deleteResource(resourceId, actor);
        assertThat(resource.getValue().url()).isEqualTo("https://example.org/guia");
    }
}
