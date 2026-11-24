package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminAuditController;
import com.mundomanitas.api.controller.admin.AdminCertificateController;
import com.mundomanitas.api.controller.admin.AdminContentController;
import com.mundomanitas.api.controller.admin.AdminController;
import com.mundomanitas.api.controller.admin.AdminCourseController;
import com.mundomanitas.api.controller.admin.AdminExportController;
import com.mundomanitas.api.controller.admin.AdminNotificationApiController;
import com.mundomanitas.api.controller.admin.AdminReportController;
import com.mundomanitas.api.controller.admin.AdminSearchController;
import com.mundomanitas.api.controller.admin.AdminUserController;
import com.mundomanitas.api.dto.admin.ActivityChart;
import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminUserForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.dto.admin.CertificateRow;
import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.dto.admin.IncomeChart;
import com.mundomanitas.api.dto.admin.ReportSummary;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.CourseCoverStorage;
import com.mundomanitas.api.service.MailService;
import com.mundomanitas.api.service.PaymentService;
import com.mundomanitas.api.service.admin.AdminAuditService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCertificateService;
import com.mundomanitas.api.service.admin.AdminDashboardService;
import com.mundomanitas.api.service.admin.AdminExportService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
class AdminPanelFlowTest {

    private AdminCatalogService catalog;
    private CourseCoverStorage covers;
    private AdminNotificationService notifications;
    private AdminDashboardService dashboard;
    private MailService mail;
    private PaymentService payments;
    private AdminCertificateService certificates;
    private AdminAuditService audit;
    private AdminExportService exporter;
    private MockMvc mvc;
    private UUID actor;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRole()).thenReturn("ADMIN");
        when(admin.getEmail()).thenReturn("admin@prueba.test");
        when(admin.getPasswordHash()).thenReturn("hash");
        when(admin.getFullName()).thenReturn("Administradora");
        AuthenticatedUser principal = new AuthenticatedUser(admin);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        catalog = mock(AdminCatalogService.class);
        covers = mock(CourseCoverStorage.class);
        notifications = mock(AdminNotificationService.class);
        dashboard = mock(AdminDashboardService.class);
        mail = mock(MailService.class);
        payments = mock(PaymentService.class);
        certificates = mock(AdminCertificateService.class);
        audit = mock(AdminAuditService.class);
        exporter = mock(AdminExportService.class);

        when(notifications.recent(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);
        when(catalog.courseOptions()).thenReturn(List.of());
        when(catalog.moduleOptions()).thenReturn(List.of());
        when(catalog.userNames()).thenReturn(Map.of(actor, "Administradora"));

        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminController(notifications, dashboard, mail, payments),
                        new AdminCourseController(notifications, catalog, covers),
                        new AdminContentController(notifications, catalog),
                        new AdminUserController(notifications, catalog),
                        new AdminCertificateController(notifications, certificates, catalog),
                        new AdminAuditController(notifications, audit),
                        new AdminReportController(notifications, dashboard),
                        new AdminExportController(exporter),
                        new AdminSearchController(notifications, catalog),
                        new AdminNotificationApiController(notifications))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("crear un curso guarda la ficha real y abre su edición")
    void createCourse() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(catalog.createCourse(any(AdminCourseForm.class), eq(actor))).thenReturn(courseId);

        mvc.perform(post("/admin/cursos")
                        .param("slug", "taller-de-lsp")
                        .param("title", "Taller de LSP")
                        .param("level", "Nivel inicial")
                        .param("free", "true")
                        .param("category", "Comunicación")
                        .param("summary", "Prácticas de Lengua de Señas Peruana.")
                        .param("image", "")
                        .param("position", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El curso se creó correctamente."));

        ArgumentCaptor<AdminCourseForm> form = ArgumentCaptor.forClass(AdminCourseForm.class);
        verify(catalog).createCourse(form.capture(), eq(actor));
        assertThat(form.getValue().title()).isEqualTo("Taller de LSP");
        assertThat(form.getValue().slug()).isEqualTo("taller-de-lsp");
        assertThat(form.getValue().free()).isTrue();
        assertThat(form.getValue().position()).isEqualTo(1);
    }

    @Test
    @DisplayName("editar, publicar y despublicar un curso usa el actor de la sesión")
    void publishAndUnpublish() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(catalog.courseForm(courseId)).thenReturn(courseForm());
        when(catalog.courseStatus(courseId)).thenReturn("BORRADOR");
        when(catalog.publishChecklist(courseId))
                .thenReturn(new AdminCatalogService.PublishChecklist(List.of(), true));

        mvc.perform(get("/admin/cursos/{id}/editar", courseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/curso-form"))
                .andExpect(model().attribute("courseId", courseId))
                .andExpect(model().attribute("courseStatus", "BORRADOR"))
                .andExpect(model().attributeExists("adminCourseForm", "checklist"));

        mvc.perform(post("/admin/cursos/{id}/publicar", courseId))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El curso ya está publicado."));

        mvc.perform(post("/admin/cursos/{id}/despublicar", courseId))
                .andExpect(flash().attribute("panelOk", "El curso pasó a borrador."));

        verify(catalog).setPublished(courseId, true, actor);
        verify(catalog).setPublished(courseId, false, actor);
    }

    @Test
    @DisplayName("la portada se guarda en disco y se limpia la anterior")
    void uploadCover() throws Exception {
        UUID courseId = UUID.randomUUID();
        String stored = "/uploads/covers/" + UUID.randomUUID() + ".jpg";
        String previous = "/uploads/covers/" + UUID.randomUUID() + ".jpg";
        MockMultipartFile file = new MockMultipartFile(
                "portada", "portada.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        when(covers.store(any())).thenReturn(stored);
        when(catalog.updateCourseCover(courseId, stored, actor)).thenReturn(previous);

        mvc.perform(multipart("/admin/cursos/{id}/portada", courseId).file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "/admin/cursos/" + courseId + "/editar#portada"))
                .andExpect(flash().attribute("panelOk", "La portada se guardó en el servidor."));

        verify(covers).delete(previous);
    }

    @Test
    @DisplayName("borrar un curso muestra el impacto real y pide confirmación en una página")
    void deleteCourse() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(catalog.courseForm(courseId)).thenReturn(courseForm());
        when(catalog.deletionImpact(courseId))
                .thenReturn(new AdminCatalogService.DeletionImpact(2, 6, 3, 4L, 1L));

        mvc.perform(get("/admin/cursos/{id}/eliminar", courseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/confirmar"))
                .andExpect(model().attribute("confirmAction",
                        "/admin/cursos/" + courseId + "/eliminar"))
                .andExpect(model().attribute("confirmDetails",
                        List.of("Curso: Taller de LSP", "Módulos: 2", "Videos: 6",
                                "Ejercicios: 3", "Inscripciones: 4", "Certificados: 1")));

        mvc.perform(post("/admin/cursos/{id}/eliminar", courseId))
                .andExpect(redirectedUrl("/admin/cursos"))
                .andExpect(flash().attribute("panelOk", "El curso se eliminó."));

        verify(catalog).deleteCourse(courseId, actor);
    }

    @Test
    @DisplayName("crear módulos, clases de YouTube y ejercicios con datos reales")
    void createContent() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        String youtube = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        when(catalog.createModule(any(AdminModuleForm.class), eq(actor))).thenReturn(moduleId);
        when(catalog.createVideo(any(AdminVideoForm.class), eq(actor))).thenReturn(videoId);
        when(catalog.createExercise(any(AdminExerciseForm.class), eq(actor))).thenReturn(exerciseId);

        mvc.perform(post("/admin/modulos")
                        .param("courseId", courseId.toString())
                        .param("title", "Unidad 1")
                        .param("position", "0"))
                .andExpect(redirectedUrl("/admin/modulos/" + moduleId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El módulo se creó correctamente."));

        mvc.perform(post("/admin/videos")
                        .param("moduleId", moduleId.toString())
                        .param("title", "Clase 1")
                        .param("position", "0")
                        .param("youtubeUrl", youtube))
                .andExpect(redirectedUrl("/admin/videos/" + videoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El video se creó correctamente."));

        mvc.perform(post("/admin/ejercicios")
                        .param("moduleId", moduleId.toString())
                        .param("title", "Práctica 1")
                        .param("prompt", "Contá las señas de la clase.")
                        .param("position", "0"))
                .andExpect(redirectedUrl("/admin/ejercicios/" + exerciseId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El ejercicio se creó correctamente."));

        ArgumentCaptor<AdminModuleForm> module = ArgumentCaptor.forClass(AdminModuleForm.class);
        ArgumentCaptor<AdminVideoForm> video = ArgumentCaptor.forClass(AdminVideoForm.class);
        ArgumentCaptor<AdminExerciseForm> exercise = ArgumentCaptor.forClass(AdminExerciseForm.class);
        verify(catalog).createModule(module.capture(), eq(actor));
        verify(catalog).createVideo(video.capture(), eq(actor));
        verify(catalog).createExercise(exercise.capture(), eq(actor));
        assertThat(module.getValue().courseId()).isEqualTo(courseId);
        assertThat(video.getValue().youtubeUrl()).isEqualTo(youtube);
        assertThat(exercise.getValue().prompt()).isEqualTo("Contá las señas de la clase.");
    }

    @Test
    @DisplayName("reordenar y borrar módulos, videos y ejercicios pasa por el actor")
    void moveAndDeleteContent() throws Exception {
        UUID moduleId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        when(catalog.moduleForm(moduleId))
                .thenReturn(new AdminModuleForm(UUID.randomUUID(), "Unidad 1", 0));
        when(catalog.videoForm(videoId))
                .thenReturn(new AdminVideoForm(moduleId, "Clase 1", 0,
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        when(catalog.exerciseForm(exerciseId))
                .thenReturn(new AdminExerciseForm(moduleId, "Práctica 1", "Consigna", 0));

        mvc.perform(post("/admin/modulos/{id}/mover", moduleId).param("up", "true"))
                .andExpect(redirectedUrl("/admin/modulos"));
        mvc.perform(post("/admin/videos/{id}/mover", videoId).param("up", "false"))
                .andExpect(redirectedUrl("/admin/videos"));

        mvc.perform(get("/admin/modulos/{id}/eliminar", moduleId))
                .andExpect(view().name("admin/confirmar"))
                .andExpect(model().attribute("confirmDetails", List.of("Módulo: Unidad 1")));
        mvc.perform(get("/admin/videos/{id}/eliminar", videoId))
                .andExpect(view().name("admin/confirmar"));
        mvc.perform(get("/admin/ejercicios/{id}/eliminar", exerciseId))
                .andExpect(view().name("admin/confirmar"));

        mvc.perform(post("/admin/modulos/{id}/eliminar", moduleId))
                .andExpect(redirectedUrl("/admin/modulos"));
        mvc.perform(post("/admin/videos/{id}/eliminar", videoId))
                .andExpect(redirectedUrl("/admin/videos"));
        mvc.perform(post("/admin/ejercicios/{id}/eliminar", exerciseId))
                .andExpect(redirectedUrl("/admin/ejercicios"));

        verify(catalog).moveModule(moduleId, true, actor);
        verify(catalog).moveVideo(videoId, false, actor);
        verify(catalog).deleteModule(moduleId, actor);
        verify(catalog).deleteVideo(videoId, actor);
        verify(catalog).deleteExercise(exerciseId, actor);
    }

    @Test
    @DisplayName("editar cuentas y emitir o revocar certificados")
    void usersAndCertificates() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();
        when(certificates.eligible()).thenReturn(List.of());
        when(certificates.certificates(anyBoolean(), any(), any(), nullable(String.class), any()))
                .thenReturn(new PageImpl<>(List.of(new CertificateRow(
                        UUID.randomUUID(), "MM-TAL-12345678", "Persona Real", "Taller de LSP",
                        "24/11/2026", true))));
        when(certificates.issue(any(), eq(actor))).thenReturn("MM-TAL-12345678");

        mvc.perform(get("/admin/usuarios"))
                .andExpect(view().name("admin/usuarios"))
                .andExpect(model().attribute("roles", List.of("STUDENT", "ADMIN")));
        mvc.perform(post("/admin/usuarios/{id}", userId)
                        .param("fullName", "Persona Real")
                        .param("email", "persona@correo.test")
                        .param("role", "STUDENT")
                        .param("active", "true")
                        .param("password", ""))
                .andExpect(redirectedUrl("/admin/usuarios/" + userId + "/editar"));

        mvc.perform(get("/admin/certificados"))
                .andExpect(view().name("admin/certificados"))
                .andExpect(model().attributeExists("page", "eligible", "courseOptions", "userOptions"));
        mvc.perform(post("/admin/certificados/emitir")
                        .param("userId", userId.toString())
                        .param("courseId", courseId.toString()))
                .andExpect(redirectedUrl("/admin/certificados"))
                .andExpect(flash().attribute("panelOk", "Certificado MM-TAL-12345678 emitido."));
        mvc.perform(get("/admin/certificados/{id}/revocar", certificateId).param("code", "MM-TAL-12345678"))
                .andExpect(view().name("admin/confirmar"))
                .andExpect(model().attribute("confirmDetails", List.of("Código: MM-TAL-12345678")));
        mvc.perform(post("/admin/certificados/{id}/revocar", certificateId))
                .andExpect(redirectedUrl("/admin/certificados"));

        verify(catalog).updateUser(eq(userId), any(), eq(actor));
        verify(certificates).issue(any(), eq(actor));
        verify(certificates).revoke(certificateId, actor);
    }

    @Test
    @DisplayName("panel, configuración, reportes, auditoría, búsqueda y avisos responden")
    void readOnlyRoutes() throws Exception {
        when(dashboard.days("30")).thenReturn(30);
        when(dashboard.days("90")).thenReturn(90);
        when(dashboard.kpis()).thenReturn(List.of());
        when(dashboard.activity(anyInt())).thenReturn(activityChart(30));
        when(dashboard.popular()).thenReturn(List.of());
        when(dashboard.recentUsers()).thenReturn(List.of());
        when(dashboard.system()).thenReturn(List.of());
        when(dashboard.reports()).thenReturn(new ReportSummary(4L, 2L, 50, 5800L, 3L, 5L, 60));
        when(dashboard.income(anyInt())).thenReturn(new IncomeChart(
                LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 24),
                List.of("26/10", "24/11"), List.of("0,40", "1,80"), 5800L, 3000L));
        when(mail.isEnabled()).thenReturn(true);
        when(payments.isEnabled()).thenReturn(false);
        when(catalog.users(any(), any(), any(), eq(actor)))
                .thenReturn(new PageImpl<>(List.of()));
        when(catalog.courses(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.modules(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.videos(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.exercises(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(audit.page(any())).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/admin"))
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("kpis", "chart", "popular", "recentUsers", "system"));
        mvc.perform(get("/admin/configuracion"))
                .andExpect(view().name("admin/configuracion"))
                .andExpect(model().attribute("mailEnabled", true))
                .andExpect(model().attribute("stripeEnabled", false));
        mvc.perform(get("/admin/reportes").param("dias", "90"))
                .andExpect(view().name("admin/reportes"))
                .andExpect(model().attribute("selectedDays", 90))
                .andExpect(model().attributeExists("report", "chart", "income"));
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
        when(exporter.export("cursos", "csv"))
                .thenReturn(new ExportResult(csv, "cursos.csv", "text/csv; charset=UTF-8"));

        mvc.perform(get("/admin/exportar/{module}", "cursos").param("formato", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cursos.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().bytes(csv));

        verify(exporter).export("cursos", "csv");
    }

    @Test
    @DisplayName("todos los listados y formularios del panel cargan con datos del servicio")
    void listsAndForms() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(catalog.courses(nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.modules(any(), nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.videos(any(), nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.exercises(any(), nullable(String.class), any())).thenReturn(new PageImpl<>(List.of()));
        when(catalog.users(any(), nullable(String.class), any(), eq(actor)))
                .thenReturn(new PageImpl<>(List.of()));
        when(catalog.courseForm(courseId)).thenReturn(courseForm());
        when(catalog.courseStatus(courseId)).thenReturn("BORRADOR");
        when(catalog.publishChecklist(courseId))
                .thenReturn(new AdminCatalogService.PublishChecklist(List.of("portada"), false));
        when(catalog.moduleForm(moduleId)).thenReturn(new AdminModuleForm(courseId, "Unidad 1", 0));
        when(catalog.videoForm(videoId)).thenReturn(new AdminVideoForm(moduleId, "Clase 1", 0,
                "https://youtu.be/dQw4w9WgXcQ"));
        when(catalog.exerciseForm(exerciseId))
                .thenReturn(new AdminExerciseForm(moduleId, "Práctica 1", "Consigna", 0));
        when(catalog.userForm(userId)).thenReturn(new AdminUserForm(
                "Persona Real", "persona@correo.test", "STUDENT", true, ""));

        mvc.perform(get("/admin/cursos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cursos"))
                .andExpect(model().attributeExists("page"));
        mvc.perform(get("/admin/cursos/nuevo"))
                .andExpect(view().name("admin/curso-form"))
                .andExpect(model().attributeExists("adminCourseForm"));
        mvc.perform(get("/admin/cursos/{id}/editar", courseId))
                .andExpect(view().name("admin/curso-form"))
                .andExpect(model().attribute("checklist",
                        new AdminCatalogService.PublishChecklist(List.of("portada"), false)));

        mvc.perform(get("/admin/modulos"))
                .andExpect(view().name("admin/modulos"))
                .andExpect(model().attributeExists("page", "options"));
        mvc.perform(get("/admin/modulos/nuevo"))
                .andExpect(view().name("admin/modulo-form"))
                .andExpect(model().attributeExists("adminModuleForm"));
        mvc.perform(get("/admin/modulos/{id}/editar", moduleId))
                .andExpect(view().name("admin/modulo-form"))
                .andExpect(model().attribute("itemId", moduleId));

        mvc.perform(get("/admin/videos"))
                .andExpect(view().name("admin/videos"))
                .andExpect(model().attributeExists("page", "options"));
        mvc.perform(get("/admin/videos/nuevo"))
                .andExpect(view().name("admin/video-form"))
                .andExpect(model().attributeExists("adminVideoForm"));
        mvc.perform(get("/admin/videos/{id}/editar", videoId))
                .andExpect(view().name("admin/video-form"))
                .andExpect(model().attribute("itemId", videoId));

        mvc.perform(get("/admin/ejercicios"))
                .andExpect(view().name("admin/ejercicios"))
                .andExpect(model().attributeExists("page", "options"));
        mvc.perform(get("/admin/ejercicios/nuevo"))
                .andExpect(view().name("admin/ejercicio-form"))
                .andExpect(model().attributeExists("adminExerciseForm"));
        mvc.perform(get("/admin/ejercicios/{id}/editar", exerciseId))
                .andExpect(view().name("admin/ejercicio-form"))
                .andExpect(model().attribute("itemId", exerciseId));

        mvc.perform(get("/admin/usuarios/{id}/editar", userId))
                .andExpect(view().name("admin/usuario-form"))
                .andExpect(model().attribute("userId", userId));
    }

    @Test
    @DisplayName("guardar las fichas existentes usa el actor de la sesión")
    void updateFichas() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/admin/cursos/{id}", courseId)
                        .param("slug", "taller-de-lsp")
                        .param("title", "Taller de LSP")
                        .param("level", "Nivel inicial")
                        .param("_free", "false")
                        .param("category", "Comunicación")
                        .param("summary", "Prácticas de Lengua de Señas Peruana.")
                        .param("image", "")
                        .param("position", "1"))
                .andExpect(redirectedUrl("/admin/cursos/" + courseId + "/editar"))
                .andExpect(flash().attribute("panelOk", "La ficha del curso se guardó."));

        mvc.perform(post("/admin/modulos/{id}", moduleId)
                        .param("courseId", courseId.toString())
                        .param("title", "Unidad 1")
                        .param("position", "0"))
                .andExpect(redirectedUrl("/admin/modulos/" + moduleId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El módulo se guardó."));

        mvc.perform(post("/admin/videos/{id}", videoId)
                        .param("moduleId", moduleId.toString())
                        .param("title", "Clase 1")
                        .param("position", "0")
                        .param("youtubeUrl", "https://youtu.be/dQw4w9WgXcQ"))
                .andExpect(redirectedUrl("/admin/videos/" + videoId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El video se guardó."));

        mvc.perform(post("/admin/ejercicios/{id}", exerciseId)
                        .param("moduleId", moduleId.toString())
                        .param("title", "Práctica 1")
                        .param("prompt", "Contá las señas de la clase.")
                        .param("position", "0"))
                .andExpect(redirectedUrl("/admin/ejercicios/" + exerciseId + "/editar"))
                .andExpect(flash().attribute("panelOk", "El ejercicio se guardó."));

        mvc.perform(post("/admin/usuarios/{id}", userId)
                        .param("fullName", "Persona Real")
                        .param("email", "persona@correo.test")
                        .param("role", "STUDENT")
                        .param("_active", "false")
                        .param("password", ""))
                .andExpect(redirectedUrl("/admin/usuarios/" + userId + "/editar"))
                .andExpect(flash().attribute("panelOk", "Los datos de la cuenta se guardaron."));

        ArgumentCaptor<AdminCourseForm> savedCourse = ArgumentCaptor.forClass(AdminCourseForm.class);
        verify(catalog).updateCourse(eq(courseId), savedCourse.capture(), eq(actor));
        assertThat(savedCourse.getValue().free()).isFalse();
        verify(catalog).updateModule(eq(moduleId), any(), eq(actor));
        verify(catalog).updateVideo(eq(videoId), any(), eq(actor));
        verify(catalog).updateExercise(eq(exerciseId), any(), eq(actor));
        ArgumentCaptor<AdminUserForm> savedUser = ArgumentCaptor.forClass(AdminUserForm.class);
        verify(catalog).updateUser(eq(userId), savedUser.capture(), eq(actor));
        assertThat(savedUser.getValue().active()).isFalse();
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
    void activeCheckboxSendsFalse() throws Exception {
        String html = Files.readString(Path.of("src", "main", "resources", "templates", "admin",
                "usuario-form.html"), StandardCharsets.UTF_8);

        assertThat(html).contains("<input type=\"hidden\" name=\"_active\" value=\"false\">");
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

    private ActivityChart activityChart(int days) {
        return new ActivityChart(days, LocalDate.now().minusDays(days), LocalDate.now(),
                List.of("inicio", "fin"), List.of("0,40", "1,80"), List.of("0,40", "2,40"),
                3, 4L, 2L);
    }

    private AdminCourseForm courseForm() {
        return new AdminCourseForm("taller-de-lsp", "Taller de LSP", "Nivel inicial",
                true, "Comunicación", "Prácticas de Lengua de Señas Peruana.", "", 1);
    }
}
