package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Confirmación de borrados con modal")
class AdminDeleteConfirmationTest {

    private static final List<String> TEMPLATES = List.of(
            "templates/admin/cursos.html",
            "templates/admin/modulos.html",
            "templates/admin/videos.html",
            "templates/admin/ejercicios.html",
            "templates/admin/contenido.html",
            "templates/admin/certificados.html",
            "templates/fragments/admin-content-item.html");

    @Test
    @DisplayName("toda acción destructiva abre el modal en vez de navegar a una página")
    void destructiveActionsOpenTheModal() throws IOException {
        for (String path : TEMPLATES) {
            String html = read(path);
            assertThat(html).as(path).contains("data-confirm-url")
                    .contains("admin-confirm-link")
                    .contains("aria-label")
                    .doesNotContainPattern("(?s)admin-confirm-link[^>]*th:href");
        }
    }

    @Test
    @DisplayName("la página de confirmación sigue siendo el respaldo sin JS y conserva datos y CSRF")
    void confirmationFallbackKeepsRealDatosAndCsrf() throws IOException {
        String html = read("templates/admin/confirmar.html");

        assertThat(html).contains("id=\"confirm-panel\"")
                .contains("admin-confirm-details")
                .contains("th:action=\"@{${confirmAction}}\"")
                .contains("method=\"post\"")
                .contains("confirmMessage")
                .contains("confirmDetails")
                .contains("cancelUrl")
                .contains("data-dialog-close");
    }

    @Test
    @DisplayName("el modal se abre con los datos reales, cierra con Escape y devuelve el foco")
    void modalScriptIsAccessible() throws IOException {
        String js = read("static/js/panel-dialogs.js");
        assertThat(js).contains("data-confirm-url")
                .contains("fetch(")
                .contains("DOMParser")
                .contains("showModal()")
                .contains(".close()")
                .contains("addEventListener(\"close\"")
                .contains(".focus()")
                .contains("preventDefault()");
    }

    @Test
    @DisplayName("el modal es pequeño, usa la paleta y respeta movimiento reducido")
    void modalStylesAreSmallAndAccessible() throws IOException {
        String css = read("static/css/styles.css");
        assertThat(css).contains(".ui-dialog {")
                .contains(".ui-dialog::backdrop")
                .contains(".ui-dialog-card")
                .contains(".ui-check")
                .contains("stroke-dashoffset")
                .contains("@media (prefers-reduced-motion: reduce)")
                .contains("animation: none");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
