package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Confirmación de borrados en páginas")
class AdminDeleteConfirmationTest {

    private static final List<String> TEMPLATES = List.of(
            "templates/admin/cursos.html",
            "templates/admin/modulos.html",
            "templates/admin/videos.html",
            "templates/admin/ejercicios.html",
            "templates/admin/certificados.html");

    @Test
    @DisplayName("los cinco borrados enlazan a una página, sin diálogos ni overlays")
    void destructiveActionsUsePages() throws IOException {
        for (String path : TEMPLATES) {
            String html = read(path);
            assertThat(html).as(path).doesNotContain("<dialog");
            assertThat(html).as(path).doesNotContain("data-confirm=");
            assertThat(html).as(path).doesNotContain("confirm-dialog");
            assertThat(html).as(path).contains("admin-confirm-link");
        }
    }

    @Test
    @DisplayName("la página compartida muestra datos reales y envía el POST con CSRF")
    void confirmationPageIsAccessible() throws IOException {
        String html = read("templates/admin/confirmar.html");

        assertThat(html).contains("<main");
        assertThat(html).contains("<h1");
        assertThat(html).contains("admin-confirm-page");
        assertThat(html).contains("th:action=\"@{${confirmAction}}\"");
        assertThat(html).contains("method=\"post\"");
        assertThat(html).contains("confirmMessage");
        assertThat(html).contains("confirmDetails");
        assertThat(html).contains("cancelUrl");
    }

    @Test
    @DisplayName("el script y el CSS ya no mantienen el diálogo modal")
    void modalConfirmationIsGone() throws IOException {
        String js = read("static/js/admin.js");
        assertThat(js).doesNotContain("showModal()");
        assertThat(js).doesNotContain("form.submit()");
        assertThat(js).doesNotContain("data-confirm");
        assertThat(js).doesNotContain("data-upload");

        String css = read("static/css/styles.css");
        assertThat(css).doesNotContain(".admin-confirm {");
        assertThat(css).contains(".admin-confirm-page");
        assertThat(css).contains("panel-flash--ok::before");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
