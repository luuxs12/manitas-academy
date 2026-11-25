package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Temario y avance que se muestran al estudiante.
 *
 * <p>Todo sale de PostgreSQL: los ejercicios acompañan a su módulo pero no
 * cuentan como lección ni modifican el progreso, que se mide solo por videos.</p>
 */
public interface StudentContentService {

    record VideoOutline(UUID id, String title) {
    }

    record ExerciseOutline(UUID id, String title, String prompt) {
    }

    record ModuleOutline(UUID id, String title, List<VideoOutline> videos,
                         List<ExerciseOutline> exercises) {
    }

    record Outline(List<ModuleOutline> modules, int videoCount) {
    }

    record Progress(int done, int total, int percent) {
    }

    /** Módulos con sus clases y ejercicios, en el orden guardado. */
    Outline outline(UUID courseId);

    /** Avance sobre los videos que siguen perteneciendo al curso. */
    Progress progress(UUID enrollmentId, UUID courseId);

    /** Avance de todas las matrículas en una sola pasada por los datos. */
    Map<UUID, Progress> progressFor(List<Enrollment> mine);

    /** Avance general de una cuenta, sobre sus matrículas vigentes. */
    Progress totals(List<Enrollment> mine);
}
