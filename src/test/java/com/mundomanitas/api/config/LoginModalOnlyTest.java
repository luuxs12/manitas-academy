package com.mundomanitas.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** El acceso vive en el modal de la portada, nunca en una página propia. */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Acceso solamente con modal")
class LoginModalOnlyTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("la URL histórica /login vuelve a la portada con el modal")
    void loginUrlReturnsToHomeModal() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?login"));
    }

    @Test
    @DisplayName("un área protegida abre el modal sobre la portada")
    void protectedAreaOpensHomeModal() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?login"));
    }

    @Test
    @DisplayName("la portada con ?login renderiza el diálogo de acceso")
    void homeWithFlagRendersDialog() throws Exception {
        mvc.perform(get("/?login"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("id=\"auth-dialog\"")));
    }

    @Test
    @DisplayName("no queda plantilla ni enlace de la página de login")
    void noStandaloneLoginPage() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();

        assertThat(Path.of("src", "main", "resources", "templates", "auth", "login.html"))
                .doesNotExist();

        String layout = read(resolver.getResource("classpath:templates/fragments/layout.html"));
        assertThat(layout).doesNotContain("@{/login}").contains("'?login'");

        String modal = read(resolver.getResource("classpath:static/js/auth-modal.js"));
        assertThat(modal).contains("params.has(\"error\")")
                .contains("params.has(\"reset\")")
                .doesNotContain("url.pathname === \"/login\"");
    }

    private static String read(Resource resource) throws IOException {
        try (var stream = resource.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
