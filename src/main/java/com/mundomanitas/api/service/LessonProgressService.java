package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Avance real por lección dentro de una matrícula.
 */
public interface LessonProgressService {

    List<LessonProgress> forEnrollment(UUID enrollmentId);

    int completed(UUID enrollmentId);

    Set<Integer> completedLessons(UUID enrollmentId);

    boolean toggle(User user, String slug, int lessonNumber);

    void completeAll(Enrollment enrollment);

    LessonTotals totals(List<Enrollment> mine);

    record LessonTotals(int completed, int total, int percent) {
    }
}
