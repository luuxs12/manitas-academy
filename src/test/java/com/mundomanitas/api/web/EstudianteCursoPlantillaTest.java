package com.mundomanitas.api.web;

import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** La vista del alumno se renderiza con el motor real y su matrícula de PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Render de la vista del curso del estudiante")
class EstudianteCursoPlantillaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository users;

    @Autowired
    private WebApplicationContext context;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AutenticadoUsuario principal = new AutenticadoUsuario(users
                .buscarPorEmailIgnorandoMayusculas("luisfernandocastrodamian8@gmail.com").orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        adapter = context.getBean(RequestMappingHandlerAdapter.class);
        originalResolvers = adapter.getArgumentResolvers();
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(originalResolvers);
        resolvers.add(0, new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return Authentication.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return SecurityContextHolder.getContext().getAuthentication();
            }
        });
        adapter.setArgumentResolvers(resolvers);
    }

    @AfterEach
    void tearDown() {
        adapter.setArgumentResolvers(originalResolvers);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("muestra las tres sesiones reales, sus temas y prácticas con datos de PostgreSQL")
    void rendersInscriptoCurso() throws Exception {
        String html = mvc.perform(get("/panel/cursos/{slug}", "redes"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/curso"))
                .andExpect(content().string(containsString("Temario")))
                .andExpect(content().string(containsString("Módulo")))
                .andExpect(content().string(containsString("Sesion 01")))
                .andExpect(content().string(containsString("Sesion 02")))
                .andExpect(content().string(containsString("Sesion 03")))
                .andExpect(content().string(containsString("Temas de la sesion")))
                .andExpect(content().string(containsString("Tema 01")))
                .andExpect(content().string(containsString("Practica 01")))
                .andExpect(content().string(containsString("MATERIAL")))
                .andExpect(content().string(containsString("VIDEO")))
                .andExpect(content().string(containsString("EJERCICIO")))
                .andExpect(content().string(containsString("0 de 7 ítems terminados")))
                .andExpect(content().string(containsString("Saltar al contenido")))
                .andExpect(content().string(not(containsString("Sin sección"))))
                .andExpect(content().string(not(containsString("Redes Neuronales"))))
                .andReturn().getResponse().getContentAsString();

        int start = html.indexOf("EJERCICIO");
        String filaEjercicio = html.substring(start, html.indexOf("</article>", start));
        assertThat(filaEjercicio).contains("Rendir cuestionario").doesNotContain("/visto");
    }
}
