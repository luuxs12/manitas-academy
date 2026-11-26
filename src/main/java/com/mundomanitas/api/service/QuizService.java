package com.mundomanitas.api.service;

import com.mundomanitas.api.model.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cuestionario de un ejercicio: una sola entrega y resultado inmediato. */
public interface QuizService {

    record AlternativeView(UUID id, String label) {
    }

    record QuestionView(UUID id, int position, String prompt,
                        List<AlternativeView> alternatives) {
    }

    record AnswerView(int position, String prompt, String selectedLabel, String correctLabel,
                      int pointsEarned) {
    }

    record AttemptView(int score, OffsetDateTime submittedAt, List<AnswerView> answers) {
    }

    record QuizView(UUID itemId, String title, String prompt, boolean ready, boolean submitted,
                    AttemptView attempt, List<QuestionView> questions) {
    }

    /** Devuelve el cuestionario o el intento definitivo si ya fue enviado. */
    QuizView view(User user, String slug, UUID itemId);

    /** Corrige y guarda el intento; el puntaje siempre lo calcula el servidor. */
    AttemptView submit(User user, String slug, UUID itemId, Map<UUID, UUID> answers);
}
