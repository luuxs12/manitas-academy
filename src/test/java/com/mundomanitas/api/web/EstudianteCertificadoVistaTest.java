package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Diseño de los certificados del estudiante")
class EstudianteCertificadoVistaTest {

    @Test
    @DisplayName("el listado muestra certificados emitidos, no matrículas completadas")
    void listUsesEmitidoCertificates() throws IOException {
        String list = read("templates/student/certificados.html");

        assertThat(list)
                .contains("th:each=\"certificado : ${certificates}\"")
                .contains("certificado.code()")
                .contains("certificado.fechaEmision()")
                .contains("certificado.videoContar()")
                .contains("Emitidos")
                .doesNotContain("enrollment.course.lessons")
                .doesNotContain("enrollment.completedAt")
                .doesNotContain("enrollment.id.toString()");
    }

    @Test
    @DisplayName("la ficha muestra el código real y el video count del curso")
    void detailUsesEmitidoCertificado() throws IOException {
        String detail = read("templates/student/certificado.html");

        assertThat(detail)
                .contains("certificado.code()")
                .contains("certificado.fechaEmision()")
                .contains("certificado.videoContar()")
                .doesNotContain("course.lessons")
                .doesNotContain("enrollment.id.toString()");
    }

    @Test
    @DisplayName("el listado de cursos solo ofrece certificados vigentes")
    void cursoListEnlacesOnlyActivoCertificates() throws IOException {
        String courses = read("templates/student/cursos.html");

        assertThat(courses)
                .contains("#lists.contains(certificadoSlugs, inscripcion.curso.slug)")
                .contains("Certificado en preparación");
    }

    @Test
    @DisplayName("la ficha del curso solo ofrece certificados vigentes")
    void cursoDetailEnlacesOnlyActivoCertificates() throws IOException {
        String curso = read("templates/student/curso.html");

        assertThat(curso)
                .contains("#lists.contains(certificadoSlugs, inscripcion.curso.slug)")
                .doesNotContain("th:if=\"${inscripcion.finalizado and usuarioActual.premium()}\"");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
