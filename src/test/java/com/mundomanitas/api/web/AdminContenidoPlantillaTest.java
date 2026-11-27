package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCursoContenidoService;
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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** La página de contenido se renderiza con el motor real y los datos de PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Render de la página de contenido administrativo")
class AdminContenidoPlantillaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CursoRepository courses;

    @Autowired
    private UsuarioRepository users;

    @Autowired
    private AdminCatalogService catalog;

    @Autowired
    private AdminCursoContenidoService adminContenido;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EntityManager entityManager;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AutenticadoUsuario principal = new AutenticadoUsuario(
                users.buscarPorEmailIgnorandoMayusculas("admin@damianprint.com").orElseThrow());
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
        UUID cursoId = courses.buscarPorSlug("python").orElseThrow().getId();

        mvc.perform(get("/admin/cursos/{id}/contenido", cursoId))
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
        UUID cursoId = catalog.createCurso(new AdminCursoFormulario(slug, "Contenido render",
                "Nivel inicial", true, "Pruebas", "Curso para verificar las filas del temario.",
                "", 0), actor);
        UUID moduloId = catalog.createModulo(
                new AdminModuloFormulario(cursoId, "Unidad render", 0), actor);
        catalog.createVideo(new AdminVideoFormulario(moduloId, "Clase render", 0,
                "https://youtu.be/dQw4w9WgXcQ"), actor);
        catalog.createEjercicio(new AdminEjercicioFormulario(moduloId, "Ejercicio render",
                "Consigna render.", 0), actor);
        adminContenido.createSeccion(moduloId,
                new AdminCursoContenidoService.SeccionFormulario("Lecturas render", 0, true), actor);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/admin/cursos/{id}/contenido", cursoId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/contenido"))
                .andExpect(content().string(containsString("Clase render")))
                .andExpect(content().string(containsString("Ejercicio render")))
                .andExpect(content().string(containsString("Lecturas render")))
                .andExpect(content().string(containsString("VIDEO")))
                .andExpect(content().string(containsString("EJERCICIO")))
                .andExpect(content().string(containsString("M3 3l18 18")))
                .andExpect(content().string(not(containsString("M2 12s3.5-6"))));
    }

    private UUID tempAdmin() {
        Usuario admin = new Usuario("admin-render-" + UUID.randomUUID() + "@test.local",
                "hash", "Administrador de render");
        admin.changeRol("ADMIN");
        return users.save(admin).getId();
    }
}
