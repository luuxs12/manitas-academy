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
        assertThat(html).doesNotContain("panel-eyebrow");
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

    @Test
    @DisplayName("los paneles tienen una jerarquía visible y controles alcanzables")
    void panelHierarchyAndTargets() throws IOException {
        String css = read("static/css/styles.css");

        assertThat(css).contains(".panel-page-head h1 {\n    margin: .2rem 0 .4rem;\n    font-size: 2.1rem;");
        assertThat(css).contains(".course-syllabus .panel-section-head h2 {\n    font-size: 1.5rem;\n}");
        assertThat(css).contains(".syllabus-group-title {\n    margin: 0;\n    color: var(--ink, #000);\n    font-size: 1.35rem;");
        assertThat(css).contains(".syllabus-section-title {\n    margin: 0;\n    color: var(--muted, #666);\n    font-size: 1.0625rem;");

        assertThat(css).contains(".admin-page-head h1 { margin: 0; font-size: 2.1rem; }");
        assertThat(css).contains(".admin-card > h2 { font-size: 1.5rem; }");
        assertThat(css).contains(".admin-module-head h3 { display: grid; gap: .1rem; margin: 0; font-size: 1.2rem; }");
        assertThat(css).contains(".admin-section-head h4 { margin: 0; font-size: 1rem; }");
        assertThat(css).contains("min-height: 2rem;");
    }

    @Test
    @DisplayName("los paneles resisten singular, nombres largos y vídeos extensos")
    void worstCaseDataIsReadable() throws IOException {
        String student = read("templates/student/curso.html");
        String admin = read("templates/admin/contenido.html");
        String css = read("static/css/styles.css");

        assertThat(student).contains("(${itemCount} == 1 ? 'ítem terminado' : 'ítems terminados')");
        assertThat(admin).contains("(${outline.totalItems()} == 1 ? ' ítem' : ' ítems')");
        assertThat(admin).contains("(${outline.doneItems()} == 1 ? ' visto' : ' vistos')");

        assertThat(student).contains("th:text=\"${item.durationLabel()}\"");
        assertThat(admin).contains("th:text=\"${item.durationLabel()}\"");
        assertThat(student).doesNotContain("item.durationSeconds() + ' s'");
        assertThat(admin).doesNotContain("item.durationSeconds() + ' s'");

        // Un ítem suelto también debe resaltarse cuando es la clase abierta.
        assertThat(student.split(java.util.regex.Pattern.quote(
                "selectedId != null and item.sourceId() != null"), -1)).hasSize(3);

        assertThat(student).contains("th:attr=\"title=${item.description()}\"");
        assertThat(student).contains("<span aria-hidden=\"true\" th:style=");
        assertThat(student).contains("(abre en pestaña nueva)");
        assertThat(admin).contains("en pestaña nueva");

        assertThat(css).contains(".syllabus-title {\n    font-weight: 700;\n    overflow-wrap: anywhere;\n}");
        assertThat(css).contains(".admin-content-copy strong { font-family: var(--nav); font-size: .84rem; overflow-wrap: anywhere; }");
        assertThat(css).contains("-webkit-line-clamp: 3;");
        assertThat(css).doesNotContain("transition: width .4s var(--ease);");
    }

    @Test
    @DisplayName("el aviso del reproductor solo acepta mensajes del propio iframe")
    void videoHintChecksMessageSource() throws IOException {
        String js = read("static/js/panel.js");

        assertThat(js).contains("event.source !== frame.contentWindow");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
