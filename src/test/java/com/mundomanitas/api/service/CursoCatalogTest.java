package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catálogo de cursos")
class CursoCatalogTest {

    @Mock
    private CursoRepository courses;

    private CursoCatalog catalog;

    @BeforeEach
    void setUp() {
        List<Curso> datos = fixture();
        datos.forEach(Curso::publicar);
        lenient().when(courses.buscarAllPorEstadoOrdenarPorPosicionAsc("PUBLICADO")).thenReturn(datos);
        lenient().when(courses.buscarPorSlugAndEstado(anyString(), eq("PUBLICADO")))
                .thenAnswer(invocation -> datos.stream()
                        .filter(curso -> curso.getSlug().equals(invocation.getArgument(0)))
                        .findFirst());
        catalog = new CursoCatalog(courses);
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
    void searchesPorCategoria() {
        assertThat(catalog.search("datos"))
                .extracting(Curso::getSlug)
                .contains("bases-de-datos-sql");
    }

    @Test
    @DisplayName("devuelve vacío cuando no hay coincidencias")
    void returnsEmptyWhenNothingMatches() {
        assertThat(catalog.search("astronomía")).isEmpty();
    }

    @Test
    @DisplayName("encuentra un curso por su slug")
    void findsPorSlug() {
        assertThat(catalog.buscarPorSlug("python-desde-cero"))
                .isPresent()
                .get()
                .extracting(Curso::getTitulo)
                .isEqualTo("Python desde cero");

        assertThat(catalog.buscarPorSlug("no-existe")).isEmpty();
    }

    @Test
    @DisplayName("no muestra cursos en borrador")
    void hidesDrafts() {
        when(courses.buscarPorSlugAndEstado("borrador", "PUBLICADO"))
                .thenReturn(java.util.Optional.empty());

        assertThat(catalog.buscarPorSlug("borrador")).isEmpty();
    }

    @Test
    @DisplayName("etiqueta el acceso según sea gratis o premium")
    void etiquetasAccess() {
        assertThat(catalog.buscarPorSlug("python-desde-cero").orElseThrow().priceLabel())
                .isEqualTo("Gratis");
        assertThat(catalog.buscarPorSlug("redes-y-soporte-tecnico").orElseThrow().priceLabel())
                .isEqualTo("Premium");
    }

    @Test
    @DisplayName("cada curso tiene slug único, portada y resumen")
    void everyCursoIsPublishable() {
        assertThat(catalog.all())
                .allSatisfy(curso -> {
                    assertThat(curso.getSlug()).isNotBlank();
                    assertThat(curso.getImagen()).startsWith("/img/cursos/");
                    assertThat(curso.getResumen()).isNotBlank();
                })
                .extracting(Curso::getSlug)
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
    void othersSkipTheActualCurso() {
        Curso actual = catalog.buscarPorSlug("python-desde-cero").orElseThrow();

        assertThat(catalog.others(actual, 3))
                .hasSize(3)
                .extracting(Curso::getSlug)
                .doesNotContain(actual.getSlug());
    }

    private List<Curso> fixture() {
        return List.of(
                new Curso("python-desde-cero", "Python desde cero", "Nivel inicial",
                        32, true, "Programación", "Variables y ciclos", "/img/cursos/python.jpg", 1),
                new Curso("desarrollo-web-html-css", "Desarrollo web con HTML y CSS", "Nivel inicial",
                        28, true, "Desarrollo web", "Primeras páginas", "/img/cursos/desarrollo-web.jpg", 2),
                new Curso("redes-y-soporte-tecnico", "Redes y soporte técnico", "Nivel intermedio",
                        24, false, "Redes", "Redes e IP", "/img/cursos/redes.jpg", 3),
                new Curso("ciberseguridad-basica", "Ciberseguridad básica", "Nivel intermedio",
                        20, false, "Seguridad", "Contraseñas", "/img/cursos/ciberseguridad.jpg", 4),
                new Curso("bases-de-datos-sql", "Bases de datos con SQL", "Nivel intermedio",
                        26, false, "Datos", "Consultas", "/img/cursos/bases-de-datos.jpg", 5),
                new Curso("mantenimiento-de-hardware", "Mantenimiento de hardware", "Nivel inicial",
                        18, false, "Hardware", "Diagnóstico", "/img/cursos/hardware.jpg", 6)
        );
    }
}
