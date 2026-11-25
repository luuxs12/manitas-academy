package com.mundomanitas.api.web;

import com.mundomanitas.api.model.Certificate;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** El documento compartido se renderiza con el motor real para admin y alumno. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Render real del certificado")
class CertificateTemplateTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private CourseRepository courses;
    @Autowired private CertificateRepository certificates;
    @Autowired private WebApplicationContext context;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;
    private Certificate certificate;
    private Course course;
    private User student;

    @BeforeEach
    void setUp() {
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

        student = users.save(new User(
                "render-cert-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Ana Quispe"));
        student.activateSubscription(Plan.MONTHLY, OffsetDateTime.now().plusDays(30));
        users.save(student);
        course = courses.save(new Course(
                "render-cert-" + UUID.randomUUID(), "Redes de prueba", "Inicial", 9,
                true, "Pruebas", "Curso para verificar el render.", "", 0));
        certificate = certificates.save(
                new Certificate(student.getId(), course.getId(), "MM-RED-RENDER1"));
    }

    @AfterEach
    void tearDown() {
        adapter.setArgumentResolvers(originalResolvers);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("el admin visualiza el certificado vigente desde su panel")
    void rendersAdminPreview() throws Exception {
        loginAs(users.findByEmailIgnoreCase("admin@damianprint.com").orElseThrow());

        mvc.perform(get("/admin/certificados/{id}/ver", certificate.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/certificado"))
                .andExpect(content().string(containsString("Certificado de finalización")))
                .andExpect(content().string(containsString("Ana Quispe")))
                .andExpect(content().string(containsString("Redes de prueba")))
                .andExpect(content().string(containsString("0 videos")))
                .andExpect(content().string(containsString("MM-RED-RENDER1")))
                .andExpect(content().string(containsString("Vigente")))
                .andExpect(content().string(containsString("Imprimir o guardar en PDF")))
                .andExpect(content().string(containsString("← Volver a certificados")));
    }

    @Test
    @DisplayName("el alumno sigue viendo el mismo documento compartido")
    void rendersStudentCertificate() throws Exception {
        loginAs(student);

        mvc.perform(get("/panel/certificados/{slug}", course.getSlug()))
                .andExpect(status().isOk())
                .andExpect(view().name("student/certificado"))
                .andExpect(content().string(containsString("Certificado de finalización")))
                .andExpect(content().string(containsString("Ana Quispe")))
                .andExpect(content().string(containsString("Redes de prueba")))
                .andExpect(content().string(containsString("MM-RED-RENDER1")))
                .andExpect(content().string(containsString("Imprimir o guardar en PDF")));
    }

    private void loginAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
    }
}
