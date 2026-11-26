package com.mundomanitas.api.web;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCursoContenidoService;
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
class AdminDocumentCargaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository users;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AdminCursoContenidoService adminContenido;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        Usuario admin = users.buscarPorEmailIgnorandoMayusculas("admin@damianprint.com").orElseThrow();
        AutenticadoUsuario principal = new AutenticadoUsuario(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        adapter = context.getBean(RequestMappingHandlerAdapter.class);
        originalResolvers = adapter.getArgumentResolvers();
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(originalResolvers);
        resolvers.add(0, new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return AutenticadoUsuario.class.isAssignableFrom(parameter.getParameterType())
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
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "guia.pdf", "application/pdf",
                "contenido".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/admin/cursos/{id}/contenido/documentos", cursoId)
                        .file(file)
                        .param("moduloId", moduloId.toString())
                        .param("titulo", "Guía de redes")
                        .param("descripcion", "Material de la sesión")
                        .param("posicion", "2")
                        .param("visible", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/admin/cursos/" + cursoId + "/contenido#modulo-" + moduloId));

        ArgumentCaptor<AdminCursoContenidoService.DocumentFormulario> captor =
                ArgumentCaptor.forClass(AdminCursoContenidoService.DocumentFormulario.class);
        verify(adminContenido).createDocument(captor.capture(), any());
        AdminCursoContenidoService.DocumentFormulario formulario = captor.getValue();
        assertThat(formulario.moduloId()).isEqualTo(moduloId);
        assertThat(formulario.titulo()).isEqualTo("Guía de redes");
        assertThat(formulario.descripcion()).isEqualTo("Material de la sesión");
        assertThat(formulario.posicion()).isEqualTo(2);
        assertThat(formulario.visible()).isTrue();
        assertThat(formulario.file().getOriginalFilename()).isEqualTo("guia.pdf");
    }
}
