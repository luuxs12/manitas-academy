package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Reglas de inscripción y avance de los cursos de una cuenta.
 */
public interface EnrollmentService {

    List<Enrollment> mine(User user);

    List<Enrollment> completed(User user);

    boolean isEnrolled(UUID userId, String slug);

    Enrollment findMine(User user, String slug);

    Enrollment enroll(User user, String slug);

    Enrollment complete(User user, String slug);

    Enrollment toggleLesson(User user, String slug, int lessonNumber);

    Enrollment next(User user);
}
