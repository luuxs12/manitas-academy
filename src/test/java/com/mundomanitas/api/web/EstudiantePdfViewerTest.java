package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Contratos del visor de PDFs y de la vista lado a lado del alumno. */
@DisplayName("Visor de PDFs del estudiante")
class EstudiantePdfViewerTest {

    @Test
    @DisplayName("el escenario muestra el PDF junto al temario y conserva la descarga")
    void viewerSitsBesideSilabo() throws IOException {
        String curso = read("templates/student/curso.html");

        assertThat(curso)
                .as("el visor comparte el escenario con el reproductor")
                .contains("class=\"course-stage\"")
                .contains("class=\"course-viewer\"")
                .contains("data-pdf-frame")
                .contains("data-pdf-url")
                .contains("data-pdf-status")
                .contains("role=\"status\" aria-live=\"polite\"");
        assertThat(curso.indexOf("class=\"course-viewer\""))
                .as("el visor va antes del temario")
                .isLessThan(curso.indexOf("class=\"course-syllabus\""));
        assertThat(curso).contains("Descargar documento");
    }

    @Test
    @DisplayName("el temario abre el PDF y marca la fila activa")
    void silaboOpensPdfAndMarksActivoFila() throws IOException {
        String row = read("templates/fragments/syllabus-item.html");

        assertThat(row)
                .contains("Ver material")
                .contains("/materiales/{itemId}/ver")
                .contains("item.id() == selectedItemId")
                .contains("Descargar documento");
    }

    @Test
    @DisplayName("el PDF se carga por fetch en un blob, sin enmarcar la respuesta protegida")
    void pdfIsFetchedIntoABlob() throws IOException {
        String head = read("templates/fragments/panel-head.html");
        String js = read("static/js/panel.js");

        assertThat(head).as("el panel carga el script del visor")
                .contains("js/panel.js");
        assertThat(js)
                .contains("data-pdf-frame")
                .contains("fetch(")
                .contains("createObjectURL")
                .contains("data-pdf-status")
                .doesNotContain("href = ");
    }

    @Test
    @DisplayName("el visor usa tokens existentes y queda documentado")
    void viewerUsesExistingTokens() throws IOException {
        String css = read("static/css/styles.css");
        String docs = leidoDocs();

        assertThat(css)
                .contains(".course-viewer {")
                .contains(".viewer-frame {")
                .contains(".viewer-status:empty { display: none; }");
        assertThat(docs)
                .contains("`.course-viewer`")
                .contains("`.viewer-frame`")
                .contains("`.syllabus-action`");
    }

    private String leidoDocs() throws IOException {
        Path path = Path.of("docs", "design-system.md");
        assertThat(path).exists();
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
