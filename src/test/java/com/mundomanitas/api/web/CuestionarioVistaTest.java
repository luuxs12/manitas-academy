package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Vista del cuestionario del estudiante")
class CuestionarioVistaTest {

    @Test
    @DisplayName("el cuestionario usa fieldsets, nombres por pregunta y resultado con aria-live")
    void cuestionarioPlantillaIsAccessibleAndComplete() throws IOException {
        String html = read("templates/student/ejercicio.html");

        assertThat(html).contains("Saltar al contenido")
                .contains("th:each=\"pregunta : ${cuestionario.questions()}\"")
                .contains("<fieldset class=\"quiz-question\"")
                .contains("<legend")
                .contains("'respuestas[' + ${pregunta.id()} + ']'")
                .contains("th:each=\"alternativa : ${pregunta.alternatives()}\"")
                .contains("type=\"radio\"")
                .contains("Enviar respuestas")
                .contains("aria-live=\"polite\"")
                .contains("'Resultado: ' + ${cuestionario.intento().score()} + ' / 20'")
                .contains("Respuesta correcta")
                .contains("Cuestionario sin preguntas cargadas");
    }

    @Test
    @DisplayName("el temario ofrece rendir el cuestionario y no marcar el ejercicio como visto")
    void ejercicioFilaUsesCuestionarioEnlace() throws IOException {
        String html = read("templates/fragments/syllabus-item.html");

        assertThat(html).contains("item.cuestionarioDone()")
                .contains("'Ver resultado'")
                .contains("'Rendir cuestionario'")
                .contains("/ejercicios/{itemId}")
                .contains("th:if=\"${item.type() != 'EJERCICIO'}\"")
                .contains("class=\"syllabus-mark-form\"");
    }

    @Test
    @DisplayName("los estilos del cuestionario usan las variables existentes y están documentados")
    void cuestionarioStylesUseDesignTokens() throws IOException {
        String css = read("static/css/styles.css");
        String docs = Files.readString(Path.of("docs", "design-system.md"), StandardCharsets.UTF_8);

        assertThat(css).contains(".quiz-card {")
                .contains(".quiz-question {")
                .contains(".quiz-alternative {")
                .contains(".quiz-result {")
                .contains("var(--line, #d0d0d0)");
        assertThat(docs).contains("`.quiz-card`")
                .contains("`.quiz-question`")
                .contains("`templates/student/ejercicio.html`");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
