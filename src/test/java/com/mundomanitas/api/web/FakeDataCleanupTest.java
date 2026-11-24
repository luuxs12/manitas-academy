package com.mundomanitas.api.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Limpieza de datos falsos")
class FakeDataCleanupTest {

    private static final List<String> SLUGS = List.of(
            "python-desde-cero",
            "desarrollo-web-html-css",
            "redes-y-soporte-tecnico",
            "ciberseguridad-basica",
            "bases-de-datos-sql",
            "mantenimiento-de-hardware");

    private static final List<String> FAKE_TEXT = List.of(
            "Python desde cero",
            "Rosa Quispe",
            "Luis Mamani",
            "Katherine Salazar",
            "32 lecciones",
            "20 lecciones",
            "2 cursos gratuitos",
            "Los 6 cursos del catálogo",
            "100% de las clases",
            "100% con intérprete",
            "0 subtítulos automáticos",
            "3 revisiones antes de publicar",
            "/img/cursos/");

    @Test
    @DisplayName("la migración nueva borra solo los seis cursos sembrados")
    void migrationDeletesSeededSlugs() throws IOException {
        String sql = read(Path.of("src", "main", "resources", "db", "migration",
                "V12__remove_seeded_courses.sql"));

        assertThat(sql).contains("delete from courses where slug in");
        SLUGS.forEach(slug -> assertThat(sql).contains("'" + slug + "'"));
        assertThat(sql).doesNotContain("truncate");
        assertThat(count(sql, "delete from courses")).isEqualTo(1);
    }

    @Test
    @DisplayName("ninguna plantilla muestra cursos, personas o cifras inventadas")
    void templatesHaveNoFakeData() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src", "main", "resources", "templates"))) {
            List<Path> templates = paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".html"))
                    .toList();
            assertThat(templates).isNotEmpty();
            for (Path template : templates) {
                String html = read(template);
                FAKE_TEXT.forEach(text -> assertThat(html).as(template.toString())
                        .doesNotContain(text));
            }
        }
    }

    @Test
    @DisplayName("la portada toma el curso destacado de PostgreSQL")
    void featuredCourseComesFromDatabase() throws IOException {
        String home = read(Path.of("src", "main", "resources", "templates", "index.html"));
        String controller = read(Path.of("src", "main", "java", "com", "mundomanitas",
                "api", "controller", "HomeController.java"));

        assertThat(home).contains("${featured");
        assertThat(controller).contains("model.addAttribute(\"featured\"");
        assertThat(controller).contains("Course::isFree");
    }

    @Test
    @DisplayName("no quedan portadas de los cursos falsos ni referencias a ellas")
    void seededImagesAreGone() throws IOException {
        Path folder = Path.of("src", "main", "resources", "static", "img", "cursos");
        assertThat(Files.exists(folder)).isFalse();

        List<String> textFiles = List.of(".html", ".css", ".js", ".md", ".yml", ".properties");
        try (Stream<Path> paths = Files.walk(Path.of("src", "main", "resources"))) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(Path.of("src", "main", "resources", "db")))
                    .filter(path -> textFiles.stream().anyMatch(path.toString()::endsWith))
                    .toList();
            for (Path file : files) {
                assertThat(read(file)).as(file.toString()).doesNotContain("/img/cursos/");
            }
        }
    }

    @Test
    @DisplayName("el README no fija cantidades de cursos")
    void readmeHasNoFixedCounts() throws IOException {
        String readme = read(Path.of("README.md"));
        assertThat(readme).doesNotContain("2 cursos abiertos");
        assertThat(readme).doesNotContain("Los 6 cursos del catálogo");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private int count(String text, String needle) {
        return text.split(needle, -1).length - 1;
    }
}
