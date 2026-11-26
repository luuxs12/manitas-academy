package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.repository.CursoRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Consultas al catálogo de PostgreSQL.
 *
 * <p>La búsqueda ignora mayúsculas y tildes; los cursos salen en el orden
 * guardado en la columna {@code posicion}.</p>
 */
@Service
public class CursoCatalog {

    private final CursoRepository courses;

    public CursoCatalog(CursoRepository courses) {
        this.courses = courses;
    }

    /** Solo los cursos publicados, en el orden del catálogo. */
    public List<Curso> all() {
        return courses.buscarAllPorEstadoOrdenarPorPosicionAsc("PUBLICADO");
    }

    /** Áreas temáticas en el orden en que aparecen en el catálogo. */
    public List<String> categories() {
        return all().stream()
                .map(Curso::getCategoria)
                .distinct()
                .toList();
    }

    /** Otros cursos del catálogo, sin repetir el que se está mirando. */
    public List<Curso> others(Curso actual, int limit) {
        return all().stream()
                .filter(curso -> !curso.getSlug().equals(actual.getSlug()))
                .limit(limit)
                .toList();
    }

    /** Filtra por texto libre; una consulta vacía devuelve todo. */
    public List<Curso> search(String query) {
        String needle = normalize(query);
        if (needle.isBlank()) {
            return all();
        }
        return all().stream()
                .filter(curso -> haystack(curso).contains(needle))
                .toList();
    }

    /** Busca un curso por su identificador de URL. */
    public Optional<Curso> buscarPorSlug(String slug) {
        return courses.buscarPorSlugAndEstado(slug, "PUBLICADO");
    }

    private static String haystack(Curso curso) {
        return normalize(curso.getTitulo() + " " + curso.getCategoria() + " "
                + curso.getNivel() + " " + curso.getResumen());
    }

    /** Minúsculas y sin tildes, para no depender de cómo se escribe. */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String simple = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return simple.replaceAll("\\p{M}", "").trim();
    }
}
