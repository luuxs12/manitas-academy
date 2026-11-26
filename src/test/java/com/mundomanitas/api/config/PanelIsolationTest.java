package com.mundomanitas.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Cada panel navega solo por su propia área. */
@DisplayName("Aislamiento de los paneles")
class PanelIsolationTest {

    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    @Test
    @DisplayName("el panel administrativo no enlaza al área del estudiante")
    void adminTemplatesDoNotEnlaceToEstudianteArea() throws IOException {
        Resource[] templates = resolver.getResources("classpath*:templates/admin/**/*.html");

        assertThat(templates).isNotEmpty();
        for (Resource plantilla : templates) {
            assertThat(html(plantilla)).as(plantilla.getFilename())
                    .doesNotContain("@{/panel");
        }
    }

    @Test
    @DisplayName("el área del estudiante no enlaza al panel administrativo")
    void estudianteTemplatesDoNotEnlaceToAdminArea() throws IOException {
        Resource[] templates = resolver.getResources("classpath*:templates/student/**/*.html");

        assertThat(templates).isNotEmpty();
        for (Resource plantilla : templates) {
            assertThat(html(plantilla)).as(plantilla.getFilename())
                    .doesNotContain("@{/admin");
        }
    }

    @Test
    @DisplayName("el acceso público elige el panel según el rol")
    void publicNavigationUsesTheRol() throws IOException {
        String header = html(resolver.getResource("classpath:templates/fragments/layout.html"));

        assertThat(header).contains("usuarioActual.admin()")
                .contains("panel-navigation.js")
                .doesNotContain("th:href=\"@{/panel}\" aria-label=\"Ir a mi panel\"");

        String authModal = html(resolver.getResource("classpath:static/js/auth-modal.js"));

        assertThat(authModal).doesNotContain("location.assign(")
                .contains("replace(response.url)");

        String history = html(resolver.getResource("classpath:static/js/panel-navigation.js"));

        assertThat(history).contains("back_forward")
                .contains("location.replace(");

        String detalle = html(resolver.getResource("classpath:templates/cursos/detalle.html"));

        assertThat(detalle).contains("!usuarioActual.admin()")
                .doesNotContain("th:if=\"${signedIn and inscripto}\"")
                .doesNotContain("th:if=\"${signedIn and !inscripto and canEnroll}\"")
                .doesNotContain("th:if=\"${signedIn and !inscripto and !canEnroll}\"");
    }

    private static String html(Resource plantilla) throws IOException {
        try (var stream = plantilla.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
