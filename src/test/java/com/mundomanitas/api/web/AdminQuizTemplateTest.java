package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

/** La carga de preguntas se renderiza con el motor real y datos creados desde el panel. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Render de la carga administrativa del cuestionario")
class AdminQuizTemplateTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AdminCatalogService catalog;
    @Autowired private WebApplicationContext context;
    @Autowired private EntityManager entityManager;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AuthenticatedUser principal = new AuthenticatedUser(
                users.findByEmailIgnoreCase("admin@damianprint.com").orElseThrow());
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
    @Transactional
    @DisplayName("muestra diez preguntas con cuatro alternativas y opción de correcta")
    void rendersQuizForm() throws Exception {
        UUID actor = tempAdmin();
        String slug = "quiz-render-" + UUID.randomUUID();
        UUID courseId = catalog.createCourse(new AdminCourseForm(slug, "Quiz render",
                "Nivel inicial", true, "Pruebas", "Curso para verificar la carga de preguntas.",
                "", 0), actor);
        UUID moduleId = catalog.createModule(new AdminModuleForm(courseId, "Unidad quiz", 0), actor);
        UUID exerciseId = catalog.createExercise(new AdminExerciseForm(moduleId, "Ejercicio quiz",
                "Consigna quiz.", 0), actor);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/admin/ejercicios/{id}/preguntas", exerciseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/preguntas-form"))
                .andExpect(content().string(containsString("Cargar cuestionario")))
                .andExpect(content().string(containsString("Pregunta 1")))
                .andExpect(content().string(containsString("Pregunta 10")))
                .andExpect(content().string(containsString("name=\"questions[0].prompt\"")))
                .andExpect(content().string(containsString("name=\"questions[0].correctIndex\"")))
                .andExpect(content().string(containsString("name=\"questions[9].alternatives[3].label\"")))
                .andExpect(content().string(containsString("Guardar cuestionario")))
                .andExpect(content().string(containsString("Saltar al contenido")));

        mvc.perform(get("/admin/ejercicios/{id}/editar", exerciseId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cargar preguntas")));
    }

    private UUID tempAdmin() {
        User admin = new User("admin-quiz-" + UUID.randomUUID() + "@test.local",
                "hash", "Administrador de cuestionarios");
        admin.changeRole("ADMIN");
        return users.save(admin).getId();
    }
}
