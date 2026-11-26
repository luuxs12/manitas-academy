package com.mundomanitas.api.software;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
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
class SoftwareFlujoTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private AdminCatalogService catalog;
    @Autowired private ContenidoItemRepository items;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("prueba de componentes")
    class Componentes {

        @Test
        @DisplayName("el hash BCrypt de una cuenta persistida coincide y publica el rol")
        void contrasenaAndPrincipalWorkTogether() {
            String clave = "ClaveSegura2026!";
            Usuario cuenta = users.save(new Usuario(
                    "componente-" + UUID.randomUUID() + "@ejemplo.test",
                    encoder.encode(clave), "Estudiante de componente"));

            assertThat(cuenta.getContrasenaHash()).isNotEqualTo(clave).startsWith("$2");
            assertThat(encoder.matches(clave, cuenta.getContrasenaHash())).isTrue();
            assertThat(encoder.matches("Incorrecta123", cuenta.getContrasenaHash())).isFalse();

            AutenticadoUsuario principal = new AutenticadoUsuario(cuenta);
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
        void signUpEnrollAndProgreso() throws Exception {
            String slug = "calidad-" + UUID.randomUUID();
            UUID itemId = publishedFreeCurso(slug);

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

            MvcResult curso = mvc.perform(get("/panel/cursos/{slug}", slug).session(session))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("itemContar", 1))
                    .andExpect(model().attribute("doneContar", 0))
                    .andReturn();

            mvc.perform(post("/panel/cursos/{slug}/items/{itemId}/visto", slug, itemId)
                            .session(session)
                            .param("_csrf", csrfToken(curso.getResponse().getContentAsString()))
                            .param("visto", "true"))
                    .andExpect(redirectedUrl("/panel/cursos/" + slug));

            mvc.perform(get("/panel/cursos/{slug}", slug).session(session))
                    .andExpect(model().attribute("doneContar", 1))
                    .andExpect(model().attribute("itemPercent", 100));
        }
    }

    @Nested
    @DisplayName("prueba de autenticación")
    class Autenticacion {

        @Test
        @DisplayName("el alta abre sesión de estudiante y no habilita el panel administrativo")
        void signUpOnlyGrantsEstudiantePanel() throws Exception {
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
            Usuario admin = new Usuario(email, encoder.encode(clave), "Administradora de software");
            admin.changeRol("ADMIN");
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
            MockHttpSession adminSesion = (MockHttpSession) login.getRequest().getSession(false);

            mvc.perform(get("/admin").session(adminSesion)).andExpect(status().isOk());
            mvc.perform(get("/panel").session(adminSesion)).andExpect(status().isForbidden());
        }
    }

    private MvcResult signUp(String email) throws Exception {
        MvcResult page = mvc.perform(get("/registro")).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        String token = csrfToken(page.getResponse().getContentAsString());

        return mvc.perform(post("/registro").session(session)
                        .param("_csrf", token)
                        .param("nombreCompleto", "Estudiante de software")
                        .param("email", email)
                        .param("contrasena", "ClaveSegura2026!")
                        .param("confirmContrasena", "ClaveSegura2026!")
                        .param("acceptTerms", "true"))
                .andReturn();
    }

    private UUID publishedFreeCurso(String slug) {
        Usuario admin = new Usuario("admin-software-" + UUID.randomUUID() + "@ejemplo.test",
                "hash", "Administradora de software");
        admin.changeRol("ADMIN");
        UUID actor = users.save(admin).getId();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin-de-prueba", "sin-clave",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            UUID cursoId = catalog.createCurso(new AdminCursoFormulario(slug, "Curso de calidad",
                    "Nivel inicial", true, "Pruebas", "Curso real para el flujo de software.",
                    "", 0), actor);
            UUID moduloId = catalog.createModulo(
                    new AdminModuloFormulario(cursoId, "Unidad 1", 0), actor);
            UUID videoId = catalog.createVideo(new AdminVideoFormulario(moduloId, "Clase 1", 0,
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ"), actor);
            catalog.setPublished(cursoId, true, actor);
            return items.buscarPorOrigenTipoAndOrigenId("VIDEO", videoId).getId();
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
