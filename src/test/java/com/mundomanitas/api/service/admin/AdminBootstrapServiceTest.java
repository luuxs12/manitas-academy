package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** La cuenta administradora se crea una vez y no se sobrescribe en cada arranque. */
@DisplayName("Bootstrap de la cuenta administradora")
class AdminBootstrapServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AdminBootstrapService service = new AdminBootstrapServiceImpl(users, encoder);

    @Test
    @DisplayName("crea el admin con el correo normalizado y la clave cifrada")
    void createsAdminWhenMissing() {
        when(users.findByEmailIgnoreCase("admin@damianprint.com")).thenReturn(Optional.empty());
        when(encoder.encode("Clave12345!")).thenReturn("hash-bcrypt");

        service.ensureAdmin(" ADMIN@DamianPrint.com ", "Clave12345!");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@damianprint.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(saved.getValue().getFullName()).isEqualTo("Administrador");
        assertThat(saved.getValue().getRole()).isEqualTo("ADMIN");
        assertThat(saved.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("promueve una cuenta existente sin cambiar su clave")
    void promotesExistingStudent() {
        User existing = new User("admin@damianprint.com", "hash-viejo", "Administrador");
        when(users.findByEmailIgnoreCase("admin@damianprint.com")).thenReturn(Optional.of(existing));

        service.ensureAdmin("admin@damianprint.com", "Clave12345!");

        verify(users).save(existing);
        assertThat(existing.getRole()).isEqualTo("ADMIN");
        assertThat(existing.getPasswordHash()).isEqualTo("hash-viejo");
        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("no toca una cuenta que ya es administradora")
    void leavesExistingAdminUntouched() {
        User existing = new User("admin@damianprint.com", "hash-viejo", "Administrador");
        existing.changeRole("ADMIN");
        when(users.findByEmailIgnoreCase("admin@damianprint.com")).thenReturn(Optional.of(existing));

        service.ensureAdmin("admin@damianprint.com", "OtraClave123!");

        verify(users, never()).save(any(User.class));
        assertThat(existing.getPasswordHash()).isEqualTo("hash-viejo");
        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("rechaza una contraseña vacía antes de tocar la base")
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> service.ensureAdmin("admin@damianprint.com", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La contraseña del administrador no puede estar vacía.");
        verifyNoInteractions(users, encoder);
    }

    @Test
    @DisplayName("rechaza un correo vacío antes de tocar la base")
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> service.ensureAdmin("  ", "Clave12345!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El correo del administrador no puede estar vacío.");
        verifyNoInteractions(users, encoder);
    }

}
