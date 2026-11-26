package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.RegistroFormulario;
import com.mundomanitas.api.exception.DuplicadoEmailException;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
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
    private UsuarioRepository users;

    @Mock
    private PasswordEncoder contrasenaEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(users, contrasenaEncoder);
    }

    @Test
    @DisplayName("normaliza el correo y guarda la contraseña cifrada")
    void registersNormalizedCuenta() {
        RegistroFormulario formulario = formulario("  ANA@Correo.COM  ", "  Ana Quispe  ");
        when(contrasenaEncoder.encode("ClaveSegura2026")).thenReturn("bcrypt-nuevo");
        when(users.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario saved = service.registro(formulario);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(users).existePorEmailIgnorandoMayusculas("ana@correo.com");
        verify(users).save(captor.capture());
        assertThat(saved).isSameAs(captor.getValue());
        assertThat(saved.getEmail()).isEqualTo("ana@correo.com");
        assertThat(saved.getNombreCompleto()).isEqualTo("Ana Quispe");
        assertThat(saved.getContrasenaHash()).isEqualTo("bcrypt-nuevo");
        assertThat(saved.getRol()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("no guarda dos cuentas con el mismo correo")
    void rejectsDuplicadoEmail() {
        when(users.existePorEmailIgnorandoMayusculas("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registro(formulario("ana@correo.com", "Ana Quispe")))
                .isInstanceOf(DuplicadoEmailException.class);

        verify(users, never()).save(any());
        verify(contrasenaEncoder, never()).encode(any());
    }

    private RegistroFormulario formulario(String email, String nombreCompleto) {
        RegistroFormulario formulario = new RegistroFormulario();
        formulario.setEmail(email);
        formulario.setNombreCompleto(nombreCompleto);
        formulario.setContrasena("ClaveSegura2026");
        formulario.setConfirmContrasena("ClaveSegura2026");
        formulario.setAcceptTerms(true);
        return formulario;
    }
}
