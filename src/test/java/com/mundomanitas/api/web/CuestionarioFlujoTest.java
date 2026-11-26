package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.InscripcionService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCursoContenidoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Rendición del cuestionario desde el panel del alumno")
class CuestionarioFlujoTest {

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository users;
    @Autowired private AdminCatalogService catalog;
    @Autowired private AdminCursoContenidoService adminContenido;
    @Autowired private InscripcionService enrollments;
    @Autowired private ContenidoItemRepository items;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext context;

    @PersistenceContext private EntityManager entityManager;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    private String slug;
    private UUID itemId;
    private UUID ejercicioId;
    private UUID inscripcionId;
    private Usuario estudiante;
    private Usuario admin;
    private List<UUID> preguntaIds;
    private List<UUID> correctIds;
    private List<UUID> incorrectaIds;

    @BeforeEach
    void setUp() {
        // El advice global pide `Authentication`; con los filtros apagados hay
        // que resolverlo igual que en el arranque real de la aplicación.
        adapter = context.getBean(RequestMappingHandlerAdapter.class);
        originalResolvers = adapter.getArgumentResolvers();
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(originalResolvers);
        resolvers.add(0, new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return Authentication.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                          NativeWebRequest request, WebDataBinderFactory binderFactory) {
                return SecurityContextHolder.getContext().getAuthentication();
            }
        });
        adapter.setArgumentResolvers(resolvers);

        slug = "curso-quiz-web-" + UUID.randomUUID();
        admin = users.save(new Usuario("admin-quiz-" + UUID.randomUUID() + "@test.local",
                "hash", "Administrador de prueba"));
        admin.changeRol("ADMIN");
        users.save(admin);
        authenticate(admin);

        UUID cursoId = catalog.createCurso(new AdminCursoFormulario(slug, "Curso quiz web",
                "Inicial", true, "Prueba", "Curso de prueba.", "", 0), admin.getId());
        UUID moduloId = catalog.createModulo(new AdminModuloFormulario(cursoId, "Sesion 1", 0),
                admin.getId());
        itemId = adminContenido.createItem(new AdminCursoContenidoService.ItemFormulario(moduloId, null,
                "EJERCICIO", "Practica 1", "Consigna del cuestionario.", null, null, null,
                0, true), admin.getId());
        ejercicioId = items.findById(itemId).orElseThrow().getOrigenId();

        estudiante = users.save(new Usuario("alumno-quiz-" + UUID.randomUUID() + "@test.local",
                "hash", "Alumno de prueba"));
        Inscripcion inscripcion = enrollments.enroll(estudiante, slug);
        inscripcionId = inscripcion.getId();
        entityManager.flush();

        preguntaIds = new ArrayList<>();
        correctIds = new ArrayList<>();
        incorrectaIds = new ArrayList<>();
        for (int posicion = 1; posicion <= 10; posicion++) {
            UUID preguntaId = UUID.randomUUID();
            jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                            + "values (?, ?, ?, ?, 2)",
                    preguntaId, ejercicioId, posicion, "Pregunta " + posicion);
            preguntaIds.add(preguntaId);
            for (int opcion = 1; opcion <= 4; opcion++) {
                UUID alternativaId = UUID.randomUUID();
                jdbc.update("insert into quiz_alternatives "
                                + "(id, question_id, position, label, is_correct) "
                                + "values (?, ?, ?, ?, ?)",
                        alternativaId, preguntaId, opcion,
                        "Alternativa " + opcion + " de la pregunta " + posicion, opcion == 1);
                if (opcion == 1) {
                    correctIds.add(alternativaId);
                } else if (opcion == 2) {
                    incorrectaIds.add(alternativaId);
                }
            }
        }
        authenticate(estudiante);
    }

    @AfterEach
    void tearDown() {
        adapter.setArgumentResolvers(originalResolvers);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("muestra las diez preguntas, guarda un solo intento y explica cada respuesta")
    void rendersSubmitsAndShowsResultadoOnce() throws Exception {
        String ejercicioPath = "/panel/cursos/" + slug + "/ejercicios/" + itemId;

        mvc.perform(get(ejercicioPath))
                .andExpect(status().isOk())
                .andExpect(view().name("student/ejercicio"))
                .andExpect(content().string(containsString("Enviar respuestas")))
                .andExpect(content().string(containsString("Pregunta 10")))
                .andExpect(content().string(containsString("Enviar una sola vez")));

        mvc.perform(answers(ejercicioPath + "/enviar"))
                .andExpect(redirectedUrl(ejercicioPath));

        mvc.perform(get(ejercicioPath))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resultado: 14 / 20")))
                .andExpect(content().string(containsString("Respuesta correcta")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")))
                .andExpect(content().string(containsString("Pregunta 10")));

        assertThat(intentoContar()).isEqualTo(1);

        mvc.perform(answers(ejercicioPath + "/enviar"))
                .andExpect(redirectedUrl(ejercicioPath));

        mvc.perform(get(ejercicioPath))
                .andExpect(content().string(containsString("Ya enviaste este cuestionario")));
        assertThat(intentoContar()).isEqualTo(1);
    }

    @Test
    @DisplayName("el temario ofrece rendir el cuestionario y cuenta el intento como visto")
    void ejercicioIsDoneDespuesSubmitting() throws Exception {
        mvc.perform(get("/panel/cursos/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rendir cuestionario")))
                .andExpect(content().string(containsString("0 de 1 ítem terminado")));

        mvc.perform(answers("/panel/cursos/" + slug + "/ejercicios/" + itemId + "/enviar"));

        mvc.perform(get("/panel/cursos/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ver resultado")))
                .andExpect(content().string(containsString("1 de 1 ítem terminado")));
    }

    @Test
    @DisplayName("una cuenta ADMIN no puede abrir el cuestionario del alumno")
    void adminCannotOpenCuestionario() throws Exception {
        authenticate(admin);

        assertThatThrownBy(() -> mvc.perform(
                get("/panel/cursos/{slug}/ejercicios/{itemId}", slug, itemId)))
                .hasRootCauseInstanceOf(AuthorizationDeniedException.class);
    }

    private MockHttpServletRequestBuilder answers(String path) {
        MockHttpServletRequestBuilder request = post(path);
        for (int index = 0; index < preguntaIds.size(); index++) {
            UUID alternativaId = index < 7 ? correctIds.get(index) : incorrectaIds.get(index);
            request.param("respuestas[" + preguntaIds.get(index) + "]", alternativaId.toString());
        }
        return request;
    }

    private int intentoContar() {
        return jdbc.queryForObject(
                "select count(*) from quiz_attempts where enrollment_id = ? and exercise_id = ?",
                Integer.class, inscripcionId, ejercicioId);
    }

    private void authenticate(Usuario usuario) {
        AutenticadoUsuario principal = new AutenticadoUsuario(usuario);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
    }
}
