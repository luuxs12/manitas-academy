package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.RegisterForm;
import com.mundomanitas.api.exception.DuplicateEmailException;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Registro de cuentas")
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(users, passwordEncoder);
    }

    @Test
    @DisplayName("normaliza el correo y guarda la contraseña cifrada")
    void registersNormalizedAccount() {
        RegisterForm form = form("  ANA@Correo.COM  ", "  Ana Quispe  ");
        when(passwordEncoder.encode("ClaveSegura2026")).thenReturn("bcrypt-nuevo");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.register(form);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).existsByEmailIgnoreCase("ana@correo.com");
        verify(users).save(captor.capture());
        assertThat(saved).isSameAs(captor.getValue());
        assertThat(saved.getEmail()).isEqualTo("ana@correo.com");
        assertThat(saved.getFullName()).isEqualTo("Ana Quispe");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-nuevo");
        assertThat(saved.getRole()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("no guarda dos cuentas con el mismo correo")
    void rejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(form("ana@correo.com", "Ana Quispe")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(users, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    private RegisterForm form(String email, String fullName) {
        RegisterForm form = new RegisterForm();
        form.setEmail(email);
        form.setFullName(fullName);
        form.setPassword("ClaveSegura2026");
        form.setConfirmPassword("ClaveSegura2026");
        form.setAcceptTerms(true);
        return form;
    }
}
