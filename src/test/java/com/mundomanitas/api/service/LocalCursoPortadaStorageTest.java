package com.mundomanitas.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Portadas locales de cursos")
class LocalCursoPortadaStorageTest {

    @TempDir Path directory;

    private CursoPortadaStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalCursoPortadaStorage(directory.toString(), 2 * 1024 * 1024);
    }

    @Test
    @DisplayName("guarda una portada válida y devuelve una ruta local")
    void storesPortada() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "cover", "curso.png", "image/png", new byte[]{1, 2, 3});

        String url = storage.store(file);

        assertThat(url).startsWith("/uploads/covers/").endsWith(".png");
        assertThat(Files.exists(directory.resolve(url.substring("/uploads/covers/".length()))))
                .isTrue();
    }

    @Test
    @DisplayName("rechaza un tipo que no es imagen")
    void rejectsInvalidoTipo() {
        MockMultipartFile file = new MockMultipartFile(
                "cover", "curso.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JPG");
    }

    @Test
    @DisplayName("rechaza una imagen demasiado grande")
    void rejectsBigFile() {
        MockMultipartFile file = new MockMultipartFile(
                "cover", "curso.jpg", "image/jpeg", new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("2 MB");
    }
}
