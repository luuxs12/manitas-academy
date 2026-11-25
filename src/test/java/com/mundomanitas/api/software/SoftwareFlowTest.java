package com.mundomanitas.api.software;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Calidad de software: componentes, integración y autenticación de un flujo real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Calidad de software: flujo completo de una cuenta")
class SoftwareFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private AdminCatalogService catalog;
    @Autowired private ContentItemRepository items;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("prueba de componentes")
    class Componentes {

        @Test
        @DisplayName("el hash BCrypt de una cuenta persistida coincide y publica el rol")
        void passwordAndPrincipalWorkTogether() {
            String clave = "ClaveSegura2026!";
            User account = users.save(new User(
                    "componente-" + UUID.randomUUID() + "@ejemplo.test",
                    encoder.encode(clave), "Estudiante de componente"));

            assertThat(account.getPasswordHash()).isNotEqualTo(clave).startsWith("$2");
            assertThat(encoder.matches(clave, account.getPasswordHash())).isTrue();
            assertThat(encoder.matches("Incorrecta123", account.getPasswordHash())).isFalse();

            AuthenticatedUser principal = new AuthenticatedUser(account);
            assertThat(principal.getAuthorities()).extracting("authority")
                    .containsExactly("ROLE_STUDENT");
            assertThat(principal.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("prueba de integración")
    class Integracion {

        @Test
        @DisplayName("alta por formulario, inscripción y avance llegan al temario real")
        void signUpEnrollAndProgress() throws Exception {
            String slug = "calidad-" + UUID.randomUUID();
            UUID itemId = publishedFreeCourse(slug);

            MvcResult signup = signUp("software-" + UUID.randomUUID() + "@ejemplo.test");
            assertThat(signup.getResponse().getRedirectedUrl()).isEqualTo("/panel?bienvenida");
            MockHttpSession session = (MockHttpSession) signup.getRequest().getSession(false);

            MvcResult panel = mvc.perform(get("/panel").session(session))
                    .andExpect(status().isOk())
                    .andReturn();
            String token = csrfToken(panel.getResponse().getContentAsString());

            mvc.perform(post("/panel/cursos/{slug}/inscribir", slug)
                            .session(session).param("_csrf", token))
                    .andExpect(redirectedUrl("/panel/cursos"));

            MvcResult course = mvc.perform(get("/panel/cursos/{slug}", slug).session(session))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("itemCount", 1))
                    .andExpect(model().attribute("doneCount", 0))
                    .andReturn();

            mvc.perform(post("/panel/cursos/{slug}/items/{itemId}/visto", slug, itemId)
                            .session(session)
                            .param("_csrf", csrfToken(course.getResponse().getContentAsString()))
                            .param("seen", "true"))
                    .andExpect(redirectedUrl("/panel/cursos/" + slug));

            mvc.perform(get("/panel/cursos/{slug}", slug).session(session))
                    .andExpect(model().attribute("doneCount", 1))
                    .andExpect(model().attribute("itemPercent", 100));
        }
    }

    @Nested
    @DisplayName("prueba de autenticación")
    class Autenticacion {

        @Test
        @DisplayName("el alta abre sesión de estudiante y no habilita el panel administrativo")
        void signUpOnlyGrantsStudentPanel() throws Exception {
            MvcResult signup = signUp("software-rol-" + UUID.randomUUID() + "@ejemplo.test");
            MockHttpSession session = (MockHttpSession) signup.getRequest().getSession(false);

            mvc.perform(get("/panel").session(session)).andExpect(status().isOk());
            mvc.perform(get("/admin").session(session)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("un login real de administrador abre su panel y no el del alumno")
        void adminLoginOnlyGrantsAdminPanel() throws Exception {
            String clave = "ClaveSegura2026!";
            String email = "software-admin-" + UUID.randomUUID() + "@ejemplo.test";
            User admin = new User(email, encoder.encode(clave), "Administradora de software");
            admin.changeRole("ADMIN");
            users.save(admin);

            MvcResult page = mvc.perform(get("/")).andReturn();
            MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
            String token = csrfToken(page.getResponse().getContentAsString());

            MvcResult login = mvc.perform(post("/login").session(session)
                            .param("_csrf", token)
                            .param("username", email)
                            .param("password", clave))
                    .andExpect(redirectedUrl("/admin"))
                    .andReturn();
            MockHttpSession adminSession = (MockHttpSession) login.getRequest().getSession(false);

            mvc.perform(get("/admin").session(adminSession)).andExpect(status().isOk());
            mvc.perform(get("/panel").session(adminSession)).andExpect(status().isForbidden());
        }
    }

    private MvcResult signUp(String email) throws Exception {
        MvcResult page = mvc.perform(get("/registro")).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        String token = csrfToken(page.getResponse().getContentAsString());

        return mvc.perform(post("/registro").session(session)
                        .param("_csrf", token)
                        .param("fullName", "Estudiante de software")
                        .param("email", email)
                        .param("password", "ClaveSegura2026!")
                        .param("confirmPassword", "ClaveSegura2026!")
                        .param("acceptTerms", "true"))
                .andReturn();
    }

    private UUID publishedFreeCourse(String slug) {
        User admin = new User("admin-software-" + UUID.randomUUID() + "@ejemplo.test",
                "hash", "Administradora de software");
        admin.changeRole("ADMIN");
        UUID actor = users.save(admin).getId();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin-de-prueba", "sin-clave",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            UUID courseId = catalog.createCourse(new AdminCourseForm(slug, "Curso de calidad",
                    "Nivel inicial", true, "Pruebas", "Curso real para el flujo de software.",
                    "", 0), actor);
            UUID moduleId = catalog.createModule(
                    new AdminModuleForm(courseId, "Unidad 1", 0), actor);
            UUID videoId = catalog.createVideo(new AdminVideoForm(moduleId, "Clase 1", 0,
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ"), actor);
            catalog.setPublished(courseId, true, actor);
            return items.findBySourceTypeAndSourceId("VIDEO", videoId).getId();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String csrfToken(String html) {
        var matcher = java.util.regex.Pattern
                .compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(html);
        assertThat(matcher.find()).as("la página incluye el token CSRF").isTrue();
        return matcher.group(1);
    }
}
