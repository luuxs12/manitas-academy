package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Catálogo de cursos")
class CourseCatalogTest {

    private final CourseCatalog catalog = new CourseCatalog();

    @Test
    @DisplayName("sin consulta devuelve todo el catálogo")
    void emptyQueryReturnsAll() {
        assertThat(catalog.search("")).hasSize(catalog.all().size());
        assertThat(catalog.search("   ")).hasSize(catalog.all().size());
        assertThat(catalog.search(null)).hasSize(catalog.all().size());
    }

    @Test
    @DisplayName("ignora mayúsculas y tildes")
    void ignoresCaseAndAccents() {
        assertThat(catalog.search("PROGRAMACION")).isNotEmpty();
        assertThat(catalog.search("programación")).isNotEmpty();
        assertThat(catalog.search("Programacion")).hasSize(catalog.search("programación").size());
    }

    @Test
    @DisplayName("busca por categoría")
    void searchesByCategory() {
        List<Course> data = catalog.search("datos");

        assertThat(data)
                .extracting(Course::slug)
                .contains("bases-de-datos-sql");
    }

    @Test
    @DisplayName("devuelve vacío cuando no hay coincidencias")
    void returnsEmptyWhenNothingMatches() {
        assertThat(catalog.search("astronomía")).isEmpty();
    }

    @Test
    @DisplayName("encuentra un curso por su slug")
    void findsBySlug() {
        assertThat(catalog.findBySlug("python-desde-cero"))
                .isPresent()
                .get()
                .extracting(Course::title)
                .isEqualTo("Python desde cero");

        assertThat(catalog.findBySlug("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("etiqueta el acceso según sea gratis o premium")
    void labelsAccess() {
        Course free = catalog.findBySlug("python-desde-cero").orElseThrow();
        Course premium = catalog.findBySlug("redes-y-soporte-tecnico").orElseThrow();

        assertThat(free.priceLabel()).isEqualTo("Gratis");
        assertThat(premium.priceLabel()).isEqualTo("Premium");
    }

    @Test
    @DisplayName("cada curso tiene slug único, portada y resumen")
    void everyCourseIsPublishable() {
        assertThat(catalog.all())
                .allSatisfy(course -> {
                    assertThat(course.slug()).isNotBlank();
                    assertThat(course.image()).startsWith("/img/cursos/");
                    assertThat(course.summary()).isNotBlank();
                })
                .extracting(Course::slug)
                .doesNotHaveDuplicates();
    }
}
