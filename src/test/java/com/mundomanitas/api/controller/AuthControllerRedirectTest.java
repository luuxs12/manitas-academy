package com.mundomanitas.api.controller;

import com.mundomanitas.api.service.AuthService;
import com.mundomanitas.api.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Al volver a una pantalla de acceso, cada rol cae en su propio panel. */
@DisplayName("Redirección de acceso según el rol")
class AuthControllerRedirectTest {

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                mock(AuthService.class),
                mock(AuthenticationManager.class),
                mock(MailService.class)
        );
    }

    @Test
    @DisplayName("un administrador con sesión vuelve al panel administrativo")
    void adminReturnsToAdminPanel() {
        assertThat(controller.login(auth("ADMIN"), null, null, null, new ExtendedModelMap()))
                .isEqualTo("redirect:/admin");
        assertThat(controller.register(auth("ADMIN"), new ExtendedModelMap()))
                .isEqualTo("redirect:/admin");
    }

    @Test
    @DisplayName("un estudiante con sesión vuelve a su panel")
    void studentReturnsToStudentPanel() {
        assertThat(controller.login(auth("STUDENT"), null, null, null, new ExtendedModelMap()))
                .isEqualTo("redirect:/panel");
        assertThat(controller.register(auth("STUDENT"), new ExtendedModelMap()))
                .isEqualTo("redirect:/panel");
    }

    private static Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                "usuario", "clave", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
