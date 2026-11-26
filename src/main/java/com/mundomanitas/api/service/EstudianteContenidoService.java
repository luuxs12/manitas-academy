package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Inscripcion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Temario y avance que se muestran al estudiante.
 *
 * <p>Todo sale de PostgreSQL: los ejercicios acompañan a su módulo pero no
 * cuentan como lección ni modifican el progreso, que se mide solo por videos.</p>
 */
public interface EstudianteContenidoService {

    record VideoEsquema(UUID id, String titulo) {
    }

    record EjercicioEsquema(UUID id, String titulo, String prompt) {
    }

    record ModuloEsquema(UUID id, String titulo, List<VideoEsquema> videos,
                         List<EjercicioEsquema> exercises) {
    }

    record Esquema(List<ModuloEsquema> modules, int videoContar) {
    }

    record Progreso(int done, int total, int percent) {
    }

    /** Módulos con sus clases y ejercicios, en el orden guardado. */
    Esquema esquema(UUID cursoId);

    /** Avance sobre los videos que siguen perteneciendo al curso. */
    Progreso progreso(UUID inscripcionId, UUID cursoId);

    /** Avance de todas las matrículas en una sola pasada por los datos. */
    Map<UUID, Progreso> progresoFor(List<Inscripcion> mine);

    /** Avance general de una cuenta, sobre sus matrículas vigentes. */
    Progreso totals(List<Inscripcion> mine);
}
