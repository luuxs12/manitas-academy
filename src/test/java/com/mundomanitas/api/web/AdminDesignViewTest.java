package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Diseño del panel administrativo")
class AdminDesignViewTest {

    @Test
    @DisplayName("el temario admin usa una fila ordenada y comparte su encabezado de módulo")
    void adminContentUsesOneOrderedRow() throws IOException {
        String content = read("templates/admin/contenido.html");
        String row = read(Path.of("src", "main", "resources", "templates", "fragments",
                "admin-content-item.html"));
        String css = read("static/css/styles.css");

        assertThat(content.split(java.util.regex.Pattern.quote(
                "~{fragments/admin-content-item :: row(${item}, ${section})}"), -1)).hasSize(3);
        assertThat(content).doesNotContain("class=\"admin-content-item\" th:each=\"item")
                .contains("class=\"admin-module-number\"")
                .contains("class=\"admin-module-name\"")
                .contains("class=\"admin-status\"");

        assertThat(indexOf(row, "class=\"admin-content-top\""))
                .isLessThan(indexOf(row, "<strong"));
        assertThat(indexOf(row, "<strong")).isLessThan(indexOf(row, "<small"));
        assertThat(indexOf(row, "<small")).isLessThan(indexOf(row, "class=\"admin-row-actions\""));
        assertThat(row).contains("th:fragment=\"row(item, section)\"")
                .contains("<small th:if=\"${section == null}\">Sin sección</small>")
                .contains("th:attr=\"aria-label='Subir ' + ${item.title()}\"")
                .contains("class=\"admin-confirm-link\"");

        assertThat(css).contains(".admin-module-number {")
                .contains(".admin-module-name {")
                .contains(".admin-module-head .admin-status {")
                .contains(".admin-content-top {")
                .contains(".admin-row-actions {")
                .contains("overflow-wrap: anywhere;");
    }

    @Test
    @DisplayName("el listado admin separa el nombre del curso de sus acciones")
    void adminCourseListGroupsCopyAndActions() throws IOException {
        String courses = read("templates/admin/cursos.html");
        String css = read("static/css/styles.css");

        assertThat(indexOf(courses, "class=\"admin-actions\""))
                .isLessThan(indexOf(courses, "admin-table-wrap"));
        assertThat(indexOf(courses, "admin-table-wrap"))
                .isLessThan(indexOf(courses, "class=\"admin-course-copy\""));
        assertThat(courses).contains("class=\"admin-course-copy\"")
                .contains("<strong th:text=\"${course.title()}\">")
                .contains("<small th:text=\"${course.slug()}\">");

        assertThat(css).contains(".admin-course-copy {")
                .contains(".admin-course-copy strong {")
                .contains(".admin-course-copy small {")
                .contains(".admin-page-head > div {");
    }

    @Test
    @DisplayName("el admin puede ver el certificado con el mismo documento que ve el alumno")
    void adminCanPreviewTheCertificate() throws IOException {
        String list = read("templates/admin/certificados.html");
        String adminDetail = read("templates/admin/certificado.html");
        String studentDetail = read("templates/student/certificado.html");
        String document = read(Path.of("src", "main", "resources", "templates",
                "fragments", "certificate-document.html"));
        String css = read("static/css/styles.css");

        assertThat(list).contains("@{/admin/certificados/{id}/ver(id=${item.id()})}")
                .contains("aria-label='Ver certificado ' + ${item.code()}");

        assertThat(adminDetail).contains("~{fragments/certificate-document :: document(")
                .contains("admin-certificate-page")
                .contains("admin-cert-status")
                .doesNotContain("<article id=\"certificado\"");
        assertThat(studentDetail).contains("~{fragments/certificate-document :: document(")
                .doesNotContain("<article id=\"certificado\"");

        assertThat(document)
                .contains("th:fragment=\"document(person, courseTitle, videoCount, issuedAt, code)\"")
                .contains("<article id=\"certificado\" class=\"certificate\"")
                .contains("th:text=\"${person}\"")
                .contains("th:text=\"${courseTitle}\"")
                .contains("th:text=\"${videoCount} + ' videos'\"")
                .contains("th:text=\"${#temporals.format(issuedAt, 'dd MMMM yyyy')}\"")
                .contains("th:text=\"${code}\"");
        assertThat(css).contains(".admin-certificate-page {")
                .contains(".admin-cert-status {");
    }

    private static int indexOf(String text, String needle) {
        int position = text.indexOf(needle);
        assertThat(position).as("fragmento %s", needle).isNotNegative();
        return position;
    }

    private String read(Path path) throws IOException {
        assertThat(path).exists();
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private String read(String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
