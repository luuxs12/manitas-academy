package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Diseño de los certificados del estudiante")
class StudentCertificateViewTest {

    @Test
    @DisplayName("el listado muestra certificados emitidos, no matrículas completadas")
    void listUsesIssuedCertificates() throws IOException {
        String list = read("templates/student/certificados.html");

        assertThat(list)
                .contains("th:each=\"certificate : ${certificates}\"")
                .contains("certificate.code()")
                .contains("certificate.issuedAt()")
                .contains("certificate.videoCount()")
                .contains("Emitidos")
                .doesNotContain("enrollment.course.lessons")
                .doesNotContain("enrollment.completedAt")
                .doesNotContain("enrollment.id.toString()");
    }

    @Test
    @DisplayName("la ficha muestra el código real y el video count del curso")
    void detailUsesIssuedCertificate() throws IOException {
        String detail = read("templates/student/certificado.html");

        assertThat(detail)
                .contains("certificate.code()")
                .contains("certificate.issuedAt()")
                .contains("certificate.videoCount()")
                .doesNotContain("course.lessons")
                .doesNotContain("enrollment.id.toString()");
    }

    @Test
    @DisplayName("el listado de cursos solo ofrece certificados vigentes")
    void courseListLinksOnlyActiveCertificates() throws IOException {
        String courses = read("templates/student/cursos.html");

        assertThat(courses)
                .contains("#lists.contains(certificateSlugs, enrollment.course.slug)")
                .contains("Certificado en preparación");
    }

    @Test
    @DisplayName("la ficha del curso solo ofrece certificados vigentes")
    void courseDetailLinksOnlyActiveCertificates() throws IOException {
        String course = read("templates/student/curso.html");

        assertThat(course)
                .contains("#lists.contains(certificateSlugs, enrollment.course.slug)")
                .doesNotContain("th:if=\"${enrollment.completed and currentUser.premium()}\"");
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
