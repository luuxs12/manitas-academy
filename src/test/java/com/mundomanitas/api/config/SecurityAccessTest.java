package com.mundomanitas.api.config;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autenticación y autorización con los filtros reales y la base real. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Autenticación y autorización")
class SecurityAccessTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private UserDetailsService usuarioDetails;

    @Nested
    @DisplayName("prueba de componentes")
    class Componentes {

        @Test
        @DisplayName("AuthenticatedUser publica el rol y respeta la cuenta activa")
        void authoritiesFollowRol() {
            Usuario estudiante = new Usuario("componente@ejemplo.test", "hash", "Estudiante");
            Usuario admin = new Usuario("componente-admin@ejemplo.test", "hash", "Administrador");
            ReflectionTestUtils.setField(admin, "rol", "ADMIN");
            ReflectionTestUtils.setField(admin, "activo", false);

            assertThat(new AutenticadoUsuario(estudiante).getAuthorities())
                    .extracting("authority").containsExactly("ROLE_STUDENT");
            assertThat(new AutenticadoUsuario(estudiante).isEnabled()).isTrue();

            AutenticadoUsuario inactiveAdmin = new AutenticadoUsuario(admin);
            assertThat(inactiveAdmin.getAuthorities())
                    .extracting("authority").containsExactly("ROLE_ADMIN");
            assertThat(inactiveAdmin.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("prueba de integración")
    class Integracion {

        @Test
        @DisplayName("UserDetailsService lee cuentas reales sin distinguir mayúsculas")
        void usuarioDetailsReadsRealCuenta() {
            var found = usuarioDetails.loadUserByUsername("ADMIN@damianprint.com");

            assertThat(found.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
            assertThat(found.isEnabled()).isTrue();
            assertThatThrownBy(() -> usuarioDetails.loadUserByUsername("nadie@ejemplo.test"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("la portada sigue abierta sin sesión")
        void publicPagesStayOpen() throws Exception {
            mvc.perform(get("/")).andExpect(status().isOk());
            mvc.perform(get("/?login")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("el área privada redirige a login cuando no hay sesión")
        void anonymousCannotEnterPrivateAreas() throws Exception {
            assertThat(redirectOf(get("/panel"))).contains("login");
            assertThat(redirectOf(get("/admin"))).contains("login");
        }

        @Test
        @DisplayName("los roles no se cruzan entre paneles")
        void rolesAreIsolated() throws Exception {
            mvc.perform(get("/panel").with(autenticadoAs("STUDENT"))).andExpect(status().isOk());
            mvc.perform(get("/admin").with(autenticadoAs("STUDENT"))).andExpect(status().isForbidden());
            mvc.perform(get("/admin").with(autenticadoAs("ADMIN"))).andExpect(status().isOk());
            mvc.perform(get("/panel").with(autenticadoAs("ADMIN"))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("un POST sin token CSRF se rechaza y con token se acepta")
        void csrfProtectsPosts() throws Exception {
            MvcResult page = mvc.perform(get("/")).andReturn();
            MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
            String token = csrfToken(page.getResponse().getContentAsString());

            mvc.perform(post("/logout").session(session).with(autenticadoAs("ADMIN")))
                    .andExpect(status().isForbidden());

            mvc.perform(post("/logout").session(session)
                            .param("_csrf", token)
                            .with(autenticadoAs("ADMIN")))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("prueba de autenticación")
    class Autenticacion {

        @Test
        @DisplayName("el login real lleva a cada panel según el rol de la cuenta")
        void formularioLoginGrantsPanelPorRol() throws Exception {
            String clave = "ClaveSegura2026!";
            Usuario estudiante = cuenta("seguridad-estudiante@ejemplo.test", clave, "STUDENT");
            Usuario administrador = cuenta("seguridad-admin@ejemplo.test", clave, "ADMIN");

            assertThat(login(estudiante.getEmail(), clave)).isEqualTo("/panel");
            assertThat(login(administrador.getEmail(), clave)).isEqualTo("/admin");
        }

        @Test
        @DisplayName("una clave incorrecta o una cuenta inexistente no abren sesión")
        void badCredentialsStayOut() throws Exception {
            Usuario estudiante = cuenta("seguridad-rechazo@ejemplo.test", "ClaveSegura2026!", "STUDENT");

            assertThat(login(estudiante.getEmail(), "Incorrecta123")).contains("login").contains("error");
            assertThat(login("nadie@ejemplo.test", "ClaveSegura2026!")).contains("login").contains("error");
        }
    }

    private Usuario cuenta(String email, String clave, String rol) {
        Usuario cuenta = new Usuario(email, encoder.encode(clave), "Prueba de seguridad");
        ReflectionTestUtils.setField(cuenta, "rol", rol);
        return users.save(cuenta);
    }

    private String login(String email, String clave) throws Exception {
        MvcResult page = mvc.perform(get("/")).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        String token = csrfToken(page.getResponse().getContentAsString());

        MvcResult resultado = mvc.perform(post("/login").session(session)
                        .param("_csrf", token)
                        .param("username", email)
                        .param("password", clave))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return resultado.getResponse().getRedirectedUrl();
    }

    private String redirectOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request).andReturn().getResponse().getRedirectedUrl();
    }

    private String csrfToken(String html) {
        var matcher = java.util.regex.Pattern
                .compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(html);
        assertThat(matcher.find()).as("el formulario de acceso incluye el token CSRF").isTrue();
        return matcher.group(1);
    }

    private RequestPostProcessor autenticadoAs(String rol) {
        String email = "ADMIN".equals(rol)
                ? "admin@damianprint.com"
                : "luisfernandocastrodamian8@gmail.com";
        AutenticadoUsuario principal = users.buscarPorEmailIgnorandoMayusculas(email)
                .map(AutenticadoUsuario::new)
                .orElseThrow(() -> new AssertionError("falta la cuenta de prueba " + email));

        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, "sin-clave", principal.getAuthorities()));
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }
}
