package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Avance real por video dentro de una matrícula.
 */
public interface LessonProgressService {

    List<LessonProgress> forEnrollment(UUID enrollmentId);

    int completed(UUID enrollmentId);

    Set<UUID> completedVideos(UUID enrollmentId);

    /** Videos vistos de varias matrículas, agrupados por matrícula. */
    Map<UUID, Set<UUID>> completedVideosFor(Set<UUID> enrollmentIds);

    boolean toggle(User user, String slug, UUID videoId);

    void completeAll(Enrollment enrollment);

    LessonTotals totals(List<Enrollment> mine);

    record LessonTotals(int completed, int total, int percent) {
    }
}
