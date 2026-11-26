package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.EnrollmentService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCourseContentService;
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
class QuizFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AdminCatalogService catalog;
    @Autowired private AdminCourseContentService adminContent;
    @Autowired private EnrollmentService enrollments;
    @Autowired private ContentItemRepository items;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private WebApplicationContext context;

    @PersistenceContext private EntityManager entityManager;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    private String slug;
    private UUID itemId;
    private UUID exerciseId;
    private UUID enrollmentId;
    private User student;
    private User admin;
    private List<UUID> questionIds;
    private List<UUID> correctIds;
    private List<UUID> wrongIds;

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
        admin = users.save(new User("admin-quiz-" + UUID.randomUUID() + "@test.local",
                "hash", "Administrador de prueba"));
        admin.changeRole("ADMIN");
        users.save(admin);
        authenticate(admin);

        UUID courseId = catalog.createCourse(new AdminCourseForm(slug, "Curso quiz web",
                "Inicial", true, "Prueba", "Curso de prueba.", "", 0), admin.getId());
        UUID moduleId = catalog.createModule(new AdminModuleForm(courseId, "Sesion 1", 0),
                admin.getId());
        itemId = adminContent.createItem(new AdminCourseContentService.ItemForm(moduleId, null,
                "EJERCICIO", "Practica 1", "Consigna del cuestionario.", null, null, null,
                0, true), admin.getId());
        exerciseId = items.findById(itemId).orElseThrow().getSourceId();

        student = users.save(new User("alumno-quiz-" + UUID.randomUUID() + "@test.local",
                "hash", "Alumno de prueba"));
        Enrollment enrollment = enrollments.enroll(student, slug);
        enrollmentId = enrollment.getId();
        entityManager.flush();

        questionIds = new ArrayList<>();
        correctIds = new ArrayList<>();
        wrongIds = new ArrayList<>();
        for (int position = 1; position <= 10; position++) {
            UUID questionId = UUID.randomUUID();
            jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                            + "values (?, ?, ?, ?, 2)",
                    questionId, exerciseId, position, "Pregunta " + position);
            questionIds.add(questionId);
            for (int option = 1; option <= 4; option++) {
                UUID alternativeId = UUID.randomUUID();
                jdbc.update("insert into quiz_alternatives "
                                + "(id, question_id, position, label, is_correct) "
                                + "values (?, ?, ?, ?, ?)",
                        alternativeId, questionId, option,
                        "Alternativa " + option + " de la pregunta " + position, option == 1);
                if (option == 1) {
                    correctIds.add(alternativeId);
                } else if (option == 2) {
                    wrongIds.add(alternativeId);
                }
            }
        }
        authenticate(student);
    }

    @AfterEach
    void tearDown() {
        adapter.setArgumentResolvers(originalResolvers);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("muestra las diez preguntas, guarda un solo intento y explica cada respuesta")
    void rendersSubmitsAndShowsResultOnce() throws Exception {
        String exercisePath = "/panel/cursos/" + slug + "/ejercicios/" + itemId;

        mvc.perform(get(exercisePath))
                .andExpect(status().isOk())
                .andExpect(view().name("student/ejercicio"))
                .andExpect(content().string(containsString("Enviar respuestas")))
                .andExpect(content().string(containsString("Pregunta 10")))
                .andExpect(content().string(containsString("Enviar una sola vez")));

        mvc.perform(answers(exercisePath + "/enviar"))
                .andExpect(redirectedUrl(exercisePath));

        mvc.perform(get(exercisePath))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resultado: 14 / 20")))
                .andExpect(content().string(containsString("Respuesta correcta")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")))
                .andExpect(content().string(containsString("Pregunta 10")));

        assertThat(attemptCount()).isEqualTo(1);

        mvc.perform(answers(exercisePath + "/enviar"))
                .andExpect(redirectedUrl(exercisePath));

        mvc.perform(get(exercisePath))
                .andExpect(content().string(containsString("Ya enviaste este cuestionario")));
        assertThat(attemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("el temario ofrece rendir el cuestionario y cuenta el intento como visto")
    void exerciseIsDoneAfterSubmitting() throws Exception {
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
    void adminCannotOpenQuiz() throws Exception {
        authenticate(admin);

        assertThatThrownBy(() -> mvc.perform(
                get("/panel/cursos/{slug}/ejercicios/{itemId}", slug, itemId)))
                .hasRootCauseInstanceOf(AuthorizationDeniedException.class);
    }

    private MockHttpServletRequestBuilder answers(String path) {
        MockHttpServletRequestBuilder request = post(path);
        for (int index = 0; index < questionIds.size(); index++) {
            UUID alternativeId = index < 7 ? correctIds.get(index) : wrongIds.get(index);
            request.param("respuestas[" + questionIds.get(index) + "]", alternativeId.toString());
        }
        return request;
    }

    private int attemptCount() {
        return jdbc.queryForObject(
                "select count(*) from quiz_attempts where enrollment_id = ? and exercise_id = ?",
                Integer.class, enrollmentId, exerciseId);
    }

    private void authenticate(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
    }
}
