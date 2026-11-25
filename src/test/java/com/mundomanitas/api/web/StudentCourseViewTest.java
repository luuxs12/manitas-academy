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
        String view = html + readSyllabusItem();

        assertThat(html).contains("th:each=\"module, i : ${modules}\"");
        assertThat(html).contains("th:text=\"'Módulo ' + ${i.count}\"");
        assertThat(html).contains("th:each=\"section : ${module.sections()}\"");
        assertThat(html).contains("th:each=\"item : ${section.items()}\"");
        assertThat(html).contains("th:each=\"item : ${module.items()}\"");
        assertThat(view).contains("th:text=\"${item.type()}\"");
        assertThat(view).contains("th:text=\"${item.title()}\"");
        assertThat(view).contains("video=${item.sourceId()}");
        assertThat(view).contains("/items/{itemId}/visto");
        assertThat(html).contains("Sin sección");
        assertThat(view).contains("Sin fecha límite");
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
        String item = readSyllabusItem();
        String admin = read("templates/admin/contenido.html");
        String adminRow = read(Path.of("src", "main", "resources", "templates", "fragments",
                "admin-content-item.html"));
        String css = read("static/css/styles.css");

        assertThat(student).contains("(${itemCount} == 1 ? 'ítem terminado' : 'ítems terminados')");
        assertThat(admin).contains("(${outline.totalItems()} == 1 ? ' ítem' : ' ítems')");
        assertThat(admin).contains("(${outline.doneItems()} == 1 ? ' visto' : ' vistos')");

        assertThat(item).contains("th:text=\"${item.durationLabel()}\"");
        assertThat(adminRow).contains("th:text=\"${item.durationLabel()}\"");
        assertThat(item).doesNotContain("item.durationSeconds() + ' s'");
        assertThat(adminRow).doesNotContain("item.durationSeconds() + ' s'");

        // Un ítem suelto también debe resaltarse cuando es la clase abierta.
        // La fila vive en un fragmento compartido por los dos caminos.
        assertThat(student.split(java.util.regex.Pattern.quote(
                "~{fragments/syllabus-item :: row(${item})}"), -1)).hasSize(3);

        assertThat(student).contains("<span aria-hidden=\"true\" th:style=");
        assertThat(item).contains("(abre en pestaña nueva)");
        assertThat(adminRow).contains("en pestaña nueva");

        assertThat(css).contains(".syllabus-title {\n    font-weight: 700;\n    overflow-wrap: anywhere;\n}");
        assertThat(css).contains(".admin-content-copy strong { font-family: var(--nav); font-size: .84rem; overflow-wrap: anywhere; }");
        assertThat(css).contains("-webkit-line-clamp: 2;")
                .contains(".syllabus-description {\n    display: -webkit-box;\n    overflow: hidden;")
                .contains("overflow-wrap: anywhere;");
        assertThat(css).doesNotContain("transition: width .4s var(--ease);");
    }

    @Test
    @DisplayName("el curso separa el escenario del temario y ordena cada ítem en una fila propia")
    void stageAndItemsHaveADeliberateOrder() throws IOException {
        String student = read("templates/student/curso.html");
        Path fragmentPath = Path.of("src", "main", "resources", "templates", "fragments",
                "syllabus-item.html");
        assertThat(fragmentPath).exists();
        String item = Files.readString(fragmentPath, StandardCharsets.UTF_8);
        String css = read("static/css/styles.css");

        assertThat(student).contains("class=\"course-stage\"")
                .contains("~{fragments/syllabus-item :: row(${item})}")
                .doesNotContain("class=\"syllabus-item\"")
                .doesNotContain("syllabus-item-copy");

        assertThat(indexOf(item, "class=\"syllabus-item-top\""))
                .isLessThan(indexOf(item, "<h5 class=\"syllabus-title\""));
        assertThat(indexOf(item, "<h5 class=\"syllabus-title\""))
                .isLessThan(indexOf(item, "<p class=\"syllabus-description\""));
        assertThat(indexOf(item, "<p class=\"syllabus-description\""))
                .isLessThan(indexOf(item, "<p class=\"syllabus-meta\""));
        assertThat(indexOf(item, "<p class=\"syllabus-meta\""))
                .isLessThan(indexOf(item, "class=\"syllabus-item-actions\""));
        assertThat(item).contains("class=\"btn btn-dark syllabus-action\"")
                .contains("class=\"syllabus-mark-form\"")
                .contains("th:attr=\"title=${item.description()}\"");

        assertThat(css).contains(".course-stage {")
                .contains(".syllabus-item-top {")
                .contains(".syllabus-action {")
                .contains(".syllabus-mark-form {")
                .contains("position: sticky");
    }

    @Test
    @DisplayName("la foto de perfil ordena vista previa, carga y acciones en dos grupos")
    void profilePhotoGroupsItsActions() throws IOException {
        String profile = read("templates/student/perfil.html");
        String css = read("static/css/styles.css");

        assertThat(indexOf(profile, "panel-avatar-current"))
                .isLessThan(indexOf(profile, "panel-avatar-editor"));
        assertThat(indexOf(profile, "Elegir una imagen"))
                .isLessThan(indexOf(profile, "panel-avatar-actions"));
        assertThat(indexOf(profile, "Subir foto")).isLessThan(indexOf(profile, "Quitar foto"));
        assertThat(profile).contains("th:formaction=\"@{/panel/perfil/foto/eliminar}\"")
                .doesNotContain("th:action=\"@{/panel/perfil/foto/eliminar}\"");

        assertThat(css).contains(".panel-avatar-current {")
                .contains(".panel-avatar-editor {")
                .contains(".panel-avatar-actions {");
    }

    private static int indexOf(String text, String needle) {
        int position = text.indexOf(needle);
        assertThat(position).as("fragmento %s", needle).isNotNegative();
        return position;
    }

    @Test
    @DisplayName("el aviso del reproductor solo acepta mensajes del propio iframe")
    void videoHintChecksMessageSource() throws IOException {
        String js = read("static/js/panel.js");

        assertThat(js).contains("event.source !== frame.contentWindow");
    }

    private String readSyllabusItem() throws IOException {
        Path path = Path.of("src", "main", "resources", "templates", "fragments",
                "syllabus-item.html");
        assertThat(path).exists();
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String read(Path path) throws IOException {
        assertThat(path).exists();
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
