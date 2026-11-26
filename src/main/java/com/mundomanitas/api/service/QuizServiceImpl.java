package com.mundomanitas.api.service;

import com.google.common.base.Preconditions;
import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.QuizAlternative;
import com.mundomanitas.api.model.QuizAttempt;
import com.mundomanitas.api.model.QuizAttemptAnswer;
import com.mundomanitas.api.model.QuizQuestion;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.QuizAlternativeRepository;
import com.mundomanitas.api.repository.QuizAttemptAnswerRepository;
import com.mundomanitas.api.repository.QuizAttemptRepository;
import com.mundomanitas.api.repository.QuizQuestionRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Implementación del cuestionario definitivo de cada práctica. */
@Service
public class QuizServiceImpl implements QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizServiceImpl.class);
    private static final int QUESTION_COUNT = 10;
    private static final int ALTERNATIVE_COUNT = 4;
    private static final int POINTS_PER_QUESTION = 2;

    private final EnrollmentRepository enrollments;
    private final CourseModuleRepository modules;
    private final ContentItemRepository items;
    private final QuizQuestionRepository questions;
    private final QuizAlternativeRepository alternatives;
    private final QuizAttemptRepository attempts;
    private final QuizAttemptAnswerRepository answerRepo;

    public QuizServiceImpl(
            EnrollmentRepository enrollments,
            CourseModuleRepository modules,
            ContentItemRepository items,
            QuizQuestionRepository questions,
            QuizAlternativeRepository alternatives,
            QuizAttemptRepository attempts,
            QuizAttemptAnswerRepository answerRepo
    ) {
        this.enrollments = enrollments;
        this.modules = modules;
        this.items = items;
        this.questions = questions;
        this.alternatives = alternatives;
        this.attempts = attempts;
        this.answerRepo = answerRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizView view(User user, String slug, UUID itemId) {
        ExerciseAccess access = requireExercise(user, slug, itemId);
        Loaded loaded = load(access.exerciseId());
        Optional<QuizAttempt> existing =
                attempts.findByEnrollmentIdAndExerciseId(access.enrollmentId(), access.exerciseId());
        AttemptView result = existing.isPresent()
                ? result(existing.get(), loaded) : null;
        return new QuizView(itemId, access.item().getTitle(),
                StringUtils.defaultString(access.item().getDescription()),
                loaded.ready(), existing.isPresent(), result, loaded.views());
    }

    @Override
    @Transactional
    public AttemptView submit(User user, String slug, UUID itemId, Map<UUID, UUID> answers) {
        ExerciseAccess access = requireExercise(user, slug, itemId);
        Loaded loaded = load(access.exerciseId());
        if (!loaded.ready()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cuestionario todavía no tiene las diez preguntas cargadas");
        }
        if (attempts.findByEnrollmentIdAndExerciseId(
                access.enrollmentId(), access.exerciseId()).isPresent()) {
            throw alreadySubmitted();
        }
        validateAnswers(answers, loaded);

        int score = 0;
        Map<UUID, UUID> chosenIds = new HashMap<>();
        for (QuizQuestion question : loaded.questions()) {
            QuizAlternative chosen = loaded.byId().get(answers.get(question.getId()));
            chosenIds.put(question.getId(), chosen.getId());
            score += chosen.isCorrect() ? POINTS_PER_QUESTION : 0;
        }

        QuizAttempt attempt = new QuizAttempt(access.enrollmentId(), access.exerciseId(), score);
        try {
            attempts.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException exception) {
            throw alreadySubmitted();
        }
        List<QuizAttemptAnswer> detail = new ArrayList<>();
        for (QuizQuestion question : loaded.questions()) {
            QuizAlternative chosen = loaded.byId().get(chosenIds.get(question.getId()));
            detail.add(new QuizAttemptAnswer(attempt.getId(), question.getId(), chosen.getId(),
                    chosen.isCorrect() ? POINTS_PER_QUESTION : 0));
        }
        answerRepo.saveAll(detail);
        answerRepo.flush();

        log.info("Cuestionario enviado item={} matrícula={} puntaje={}/20",
                itemId, access.enrollmentId(), score);
        return attemptView(attempt.getScore(), attempt.getSubmittedAt(), loaded, answers);
    }

    private ExerciseAccess requireExercise(User user, String slug, UUID itemId) {
        Preconditions.checkNotNull(user, "user");
        Preconditions.checkNotNull(itemId, "itemId");
        Enrollment enrollment = enrollments.findByUserAndCourseSlug(user, requiredSlug(slug))
                .orElseThrow(() -> missing("No estás inscripto en ese curso"));
        Course course = enrollment.getCourse();
        if (!course.isFree() && !user.isPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso requiere plan Premium");
        }
        ContentItem item = items.findById(itemId)
                .orElseThrow(() -> missing("El ejercicio no existe"));
        if (!item.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ejercicio no está disponible");
        }
        if (!"EJERCICIO".equals(item.getSourceType()) || item.getSourceId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no es un cuestionario");
        }
        CourseModule module = modules.findById(item.getModuleId())
                .orElseThrow(() -> missing("El módulo no existe"));
        if (!course.getId().equals(module.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ejercicio no pertenece a este curso");
        }
        return new ExerciseAccess(enrollment.getId(), item.getSourceId(), item);
    }

    private Loaded load(UUID exerciseId) {
        List<QuizQuestion> questionList =
                questions.findByExerciseIdOrderByPositionAsc(exerciseId);
        Map<UUID, QuizQuestion> questionById = new LinkedHashMap<>();
        for (QuizQuestion question : questionList) {
            questionById.put(question.getId(), question);
        }
        List<QuizAlternative> alternativeList = questionById.isEmpty()
                ? List.of()
                : alternatives.findByQuestionIdInOrderByQuestionIdAscPositionAsc(
                        questionById.keySet());
        Map<UUID, List<QuizAlternative>> byQuestion = new LinkedHashMap<>();
        Map<UUID, QuizAlternative> byId = new LinkedHashMap<>();
        Map<UUID, QuizAlternative> correct = new LinkedHashMap<>();
        for (QuizAlternative alternative : alternativeList) {
            byQuestion.computeIfAbsent(alternative.getQuestionId(), ignored -> new ArrayList<>())
                    .add(alternative);
            byId.put(alternative.getId(), alternative);
            if (alternative.isCorrect()) {
                correct.put(alternative.getQuestionId(), alternative);
            }
        }

        List<QuestionView> views = questionList.stream()
                .map(question -> new QuestionView(question.getId(), question.getPosition(),
                        question.getPrompt(), byQuestion.getOrDefault(question.getId(), List.of())
                        .stream()
                        .map(alternative -> new AlternativeView(alternative.getId(),
                                alternative.getLabel()))
                        .toList()))
                .toList();
        boolean ready = questionList.size() == QUESTION_COUNT
                && questionList.stream().allMatch(question -> question.getPoints() == POINTS_PER_QUESTION
                && byQuestion.getOrDefault(question.getId(), List.of()).size() == ALTERNATIVE_COUNT
                && correct.containsKey(question.getId()));
        return new Loaded(questionList, byQuestion, byId, correct, views, ready);
    }

    private void validateAnswers(Map<UUID, UUID> answers, Loaded loaded) {
        Set<UUID> expected = loaded.questions().stream()
                .map(QuizQuestion::getId).collect(Collectors.toSet());
        if (answers == null || answers.size() != expected.size()
                || !answers.keySet().equals(expected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Respondé las diez preguntas antes de enviar");
        }
        for (Map.Entry<UUID, UUID> answer : answers.entrySet()) {
            QuizAlternative chosen = loaded.byId().get(answer.getValue());
            if (chosen == null || !answer.getKey().equals(chosen.getQuestionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hay una respuesta que no pertenece a esa pregunta");
            }
        }
    }

    private AttemptView result(QuizAttempt attempt, Loaded loaded) {
        Map<UUID, QuizAttemptAnswer> byQuestion = new HashMap<>();
        for (QuizAttemptAnswer answer : answerRepo.findByAttemptId(attempt.getId())) {
            byQuestion.put(answer.getQuestionId(), answer);
        }
        List<AnswerView> detail = new ArrayList<>();
        for (QuizQuestion question : loaded.questions()) {
            QuizAttemptAnswer answer = byQuestion.get(question.getId());
            QuizAlternative selected = answer == null
                    ? null : loaded.byId().get(answer.getAlternativeId());
            detail.add(new AnswerView(question.getPosition(), question.getPrompt(),
                    label(selected), label(loaded.correct().get(question.getId())),
                    answer == null ? 0 : answer.getPointsEarned()));
        }
        return new AttemptView(attempt.getScore(), attempt.getSubmittedAt(), detail);
    }

    private AttemptView attemptView(int score, java.time.OffsetDateTime submittedAt,
                                    Loaded loaded, Map<UUID, UUID> answers) {
        List<AnswerView> detail = new ArrayList<>();
        for (QuizQuestion question : loaded.questions()) {
            QuizAlternative chosen = loaded.byId().get(answers.get(question.getId()));
            detail.add(new AnswerView(question.getPosition(), question.getPrompt(),
                    label(chosen), label(loaded.correct().get(question.getId())),
                    chosen != null && chosen.isCorrect() ? POINTS_PER_QUESTION : 0));
        }
        return new AttemptView(score, submittedAt, detail);
    }

    private String label(QuizAlternative alternative) {
        return alternative == null ? "Sin respuesta" : alternative.getLabel();
    }

    private ResponseStatusException alreadySubmitted() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya enviaste este cuestionario; no se puede repetir");
    }

    private ResponseStatusException missing(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private String requiredSlug(String slug) {
        String clean = StringUtils.trimToNull(slug);
        if (clean == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso es obligatorio");
        }
        return clean;
    }

    private record ExerciseAccess(UUID enrollmentId, UUID exerciseId, ContentItem item) {
    }

    private record Loaded(List<QuizQuestion> questions,
                          Map<UUID, List<QuizAlternative>> grouped,
                          Map<UUID, QuizAlternative> byId,
                          Map<UUID, QuizAlternative> correct,
                          List<QuestionView> views,
                          boolean ready) {
    }
}
