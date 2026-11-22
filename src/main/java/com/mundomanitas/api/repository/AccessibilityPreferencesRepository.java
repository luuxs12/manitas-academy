package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.AccessibilityPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessibilityPreferencesRepository
        extends JpaRepository<AccessibilityPreferences, UUID> {
}
