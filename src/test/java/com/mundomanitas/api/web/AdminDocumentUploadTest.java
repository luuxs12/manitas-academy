package com.mundomanitas.api.web;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCourseContentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP del alta de documentos desde el panel administrativo. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Alta administrativa de documentos")
class AdminDocumentUploadTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AdminCourseContentService adminContent;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        User admin = users.findByEmailIgnoreCase("admin@damianprint.com").orElseThrow();
        AuthenticatedUser principal = new AuthenticatedUser(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        adapter = context.getBean(RequestMappingHandlerAdapter.class);
        originalResolvers = adapter.getArgumentResolvers();
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(originalResolvers);
        resolvers.add(0, new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType())
                        || Authentication.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                return Authentication.class.isAssignableFrom(parameter.getParameterType())
                        ? authentication
                        : authentication.getPrincipal();
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
    @DisplayName("recibe el archivo y crea el documento en el módulo elegido")
    void uploadsDocument() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "guia.pdf", "application/pdf",
                "contenido".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/admin/cursos/{id}/contenido/documentos", courseId)
                        .file(file)
                        .param("moduleId", moduleId.toString())
                        .param("title", "Guía de redes")
                        .param("description", "Material de la sesión")
                        .param("position", "2")
                        .param("visible", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/admin/cursos/" + courseId + "/contenido#modulo-" + moduleId));

        ArgumentCaptor<AdminCourseContentService.DocumentForm> captor =
                ArgumentCaptor.forClass(AdminCourseContentService.DocumentForm.class);
        verify(adminContent).createDocument(captor.capture(), any());
        AdminCourseContentService.DocumentForm form = captor.getValue();
        assertThat(form.moduleId()).isEqualTo(moduleId);
        assertThat(form.title()).isEqualTo("Guía de redes");
        assertThat(form.description()).isEqualTo("Material de la sesión");
        assertThat(form.position()).isEqualTo(2);
        assertThat(form.visible()).isTrue();
        assertThat(form.file().getOriginalFilename()).isEqualTo("guia.pdf");
    }
}
