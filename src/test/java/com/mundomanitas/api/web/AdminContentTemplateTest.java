package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCourseContentService;
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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** La página de contenido se renderiza con el motor real y los datos de PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Render de la página de contenido administrativo")
class AdminContentTemplateTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    @Autowired
    private AdminCatalogService catalog;

    @Autowired
    private AdminCourseContentService adminContent;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AuthenticatedUser principal = new AuthenticatedUser(
                users.findByEmailIgnoreCase("admin@damianprint.com").orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        // Sin filtros, MVC no resuelve Authentication: lo hacemos desde el contexto de prueba.
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
    @DisplayName("muestra temario, formularios y sílabo sin depender de datos inventados")
    void rendersPage() throws Exception {
        UUID courseId = courses.findBySlug("python").orElseThrow().getId();

        mvc.perform(get("/admin/cursos/{id}/contenido", courseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/contenido"))
                .andExpect(content().string(containsString("Temario")))
                .andExpect(content().string(containsString("Módulo")))
                .andExpect(content().string(containsString("Nueva sección")))
                .andExpect(content().string(containsString("Nuevo ítem")))
                .andExpect(content().string(containsString("Nuevo documento")))
                .andExpect(content().string(containsString("type=\"file\"")))
                .andExpect(content().string(containsString("Sílabo")))
                .andExpect(content().string(containsString("Saltar al contenido")));
    }

    @Test
    @Transactional
    @DisplayName("renderiza los ítems reales con su tipo y su sección")
    void rendersItems() throws Exception {
        UUID actor = tempAdmin();
        String slug = "render-items-" + UUID.randomUUID();
        UUID courseId = catalog.createCourse(new AdminCourseForm(slug, "Contenido render",
                "Nivel inicial", true, "Pruebas", "Curso para verificar las filas del temario.",
                "", 0), actor);
        UUID moduleId = catalog.createModule(
                new AdminModuleForm(courseId, "Unidad render", 0), actor);
        catalog.createVideo(new AdminVideoForm(moduleId, "Clase render", 0,
                "https://youtu.be/dQw4w9WgXcQ"), actor);
        catalog.createExercise(new AdminExerciseForm(moduleId, "Ejercicio render",
                "Consigna render.", 0), actor);
        adminContent.createSection(moduleId,
                new AdminCourseContentService.SectionForm("Lecturas render", 0, true), actor);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/admin/cursos/{id}/contenido", courseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/contenido"))
                .andExpect(content().string(containsString("Clase render")))
                .andExpect(content().string(containsString("Ejercicio render")))
                .andExpect(content().string(containsString("Lecturas render")))
                .andExpect(content().string(containsString("VIDEO")))
                .andExpect(content().string(containsString("EJERCICIO")));
    }

    private UUID tempAdmin() {
        User admin = new User("admin-render-" + UUID.randomUUID() + "@test.local",
                "hash", "Administrador de render");
        admin.changeRole("ADMIN");
        return users.save(admin).getId();
    }
}
