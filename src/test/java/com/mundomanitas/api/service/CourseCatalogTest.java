package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catálogo de cursos")
class CourseCatalogTest {

    @Mock
    private CourseRepository courses;

    private CourseCatalog catalog;

    @BeforeEach
    void setUp() {
        List<Course> data = fixture();
        lenient().when(courses.findAllByOrderByPositionAsc()).thenReturn(data);
        lenient().when(courses.findBySlug(anyString())).thenAnswer(invocation ->
                data.stream()
                        .filter(course -> course.getSlug().equals(invocation.getArgument(0)))
                        .findFirst());
        catalog = new CourseCatalog(courses);
    }

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
        assertThat(catalog.search("datos"))
                .extracting(Course::getSlug)
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
                .extracting(Course::getTitle)
                .isEqualTo("Python desde cero");

        assertThat(catalog.findBySlug("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("etiqueta el acceso según sea gratis o premium")
    void labelsAccess() {
        assertThat(catalog.findBySlug("python-desde-cero").orElseThrow().priceLabel())
                .isEqualTo("Gratis");
        assertThat(catalog.findBySlug("redes-y-soporte-tecnico").orElseThrow().priceLabel())
                .isEqualTo("Premium");
    }

    @Test
    @DisplayName("cada curso tiene slug único, portada y resumen")
    void everyCourseIsPublishable() {
        assertThat(catalog.all())
                .allSatisfy(course -> {
                    assertThat(course.getSlug()).isNotBlank();
                    assertThat(course.getImage()).startsWith("/img/cursos/");
                    assertThat(course.getSummary()).isNotBlank();
                })
                .extracting(Course::getSlug)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("las áreas del filtro salen del catálogo y no se repiten")
    void categoriesComeFromTheCatalog() {
        assertThat(catalog.categories())
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .contains("Programación", "Redes", "Hardware");
    }

    @Test
    @DisplayName("el detalle sugiere otros cursos sin repetir el actual")
    void othersSkipTheCurrentCourse() {
        Course current = catalog.findBySlug("python-desde-cero").orElseThrow();

        assertThat(catalog.others(current, 3))
                .hasSize(3)
                .extracting(Course::getSlug)
                .doesNotContain(current.getSlug());
    }

    private List<Course> fixture() {
        return List.of(
                new Course("python-desde-cero", "Python desde cero", "Nivel inicial",
                        32, true, "Programación", "Variables y ciclos", "/img/cursos/python.jpg", 1),
                new Course("desarrollo-web-html-css", "Desarrollo web con HTML y CSS", "Nivel inicial",
                        28, true, "Desarrollo web", "Primeras páginas", "/img/cursos/desarrollo-web.jpg", 2),
                new Course("redes-y-soporte-tecnico", "Redes y soporte técnico", "Nivel intermedio",
                        24, false, "Redes", "Redes e IP", "/img/cursos/redes.jpg", 3),
                new Course("ciberseguridad-basica", "Ciberseguridad básica", "Nivel intermedio",
                        20, false, "Seguridad", "Contraseñas", "/img/cursos/ciberseguridad.jpg", 4),
                new Course("bases-de-datos-sql", "Bases de datos con SQL", "Nivel intermedio",
                        26, false, "Datos", "Consultas", "/img/cursos/bases-de-datos.jpg", 5),
                new Course("mantenimiento-de-hardware", "Mantenimiento de hardware", "Nivel inicial",
                        18, false, "Hardware", "Diagnóstico", "/img/cursos/hardware.jpg", 6)
        );
    }
}
