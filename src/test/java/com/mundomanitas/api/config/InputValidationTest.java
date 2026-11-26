package com.mundomanitas.api.config;

import com.mundomanitas.api.dto.RegistroFormulario;
import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminItemFormulario;
import com.mundomanitas.api.dto.admin.AdminRecursoFormulario;
import com.mundomanitas.api.service.ContenidoEnlaces;
import com.mundomanitas.api.service.YouTubeLink;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Inyección y validación de entradas en DTOs, enlaces y formularios reales. */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Inyección y validación de entradas")
class InputValidationTest {

    @Autowired private MockMvc mvc;
    @Autowired private Validator validator;

    @Nested
    @DisplayName("prueba de componentes")
    class Componentes {

        @Test
        @DisplayName("Bean Validation rechaza campos vacíos, largos y con formato inválido")
        void beanValidationRejectsMalformedForms() {
            RegistroFormulario registro = new RegistroFormulario();
            registro.setNombreCompleto("a".repeat(121));
            registro.setEmail("no-es-correo");
            registro.setContrasena("corta");
            registro.setConfirmContrasena("distinta");

            assertThat(paths(validator.validate(registro)))
                    .contains("nombreCompleto", "email", "contrasena", "acceptTerms");

            AdminCursoFormulario curso = new AdminCursoFormulario(
                    "Curso <script>", "Título", "Básico", false, "Programación",
                    "Resumen", "https://evil.example/portada.png", -1);
            assertThat(paths(validator.validate(curso)))
                    .contains("slug", "imagen", "posicion");

            AdminRecursoFormulario recurso = new AdminRecursoFormulario(
                    "Recurso", "javascript:alert(1)", 0);
            assertThat(paths(validator.validate(recurso))).contains("url");

            AdminItemFormulario item = new AdminItemFormulario(
                    UUID.randomUUID(), null, "SCRIPT", "Ítem", null, null, null, -1, -1, false);
            assertThat(paths(validator.validate(item)))
                    .contains("type", "duracionSegundos", "posicion");
        }

        @Test
        @DisplayName("los enlaces solo aceptan https y YouTube valida el ID antes de embeder")
        void enlaceHelpersRejectInjection() {
            assertThat(ContenidoEnlaces.https("https://example.com/material", "enlace"))
                    .isEqualTo("https://example.com/material");
            assertThatThrownBy(() -> ContenidoEnlaces.https("javascript:alert(1)", "enlace"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ContenidoEnlaces.https("http://example.com", "enlace"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ContenidoEnlaces.https(
                    "https://example.com/a\" onmouseover=\"alert(1)", "enlace"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(YouTubeLink.id("https://youtu.be/abcdefghijk")).isEqualTo("abcdefghijk");
            assertThat(YouTubeLink.id("https://evil.example/watch?v=abcdefghijk")).isNull();
            assertThat(YouTubeLink.id("javascript:alert(1)")).isNull();
            assertThat(YouTubeLink.embedUrl("abcdefghijk\"><script>alert(1)</script>")).isNull();
        }

        @Test
        @DisplayName("ninguna plantilla usa salida sin escapar")
        void templatesDoNotUseUnescapedOutput() throws IOException {
            Resource[] templates = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:templates/**/*.html");

            assertThat(templates).isNotEmpty();
            for (Resource plantilla : templates) {
                String html = new String(plantilla.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertThat(html).as(plantilla.getFilename()).doesNotContain("th:utext", "th:html");
            }
        }
    }

    @Nested
    @DisplayName("prueba de integración")
    class Integracion {

        @Test
        @DisplayName("un registro sin contraseña muestra errores, no revienta")
        void missingContrasenaRendersErrors() throws Exception {
            MvcResult resultado = registro("Ana Quispe", "ana@ejemplo.test", null, null);

            assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
            assertThat(resultado.getModelAndView().getViewName()).isEqualTo("auth/register");
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("Elegí una contraseña.");
        }

        @Test
        @DisplayName("un restablecimiento sin contraseña muestra errores, no revienta")
        void missingRestablecerContrasenaRendersErrors() throws Exception {
            MvcResult resultado = restablecer(null, null);

            assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
            assertThat(resultado.getModelAndView().getViewName()).isEqualTo("auth/reset-password");
            assertThat(resultado.getResponse().getContentAsString())
                    .contains("Elegí una contraseña.");
        }

        @Test
        @DisplayName("el markup enviado en el formulario se escapa en el HTML devuelto")
        void submittedMarkupIsEscaped() throws Exception {
            String payload = "<script>alert(1)</script>";
            MvcResult resultado = registro(payload, "no-es-correo", "ClaveSegura2026!", "ClaveSegura2026!");

            String html = resultado.getResponse().getContentAsString();
            assertThat(html).doesNotContain(payload);
            assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        }
    }

    private MvcResult registro(String nombreCompleto, String email, String contrasena, String confirmContrasena)
            throws Exception {
        MvcResult page = mvc.perform(get("/registro")).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        String token = csrfToken(page.getResponse().getContentAsString());

        var request = post("/registro").session(session).param("_csrf", token)
                .param("nombreCompleto", nombreCompleto)
                .param("email", email)
                .param("acceptTerms", "true");
        if (contrasena != null) request.param("contrasena", contrasena);
        if (confirmContrasena != null) request.param("confirmContrasena", confirmContrasena);

        return mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andReturn();
    }

    private MvcResult restablecer(String contrasena, String confirmContrasena) throws Exception {
        MvcResult page = mvc.perform(get("/registro")).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        String token = csrfToken(page.getResponse().getContentAsString());

        var request = post("/restablecer").session(session)
                .param("_csrf", token)
                .param("token", "token-de-prueba");
        if (contrasena != null) request.param("contrasena", contrasena);
        if (confirmContrasena != null) request.param("confirmContrasena", confirmContrasena);

        return mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andReturn();
    }

    private String csrfToken(String html) {
        var matcher = java.util.regex.Pattern
                .compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(html);
        assertThat(matcher.find()).as("el formulario de registro incluye el token CSRF").isTrue();
        return matcher.group(1);
    }

    private Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
