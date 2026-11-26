package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminSessionPdfController;
import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminSessionPdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Descarga del PDF de una sesión desde el panel")
class AdminSessionPdfFlowTest {

    private AdminSessionPdfService sessions;
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
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        sessions = mock(AdminSessionPdfService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminSessionPdfController(sessions))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("la descarga devuelve el PDF con la sesión del actor")
    void sessionPdfDownload() throws Exception {
        UUID moduleId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
        when(sessions.modulePdf(moduleId, actor))
                .thenReturn(new ExportResult(pdf, "mundo-manitas-sesion.pdf", "application/pdf"));

        mvc.perform(get("/admin/exportar/sesion/modulos/{moduleId}/pdf", moduleId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"mundo-manitas-sesion.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));

        verify(sessions).modulePdf(moduleId, actor);
    }

    @Test
    @DisplayName("una sesión inexistente responde 404")
    void missingSession() throws Exception {
        UUID moduleId = UUID.randomUUID();
        when(sessions.modulePdf(moduleId, actor))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El módulo solicitado no existe."));

        mvc.perform(get("/admin/exportar/sesion/modulos/{moduleId}/pdf", moduleId))
                .andExpect(status().isNotFound());
    }
}
