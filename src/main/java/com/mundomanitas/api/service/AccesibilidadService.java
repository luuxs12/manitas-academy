package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.AccesibilidadFormulario;
import com.mundomanitas.api.model.AccesibilidadPreferencias;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.AccesibilidadPreferenciasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Preferencias de accesibilidad guardadas en PostgreSQL desde V1.
 *
 * <p>Si la persona nunca las abrió, se crea la fila con los valores
 * iniciales del esquema. No hay preferencias "de relleno".</p>
 */
@Service
public class AccesibilidadService {

    private final AccesibilidadPreferenciasRepository preferencias;

    public AccesibilidadService(AccesibilidadPreferenciasRepository preferencias) {
        this.preferencias = preferencias;
    }

    @Transactional
    public AccesibilidadPreferencias forUsuario(Usuario usuario) {
        UUID usuarioId = usuario.getId();
        return preferencias.findById(usuarioId)
                .orElseGet(() -> preferencias.save(new AccesibilidadPreferencias(usuarioId)));
    }

    @Transactional
    public AccesibilidadPreferencias save(Usuario usuario, AccesibilidadFormulario formulario) {
        AccesibilidadPreferencias actual = forUsuario(usuario);
        String variant = formulario.isSignLanguageEnabled()
                ? nullIfBlank(formulario.getSignLanguageVariant())
                : null;
        actual.apply(
                formulario.isSignLanguageEnabled(),
                variant,
                formulario.isCaptionsEnabled(),
                formulario.getCaptionSize(),
                formulario.getReproduccionSpeed()
        );
        return preferencias.save(actual);
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
