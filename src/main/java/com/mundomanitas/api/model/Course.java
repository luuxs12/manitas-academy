package com.mundomanitas.api.model;

/**
 * Curso del catálogo público.
 *
 * <p>Registro inmutable; cuando el catálogo pase a PostgreSQL se reemplaza
 * por una entidad JPA.</p>
 *
 * @param slug      identificador legible para la URL
 * @param title     nombre del curso
 * @param level     nivel (inicial, intermedio, avanzado)
 * @param lessons   cantidad de lecciones
 * @param free      {@code true} si se puede ver sin membresía
 * @param category  área temática, usada en la búsqueda
 * @param summary   descripción breve para las tarjetas
 * @param image     ruta de la foto de portada dentro de {@code /img/cursos}
 */
public record Course(
        String slug,
        String title,
        String level,
        int lessons,
        boolean free,
        String category,
        String summary,
        String image
) {

    /**
     * Texto de acceso que se muestra en la tarjeta y en el detalle.
     */
    public String priceLabel() {
        return free ? "Gratis" : "Premium";
    }
}
