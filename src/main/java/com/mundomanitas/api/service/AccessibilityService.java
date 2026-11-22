package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.AccessibilityForm;
import com.mundomanitas.api.model.AccessibilityPreferences;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.AccessibilityPreferencesRepository;
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
public class AccessibilityService {

    private final AccessibilityPreferencesRepository preferences;

    public AccessibilityService(AccessibilityPreferencesRepository preferences) {
        this.preferences = preferences;
    }

    @Transactional
    public AccessibilityPreferences forUser(User user) {
        UUID userId = user.getId();
        return preferences.findById(userId)
                .orElseGet(() -> preferences.save(new AccessibilityPreferences(userId)));
    }

    @Transactional
    public AccessibilityPreferences save(User user, AccessibilityForm form) {
        AccessibilityPreferences current = forUser(user);
        String variant = form.isSignLanguageEnabled()
                ? nullIfBlank(form.getSignLanguageVariant())
                : null;
        current.apply(
                form.isSignLanguageEnabled(),
                variant,
                form.isCaptionsEnabled(),
                form.getCaptionSize(),
                form.getPlaybackSpeed()
        );
        return preferences.save(current);
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
