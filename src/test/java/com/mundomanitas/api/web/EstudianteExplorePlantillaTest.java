package com.mundomanitas.api.web;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.repository.CursoRepository;
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
import org.springframework.transaction.annotation.Transactional;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** El alta de inscripción se renderiza con el motor real y cursos de PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Render del explorador del estudiante")
class EstudianteExplorePlantillaTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository users;
    @Autowired private CursoRepository courses;
    @Autowired private WebApplicationContext context;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AutenticadoUsuario principal = new AutenticadoUsuario(users
                .buscarPorEmailIgnorandoMayusculas("luisfernandocastrodamian8@gmail.com").orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        Curso curso = courses.save(new Curso(
                "render-explorar-" + UUID.randomUUID(),
                "Curso temporal del explorador", "Inicial", 1,
                true, "Pruebas", "Curso creado solo para verificar el render.", "", 999));
        curso.publicar();
        courses.save(curso);

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
    @DisplayName("el formulario de inscripción pide confirmación con el título real")
    void inscripcionAsksForConfirmation() throws Exception {
        mvc.perform(get("/panel/explorar"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/explorar"))
                .andExpect(content().string(containsString("data-confirm-form")))
                .andExpect(content().string(containsString("¿Inscribirte en")))
                .andExpect(content().string(containsString("/js/panel-dialogs.js")));
    }
}
