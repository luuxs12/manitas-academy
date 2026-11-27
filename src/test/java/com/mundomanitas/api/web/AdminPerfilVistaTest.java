package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.UsuarioActual;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La cuenta del admin tiene perfil propio, foto y las mismas acciones del panel. */
@SpringBootTest
@DisplayName("Perfil y acciones del panel administrativo")
class AdminPerfilVistaTest {

    @Autowired private SpringTemplateEngine engine;

    @Test
    @DisplayName("el perfil del admin renderiza la foto, el selector y el menú de cuenta")
    void adminPerfilRenderiza() {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication
                .buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext),
                        new MockHttpServletResponse()));
        context.setVariable("usuarioActual", new UsuarioActual(
                "Luis Fernando", "admin@damianprint.com", Plan.FREE, null, "light", "ADMIN"));
        context.setVariable("activo", "perfil");
        context.setVariable("adminEvents", List.of());
        context.setVariable("adminUnread", 0L);

        Usuario perfil = mock(Usuario.class);
        when(perfil.getNombreCompleto()).thenReturn("Luis Fernando");
        when(perfil.getEmail()).thenReturn("admin@damianprint.com");
        when(perfil.getRol()).thenReturn("ADMIN");
        context.setVariable("perfil", perfil);

        String html = engine.process("admin/perfil", context);

        assertThat(html).contains("Mi perfil")
                .contains("Foto de perfil")
                .contains("action=\"/admin/perfil/foto\"")
                .contains("type=\"file\"")
                .contains("class=\"admin-file-drop\"")
                .contains("Mi perfil</span>")
                .contains("Cerrar sesión</span>")
                .doesNotContain("Quitar foto");
    }

    @Test
    @DisplayName("cada fila del temario abre un modal con una sola acción destructiva")
    void temarioUsaUnSoloMenuDeAcciones() throws Exception {
        String contenido = new String(getClass().getResourceAsStream(
                "/templates/admin/contenido.html").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String fila = new String(getClass().getResourceAsStream(
                "/templates/fragments/admin-content-item.html").readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(contenido).contains("data-actions-trigger")
                .contains("class=\"admin-action-menu\" data-actions")
                .contains("data-action-label=\"Editar módulo\"")
                .contains("data-action-label=\"Eliminar sección\"")
                .contains("data-confirm-form");
        assertThat(fila).contains("data-actions-trigger")
                .contains("class=\"admin-action-menu\" data-actions")
                .contains("data-action-label=\"Eliminar\"")
                .contains("data-confirm-form");
        assertThat(fila.split("admin-confirm-link", -1)).hasSize(2);
    }
}
