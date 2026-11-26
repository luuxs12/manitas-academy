package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminCuestionarioController;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminCuestionarioFormulario;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import com.mundomanitas.api.service.admin.AdminCuestionarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("Flujo administrativo de carga del cuestionario")
class AdminCuestionarioFlujoTest {

    private AdminCuestionarioService cuestionarioService;
    private MockMvc mvc;
    private UUID actor;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        Usuario admin = mock(Usuario.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRol()).thenReturn("ADMIN");
        when(admin.getEmail()).thenReturn("admin@prueba.test");
        when(admin.getContrasenaHash()).thenReturn("hash");
        when(admin.getNombreCompleto()).thenReturn("Administradora");
        AutenticadoUsuario principal = new AutenticadoUsuario(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        AdminNotificacionService notifications = mock(AdminNotificacionService.class);
        AdminCatalogService catalog = mock(AdminCatalogService.class);
        cuestionarioService = mock(AdminCuestionarioService.class);
        when(notifications.reciente(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);
        when(catalog.ejercicioFormulario(any())).thenReturn(
                new AdminEjercicioFormulario(UUID.randomUUID(), "Ejercicio quiz", "Consigna quiz", 0));
        when(cuestionarioService.formulario(any())).thenReturn(new AdminCuestionarioFormulario());

        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminCuestionarioController(notifications, catalog, cuestionarioService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("abre el formulario de preguntas del ejercicio")
    void rendersFormulario() throws Exception {
        UUID ejercicioId = UUID.randomUUID();

        mvc.perform(get("/admin/ejercicios/{id}/preguntas", ejercicioId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/preguntas-form"))
                .andExpect(model().attributeExists("adminCuestionarioFormulario"))
                .andExpect(model().attribute("itemId", ejercicioId));

        verify(cuestionarioService).formulario(ejercicioId);
    }

    @Test
    @DisplayName("guarda las diez preguntas con sus alternativas desde el panel")
    void savesCuestionario() throws Exception {
        UUID ejercicioId = UUID.randomUUID();

        mvc.perform(cuestionarioRequest(ejercicioId, 10))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ejercicios/" + ejercicioId + "/preguntas"))
                .andExpect(flash().attribute("panelOk", "El cuestionario se guardó."));

        ArgumentCaptor<AdminCuestionarioFormulario> captor = ArgumentCaptor.forClass(AdminCuestionarioFormulario.class);
        verify(cuestionarioService).save(eq(ejercicioId), captor.capture(), eq(actor));
        assertThat(captor.getValue().getQuestions()).hasSize(10);
        assertThat(captor.getValue().getQuestions().get(0).getAlternatives()).hasSize(4);
        assertThat(captor.getValue().getQuestions().get(0).getCorrectIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("muestra errores de carga sin llamar al servicio")
    void showsValidationErrors() throws Exception {
        mvc.perform(cuestionarioRequest(UUID.randomUUID(), 9))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/preguntas-form"))
                .andExpect(model().attributeHasFieldErrors("adminCuestionarioFormulario", "questions"));

        verify(cuestionarioService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("muestra el error de negocio del servicio como aviso del panel")
    void showsServiceError() throws Exception {
        UUID ejercicioId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Ya hay intentos de este cuestionario; no se puede modificar."))
                .when(cuestionarioService).save(eq(ejercicioId), any(), eq(actor));

        mvc.perform(cuestionarioRequest(ejercicioId, 10))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ejercicios/" + ejercicioId + "/preguntas"))
                .andExpect(flash().attribute("panelError",
                        "Ya hay intentos de este cuestionario; no se puede modificar."));
    }

    private MockHttpServletRequestBuilder cuestionarioRequest(UUID ejercicioId, int preguntaContar) {
        MockHttpServletRequestBuilder request = post("/admin/ejercicios/{id}/preguntas", ejercicioId);
        for (int index = 0; index < preguntaContar; index++) {
            request.param("questions[" + index + "].posicion", String.valueOf(index + 1));
            request.param("questions[" + index + "].prompt", "Pregunta " + (index + 1));
            request.param("questions[" + index + "].correctIndex", "1");
            for (int opcion = 0; opcion < 4; opcion++) {
                request.param("questions[" + index + "].alternatives[" + opcion + "].label",
                        "Alternativa " + (index + 1) + "-" + (opcion + 1));
            }
        }
        return request;
    }
}
