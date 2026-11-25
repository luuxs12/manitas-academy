package com.mundomanitas.api.web;

import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.CourseContentService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Contratos visuales del panel del alumno antes de implementar el rediseño. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Organización visual del panel del estudiante")
class StudentDesignTemplateTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CourseContentService structure;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AuthenticatedUser principal = new AuthenticatedUser(users
                .findByEmailIgnoreCase("luisfernandocastrodamian8@gmail.com").orElseThrow());
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
    @DisplayName("el temario separa clase, material de estudio y práctica, con descarga")
    void rendersGroupedSyllabus() throws Exception {
        CourseContentService.ItemView video = new CourseContentService.ItemView(
                UUID.randomUUID(), "VIDEO", "Clase 01", "Video de la sesion",
                null, "mENHDQ8SLsI", 300, true, false, false, UUID.randomUUID());
        CourseContentService.ItemView material = new CourseContentService.ItemView(
                UUID.randomUUID(), "MATERIAL", "Tema 01: Guía",
                "Contenido de la sesion", "/uploads/materials/abc.pdf", null, null,
                true, false, false, null);
        CourseContentService.ItemView practice = new CourseContentService.ItemView(
                UUID.randomUUID(), "EJERCICIO", "Practica 01",
                "Escriba un programa que imprima Hola Mundo.", null, null, null,
                true, false, false, null);
        CourseContentService.Outline outline = new CourseContentService.Outline(
                List.of(new CourseContentService.ModuleView(
                        UUID.randomUUID(), "Sesion 01", true,
                        List.of(new CourseContentService.SectionView(
                                UUID.randomUUID(), "Temas de la sesion", true,
                                List.of(video, material, practice))),
                        List.of(), 0)),
                3, 0, 0, null);
        when(structure.outline(any(), any(), any(), eq(false))).thenReturn(outline);

        mvc.perform(get("/panel/cursos/{slug}", "redes"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/curso"))
                .andExpect(content().string(containsString("Clase en video")))
                .andExpect(content().string(containsString("Material de estudio")))
                .andExpect(content().string(containsString("Práctica")))
                .andExpect(content().string(containsString("Descargar documento")))
                .andExpect(content().string(containsString("Tema 01: Guía")));
    }

    @Test
    @DisplayName("el perfil usa un avatar compacto en lugar de un cuadrado vacío")
    void rendersCompactAvatar() throws Exception {
        mvc.perform(get("/panel/perfil"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/perfil"))
                .andExpect(content().string(containsString("panel-avatar-compact")))
                .andExpect(content().string(not(containsString("panel-avatar-preview"))));
    }
}
