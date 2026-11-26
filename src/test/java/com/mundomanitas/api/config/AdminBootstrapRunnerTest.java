package com.mundomanitas.api.config;

import com.mundomanitas.api.service.admin.AdminBootstrapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** El arranque solo crea la cuenta cuando hay una clave configurada. */
@DisplayName("Arranque de la cuenta administradora")
class AdminBootstrapRunnerTest {

    private final AdminBootstrapService bootstrap = mock(AdminBootstrapService.class);

    @Test
    @DisplayName("crea o promueve el admin con la clave del entorno")
    void runsWithConfiguredPassword() {
        new AdminBootstrapRunner(bootstrap, "admin@damianprint.com", "Clave12345!").run(null);

        verify(bootstrap).ensureAdmin("admin@damianprint.com", "Clave12345!");
    }

    @Test
    @DisplayName("no hace nada si no hay clave configurada")
    void skipsWithoutPassword() {
        new AdminBootstrapRunner(bootstrap, "admin@damianprint.com", "   ").run(null);

        verifyNoInteractions(bootstrap);
    }
}
