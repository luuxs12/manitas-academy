package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Consultas al catálogo de PostgreSQL.
 *
 * <p>La búsqueda ignora mayúsculas y tildes; los cursos salen en el orden
 * guardado en la columna {@code position}.</p>
 */
@Service
public class CourseCatalog {

    private final CourseRepository courses;

    public CourseCatalog(CourseRepository courses) {
        this.courses = courses;
    }

    /** Todos los cursos, en el orden del catálogo. */
    public List<Course> all() {
        return courses.findAllByOrderByPositionAsc();
    }

    /** Áreas temáticas en el orden en que aparecen en el catálogo. */
    public List<String> categories() {
        return all().stream()
                .map(Course::getCategory)
                .distinct()
                .toList();
    }

    /** Otros cursos del catálogo, sin repetir el que se está mirando. */
    public List<Course> others(Course current, int limit) {
        return all().stream()
                .filter(course -> !course.getSlug().equals(current.getSlug()))
                .limit(limit)
                .toList();
    }

    /** Filtra por texto libre; una consulta vacía devuelve todo. */
    public List<Course> search(String query) {
        String needle = normalize(query);
        if (needle.isBlank()) {
            return all();
        }
        return all().stream()
                .filter(course -> haystack(course).contains(needle))
                .toList();
    }

    /** Busca un curso por su identificador de URL. */
    public Optional<Course> findBySlug(String slug) {
        return courses.findBySlug(slug);
    }

    private static String haystack(Course course) {
        return normalize(course.getTitle() + " " + course.getCategory() + " "
                + course.getLevel() + " " + course.getSummary());
    }

    /** Minúsculas y sin tildes, para no depender de cómo se escribe. */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String plain = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return plain.replaceAll("\\p{M}", "").trim();
    }
}
