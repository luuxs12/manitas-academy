package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Accesibilidad de las vistas revisadas")
class AccessibilityViewTest {

    @Test
    @DisplayName("los avisos viven en el modal compartido y declaran cada cambio de estado")
    void reviewedViewsDeclareLiveRegions() throws IOException {
        String dialogs = read("templates/fragments/panel-dialogs.html");
        assertThat(dialogs).as("aviso correcto en el modal")
                .containsPattern("role=\"status\"\\s+aria-live=\"polite\"");
        assertThat(dialogs).as("aviso de error en el modal")
                .containsPattern("role=\"alert\"\\s+aria-live=\"assertive\"");

        List<String> views = List.of(
                "templates/student/curso.html",
                "templates/student/perfil.html",
                "templates/admin/contenido.html",
                "templates/admin/cursos.html");

        for (String view : views) {
            assertThat(read(view)).as("sin avisos inline en %s", view)
                    .doesNotContain("panel-flash--ok")
                    .doesNotContain("panel-flash--error");
        }
    }

    @Test
    @DisplayName("el fin del video se anuncia desde una región viva que ya existe")
    void videoEndHintIsALiveRegionBeforeItAppears() throws IOException {
        String course = read("templates/student/curso.html");
        String css = read("static/css/styles.css");
        String js = read("static/js/panel.js");

        assertThat(course)
                .contains("<p class=\"player-hint\" data-video-end-hint aria-live=\"polite\"></p>")
                .doesNotContain("hidden role=\"status\"");
        assertThat(css).contains(".player-hint:empty { display: none; }");
        assertThat(js).contains("hint.textContent = ")
                .doesNotContain("hint.hidden = false");
    }

    @Test
    @DisplayName("el perfil enlaza cada pista y cada error con su campo")
    void profileFieldsDescribeTheirHintsAndErrors() throws IOException {
        String profile = read("templates/student/perfil.html");

        assertThat(profile).contains("aria-describedby=\"hint-avatar\"")
                .contains("id=\"hint-avatar\"");

        for (String field : List.of("fullName", "email", "currentPassword", "confirmPassword")) {
            assertThat(profile).as("aria-invalid en %s", field)
                    .contains("aria-invalid=${#fields.hasErrors('" + field + "')} ? 'true' : null");
            assertThat(profile).as("descripción en %s", field)
                    .contains("aria-describedby=${#fields.hasErrors('" + field + "')} ? 'error-" + field + "' : null");
            assertThat(profile).as("error de %s", field)
                    .contains("id=\"error-" + field + "\"");
        }

        assertThat(profile)
                .contains("aria-invalid=${#fields.hasErrors('newPassword')} ? 'true' : null")
                .contains("aria-describedby=${#fields.hasErrors('newPassword')} ? "
                        + "'hint-newPassword error-newPassword' : 'hint-newPassword'")
                .contains("id=\"hint-newPassword\"")
                .contains("id=\"error-newPassword\"");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
