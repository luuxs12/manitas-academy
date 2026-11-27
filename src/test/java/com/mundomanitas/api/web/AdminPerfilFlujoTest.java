package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.admin.AdminPerfilController;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** La foto del administrador usa el mismo almacenamiento que la del alumno. */
@DisplayName("Cuenta del administrador")
class AdminPerfilFlujoTest {

    private CuentaService accounts;
    private MockMvc mvc;
    private Usuario admin;
    private UUID actor;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        admin = mock(Usuario.class);
        when(admin.getId()).thenReturn(actor);
        when(admin.getRol()).thenReturn("ADMIN");
        AutenticadoUsuario principal = new AutenticadoUsuario(admin);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));
        SecurityContextHolder.setContext(context);

        AdminNotificacionService notifications = mock(AdminNotificacionService.class);
        when(notifications.reciente(any())).thenReturn(List.of());
        when(notifications.unread(any())).thenReturn(0L);
        accounts = mock(CuentaService.class);
        when(accounts.require(actor)).thenReturn(admin);

        mvc = MockMvcBuilders.standaloneSetup(new AdminPerfilController(notifications, accounts))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("abre la página de perfil del administrador")
    void abrePerfil() throws Exception {
        mvc.perform(get("/admin/perfil"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/perfil"))
                .andExpect(model().attribute("perfil", admin));
    }

    @Test
    @DisplayName("guarda la foto y vuelve a la sección de la foto")
    void guardaFoto() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "foto.png", "image/png", new byte[] {1, 2, 3});

        mvc.perform(multipart("/admin/perfil/foto").file(avatar))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/perfil#foto"))
                .andExpect(flash().attribute("panelOk", "Tu foto de perfil se actualizó."));

        verify(accounts).updateAvatar(eq(admin), any());
    }

    @Test
    @DisplayName("quita la foto y vuelve a las iniciales")
    void quitaFoto() throws Exception {
        mvc.perform(post("/admin/perfil/foto/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/perfil#foto"))
                .andExpect(flash().attribute("panelOk", "Volviste a usar tus iniciales."));

        verify(accounts).removeAvatar(admin);
    }
}
