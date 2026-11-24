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

/** Al crear una cuenta con sesión iniciada, cada rol cae en su panel. */
@DisplayName("Redirección de registro según el rol")
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
        assertThat(controller.register(auth("ADMIN"), new ExtendedModelMap()))
                .isEqualTo("redirect:/admin");
    }

    @Test
    @DisplayName("un estudiante con sesión vuelve a su panel")
    void studentReturnsToStudentPanel() {
        assertThat(controller.register(auth("STUDENT"), new ExtendedModelMap()))
                .isEqualTo("redirect:/panel");
    }

    @Test
    @DisplayName("la vieja URL /login no muestra pantalla: vuelve al modal o al panel")
    void legacyLoginNeverRendersPage() {
        assertThat(controller.legacyLogin(null)).isEqualTo("redirect:/?login");
        assertThat(controller.legacyLogin(auth("ADMIN"))).isEqualTo("redirect:/admin");
        assertThat(controller.legacyLogin(auth("STUDENT"))).isEqualTo("redirect:/panel");
    }

    private static Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                "usuario", "clave", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
