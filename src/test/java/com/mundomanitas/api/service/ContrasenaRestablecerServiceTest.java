package com.mundomanitas.api.service;

import com.mundomanitas.api.config.CorreoProperties;
import com.mundomanitas.api.exception.InvalidoRestablecerTokenException;
import com.mundomanitas.api.model.ContrasenaRestablecerToken;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContrasenaRestablecerTokenRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
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
class ContrasenaRestablecerServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Mock
    private UsuarioRepository users;

    @Mock
    private ContrasenaRestablecerTokenRepository tokens;

    @Mock
    private PasswordEncoder contrasenaEncoder;

    @Mock
    private CorreoService correo;

    private ContrasenaRestablecerService service;

    @BeforeEach
    void setUp() {
        CorreoProperties properties = new CorreoProperties();
        properties.setRestablecerMinutes(60);
        service = new ContrasenaRestablecerService(
                users,
                tokens,
                contrasenaEncoder,
                correo,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("El pedido guarda un hash y envía el token original por correo")
    void requestStoresHashAndSendsCorreo() {
        Usuario usuario = usuario();
        when(users.buscarPorEmailIgnorandoMayusculas("ana@correo.com")).thenReturn(Optional.of(usuario));

        service.requestRestablecer("  ana@correo.com ");

        ArgumentCaptor<ContrasenaRestablecerToken> saved = ArgumentCaptor.forClass(ContrasenaRestablecerToken.class);
        verify(tokens).deleteAllForUsuario(usuario.getId());
        verify(tokens).save(saved.capture());
        verify(correo).sendContrasenaRestablecer(any(Usuario.class), anyString());

        ContrasenaRestablecerToken token = saved.getValue();
        assertThat(token.getTokenHash()).hasSize(64).doesNotContain("=");
        assertThat(token.getFechaExpiracion()).isEqualTo(OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)).plusMinutes(60));
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("Un correo desconocido no crea tokens ni envía nada")
    void unknownEmailDoesNothing() {
        when(users.buscarPorEmailIgnorandoMayusculas("nadie@correo.com")).thenReturn(Optional.empty());

        service.requestRestablecer("nadie@correo.com");

        verify(tokens, never()).save(any());
        verify(correo, never()).sendContrasenaRestablecer(any(), anyString());
    }

    @Test
    @DisplayName("Un token vencido no se puede usar")
    void expiredTokenIsRejected() {
        ContrasenaRestablecerToken token = new ContrasenaRestablecerToken(
                usuario(),
                "hash",
                OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)).minusMinutes(1)
        );
        when(tokens.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThat(service.isUsable("token")).isFalse();
        assertThatThrownBy(() -> service.restablecerContrasena("token", "NuevaClave2026"))
                .isInstanceOf(InvalidoRestablecerTokenException.class);
    }

    @Test
    @DisplayName("Un token válido cambia la contraseña y queda marcado como usado")
    void validTokenChangesContrasenaOnce() {
        Usuario usuario = usuario();
        ContrasenaRestablecerToken token = new ContrasenaRestablecerToken(
                usuario,
                "hash",
                OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)).plusMinutes(10)
        );
        when(tokens.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));
        when(contrasenaEncoder.encode("NuevaClave2026")).thenReturn("bcrypt-nuevo");

        service.restablecerContrasena("token", "NuevaClave2026");

        assertThat(usuario.getContrasenaHash()).isEqualTo("bcrypt-nuevo");
        assertThat(token.isUsable(OffsetDateTime.now(Clock.fixed(NOW, ZoneOffset.UTC)))).isFalse();
    }

    private Usuario usuario() {
        return new Usuario("ana@correo.com", "bcrypt-viejo", "Ana Quispe");
    }
}
