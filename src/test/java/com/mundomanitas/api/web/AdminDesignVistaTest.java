package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Diseño del panel administrativo")
class AdminDesignVistaTest {

    @Test
    @DisplayName("el temario admin usa una fila ordenada y comparte su encabezado de módulo")
    void adminContenidoUsesOneOrderedFila() throws IOException {
        String content = read("templates/admin/contenido.html");
        String row = read(Path.of("src", "main", "resources", "templates", "fragments",
                "admin-content-item.html"));
        String css = read("static/css/styles.css");

        assertThat(content.split(java.util.regex.Pattern.quote(
                "~{fragments/admin-content-item :: row(${item}, ${seccion})}"), -1)).hasSize(3);
        assertThat(content).doesNotContain("class=\"admin-content-item\" th:each=\"item")
                .contains("class=\"admin-module-number\"")
                .contains("class=\"admin-module-name\"")
                .contains("class=\"admin-status\"");

        assertThat(indexOf(row, "class=\"admin-content-top\""))
                .isLessThan(indexOf(row, "<strong"));
        assertThat(indexOf(row, "<strong")).isLessThan(indexOf(row, "<small"));
        assertThat(indexOf(row, "<small")).isLessThan(indexOf(row, "class=\"admin-row-actions\""));
        assertThat(row).contains("th:fragment=\"row(item, section)\"")
                .contains("<small th:if=\"${seccion == null}\">Sin sección</small>")
                .contains("th:attr=\"aria-label='Subir ' + ${item.titulo()}\"")
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
    void adminCursoListGroupsCopyAndActions() throws IOException {
        String courses = read("templates/admin/cursos.html");
        String css = read("static/css/styles.css");

        assertThat(indexOf(courses, "class=\"admin-actions\""))
                .isLessThan(indexOf(courses, "admin-table-wrap"));
        assertThat(indexOf(courses, "admin-table-wrap"))
                .isLessThan(indexOf(courses, "class=\"admin-course-copy\""));
        assertThat(courses).contains("class=\"admin-course-copy\"")
                .contains("<strong th:text=\"${curso.titulo()}\">")
                .contains("<small th:text=\"${curso.slug()}\">");

        assertThat(css).contains(".admin-course-copy {")
                .contains(".admin-course-copy strong {")
                .contains(".admin-course-copy small {")
                .contains(".admin-page-head > div {");
    }

    @Test
    @DisplayName("el admin puede ver el certificado con el mismo documento que ve el alumno")
    void adminCanVistapreviaTheCertificado() throws IOException {
        String list = read("templates/admin/certificados.html");
        String adminDetail = read("templates/admin/certificado.html");
        String estudianteDetail = read("templates/student/certificado.html");
        String document = read(Path.of("src", "main", "resources", "templates",
                "fragments", "certificate-document.html"));
        String css = read("static/css/styles.css");

        assertThat(list).contains("@{/admin/certificados/{id}/ver(id=${item.id()})}")
                .contains("aria-label='Ver certificado ' + ${item.code()}");

        assertThat(adminDetail).contains("~{fragments/certificate-document :: document(")
                .contains("admin-certificate-page")
                .contains("admin-cert-status")
                .doesNotContain("<article id=\"certificado\"");
        assertThat(estudianteDetail).contains("~{fragments/certificate-document :: document(")
                .doesNotContain("<article id=\"certificado\"");

        assertThat(document)
                .contains("th:fragment=\"document(person, courseTitle, videoCount, issuedAt, code)\"")
                .contains("<article id=\"certificado\" class=\"certificate\"")
                .contains("th:text=\"${person}\"")
                .contains("th:text=\"${courseTitle}\"")
                .contains("th:text=\"${videoCount} + ' videos'\"")
                .contains("th:text=\"${#temporals.format(fechaEmision, 'dd MMMM yyyy')}\"")
                .contains("th:text=\"${code}\"");
        assertThat(css).contains(".admin-certificate-page {")
                .contains(".admin-cert-status {");
    }

    @Test
    @DisplayName("los formularios eligen curso antes que módulo y los cuadros respiran en ambos temas")
    void courseBeforeModuleAndReadableCards() throws IOException {
        String video = read("templates/admin/video-form.html");
        String ejercicio = read("templates/admin/ejercicio-form.html");
        String css = read("static/css/styles.css");
        String js = read("static/js/admin-form.js");

        assertThat(indexOf(video, "for=\"video-curso\""))
                .isLessThan(indexOf(video, "for=\"video-modulo\""));
        assertThat(indexOf(ejercicio, "for=\"ejercicio-curso\""))
                .isLessThan(indexOf(ejercicio, "for=\"ejercicio-modulo\""));
        for (String html : List.of(video, ejercicio)) {
            assertThat(html).contains("data-admin-curso")
                    .contains("data-course-select")
                    .contains("data-module-select")
                    .contains("/js/admin-form.js");
        }
        assertThat(js).contains("option.dataset.curso")
                .contains("course.addEventListener(\"change\"");

        assertThat(read("templates/admin/cursos.html"))
                .contains("<h1>Cursos</h1>").contains("<caption>Cursos</caption>");
        assertThat(read("templates/admin/modulos.html"))
                .contains("<h1>Módulos</h1>").contains("<caption>Módulos por curso</caption>");
        assertThat(read("templates/admin/videos.html"))
                .contains("<h1>Videos</h1>").contains("<caption>Videos por módulo</caption>");
        assertThat(read("templates/admin/ejercicios.html"))
                .contains("<h1>Ejercicios</h1>").contains("<caption>Ejercicios por módulo</caption>");

        assertThat(css).contains(".admin-dependiente {")
                .contains(".admin-page-head > div > p {")
                .contains("html[data-theme=\"dark\"] .admin-status.is-bad");
    }

    @Test
    @DisplayName("el monitor muestra señales reales con gráficos propios y actualización segura")
    void monitorConGraficosReales() throws IOException {
        String nav = read("templates/admin/fragments/admin-nav.html");
        String vista = read("templates/admin/monitoreo.html");
        String js = read("static/js/monitoreo.js");
        String css = read("static/css/styles.css");

        assertThat(indexOf(nav, "@{/admin/reportes}"))
                .isLessThan(indexOf(nav, "@{/admin/monitoreo}"));
        assertThat(indexOf(nav, "@{/admin/monitoreo}"))
                .isLessThan(indexOf(nav, "@{/admin/auditoria}"));
        assertThat(nav).contains("aria-current=${activo == 'monitoreo'} ? 'page' : null")
                .contains("<span>Monitoreo</span>");

        assertThat(vista).contains("adminHead('Monitoreo')")
                .contains("sidebar('monitoreo')")
                .contains("data-monitoreo")
                .contains("monitor-console-head")
                .contains("data-monitoreo-reloj")
                .contains("data-monitoreo-ultimo-minuto")
                .contains("data-monitoreo-recientes")
                .contains("data-monitoreo-tabla-errores")
                .contains("data-monitoreo-errores-count")
                .contains("data-grafico-trafico")
                .contains("data-grafico-latencia")
                .contains("data-monitoreo-franjas")
                .contains("data-grafico-actividad")
                .contains("/js/monitoreo.js")
                .doesNotContain("th:utext")
                .doesNotContain("th:html");

        assertThat(js).contains("/admin/api/monitoreo")
                .contains("setInterval(refrescar, 2000)")
                .contains("data-barras-trafico")
                .contains("data-linea-latencia-p99")
                .contains("data-monitoreo-tabla-errores")
                .contains("data-monitoreo-errores-count")
                .contains("textContent")
                .doesNotContain("innerHTML");
        assertThat(css).contains(".admin-line--trafico {")
                .contains(".admin-line--latencia {")
                .contains(".monitor-barra.is-bad {")
                .contains(".admin-franja.is-bad {")
                .contains("@media (prefers-reduced-motion: reduce)");
    }

    private static int indexOf(String text, String needle) {
        int posicion = text.indexOf(needle);
        assertThat(posicion).as("fragmento %s", needle).isNotNegative();
        return posicion;
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
