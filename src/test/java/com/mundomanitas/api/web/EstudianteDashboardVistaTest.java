package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Panel del estudiante")
class EstudianteDashboardVistaTest {

    @Test
    @DisplayName("las tarjetas y el próximo curso llevan a la ficha real")
    void cardsEnlaceToConcreteCurso() throws IOException {
        String panel = read("templates/student/panel.html");

        assertThat(panel)
                .contains("th:href=\"@{/panel/cursos/{slug}(slug=${inscripcion.curso.slug})}\"")
                .contains("th:href=\"@{/panel/cursos/{slug}(slug=${next.curso.slug})}\"")
                .contains("Continuar curso")
                .doesNotContain("<a class=\"panel-curso-cta\" th:href=\"@{/panel/cursos}\">")
                .doesNotContain("Ver mis cursos <span aria-hidden=\"true\">→</span>");
    }

    @Test
    @DisplayName("cada tarjeta muestra el avance real de videos")
    void cardsUseRealProgreso() throws IOException {
        String panel = read("templates/student/panel.html");

        assertThat(panel)
                .contains("doneCounts[inscripcion.id]")
                .contains("videoCounts[inscripcion.id]")
                .contains("videoProgreso[inscripcion.id]")
                .contains("class=\"panel-lesson-track\" role=\"img\"")
                .contains("th:attr=\"aria-label=${doneCounts[inscripcion.id]");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
