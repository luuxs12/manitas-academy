package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminAuditoriaController;
import com.mundomanitas.api.controller.admin.AdminCertificadoController;
import com.mundomanitas.api.controller.admin.AdminContenidoController;
import com.mundomanitas.api.controller.admin.AdminController;
import com.mundomanitas.api.controller.admin.AdminCursoController;
import com.mundomanitas.api.controller.admin.AdminExportacionController;
import com.mundomanitas.api.controller.admin.AdminNotificacionApiController;
import com.mundomanitas.api.controller.admin.AdminReporteController;
import com.mundomanitas.api.controller.admin.AdminSearchController;
import com.mundomanitas.api.controller.admin.AdminUsuarioController;
import com.mundomanitas.api.dto.admin.GraficoActividad;
import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminUsuarioFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.dto.admin.FilaCertificado;
import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.dto.admin.GraficoIngresos;
import com.mundomanitas.api.dto.admin.ResumenReporte;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CursoPortadaStorage;
import com.mundomanitas.api.service.CorreoService;
import com.mundomanitas.api.service.PagoService;
import com.mundomanitas.api.service.admin.AdminAuditoriaService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCertificadoVistapreviaService;
import com.mundomanitas.api.service.admin.AdminCertificadoService;
import com.mundomanitas.api.service.admin.AdminDashboardService;
import com.mundomanitas.api.service.admin.AdminExportacionService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("Flujos completos del panel administrativo")
class AdminPanelFlujoTest {

    private AdminCatalogService catalog;
    private CursoPortadaStorage covers;
    private AdminNotificacionService notifications;
    private AdminDashboardService dashboard;
    private CorreoService correo;
    private PagoService payments;
    private AdminCertificadoService certificates;
    private AdminCertificadoVistapreviaService vistaprevia;
    private AdminAuditoriaService auditoria;
    private AdminExportacionService exporter;
    private MockMvc mvc;
    private UUID actor;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        Usuario admin = mock(Usuario.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRol()).thenReturn("ADMIN");
        when(admin.getEmail()).thenReturn("admin@prueba.test");
        when(admin.getContrasenaHash()).thenReturn("hash");
        when(admin.getNombreCompleto()).thenReturn("Administradora");
        AutenticadoUsuario principal = new AutenticadoUsuario(admin);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        catalog = mock(AdminCatalogService.class);
        covers = mock(CursoPortadaStorage.class);
        notifications = mock(AdminNotificacionService.class);
        dashboard = mock(AdminDashboardService.class);
        correo = mock(CorreoService.class);
        payments = mock(PagoService.class);
        certificates = mock(AdminCertificadoService.class);
        vistaprevia = mock(AdminCertificadoVistapreviaService.class);
        auditoria = mock(AdminAuditoriaService.class);
        exporter = mock(AdminExportacionService.class);

        when(notifications.reciente(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);
        when(catalog.cursoOptions()).thenReturn(List.of());
        when(catalog.moduloOptions()).thenReturn(List.of());
        when(catalog.usuarioNames()).thenReturn(Map.of(actor, "Administradora"));

        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminController(notifications, dashboard, correo, payments),
                        new AdminCursoController(notifications, catalog, covers),
                        new AdminContenidoController(notifications, catalog),
                        new AdminUsuarioController(notifications, catalog),
                        new AdminCertificadoController(notifications, certificates, catalog, vistaprevia),
                        new AdminAuditoriaController(notifications, auditoria),
                        new AdminReporteController(notifications, dashboard),
                        new AdminExportacionController(exporter),
                        new AdminSearchController(notifications, catalog),
                        new AdminNotificacionApiController(notifications))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("crear un curso guarda la ficha real y abre su edición")
    void createCurso() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(catalog.createCurso(any(AdminCursoFormulario.class), eq(actor))).thenReturn(cursoId);

