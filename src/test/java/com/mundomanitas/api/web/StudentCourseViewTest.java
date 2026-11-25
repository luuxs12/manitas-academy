package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Vista del curso del estudiante")
class StudentCourseViewTest {

    private static final List<String> TEMPLATES = List.of(
            "templates/student/panel.html",
            "templates/student/cursos.html",
            "templates/student/curso.html",
            "templates/student/explorar.html",
            "templates/student/certificados.html",
            "templates/student/certificado.html");

    @Test
    @DisplayName("el temario sale de módulos, secciones e ítems reales de PostgreSQL")
    void syllabusUsesDatabaseContent() throws IOException {
        String html = read("templates/student/curso.html");

        assertThat(html).contains("th:each=\"module, i : ${modules}\"");
        assertThat(html).contains("th:text=\"'Módulo ' + ${i.count}\"");
        assertThat(html).contains("th:each=\"section : ${module.sections()}\"");
        assertThat(html).contains("th:each=\"item : ${section.items()}\"");
        assertThat(html).contains("th:each=\"item : ${module.items()}\"");
        assertThat(html).contains("th:text=\"${item.type()}\"");
        assertThat(html).contains("th:text=\"${item.title()}\"");
        assertThat(html).contains("video=${item.sourceId()}");
        assertThat(html).contains("/items/{itemId}/visto");
        assertThat(html).contains("Sin sección");
        assertThat(html).contains("Sin fecha límite");
        assertThat(html).contains("th:title=\"${player.title()}\"");
    }

    @Test
    @DisplayName("el avance del listado usa el conteo vigente de cada matrícula")
    void listProgressUsesCurrentCounts() throws IOException {
        String html = read("templates/student/cursos.html");

        assertThat(html).contains("doneCounts[enrollment.id]");
        assertThat(html).doesNotContain("doneVideos[enrollment.id]");
    }

    @Test
    @DisplayName("no quedan cursos ni cifras de ejemplo en las plantillas del alumno")
    void noFakeCourseData() throws IOException {
        for (String path : TEMPLATES) {
            String html = read(path);
            assertThat(html).as(path).doesNotContain("Python desde cero");
            assertThat(html).as(path).doesNotContain("32 lecciones");
            assertThat(html).as(path).doesNotContain("20 lecciones");
        }
    }

    @Test
    @DisplayName("los estilos de módulos y ejercicios están documentados")
    void componentsAreStyledAndDocumented() throws IOException {
        String css = read("static/css/styles.css");
        String docs = Files.readString(Path.of("docs", "design-system.md"), StandardCharsets.UTF_8);

        assertThat(css).contains(".syllabus-group {");
        assertThat(css).contains(".syllabus-section {");
        assertThat(css).contains(".syllabus-item-actions {");
        assertThat(docs).contains("`.syllabus-section`");
        assertThat(docs).contains("`.syllabus-item-actions`");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
