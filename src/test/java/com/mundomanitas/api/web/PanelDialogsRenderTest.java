package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Render real del fragmento: el mensaje termina dentro del modal, no en la página. */
@SpringBootTest
@DisplayName("Render de los modales del panel")
class PanelDialogsRenderTest {

    @Autowired private SpringTemplateEngine engine;

    @Test
    @DisplayName("el aviso de éxito se renderiza con check animado y región viva")
    void successDialogRendersRealMessage() {
        Context context = new Context();
        context.setVariable("panelOk", "El curso se creó correctamente.");

        String html = engine.process("fragments/panel-dialogs", Set.of("notices"), context);

        assertThat(html).contains("id=\"success-dialog\"")
                .contains("El curso se creó correctamente.")
                .contains("class=\"ui-check\"")
                .contains("role=\"status\"")
                .contains("aria-live=\"polite\"")
                .doesNotContain("id=\"error-dialog\"");
    }

    @Test
    @DisplayName("el error se renderiza en su propio modal con aviso asertivo")
    void errorDialogRendersRealMessage() {
        Context context = new Context();
        context.setVariable("panelError", "Elegí una persona y un curso válidos.");

        String html = engine.process("fragments/panel-dialogs", Set.of("notices"), context);

        assertThat(html).contains("id=\"error-dialog\"")
                .contains("Elegí una persona y un curso válidos.")
                .contains("role=\"alert\"")
                .contains("aria-live=\"assertive\"")
                .doesNotContain("id=\"success-dialog\"");
    }

    @Test
    @DisplayName("sin mensajes no se renderiza ningún modal de aviso")
    void noMessageMeansNoDialog() {
        String html = engine.process("fragments/panel-dialogs", Set.of("notices"), new Context());

        assertThat(html).doesNotContain("success-dialog").doesNotContain("error-dialog");
    }

    @Test
    @DisplayName("la confirmación abre un diálogo vacío que el script llena con datos reales")
    void confirmDialogIsAnEmptyShell() {
        String html = engine.process("fragments/panel-dialogs", Set.of("confirm"), new Context());

        assertThat(html).contains("id=\"confirm-dialog\"")
                .contains("data-confirm-body")
                .contains("data-confirm-details")
                .contains("data-dialog-close")
                .contains("aria-labelledby=\"confirm-dialog-title\"")
                .contains("aria-describedby=\"confirm-dialog-message\"");
    }
}
