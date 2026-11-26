package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminQuizForm;
import com.mundomanitas.api.model.QuizAlternative;
import com.mundomanitas.api.model.QuizQuestion;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.QuizAlternativeRepository;
import com.mundomanitas.api.repository.QuizAttemptRepository;
import com.mundomanitas.api.repository.QuizQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/** Guarda el cuestionario desde el panel, con validación y auditoría. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuizServiceImpl implements AdminQuizService {

    private static final Logger log = LoggerFactory.getLogger(AdminQuizServiceImpl.class);

    private final ExerciseRepository exercises;
    private final QuizQuestionRepository questions;
    private final QuizAlternativeRepository alternatives;
    private final QuizAttemptRepository attempts;
    private final AdminAuditService audit;

    public AdminQuizServiceImpl(ExerciseRepository exercises,
                                QuizQuestionRepository questions,
                                QuizAlternativeRepository alternatives,
                                QuizAttemptRepository attempts,
                                AdminAuditService audit) {
        this.exercises = exercises;
        this.questions = questions;
        this.alternatives = alternatives;
        this.attempts = attempts;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminQuizForm form(UUID exerciseId) {
        requireExercise(exerciseId);
        List<QuizQuestion> existing = questions.findByExerciseIdOrderByPositionAsc(exerciseId);
        if (existing.size() != 10) return blankForm();

        List<UUID> questionIds = existing.stream().map(QuizQuestion::getId).toList();
        Map<UUID, List<QuizAlternative>> byQuestion = alternatives
                .findByQuestionIdInOrderByQuestionIdAscPositionAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(QuizAlternative::getQuestionId));

        AdminQuizForm form = new AdminQuizForm();
        for (QuizQuestion question : existing) {
            AdminQuizForm.Question target = new AdminQuizForm.Question();
            target.setPosition(question.getPosition());
            target.setPrompt(question.getPrompt());
            List<QuizAlternative> options = byQuestion.getOrDefault(question.getId(), List.of());
            target.setCorrectIndex(options.stream()
                    .filter(QuizAlternative::isCorrect)
                    .findFirst()
                    .map(QuizAlternative::getPosition)
                    .orElse(1));
            for (QuizAlternative option : options) {
                AdminQuizForm.Alternative alternative = new AdminQuizForm.Alternative();
                alternative.setLabel(option.getLabel());
                target.getAlternatives().add(alternative);
            }
            form.getQuestions().add(target);
        }
        return form;
    }

    @Override
    @Transactional
    public void save(UUID exerciseId, AdminQuizForm form, UUID actor) {
        validate(form);
        requireExercise(exerciseId);
        checkArgument(!attempts.existsByExerciseId(exerciseId),
                "Ya hay intentos de este cuestionario; no se puede modificar.");

        List<QuizQuestion> existing = questions.findByExerciseIdOrderByPositionAsc(exerciseId);
        boolean replacing = !existing.isEmpty();
        if (replacing) {
            questions.deleteAll(existing);
            questions.flush();
        }

        List<QuizAlternative> toSave = new ArrayList<>();
        for (AdminQuizForm.Question question : form.getQuestions()) {
            QuizQuestion saved = questions.save(new QuizQuestion(
                    exerciseId, question.getPosition(), question.getPrompt().trim()));
            List<AdminQuizForm.Alternative> options = question.getAlternatives();
            for (int index = 0; index < options.size(); index++) {
                int position = index + 1;
                toSave.add(new QuizAlternative(saved.getId(), position,
                        options.get(index).getLabel().trim(),
                        position == question.getCorrectIndex()));
            }
        }
        alternatives.saveAll(toSave);

        audit.record(actor, replacing ? "UPDATE" : "CREATE", "CUESTIONARIO", exerciseId,
                "10 preguntas");
        log.info("Cuestionario guardado exercise={} preguntas={} reemplaza={} actor={}",
                exerciseId, form.getQuestions().size(), replacing, actor);
    }

    private AdminQuizForm blankForm() {
        AdminQuizForm form = new AdminQuizForm();
        for (int position = 1; position <= 10; position++) {
            AdminQuizForm.Question question = new AdminQuizForm.Question();
            question.setPosition(position);
            question.setPrompt("");
            question.setCorrectIndex(1);
            for (int option = 1; option <= 4; option++) {
                AdminQuizForm.Alternative alternative = new AdminQuizForm.Alternative();
                alternative.setLabel("");
                question.getAlternatives().add(alternative);
            }
            form.getQuestions().add(question);
        }
        return form;
    }

    private void validate(AdminQuizForm form) {
        checkArgument(form != null && form.getQuestions() != null && form.getQuestions().size() == 10,
                "El cuestionario debe tener diez preguntas.");
        Set<Integer> positions = new HashSet<>();
        for (AdminQuizForm.Question question : form.getQuestions()) {
            checkArgument(question != null && question.getPrompt() != null
                            && !question.getPrompt().isBlank(),
                    "Ingresá el enunciado de la pregunta.");
            checkArgument(question.getPosition() != null
                            && question.getPosition() >= 1 && question.getPosition() <= 10
                            && positions.add(question.getPosition()),
                    "Las posiciones deben ir de 1 a 10 sin repetir.");
            checkArgument(question.getCorrectIndex() != null
                            && question.getCorrectIndex() >= 1 && question.getCorrectIndex() <= 4,
                    "Marcá una sola respuesta correcta por pregunta.");
            checkArgument(question.getAlternatives() != null
                            && question.getAlternatives().size() == 4,
                    "Cada pregunta debe tener cuatro alternativas.");
            for (AdminQuizForm.Alternative alternative : question.getAlternatives()) {
                checkArgument(alternative != null && alternative.getLabel() != null
                                && !alternative.getLabel().isBlank(),
                        "Ingresá el texto de la alternativa.");
            }
        }
    }

    private void requireExercise(UUID exerciseId) {
        checkArgument(exerciseId != null && exercises.findById(exerciseId).isPresent(),
                "No existe ese ejercicio.");
    }
}
