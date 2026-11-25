package com.mundomanitas.api.service;

import com.mundomanitas.api.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Estructura y avance reales del contenido de un curso.
 *
 * <p>El estado de un VIDEO se lee del progreso existente por video; el de un
 * MATERIAL o EJERCICIO, del avance por ítem. Los porcentajes cuentan solo los
 * ítems visibles que existen en PostgreSQL.</p>
 */
public interface CourseContentService {

    record ResourceView(UUID id, String title, String url) {
    }

    record Syllabus(String summary, String objectives, String requirements,
                    List<ResourceView> resources) {
    }

    record ItemView(UUID id, String type, String title, String description, String url,
                    String youtubeId, Integer durationSeconds, boolean visible, boolean seen,
                    boolean locked, UUID sourceId) {

        /** Conserva la firma anterior para vistas que no necesitan la referencia al video. */
        public ItemView(UUID id, String type, String title, String description, String url,
                        String youtubeId, Integer durationSeconds, boolean visible, boolean seen,
                        boolean locked) {
            this(id, type, title, description, url, youtubeId, durationSeconds, visible, seen,
                    locked, null);
        }

        /** Duración legible en el temario: segundos, minutos u horas según corresponda. */
        public String durationLabel() {
            if (durationSeconds == null) {
                return "Sin duración";
            }
            int total = Math.max(0, durationSeconds);
            int hours = total / 3600;
            int minutes = (total % 3600) / 60;
            int seconds = total % 60;
            if (hours > 0) {
                return minutes > 0 ? hours + " h " + minutes + " min" : hours + " h";
            }
            if (minutes > 0) {
                return seconds > 0 ? minutes + " min " + seconds + " s" : minutes + " min";
            }
            return seconds + " s";
        }
    }

    record SectionView(UUID id, String title, boolean visible, List<ItemView> items) {
    }

    record ModuleView(UUID id, String title, boolean visible, List<SectionView> sections,
                      List<ItemView> items, int percent) {
    }

    record Outline(List<ModuleView> modules, int totalItems, int doneItems, int percent,
                   UUID nextPending) {
    }

    record Progress(int done, int total, int percent) {
    }

    Syllabus syllabus(UUID courseId);

    /** Temario con secciones y estados; {@code includeHidden} habilita la vista previa. */
    Outline outline(UUID courseId, UUID enrollmentId, User viewer, boolean includeHidden);

    Progress progress(UUID enrollmentId, UUID courseId);

    UUID nextPending(UUID courseId, UUID enrollmentId);

    /** Marca o desmarca un ítem para la matrícula de la persona. */
    void markSeen(User user, String slug, UUID itemId, boolean seen);
}
