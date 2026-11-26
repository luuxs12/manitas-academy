package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminQuizController;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminQuizForm;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import com.mundomanitas.api.service.admin.AdminQuizService;
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
class AdminQuizFlowTest {

    private AdminQuizService quizService;
    private MockMvc mvc;
    private UUID actor;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRole()).thenReturn("ADMIN");
        when(admin.getEmail()).thenReturn("admin@prueba.test");
        when(admin.getPasswordHash()).thenReturn("hash");
        when(admin.getFullName()).thenReturn("Administradora");
        AuthenticatedUser principal = new AuthenticatedUser(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        AdminNotificationService notifications = mock(AdminNotificationService.class);
        AdminCatalogService catalog = mock(AdminCatalogService.class);
        quizService = mock(AdminQuizService.class);
        when(notifications.recent(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);
        when(catalog.exerciseForm(any())).thenReturn(
                new AdminExerciseForm(UUID.randomUUID(), "Ejercicio quiz", "Consigna quiz", 0));
        when(quizService.form(any())).thenReturn(new AdminQuizForm());

        mvc = MockMvcBuilders.standaloneSetup(
                        new AdminQuizController(notifications, catalog, quizService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("abre el formulario de preguntas del ejercicio")
    void rendersForm() throws Exception {
        UUID exerciseId = UUID.randomUUID();

        mvc.perform(get("/admin/ejercicios/{id}/preguntas", exerciseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/preguntas-form"))
                .andExpect(model().attributeExists("adminQuizForm"))
                .andExpect(model().attribute("itemId", exerciseId));

        verify(quizService).form(exerciseId);
    }

    @Test
    @DisplayName("guarda las diez preguntas con sus alternativas desde el panel")
    void savesQuiz() throws Exception {
        UUID exerciseId = UUID.randomUUID();

        mvc.perform(quizRequest(exerciseId, 10))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ejercicios/" + exerciseId + "/preguntas"))
                .andExpect(flash().attribute("panelOk", "El cuestionario se guardó."));

        ArgumentCaptor<AdminQuizForm> captor = ArgumentCaptor.forClass(AdminQuizForm.class);
        verify(quizService).save(eq(exerciseId), captor.capture(), eq(actor));
        assertThat(captor.getValue().getQuestions()).hasSize(10);
        assertThat(captor.getValue().getQuestions().get(0).getAlternatives()).hasSize(4);
        assertThat(captor.getValue().getQuestions().get(0).getCorrectIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("muestra errores de carga sin llamar al servicio")
    void showsValidationErrors() throws Exception {
        mvc.perform(quizRequest(UUID.randomUUID(), 9))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/preguntas-form"))
                .andExpect(model().attributeHasFieldErrors("adminQuizForm", "questions"));

        verify(quizService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("muestra el error de negocio del servicio como aviso del panel")
    void showsServiceError() throws Exception {
        UUID exerciseId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Ya hay intentos de este cuestionario; no se puede modificar."))
                .when(quizService).save(eq(exerciseId), any(), eq(actor));

        mvc.perform(quizRequest(exerciseId, 10))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ejercicios/" + exerciseId + "/preguntas"))
                .andExpect(flash().attribute("panelError",
                        "Ya hay intentos de este cuestionario; no se puede modificar."));
    }

    private MockHttpServletRequestBuilder quizRequest(UUID exerciseId, int questionCount) {
        MockHttpServletRequestBuilder request = post("/admin/ejercicios/{id}/preguntas", exerciseId);
        for (int index = 0; index < questionCount; index++) {
            request.param("questions[" + index + "].position", String.valueOf(index + 1));
            request.param("questions[" + index + "].prompt", "Pregunta " + (index + 1));
            request.param("questions[" + index + "].correctIndex", "1");
            for (int option = 0; option < 4; option++) {
                request.param("questions[" + index + "].alternatives[" + option + "].label",
                        "Alternativa " + (index + 1) + "-" + (option + 1));
            }
        }
        return request;
    }
}
