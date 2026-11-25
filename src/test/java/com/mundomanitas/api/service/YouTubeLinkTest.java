package com.mundomanitas.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enlaces de YouTube")
class YouTubeLinkTest {

    @Test
    @DisplayName("extrae el ID de un enlace watch")
    void readsWatchUrl() {
        assertThat(YouTubeLink.id("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("extrae el ID de un enlace corto con parámetros")
    void readsShortUrl() {
        assertThat(YouTubeLink.id("https://youtu.be/dQw4w9WgXcQ?t=30"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("extrae el ID de un enlace embebido")
    void readsEmbedUrl() {
        assertThat(YouTubeLink.id("https://www.youtube.com/embed/dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("acepta el ID directo")
    void readsRawId() {
        assertThat(YouTubeLink.id("  dQw4w9WgXcQ  ")).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("rechaza enlaces de otros sitios e IDs inválidos")
    void rejectsInvalidLinks() {
        assertThat(YouTubeLink.id("https://ejemplo.com/watch?v=dQw4w9WgXcQ")).isNull();
        assertThat(YouTubeLink.id("https://www.youtube.com/watch?v=corto")).isNull();
        assertThat(YouTubeLink.id("   ")).isNull();
    }

    @Test
    @DisplayName("arma el embed sin cookies con un ID validado")
    void buildsPrivateEmbed() {
        assertThat(YouTubeLink.embedUrl("dQw4w9WgXcQ"))
                .isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?rel=0&enablejsapi=1");
    }
}
