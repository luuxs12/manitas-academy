package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Descarga de la sesión en la vista del temario")
class AdminSessionPdfViewTest {

    @Test
    @DisplayName("cada módulo ofrece su PDF con un enlace accesible y documentado")
    void moduleHeadOffersSessionPdf() throws IOException {
        String content = read("src/main/resources/templates/admin/contenido.html");
        String css = read("src/main/resources/static/css/styles.css");
        String design = read("docs/design-system.md");
        String guide = read("docs/guia-administrador-contenido.md");

        assertThat(content).contains("class=\"btn btn-admin admin-session-download\"")
                .contains("@{/admin/exportar/sesion/modulos/{m}/pdf(m=${module.id()})}")
                .contains("aria-label='Descargar PDF de la sesión ' + ${module.title()}")
                .contains(">Descargar PDF de la sesión</a>");
        assertThat(css).contains(".admin-session-download {")
                .contains("align-self: flex-start;");
        assertThat(design).contains("`.admin-session-download`");
        assertThat(guide).contains("Descargar PDF de la sesión")
                .contains("/admin/exportar/sesion/modulos/{moduleId}/pdf");
    }

    private String read(String path) throws IOException {
        Path file = Path.of(path);
        assertThat(file).exists();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
