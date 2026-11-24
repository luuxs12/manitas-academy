package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Confirmación accesible de borrados")
class AdminDeleteConfirmationTest {

    private static final List<String> TEMPLATES = List.of(
            "templates/admin/cursos.html",
            "templates/admin/modulos.html",
            "templates/admin/videos.html",
            "templates/admin/ejercicios.html",
            "templates/admin/certificados.html");

    @Test
    @DisplayName("los cinco borrados usan el diálogo compartido y no el confirm nativo")
    void destructiveActionsUseSharedDialog() throws IOException {
        for (String path : TEMPLATES) {
            String html = read(path);
            assertThat(html).as(path).doesNotContain("confirm(");
            assertThat(html).as(path).contains("data-confirm=");
            assertThat(html).as(path).contains("admin/fragments/confirm-dialog :: confirmDialog");
        }
    }

    @Test
    @DisplayName("el diálogo compartido nombra el aviso y separa cancelar de confirmar")
    void dialogIsAccessible() throws IOException {
        String html = read("templates/admin/fragments/confirm-dialog.html");

        assertThat(html).contains("<dialog");
        assertThat(html).contains("aria-labelledby=");
        assertThat(html).contains("aria-describedby=");
        assertThat(html).contains("data-confirm-message");
        assertThat(html).contains("data-confirm-cancel");
        assertThat(html).contains("data-confirm-accept");
    }

    @Test
    @DisplayName("el script enfoca cancelar, usa el diálogo modal y envía solo al confirmar")
    void scriptFocusesCancelAndSubmitsOnConfirm() throws IOException {
        String js = read("static/js/admin.js");

        assertThat(js).contains("showModal()");
        assertThat(js).contains("cancel.focus()");
        assertThat(js).contains("form.submit()");

        String css = read("static/css/styles.css");
        assertThat(css).contains("admin-confirm");
        assertThat(css).contains("panel-flash--ok::before");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
