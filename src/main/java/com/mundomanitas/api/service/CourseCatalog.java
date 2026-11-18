package com.mundomanitas.api.service;

import com.google.common.collect.ImmutableList;
import com.mundomanitas.api.model.Course;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Catálogo de cursos en memoria.
 *
 * <p>Permite que el buscador y el catálogo funcionen mientras los datos
 * reales llegan a PostgreSQL. La búsqueda ignora mayúsculas y tildes.</p>
 */
@Service
public class CourseCatalog {

    private static final ImmutableList<Course> COURSES = ImmutableList.of(
            new Course(
                    "python-desde-cero",
                    "Python desde cero",
                    "Nivel inicial",
                    32,
                    true,
                    "Programación",
                    "Variables, ciclos y funciones con ejercicios cortos para automatizar tareas reales.",
                    "/img/cursos/python.jpg"),
            new Course(
                    "desarrollo-web-html-css",
                    "Desarrollo web con HTML y CSS",
                    "Nivel inicial",
                    28,
                    true,
                    "Desarrollo web",
                    "Armá tus primeras páginas: estructura, estilos y diseño adaptable desde el primer día.",
                    "/img/cursos/desarrollo-web.jpg"),
            new Course(
                    "redes-y-soporte-tecnico",
                    "Redes y soporte técnico",
                    "Nivel intermedio",
                    24,
                    false,
                    "Redes",
                    "Cableado, direcciones IP y diagnóstico de fallas para dejar una red funcionando.",
                    "/img/cursos/redes.jpg"),
            new Course(
                    "ciberseguridad-basica",
                    "Ciberseguridad básica",
                    "Nivel intermedio",
                    20,
                    false,
                    "Seguridad",
                    "Contraseñas, phishing y buenas prácticas para proteger cuentas y equipos.",
                    "/img/cursos/ciberseguridad.jpg"),
            new Course(
                    "bases-de-datos-sql",
                    "Bases de datos con SQL",
                    "Nivel intermedio",
                    26,
                    false,
                    "Datos",
                    "Modelá tablas y escribí consultas para convertir datos sueltos en respuestas.",
                    "/img/cursos/bases-de-datos.jpg"),
            new Course(
                    "mantenimiento-de-hardware",
                    "Mantenimiento de hardware",
                    "Nivel inicial",
                    18,
                    false,
                    "Hardware",
                    "Limpieza, diagnóstico y cambio de piezas: el ABC del soporte en sitio.",
                    "/img/cursos/hardware.jpg")
    );

    /** Todos los cursos, en el orden del catálogo. */
    public List<Course> all() {
        return COURSES;
    }

    /** Áreas temáticas en el orden en que aparecen en el catálogo. */
    public List<String> categories() {
        return COURSES.stream()
                .map(Course::category)
                .distinct()
                .toList();
    }

    /** Otros cursos del catálogo, sin repetir el que se está mirando. */
    public List<Course> others(Course current, int limit) {
        return COURSES.stream()
                .filter(course -> !course.slug().equals(current.slug()))
                .limit(limit)
                .toList();
    }

    /** Filtra por texto libre; una consulta vacía devuelve todo. */
    public List<Course> search(String query) {
        String needle = normalize(query);
        if (needle.isBlank()) {
            return COURSES;
        }
        return COURSES.stream()
                .filter(course -> haystack(course).contains(needle))
                .toList();
    }

    /** Busca un curso por su identificador de URL. */
    public Optional<Course> findBySlug(String slug) {
        return COURSES.stream()
                .filter(course -> course.slug().equals(slug))
                .findFirst();
    }

    private static String haystack(Course course) {
        return normalize(course.title() + " " + course.category() + " "
                + course.level() + " " + course.summary());
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
