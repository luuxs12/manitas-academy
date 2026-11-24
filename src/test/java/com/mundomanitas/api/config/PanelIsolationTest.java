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
    void adminTemplatesDoNotLinkToStudentArea() throws IOException {
        Resource[] templates = resolver.getResources("classpath*:templates/admin/**/*.html");

        assertThat(templates).isNotEmpty();
        for (Resource template : templates) {
            assertThat(html(template)).as(template.getFilename())
                    .doesNotContain("@{/panel");
        }
    }

    @Test
    @DisplayName("el área del estudiante no enlaza al panel administrativo")
    void studentTemplatesDoNotLinkToAdminArea() throws IOException {
        Resource[] templates = resolver.getResources("classpath*:templates/student/**/*.html");

        assertThat(templates).isNotEmpty();
        for (Resource template : templates) {
            assertThat(html(template)).as(template.getFilename())
                    .doesNotContain("@{/admin");
        }
    }

    private static String html(Resource template) throws IOException {
        try (var stream = template.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
