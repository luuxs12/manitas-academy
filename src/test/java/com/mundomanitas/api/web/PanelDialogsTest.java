package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Los avisos del panel salen del documento y viven en un modal pequeño. */
@DisplayName("Modales del panel")
class PanelDialogsTest {

    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    @Test
    @DisplayName("el fragmento compartido trae confirmación, éxito con check y error")
    void sharedFragmentHasTheThreeDialogs() throws IOException {
        String html = html(resolver.getResource("classpath:templates/fragments/panel-dialogs.html"));

        assertThat(html).contains("th:fragment=\"confirm\"")
                .contains("id=\"confirm-dialog\"")
                .contains("th:fragment=\"notices\"")
                .contains("id=\"success-dialog\"")
                .contains("id=\"error-dialog\"")
                .contains("class=\"ui-check\"")
                .contains("<circle")
                .contains("aria-labelledby=")
                .contains("aria-describedby=")
                .contains("data-dialog-close")
                .contains("th:text=\"${panelOk}\"")
                .contains("th:text=\"${panelError}\"");
    }

    @Test
    @DisplayName("los dos paneles incluyen los modales y el script en la cabecera")
    void bothPanelsIncludeTheDialogs() throws IOException {
        String adminNav = html(resolver.getResource(
                "classpath:templates/admin/fragments/admin-nav.html"));
        String estudianteNav = html(resolver.getResource(
                "classpath:templates/fragments/panel-nav.html"));
        String adminHead = html(resolver.getResource(
                "classpath:templates/admin/fragments/admin-head.html"));
        String estudianteHead = html(resolver.getResource(
                "classpath:templates/fragments/panel-head.html"));

        assertThat(adminNav).contains("~{fragments/panel-dialogs :: confirm}")
                .contains("~{fragments/panel-dialogs :: notices}");
        assertThat(estudianteNav).contains("~{fragments/panel-dialogs :: notices}");
        assertThat(adminHead).contains("/js/panel-dialogs.js");
        assertThat(estudianteHead).contains("/js/panel-dialogs.js");
    }

    @Test
    @DisplayName("ninguna vista vuelve a dibujar el aviso horrible dentro de la página")
    void noInlineFlashMessagesRemain() throws IOException {
        Resource[] templates = resolver.getResources("classpath*:templates/**/*.html");
        assertThat(templates).isNotEmpty();

        for (Resource plantilla : templates) {
            if (plantilla.getFilename().equals("panel-dialogs.html")) continue;
            assertThat(html(plantilla)).as(plantilla.getFilename())
                    .doesNotContain("th:if=\"${panelOk}\"")
                    .doesNotContain("th:if=\"${panelError}\"");
        }
    }

    @Test
    @DisplayName("crear un curso y emitir un certificado piden confirmación antes de enviar")
    void importantFormsAskForConfirmation() throws IOException {
        String cursoFormulario = html(resolver.getResource(
                "classpath:templates/admin/curso-form.html"));
        String certificates = html(resolver.getResource(
                "classpath:templates/admin/certificados.html"));

        assertThat(cursoFormulario).contains("data-confirm-form")
                .contains("data-confirm-message");
        assertThat(certificates).contains("data-confirm-form")
                .contains("data-confirm-message");
    }

    @Test
    @DisplayName("el alumno confirma al cerrar la cuenta, cancelar el plan y terminar un curso")
    void estudianteIrreversibleActionsAskForConfirmation() throws IOException {
        for (String path : List.of("templates/student/preferencias.html",
                "templates/student/facturacion.html",
                "templates/student/curso.html",
                "templates/student/cursos.html",
                "templates/student/explorar.html",
                "templates/student/panel.html")) {
            assertThat(html(resolver.getResource("classpath:" + path)))
                    .as(path).contains("data-confirm-form");
        }
    }

    @Test
    @DisplayName("el mismo modal confirma formularios sin perder los datos ya cargados")
    void modalScriptHandlesForms() throws IOException {
        String js = html(resolver.getResource("classpath:static/js/panel-dialogs.js"));

        assertThat(js).contains("data-confirm-form")
                .contains("data-confirm-message")
                .contains("addEventListener(\"submit\"")
                .contains("requestSubmit")
                .contains("const dialog = close.closest(\"dialog\")");
    }

    @Test
    @DisplayName("los controladores siguen usando el flash como transporte del mensaje")
    void controllersStillSendTheMessage() throws IOException {
        String admin = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "mundomanitas", "api", "controller", "admin",
                "AdminCursoController.java"));
        String estudiante = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "com", "mundomanitas", "api", "controller",
                "EstudianteController.java"));

        assertThat(admin).contains("addFlashAttribute(\"panelOk\"");
        assertThat(estudiante).contains("addFlashAttribute(\"panelOk\"");
    }

    private static String html(Resource recurso) throws IOException {
        try (var stream = recurso.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
