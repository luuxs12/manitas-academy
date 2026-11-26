package com.mundomanitas.api.web;

import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CursoContenidoService;
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
class EstudianteDesignPlantillaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository users;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CursoContenidoService estructura;

    private RequestMappingHandlerAdapter adapter;
    private List<HandlerMethodArgumentResolver> originalResolvers;

    @BeforeEach
    void setUp() {
        AutenticadoUsuario principal = new AutenticadoUsuario(users
                .buscarPorEmailIgnorandoMayusculas("luisfernandocastrodamian8@gmail.com").orElseThrow());
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
    void rendersGroupedSilabo() throws Exception {
        CursoContenidoService.ItemVista video = new CursoContenidoService.ItemVista(
                UUID.randomUUID(), "VIDEO", "Clase 01", "Video de la sesion",
                null, "mENHDQ8SLsI", 300, true, false, false, UUID.randomUUID());
        CursoContenidoService.ItemVista material = new CursoContenidoService.ItemVista(
                UUID.randomUUID(), "MATERIAL", "Tema 01: Guía",
                "Contenido de la sesion", "/uploads/materials/abc.pdf", null, null,
                true, false, false, null);
        CursoContenidoService.ItemVista practice = new CursoContenidoService.ItemVista(
                UUID.randomUUID(), "EJERCICIO", "Practica 01",
                "Escriba un programa que imprima Hola Mundo.", null, null, null,
                true, false, false, null);
        CursoContenidoService.Esquema esquema = new CursoContenidoService.Esquema(
                List.of(new CursoContenidoService.ModuloVista(
                        UUID.randomUUID(), "Sesion 01", true,
                        List.of(new CursoContenidoService.SeccionVista(
                                UUID.randomUUID(), "Temas de la sesion", true,
                                List.of(video, material, practice))),
                        List.of(), 0)),
                3, 0, 0, null);
        when(estructura.esquema(any(), any(), any(), eq(false))).thenReturn(esquema);

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
    @DisplayName("el PDF abierto se muestra en el escenario junto al temario")
    void rendersPdfViewerBesideSilabo() throws Exception {
        UUID itemId = UUID.randomUUID();
        CursoContenidoService.ItemVista material = new CursoContenidoService.ItemVista(
                UUID.randomUUID(), "MATERIAL", "Tema 01: Guía",
                "Contenido de la sesion", "/uploads/materials/abc.pdf", null, null,
                true, false, false, null, "guia.pdf", false);
        CursoContenidoService.Esquema esquema = new CursoContenidoService.Esquema(
                List.of(new CursoContenidoService.ModuloVista(
                        UUID.randomUUID(), "Sesion 01", true,
                        List.of(new CursoContenidoService.SeccionVista(
                                UUID.randomUUID(), "Temas de la sesion", true, List.of(material))),
                        List.of(), 0)),
                1, 0, 0, null);
        when(estructura.esquema(any(), any(), any(), eq(false))).thenReturn(esquema);
        when(estructura.vistaMaterial(any(), eq("redes"), eq(itemId))).thenReturn(
                new CursoContenidoService.MaterialVista(
                        itemId, "Tema 01: Guía", "guia.pdf", "application/pdf"));

        mvc.perform(get("/panel/cursos/{slug}", "redes").param("material", itemId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("student/curso"))
                .andExpect(content().string(containsString("course-viewer")))
                .andExpect(content().string(containsString("data-pdf-frame")))
                .andExpect(content().string(containsString("Tema 01: Guía")))
                .andExpect(content().string(containsString(
                        "/panel/cursos/redes/materiales/" + itemId + "/ver")))
                .andExpect(content().string(containsString("Descargar documento")));
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
