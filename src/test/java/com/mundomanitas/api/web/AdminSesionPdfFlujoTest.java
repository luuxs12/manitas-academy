package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminSesionPdfController;
import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminSesionPdfService;
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
class AdminSesionPdfFlujoTest {

    private AdminSesionPdfService sessions;
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
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        sessions = mock(AdminSesionPdfService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminSesionPdfController(sessions))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("la descarga devuelve el PDF con la sesión del actor")
    void sesionPdfDescarga() throws Exception {
        UUID moduloId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
        when(sessions.moduloPdf(moduloId, actor))
                .thenReturn(new ResultadoExportacion(pdf, "mundo-manitas-sesion.pdf", "application/pdf"));

        mvc.perform(get("/admin/exportar/sesion/modulos/{moduleId}/pdf", moduloId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"mundo-manitas-sesion.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));

        verify(sessions).moduloPdf(moduloId, actor);
    }

    @Test
    @DisplayName("una sesión inexistente responde 404")
    void missingSesion() throws Exception {
        UUID moduloId = UUID.randomUUID();
        when(sessions.moduloPdf(moduloId, actor))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El módulo solicitado no existe."));

        mvc.perform(get("/admin/exportar/sesion/modulos/{moduleId}/pdf", moduloId))
                .andExpect(status().isNotFound());
    }
}
