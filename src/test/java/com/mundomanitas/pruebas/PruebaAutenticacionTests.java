package com.mundomanitas.pruebas;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de autenticación.
 *
 * <p>Verifica el ciclo completo de identificación de un usuario: formulario web
 * con sesión, emisión y uso del JWT en la API, rechazo de credenciales
 * incorrectas y cierre de sesión.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Prueba de autenticación")
class PruebaAutenticacionTests {

    @Autowired
    private MockMvc mockMvc;

    private String tokenDeAlumno() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"alumno@mundomanitas.com","password":"123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    @Test
    @DisplayName("El login web con credenciales válidas abre el dashboard")
    void loginWebValidoRedirigeAlDashboard() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "alumno@mundomanitas.com")
                .param("password", "123456")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("El login web con contraseña incorrecta vuelve al formulario con error")
    void loginWebInvalidoVuelveAlLoginConError() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "alumno@mundomanitas.com")
                .param("password", "clave-incorrecta")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Una ruta protegida sin sesión redirige al login")
    void rutaProtegidaSinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("La API de login emite un JWT con tres segmentos")
    void laApiDeLoginEmiteUnJwtValido() throws Exception {
        String token = tokenDeAlumno();

        assertFalse(token.isBlank(), "el token no debe estar vacío");
        assertEquals(3, token.split("\\.").length, "un JWT tiene header, payload y firma");
    }

    @Test
    @DisplayName("La API rechaza credenciales incorrectas con 401 y sin filtrar datos")
    void laApiRechazaCredencialesIncorrectas() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"alumno@mundomanitas.com","password":"clave-incorrecta"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("No autenticado"))
            .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("El JWT permite consumir un recurso protegido")
    void elJwtPermiteConsumirUnRecursoProtegido() throws Exception {
        mockMvc.perform(get("/api/cursos")
                .header("Authorization", "Bearer " + tokenDeAlumno()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Un token manipulado o inválido no autentica la petición")
    void unTokenInvalidoNoAutentica() throws Exception {
        mockMvc.perform(get("/api/cursos")
                .header("Authorization", "Bearer token.invalido.firmado"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    @DisplayName("Cerrar sesión devuelve al login con confirmación")
    @WithUserDetails("alumno@mundomanitas.com")
    void cerrarSesionDevuelveAlLogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    @DisplayName("Un usuario autenticado no puede abrir una ruta de otro rol")
    void unUsuarioAutenticadoNoEntraARutasDeOtroRol() throws Exception {
        mockMvc.perform(get("/admin/usuarios")
                .with(user("alumno@mundomanitas.com").roles("ESTUDIANTE")))
            .andExpect(status().isForbidden());
    }
}
