package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminCursoEstructuraController;
import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CursoContenidoService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCursoContenidoService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
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
class AdminCursoEstructuraFlujoTest {

    private AdminCatalogService catalog;
    private AdminCursoContenidoService adminContenido;
    private CursoContenidoService content;
    private MockMvc mvc;
    private UUID actor;
    private UUID cursoId;
    private UUID moduloId;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        cursoId = UUID.randomUUID();
        moduloId = UUID.randomUUID();
        Usuario admin = mock(Usuario.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRol()).thenReturn("ADMIN");
        when(admin.getEmail()).thenReturn("admin@prueba.test");
        when(admin.getContrasenaHash()).thenReturn("hash");
        when(admin.getNombreCompleto()).thenReturn("Administradora");
        AutenticadoUsuario principal = new AutenticadoUsuario(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        catalog = mock(AdminCatalogService.class);
        adminContenido = mock(AdminCursoContenidoService.class);
        content = mock(CursoContenidoService.class);
        AdminNotificacionService notifications = mock(AdminNotificacionService.class);
        when(notifications.reciente(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);

        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminCursoEstructuraController(notifications, catalog, adminContenido, content))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("muestra módulos, secciones, ítems y sílabo leídos de PostgreSQL")
    void rendersEstructura() throws Exception {
        UUID seccionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CursoContenidoService.ItemVista item = new CursoContenidoService.ItemVista(itemId, "VIDEO",
                "Clase 1", "Introducción", null, "dQw4w9WgXcQ", 600, true, false, false);
        CursoContenidoService.Esquema esquema = new CursoContenidoService.Esquema(
                List.of(new CursoContenidoService.ModuloVista(moduloId, "Unidad 1", true,
                        List.of(new CursoContenidoService.SeccionVista(seccionId, "Lecturas", true,
                                List.of(item))), List.of(), 0)),
                1, 0, 0, itemId);
        when(catalog.cursoFormulario(cursoId)).thenReturn(new AdminCursoFormulario("python", "Python",
                "Inicial", true, "Programación", "Resumen real", "", 0));
        when(content.esquema(cursoId, null, null, true)).thenReturn(esquema);
        when(content.silabo(cursoId)).thenReturn(new CursoContenidoService.Silabo(
                "Resumen real", "Objetivos reales", "Requisitos reales",
                List.of(new CursoContenidoService.RecursoVista(UUID.randomUUID(), "Guía",
                        "https://example.org/guia"))));

        mvc.perform(get("/admin/cursos/{id}/contenido", cursoId))
                .andExpect(view().name("admin/contenido"))
                .andExpect(model().attribute("cursoTitulo", "Python"))
                .andExpect(model().attribute("esquema", esquema))
                .andExpect(model().attributeExists("silabo", "seccionFormulario", "itemFormulario",
                        "silaboFormulario", "recursoFormulario"));
    }

    @Test
    @DisplayName("carga secciones e ítems con el actor de la sesión")
    void createsSectionsAndItems() throws Exception {
        UUID seccionId = UUID.randomUUID();
        when(adminContenido.createSeccion(eq(moduloId), any(AdminCursoContenidoService.SeccionFormulario.class),
                eq(actor))).thenReturn(seccionId);
        when(adminContenido.createItem(any(AdminCursoContenidoService.ItemFormulario.class), eq(actor)))
                .thenReturn(UUID.randomUUID());

        mvc.perform(post("/admin/cursos/{id}/contenido/secciones", cursoId)
                        .param("moduloId", moduloId.toString())
                        .param("titulo", "Lecturas")
                        .param("posicion", "0")
                        .param("visible", "true"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido#modulo-" + moduloId))
                .andExpect(flash().attribute("panelOk", "La sección se creó correctamente."));

        mvc.perform(post("/admin/cursos/{id}/contenido/items", cursoId)
                        .param("moduloId", moduloId.toString())
                        .param("seccionId", seccionId.toString())
                        .param("type", "VIDEO")
                        .param("titulo", "Clase 1")
                        .param("descripcion", "Introducción")
                        .param("youtubeUrl", "https://youtu.be/dQw4w9WgXcQ")
                        .param("duracionSegundos", "600")
                        .param("posicion", "0")
                        .param("visible", "true"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido#modulo-" + moduloId))
                .andExpect(flash().attribute("panelOk", "El ítem se creó correctamente."));

        ArgumentCaptor<AdminCursoContenidoService.SeccionFormulario> seccion =
                ArgumentCaptor.forClass(AdminCursoContenidoService.SeccionFormulario.class);
        ArgumentCaptor<AdminCursoContenidoService.ItemFormulario> item =
                ArgumentCaptor.forClass(AdminCursoContenidoService.ItemFormulario.class);
        verify(adminContenido).createSeccion(eq(moduloId), seccion.capture(), eq(actor));
        verify(adminContenido).createItem(item.capture(), eq(actor));
        assertThat(seccion.getValue().titulo()).isEqualTo("Lecturas");
        assertThat(seccion.getValue().visible()).isTrue();
        assertThat(item.getValue().type()).isEqualTo("VIDEO");
        assertThat(item.getValue().youtubeUrl()).isEqualTo("https://youtu.be/dQw4w9WgXcQ");
        assertThat(item.getValue().duracionSegundos()).isEqualTo(600);
    }

    @Test
    @DisplayName("guarda el sílabo sin perder recursos y reordena o borra ítems")
    void savesSilaboAndReorders() throws Exception {
        UUID recursoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID seccionId = UUID.randomUUID();
        when(content.silabo(cursoId)).thenReturn(new CursoContenidoService.Silabo("resumen",
                "Objetivos viejos", "Requisitos viejos", List.of(
                        new CursoContenidoService.RecursoVista(recursoId, "Guía",
                                "https://example.org/guia"))));

        mvc.perform(post("/admin/cursos/{id}/contenido/syllabus", cursoId)
                        .param("objectives", "Objetivos nuevos")
                        .param("requirements", "Requisitos nuevos"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido#syllabus"));

        ArgumentCaptor<AdminCursoContenidoService.SilaboFormulario> formulario =
                ArgumentCaptor.forClass(AdminCursoContenidoService.SilaboFormulario.class);
        verify(adminContenido).saveSilabo(eq(cursoId), formulario.capture(), eq(actor));
        assertThat(formulario.getValue().objectives()).isEqualTo("Objetivos nuevos");
        assertThat(formulario.getValue().resources()).extracting(
                AdminCursoContenidoService.RecursoFormulario::id).containsExactly(recursoId);

        mvc.perform(post("/admin/cursos/{id}/contenido/items/{itemId}/mover", cursoId, itemId)
                        .param("up", "true"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido"));
        mvc.perform(post("/admin/cursos/{id}/contenido/secciones/{sectionId}/mover", cursoId, seccionId)
                        .param("up", "false"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido"));
        mvc.perform(post("/admin/cursos/{id}/contenido/items/{itemId}/eliminar", cursoId, itemId))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido"));

        verify(adminContenido).moveItem(itemId, true, actor);
        verify(adminContenido).moveSeccion(seccionId, false, actor);
        verify(adminContenido).deleteItem(itemId, actor);
    }

    @Test
    @DisplayName("agrega y quita recursos complementarios del sílabo")
    void managesResources() throws Exception {
        UUID recursoId = UUID.randomUUID();
        when(adminContenido.createRecurso(eq(cursoId), any(AdminCursoContenidoService.RecursoFormulario.class),
                eq(actor))).thenReturn(recursoId);

        mvc.perform(post("/admin/cursos/{id}/contenido/recursos", cursoId)
                        .param("titulo", "Guía")
                        .param("url", "https://example.org/guia")
                        .param("posicion", "0"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido#syllabus"));
        mvc.perform(post("/admin/cursos/{id}/contenido/recursos/{resourceId}/eliminar",
                        cursoId, recursoId))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/contenido#syllabus"));

        ArgumentCaptor<AdminCursoContenidoService.RecursoFormulario> recurso =
                ArgumentCaptor.forClass(AdminCursoContenidoService.RecursoFormulario.class);
        verify(adminContenido).createRecurso(eq(cursoId), recurso.capture(), eq(actor));
        verify(adminContenido).deleteRecurso(recursoId, actor);
        assertThat(recurso.getValue().url()).isEqualTo("https://example.org/guia");
    }
}
