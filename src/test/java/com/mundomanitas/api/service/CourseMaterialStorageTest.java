package com.mundomanitas.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contratos del almacenamiento local de documentos del curso. */
@DisplayName("Almacenamiento de materiales descargables")
class CourseMaterialStorageTest {

    @TempDir
    Path directory;

    @Test
    @DisplayName("guarda un PDF con nombre seguro y permite leerlo y borrarlo")
    void storesReadsAndDeletesPdf() {
        CourseMaterialStorage storage = storage(1024);
        byte[] content = "contenido real".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "Guía de redes.pdf", "application/pdf", content);

        String url = storage.store(file);

        assertThat(url).matches("/uploads/materials/[0-9a-f-]+\\.pdf");
        assertThat(storage.read(url)).isEqualTo(content);
        assertThat(storage.contentType(url)).isEqualTo("application/pdf");

        storage.delete(url);
        assertThatThrownBy(() -> storage.read(url))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("rechaza formatos que no son documento y archivos demasiado grandes")
    void rejectsInvalidDocuments() {
        CourseMaterialStorage storage = storage(4);
        MockMultipartFile image = new MockMultipartFile(
                "file", "portada.png", "image/png", "png".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile large = new MockMultipartFile(
                "file", "guia.pdf", "application/pdf", "12345".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PDF");
        assertThatThrownBy(() -> storage.store(large))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tamaño");
    }

    @Test
    @DisplayName("no permite leer rutas que salgan de la carpeta de materiales")
    void rejectsPathsOutsideMaterials() {
        CourseMaterialStorage storage = storage(1024);

        assertThatThrownBy(() -> storage.read("/uploads/materials/../secreto.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> storage.read("https://example.org/guia.pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    private CourseMaterialStorage storage(long maxBytes) {
        return new LocalCourseMaterialStorage(directory.toString(), maxBytes);
    }
}
