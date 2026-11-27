package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("El temario no ofrece la descarga de PDF de la sesión")
class AdminSesionPdfVistaTest {

    @Test
    @DisplayName("el encabezado del módulo queda sin enlace innecesario")
    void moduloHeadDoesNotOfferSesionPdf() throws IOException {
        String content = read("src/main/resources/templates/admin/contenido.html");
        String css = read("src/main/resources/static/css/styles.css");
        String design = read("docs/design-system.md");
        String guide = read("docs/guia-administrador-contenido.md");

        assertThat(content).doesNotContain("admin-session-download")
                .doesNotContain("@{/admin/exportar/sesion/modulos/{m}/pdf(m=${modulo.id()})}")
                .doesNotContain("Descargar PDF de la sesión");
        assertThat(css).doesNotContain(".admin-session-download");
        assertThat(design).doesNotContain("`.admin-session-download`");
        assertThat(guide).doesNotContain("Descargar PDF de la sesión")
                .doesNotContain("/admin/exportar/sesion/modulos/{moduleId}/pdf");
    }

    private String read(String path) throws IOException {
        Path file = Path.of(path);
        assertThat(file).exists();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