        mvc.perform(post("/admin/cursos")
                        .param("slug", "taller-de-lsp")
                        .param("titulo", "Taller de LSP")
                        .param("nivel", "Nivel inicial")
                        .param("free", "true")
                        .param("categoria", "Comunicación")
                        .param("resumen", "Prácticas de Lengua de Señas Peruana.")
                        .param("imagen", "")
                        .param("posicion", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El curso se creó correctamente."));

        ArgumentCaptor<AdminCursoFormulario> formulario = ArgumentCaptor.forClass(AdminCursoFormulario.class);
        verify(catalog).createCurso(formulario.capture(), eq(actor));
        assertThat(formulario.getValue().titulo()).isEqualTo("Taller de LSP");
        assertThat(formulario.getValue().slug()).isEqualTo("taller-de-lsp");
        assertThat(formulario.getValue().free()).isTrue();
        assertThat(formulario.getValue().posicion()).isEqualTo(1);
    }

    @Test
    @DisplayName("editar, publicar y despublicar un curso usa el actor de la sesión")
    void publicarAndUnpublish() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(catalog.cursoFormulario(cursoId)).thenReturn(cursoFormulario());
        when(catalog.cursoEstado(cursoId)).thenReturn("BORRADOR");
        when(catalog.publicarLista(cursoId))
                .thenReturn(new AdminCatalogService.PublicarLista(List.of(), true));

        mvc.perform(get("/admin/cursos/{id}/editar", cursoId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/curso-form"))
                .andExpect(model().attribute("cursoId", cursoId))
                .andExpect(model().attribute("cursoEstado", "BORRADOR"))
                .andExpect(model().attributeExists("adminCursoFormulario", "lista"));

        mvc.perform(post("/admin/cursos/{id}/publicar", cursoId))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El curso ya está publicado."));

        mvc.perform(post("/admin/cursos/{id}/despublicar", cursoId))
                .andExpect(flash().attribute("panelOk", "El curso pasó a borrador."));

        verify(catalog).setPublished(cursoId, true, actor);
        verify(catalog).setPublished(cursoId, false, actor);
    }

    @Test
    @DisplayName("la portada se guarda en disco y se limpia la anterior")
    void cargaPortada() throws Exception {
        UUID cursoId = UUID.randomUUID();
        String stored = "/uploads/covers/" + UUID.randomUUID() + ".jpg";
        String previous = "/uploads/covers/" + UUID.randomUUID() + ".jpg";
        MockMultipartFile file = new MockMultipartFile(
                "portada", "portada.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        when(covers.store(any())).thenReturn(stored);
        when(catalog.updateCursoPortada(cursoId, stored, actor)).thenReturn(previous);

        mvc.perform(multipart("/admin/cursos/{id}/portada", cursoId).file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "/admin/cursos/" + cursoId + "/editar#portada"))
                .andExpect(flash().attribute("panelOk", "La portada se guardó en el servidor."));

        verify(covers).delete(previous);
    }

    @Test
    @DisplayName("borrar un curso muestra el impacto real y pide confirmación en una página")
    void deleteCurso() throws Exception {
        UUID cursoId = UUID.randomUUID();
        when(catalog.cursoFormulario(cursoId)).thenReturn(cursoFormulario());
        when(catalog.eliminacionImpacto(cursoId))
                .thenReturn(new AdminCatalogService.EliminacionImpacto(2, 6, 3, 4L, 1L));

        mvc.perform(get("/admin/cursos/{id}/eliminar", cursoId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/confirmar"))
                .andExpect(model().attribute("confirmAction",
                        "/admin/cursos/" + cursoId + "/eliminar"))
                .andExpect(model().attribute("confirmDetails",
                        List.of("Curso: Taller de LSP", "Módulos: 2", "Videos: 6",
                                "Ejercicios: 3", "Inscripciones: 4", "Certificados: 1")));

        mvc.perform(post("/admin/cursos/{id}/eliminar", cursoId))
                .andExpect(redirectedUrl("/admin/cursos"))
                .andExpect(flash().attribute("panelOk", "El curso se eliminó."));

        verify(catalog).deleteCurso(cursoId, actor);
    }

    @Test
    @DisplayName("crear módulos, clases de YouTube y ejercicios con datos reales")
    void createContenido() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID ejercicioId = UUID.randomUUID();
        String youtube = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        when(catalog.createModulo(any(AdminModuloFormulario.class), eq(actor))).thenReturn(moduloId);
        when(catalog.createVideo(any(AdminVideoFormulario.class), eq(actor))).thenReturn(videoId);
        when(catalog.createEjercicio(any(AdminEjercicioFormulario.class), eq(actor))).thenReturn(ejercicioId);

        mvc.perform(post("/admin/modulos")
                        .param("cursoId", cursoId.toString())
                        .param("titulo", "Unidad 1")
                        .param("posicion", "0"))
                .andExpect(redirectedUrl("/admin/modulos/" + moduloId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El módulo se creó correctamente."));

        mvc.perform(post("/admin/videos")
                        .param("moduloId", moduloId.toString())
                        .param("titulo", "Clase 1")
                        .param("posicion", "0")
                        .param("youtubeUrl", youtube))
                .andExpect(redirectedUrl("/admin/videos/" + videoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El video se creó correctamente."));

        mvc.perform(post("/admin/ejercicios")
                        .param("moduloId", moduloId.toString())
                        .param("titulo", "Práctica 1")
                        .param("prompt", "Contá las señas de la clase.")
                        .param("posicion", "0"))
                .andExpect(redirectedUrl("/admin/ejercicios/" + ejercicioId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El ejercicio se creó correctamente."));

        ArgumentCaptor<AdminModuloFormulario> modulo = ArgumentCaptor.forClass(AdminModuloFormulario.class);
        ArgumentCaptor<AdminVideoFormulario> video = ArgumentCaptor.forClass(AdminVideoFormulario.class);
        ArgumentCaptor<AdminEjercicioFormulario> ejercicio = ArgumentCaptor.forClass(AdminEjercicioFormulario.class);
        verify(catalog).createModulo(modulo.capture(), eq(actor));
        verify(catalog).createVideo(video.capture(), eq(actor));
        verify(catalog).createEjercicio(ejercicio.capture(), eq(actor));
        assertThat(modulo.getValue().cursoId()).isEqualTo(cursoId);
        assertThat(video.getValue().youtubeUrl()).isEqualTo(youtube);
        assertThat(ejercicio.getValue().prompt()).isEqualTo("Contá las señas de la clase.");
    }

    @Test
    @DisplayName("reordenar y borrar módulos, videos y ejercicios pasa por el actor")
    void moveAndDeleteContenido() throws Exception {
        UUID moduloId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID ejercicioId = UUID.randomUUID();
        when(catalog.moduloFormulario(moduloId))
                .thenReturn(new AdminModuloFormulario(UUID.randomUUID(), "Unidad 1", 0));
        when(catalog.videoFormulario(videoId))
                .thenReturn(new AdminVideoFormulario(moduloId, "Clase 1", 0,
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        when(catalog.ejercicioFormulario(ejercicioId))
                .thenReturn(new AdminEjercicioFormulario(moduloId, "Práctica 1", "Consigna", 0));

        mvc.perform(post("/admin/modulos/{id}/mover", moduloId).param("up", "true"))
                .andExpect(redirectedUrl("/admin/modulos"));
        mvc.perform(post("/admin/videos/{id}/mover", videoId).param("up", "false"))
                .andExpect(redirectedUrl("/admin/videos"));

        mvc.perform(get("/admin/modulos/{id}/eliminar", moduloId))
                .andExpect(view().name("admin/confirmar"))
                .andExpect(model().attribute("confirmDetails", List.of("Módulo: Unidad 1")));
        mvc.perform(get("/admin/videos/{id}/eliminar", videoId))
                .andExpect(view().name("admin/confirmar"));
        mvc.perform(get("/admin/ejercicios/{id}/eliminar", ejercicioId))
                .andExpect(view().name("admin/confirmar"));

        mvc.perform(post("/admin/modulos/{id}/eliminar", moduloId))
                .andExpect(redirectedUrl("/admin/modulos"));
        mvc.perform(post("/admin/videos/{id}/eliminar", videoId))
                .andExpect(redirectedUrl("/admin/videos"));
        mvc.perform(post("/admin/ejercicios/{id}/eliminar", ejercicioId))
                .andExpect(redirectedUrl("/admin/ejercicios"));

        verify(catalog).moveModulo(moduloId, true, actor);
        verify(catalog).moveVideo(videoId, false, actor);
        verify(catalog).deleteModulo(moduloId, actor);
        verify(catalog).deleteVideo(videoId, actor);
        verify(catalog).deleteEjercicio(ejercicioId, actor);
    }

    @Test
    @DisplayName("editar cuentas y emitir o revocar certificados")
    void usersAndCertificates() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID certificadoId = UUID.randomUUID();
        when(certificates.elegible()).thenReturn(List.of());
        when(certificates.certificates(anyBoolean(), any(), any(), nullable(String.class), any()))
                .thenReturn(new PageImpl<>(List.of(new FilaCertificado(
                        UUID.randomUUID(), "MM-TAL-12345678", "Persona Real", "Taller de LSP",
                        "24/11/2026", true))));
        when(certificates.issue(any(), eq(actor))).thenReturn("MM-TAL-12345678");

        mvc.perform(get("/admin/usuarios"))
                .andExpect(view().name("admin/usuarios"))
                .andExpect(model().attribute("roles", List.of("STUDENT", "ADMIN")));
        mvc.perform(post("/admin/usuarios/{id}", usuarioId)
                        .param("nombreCompleto", "Persona Real")
                        .param("email", "persona@correo.test")
                        .param("rol", "STUDENT")
                        .param("activo", "true")
                        .param("contrasena", ""))
                .andExpect(redirectedUrl("/admin/usuarios/" + usuarioId + "/editar"));

        mvc.perform(get("/admin/certificados"))
                .andExpect(view().name("admin/certificados"))
                .andExpect(model().attributeExists("page", "elegible", "cursoOptions", "usuarioOptions"));
        mvc.perform(post("/admin/certificados/emitir")
                        .param("usuarioId", usuarioId.toString())
                        .param("cursoId", cursoId.toString()))
                .andExpect(redirectedUrl("/admin/certificados"))
                .andExpect(flash().attribute("panelOk", "Certificado MM-TAL-12345678 emitido."));
        mvc.perform(get("/admin/certificados/{id}/revocar", certificadoId).param("code", "MM-TAL-12345678"))
                .andExpect(view().name("admin/confirmar"))
                .andExpect(model().attribute("confirmDetails", List.of("Código: MM-TAL-12345678")));
        mvc.perform(post("/admin/certificados/{id}/revocar", certificadoId))
                .andExpect(redirectedUrl("/admin/certificados"));

        verify(catalog).updateUsuario(eq(usuarioId), any(), eq(actor));
        verify(certificates).issue(any(), eq(actor));
        verify(certificates).revoke(certificadoId, actor);
    }

    @Test
    @DisplayName("panel, configuración, reportes, auditoría, búsqueda y avisos responden")
    void leidoOnlyRoutes() throws Exception {
        when(dashboard.days("30")).thenReturn(30);
        when(dashboard.days("90")).thenReturn(90);
        when(dashboard.kpis()).thenReturn(List.of());
        when(dashboard.actividad(anyInt())).thenReturn(graficoActividad(30));
        when(dashboard.popular()).thenReturn(List.of());
        when(dashboard.recienteUsers()).thenReturn(List.of());
        when(dashboard.sistema()).thenReturn(List.of());
        when(dashboard.reports()).thenReturn(new ResumenReporte(4L, 2L, 50, 5800L, 3L, 5L, 60));
        when(dashboard.ingresos(anyInt())).thenReturn(new GraficoIngresos(
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 24),
                List.of("26/10", "24/11"), List.of("0,40", "1,80"), 5800L, 3000L));
        when(correo.isEnabled()).thenReturn(true);
        when(payments.isEnabled()).thenReturn(false);
        when(catalog.users(any(), any(), any(), eq(actor)))
                .thenReturn(new PageImpl<>(List.of()));
        when(catalog.courses(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.modules(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.videos(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.exercises(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(auditoria.page(any())).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/admin"))
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("kpis", "grafico", "popular", "recienteUsers", "sistema"));
        mvc.perform(get("/admin/configuracion"))
                .andExpect(view().name("admin/configuracion"))
                .andExpect(model().attribute("correoEnabled", true))
                .andExpect(model().attribute("stripeEnabled", false));
        mvc.perform(get("/admin/reportes").param("dias", "90"))
                .andExpect(view().name("admin/reportes"))
                .andExpect(model().attribute("selectedDays", 90))
                .andExpect(model().attributeExists("reporte", "grafico", "ingresos"));
        mvc.perform(get("/admin/auditoria"))
                .andExpect(view().name("admin/auditoria"))
                .andExpect(model().attributeExists("page"));
        mvc.perform(get("/admin/buscar").param("q", "señas"))
                .andExpect(view().name("admin/buscar"))
                .andExpect(model().attributeExists("users", "courses", "modules", "videos", "exercises"));
        mvc.perform(get("/admin/api/notificaciones")
                        .param("desde", "2026-11-20T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos").isArray())
                .andExpect(jsonPath("$.sinLeer").value(0));
    }

    @Test
    @DisplayName("las exportaciones devuelven el archivo real con sus encabezados")
    void exports() throws Exception {
        byte[] csv = "Título;Identificador\n".getBytes(StandardCharsets.UTF_8);
        when(exporter.exportacion("cursos", "csv"))
                .thenReturn(new ResultadoExportacion(csv, "cursos.csv", "text/csv; charset=UTF-8"));

        mvc.perform(get("/admin/exportar/{module}", "cursos").param("formato", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cursos.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().bytes(csv));

        verify(exporter).exportacion("cursos", "csv");
    }

    @Test
    @DisplayName("todos los listados y formularios del panel cargan con datos del servicio")
    void listsAndForms() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID ejercicioId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(catalog.courses(nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.modules(any(), nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.videos(any(), nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.exercises(any(), nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.users(any(), nullable(String.class), any(), eq(actor)))
                .thenReturn(new PageImpl<>(List.of()));
        when(catalog.cursoFormulario(cursoId)).thenReturn(cursoFormulario());
        when(catalog.cursoEstado(cursoId)).thenReturn("BORRADOR");
        when(catalog.publicarLista(cursoId))
                .thenReturn(new AdminCatalogService.PublicarLista(List.of("portada"), false));
        when(catalog.moduloFormulario(moduloId)).thenReturn(new AdminModuloFormulario(cursoId, "Unidad 1", 0));
        when(catalog.videoFormulario(videoId)).thenReturn(new AdminVideoFormulario(moduloId, "Clase 1", 0,
                "https://youtu.be/dQw4w9WgXcQ"));
        when(catalog.ejercicioFormulario(ejercicioId))
                .thenReturn(new AdminEjercicioFormulario(moduloId, "Práctica 1", "Consigna", 0));
        when(catalog.usuarioFormulario(usuarioId)).thenReturn(new AdminUsuarioFormulario(
                "Persona Real", "persona@correo.test", "STUDENT", true, ""));

        mvc.perform(get("/admin/cursos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cursos"))
                .andExpect(model().attributeExists("page"));
        mvc.perform(get("/admin/cursos/nuevo"))
                .andExpect(view().name("admin/curso-form"))
                .andExpect(model().attributeExists("adminCursoFormulario"));
        mvc.perform(get("/admin/cursos/{id}/editar", cursoId))
                .andExpect(view().name("admin/curso-form"))
                .andExpect(model().attribute("lista",
                        new AdminCatalogService.PublicarLista(List.of("portada"), false)));

        mvc.perform(get("/admin/modulos"))
                .andExpect(view().name("admin/modulos"))
                .andExpect(model().attributeExists("page", "options"));
        mvc.perform(get("/admin/modulos/nuevo"))
                .andExpect(view().name("admin/modulo-form"))
                .andExpect(model().attributeExists("adminModuloFormulario"));
        mvc.perform(get("/admin/modulos/{id}/editar", moduloId))
                .andExpect(view().name("admin/modulo-form"))
                .andExpect(model().attribute("itemId", moduloId));

        mvc.perform(get("/admin/videos"))
                .andExpect(view().name("admin/videos"))
                .andExpect(model().attributeExists("page", "options"));
        mvc.perform(get("/admin/videos/nuevo"))
                .andExpect(view().name("admin/video-form"))
                .andExpect(model().attributeExists("adminVideoFormulario"));
        mvc.perform(get("/admin/videos/{id}/editar", videoId))
                .andExpect(view().name("admin/video-form"))
                .andExpect(model().attribute("itemId", videoId));

        mvc.perform(get("/admin/ejercicios"))
                .andExpect(view().name("admin/ejercicios"))
                .andExpect(model().attributeExists("page", "options"));
        mvc.perform(get("/admin/ejercicios/nuevo"))
                .andExpect(view().name("admin/ejercicio-form"))
                .andExpect(model().attributeExists("adminEjercicioFormulario"));
        mvc.perform(get("/admin/ejercicios/{id}/editar", ejercicioId))
                .andExpect(view().name("admin/ejercicio-form"))
                .andExpect(model().attribute("itemId", ejercicioId));

        mvc.perform(get("/admin/usuarios/{id}/editar", usuarioId))
                .andExpect(view().name("admin/usuario-form"))
                .andExpect(model().attribute("usuarioId", usuarioId));
    }

    @Test
    @DisplayName("guardar las fichas existentes usa el actor de la sesión")
    void updateFichas() throws Exception {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID ejercicioId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        mvc.perform(post("/admin/cursos/{id}", cursoId)
                        .param("slug", "taller-de-lsp")
                        .param("titulo", "Taller de LSP")
                        .param("nivel", "Nivel inicial")
                        .param("_free", "false")
                        .param("categoria", "Comunicación")
                        .param("resumen", "Prácticas de Lengua de Señas Peruana.")
                        .param("imagen", "")
                        .param("posicion", "1"))
                .andExpect(redirectedUrl("/admin/cursos/" + cursoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "La ficha del curso se guardó."));

        mvc.perform(post("/admin/modulos/{id}", moduloId)
                        .param("cursoId", cursoId.toString())
                        .param("titulo", "Unidad 1")
                        .param("posicion", "0"))
                .andExpect(redirectedUrl("/admin/modulos/" + moduloId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El módulo se guardó."));

        mvc.perform(post("/admin/videos/{id}", videoId)
                        .param("moduloId", moduloId.toString())
                        .param("titulo", "Clase 1")
                        .param("posicion", "0")
                        .param("youtubeUrl", "https://youtu.be/dQw4w9WgXcQ"))
                .andExpect(redirectedUrl("/admin/videos/" + videoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El video se guardó."));

        mvc.perform(post("/admin/ejercicios/{id}", ejercicioId)
                        .param("moduloId", moduloId.toString())
                        .param("titulo", "Práctica 1")
                        .param("prompt", "Contá las señas de la clase.")
                        .param("posicion", "0"))
                .andExpect(redirectedUrl("/admin/ejercicios/" + ejercicioId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El ejercicio se guardó."));

        mvc.perform(post("/admin/usuarios/{id}", usuarioId)
                        .param("nombreCompleto", "Persona Real")
                        .param("email", "persona@correo.test")
                        .param("rol", "STUDENT")
                        .param("_activo", "false")
                        .param("contrasena", ""))
                .andExpect(redirectedUrl("/admin/usuarios/" + usuarioId + "/editar"))
                .andExpect(flash().attribute("panelOk", "Los datos de la cuenta se guardaron."));

        ArgumentCaptor<AdminCursoFormulario> savedCurso = ArgumentCaptor.forClass(AdminCursoFormulario.class);
        verify(catalog).updateCurso(eq(cursoId), savedCurso.capture(), eq(actor));
        assertThat(savedCurso.getValue().free()).isFalse();
        verify(catalog).updateModulo(eq(moduloId), any(), eq(actor));
        verify(catalog).updateVideo(eq(videoId), any(), eq(actor));
        verify(catalog).updateEjercicio(eq(ejercicioId), any(), eq(actor));
        ArgumentCaptor<AdminUsuarioFormulario> savedUsuario = ArgumentCaptor.forClass(AdminUsuarioFormulario.class);
        verify(catalog).updateUsuario(eq(usuarioId), savedUsuario.capture(), eq(actor));
        assertThat(savedUsuario.getValue().activo()).isFalse();
    }

    @Test
    @DisplayName("el checkbox de curso gratuito envía false cuando queda desmarcado")
    void freeCheckboxSendsFalse() throws Exception {
        String html = Files.readString(Path.of("src", "main", "resources", "templates", "admin",
                "curso-form.html"), StandardCharsets.UTF_8);

        assertThat(html).contains("<input type=\"hidden\" name=\"_free\" value=\"false\">");
    }

    @Test
    @DisplayName("el checkbox de cuenta activa envía false cuando queda desmarcado")
    void activoCheckboxSendsFalse() throws Exception {
        String html = Files.readString(Path.of("src", "main", "resources", "templates", "admin",
                "usuario-form.html"), StandardCharsets.UTF_8);

        assertThat(html).contains("<input type=\"hidden\" name=\"_activo\" value=\"false\">");
    }

    @Test
    @DisplayName("CSRF queda activo en todo el sitio salvo el webhook de pagos")
    void csrfIsEnabled() throws Exception {
        String security = Files.readString(Path.of("src", "main", "java", "com", "mundomanitas",
                "api", "config", "SecurityConfig.java"), StandardCharsets.UTF_8);

        assertThat(security).contains(".csrf(csrf ->");
        assertThat(security).contains("ignoringRequestMatchers(\"/pagos/stripe/webhook\")");
        assertThat(security).doesNotContain("csrf(AbstractHttpConfigurer::disable)");
    }

    @Test
    @DisplayName("el admin abre la vista previa del certificado por su id real")
    void opensCertificadoVistaprevia() throws Exception {
        UUID id = UUID.randomUUID();
        AdminCertificadoVistapreviaService.Vistaprevia view = new AdminCertificadoVistapreviaService.Vistaprevia(
                id, "MM-RED-ABCD1234", OffsetDateTime.parse("2026-11-20T10:15:00Z"),
                "Ana Quispe", "Redes", 3, true);
        when(vistaprevia.vistaprevia(id)).thenReturn(Optional.of(view));

        mvc.perform(get("/admin/certificados/{id}/ver", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/certificado"))
                .andExpect(model().attribute("vistaprevia", view))
                .andExpect(model().attribute("activo", "certificados"))
                .andExpect(model().attribute("titulo", "Certificado"));

        verify(vistaprevia).vistaprevia(id);
    }

    @Test
    @DisplayName("un certificado que no existe no abre vista previa")
    void missingCertificadoVistapreviaIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(vistaprevia.vistaprevia(id)).thenReturn(Optional.empty());

        mvc.perform(get("/admin/certificados/{id}/ver", id))
                .andExpect(status().isNotFound());
    }

    private GraficoActividad graficoActividad(int days) {
        return new GraficoActividad(days, LocalDate.now().minusDays(days), LocalDate.now(),
                List.of("inicio", "fin"), List.of("0,40", "1,80"), List.of("0,40", "2,40"),
                3, 4L, 2L);
    }

    private AdminCursoFormulario cursoFormulario() {
        return new AdminCursoFormulario("taller-de-lsp", "Taller de LSP", "Nivel inicial",
                true, "Comunicación", "Prácticas de Lengua de Señas Peruana.", "", 1);
    }
}
