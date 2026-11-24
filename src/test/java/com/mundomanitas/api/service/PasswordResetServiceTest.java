package com.mundomanitas.api.service;

import com.mundomanitas.api.config.MailProperties;
import com.mundomanitas.api.exception.InvalidResetTokenException;
import com.mundomanitas.api.model.PasswordResetToken;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.PasswordResetTokenRepository;
import com.mundomanitas.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas del enlace de restablecimiento: hash, vencimiento y un solo uso. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Mock
    private UserRepository users;

    @Mock
    private PasswordResetTokenRepository tokens;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mail;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setResetMinutes(60);
        service = new PasswordResetService(
                users,
                tokens,
                passwordEncoder,
                mail,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("El pedido guarda un hash y envía el token original por correo")
    void requestStoresHashAndSendsMail() {
        User user = user();
        when(users.findByEmailIgnoreCase("ana@correo.com")).thenReturn(Optional.of(user));

        service.requestReset("  ana@correo.com ");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).deleteAllForUser(user.getId());
        verify(tokens).save(saved.capture());
        verify(mail).sendPasswordReset(any(User.class), anyString());

        PasswordResetToken token = saved.getValue();
        assertThat(token.getTokenHash()).hasSize(64).doesNotContain("=");
        assertThat(token.getExpiresAt()).isEqualTo(OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)).plusMinutes(60));
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("Un correo desconocido no crea tokens ni envía nada")
    void unknownEmailDoesNothing() {
        when(users.findByEmailIgnoreCase("nadie@correo.com")).thenReturn(Optional.empty());

        service.requestReset("nadie@correo.com");

        verify(tokens, never()).save(any());
        verify(mail, never()).sendPasswordReset(any(), anyString());
    }

    @Test
    @DisplayName("Un token vencido no se puede usar")
    void expiredTokenIsRejected() {
        PasswordResetToken token = new PasswordResetToken(
                user(),
                "hash",
                OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)).minusMinutes(1)
        );
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThat(service.isUsable("token")).isFalse();
        assertThatThrownBy(() -> service.resetPassword("token", "NuevaClave2026"))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    @DisplayName("Un token válido cambia la contraseña y queda marcado como usado")
    void validTokenChangesPasswordOnce() {
        User user = user();
        PasswordResetToken token = new PasswordResetToken(
                user,
                "hash",
                OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)).plusMinutes(10)
        );
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaClave2026")).thenReturn("bcrypt-nuevo");

        service.resetPassword("token", "NuevaClave2026");

        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-nuevo");
        assertThat(token.isUsable(OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)))).isFalse();
    }

    private User user() {
        return new User("ana@correo.com", "bcrypt-viejo", "Ana Quispe");
    }
}
