package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.AccesibilidadFormulario;
import com.mundomanitas.api.model.AccesibilidadPreferencias;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.AccesibilidadPreferenciasRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Preferencias de accesibilidad")
class AccesibilidadServiceTest {

    @Mock private AccesibilidadPreferenciasRepository preferencias;
    @Mock private UsuarioRepository users;

    @InjectMocks private AccesibilidadService service;

    @Test
    @DisplayName("la fila inicial respeta los valores por defecto y el LSP de la cuenta")
    void initializesFromAccount() {
        Usuario usuario = usuario();
        usuario.changeUsaLsp(true);
        when(preferencias.findById(usuario.getId())).thenReturn(Optional.empty());
        when(preferencias.save(any(AccesibilidadPreferencias.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccesibilidadPreferencias actuales = service.forUsuario(usuario);

        assertThat(actuales.isSignLanguageEnabled()).isTrue();
        assertThat(actuales.isCaptionsEnabled()).isTrue();
        assertThat(actuales.getCaptionSize()).isEqualTo("MEDIUM");
        assertThat(actuales.getReproduccionSpeed()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("guardar preferencias mantiene iguales la cuenta y la preferencia LSP")
    void keepsAccountAndPreferenceInSync() {
        Usuario usuario = usuario();
        AccesibilidadPreferencias actuales = new AccesibilidadPreferencias(usuario.getId(), false);
        when(preferencias.findById(usuario.getId())).thenReturn(Optional.of(actuales));
        when(preferencias.save(actuales)).thenReturn(actuales);
        AccesibilidadFormulario formulario = new AccesibilidadFormulario();
        formulario.setSignLanguageEnabled(true);
        formulario.setSignLanguageVariant("LSP");
        formulario.setCaptionsEnabled(true);
        formulario.setCaptionSize("LARGE");
        formulario.setReproduccionSpeed(BigDecimal.valueOf(1.25));

        AccesibilidadPreferencias guardadas = service.save(usuario, formulario);

        assertThat(guardadas.isSignLanguageEnabled()).isTrue();
        assertThat(guardadas.getCaptionSize()).isEqualTo("LARGE");
        assertThat(usuario.isUsaLsp()).isTrue();
        verify(users).save(usuario);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario("ana@example.com", "hash", "Ana Quispe");
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }
}
