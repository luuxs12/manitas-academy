package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Usuario actual de la sesión")
class ActualUsuarioServiceTest {

    @Mock
    private UsuarioRepository users;

    @Mock
    private NotificacionService notifications;

    @Mock
    private Authentication authentication;

    @Mock
    private AutenticadoUsuario principal;

    private ActualUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new ActualUsuarioServiceImpl(users, notifications);
    }

    @Test
    @DisplayName("sin autenticación no consulta la base")
    void anonymousDoesNotQueryTheBase() {
        ActualUsuarioService.SesionVista view = service.load(null);

        assertThat(view.usuarioActual()).isNull();
        assertThat(view.unreadContar()).isZero();
        assertThat(view.recienteNotifications()).isEmpty();
        verifyNoInteractions(users, notifications);
    }

    @Test
    @DisplayName("con sesión carga cuenta y avisos una sola vez")
    void autenticadoLoadsCuentaAndNotifications() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("ana@correo.com", "bcrypt", "Ana Quispe");
        when(principal.getId()).thenReturn(id);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(users.findById(id)).thenReturn(Optional.of(usuario));
        when(notifications.unreadContar(usuario)).thenReturn(4L);
        when(notifications.reciente(usuario)).thenReturn(List.of());

        ActualUsuarioService.SesionVista view = service.load(authentication);

        assertThat(view.usuarioActual()).isNotNull();
        assertThat(view.usuarioActual().email()).isEqualTo("ana@correo.com");
        assertThat(view.usuarioActual().nombreCompleto()).isEqualTo("Ana Quispe");
        assertThat(view.unreadContar()).isEqualTo(4);
        assertThat(view.recienteNotifications()).isEmpty();
    }

    @Test
    @DisplayName("si la cuenta ya no existe devuelve una sesión vacía")
    void missingCuentaReturnsEmptySesion() {
        UUID id = UUID.randomUUID();
        when(principal.getId()).thenReturn(id);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(users.findById(id)).thenReturn(Optional.empty());

        ActualUsuarioService.SesionVista view = service.load(authentication);

        assertThat(view.usuarioActual()).isNull();
        assertThat(view.unreadContar()).isZero();
        assertThat(view.recienteNotifications()).isEmpty();
    }
}
