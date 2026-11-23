package com.mundomanitas.api.service;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
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
class CurrentUserServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private NotificationService notifications;

    @Mock
    private Authentication authentication;

    @Mock
    private AuthenticatedUser principal;

    private CurrentUserService service;

    @BeforeEach
    void setUp() {
        service = new CurrentUserServiceImpl(users, notifications);
    }

    @Test
    @DisplayName("sin autenticación no consulta la base")
    void anonymousDoesNotQueryTheDatabase() {
        CurrentUserService.SessionView view = service.load(null);

        assertThat(view.currentUser()).isNull();
        assertThat(view.unreadCount()).isZero();
        assertThat(view.recentNotifications()).isEmpty();
        verifyNoInteractions(users, notifications);
    }

    @Test
    @DisplayName("con sesión carga cuenta y avisos una sola vez")
    void authenticatedLoadsAccountAndNotifications() {
        UUID id = UUID.randomUUID();
        User user = new User("ana@correo.com", "bcrypt", "Ana Quispe");
        when(principal.getId()).thenReturn(id);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(notifications.unreadCount(user)).thenReturn(4L);
        when(notifications.recent(user)).thenReturn(List.of());

        CurrentUserService.SessionView view = service.load(authentication);

        assertThat(view.currentUser()).isNotNull();
        assertThat(view.currentUser().email()).isEqualTo("ana@correo.com");
        assertThat(view.currentUser().fullName()).isEqualTo("Ana Quispe");
        assertThat(view.unreadCount()).isEqualTo(4);
        assertThat(view.recentNotifications()).isEmpty();
    }

    @Test
    @DisplayName("si la cuenta ya no existe devuelve una sesión vacía")
    void missingAccountReturnsEmptySession() {
        UUID id = UUID.randomUUID();
        when(principal.getId()).thenReturn(id);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(users.findById(id)).thenReturn(Optional.empty());

        CurrentUserService.SessionView view = service.load(authentication);

        assertThat(view.currentUser()).isNull();
        assertThat(view.unreadCount()).isZero();
        assertThat(view.recentNotifications()).isEmpty();
    }
}
