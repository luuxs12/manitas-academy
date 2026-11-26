package com.mundomanitas.api.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cuestionarios de una sola oportunidad")
class QuizServiceTest {

    @Mock private EnrollmentRepository enrollments;
    @Mock private CourseModuleRepository modules;
    @Mock private ContentItemRepository items;
    @Mock private QuizQuestionRepository questions;
    @Mock private QuizAlternativeRepository alternatives;
    @Mock private QuizAttemptRepository attempts;
    @Mock private QuizAttemptAnswerRepository answers;

    @InjectMocks private QuizServiceImpl service;

    private User student;
    private UUID enrollmentId;
    private UUID itemId;
    private UUID exerciseId;
    private Map<UUID, QuizQuestion> byQuestion;
    private Map<UUID, QuizAlternative> correctByQuestion;
    private Map<UUID, QuizAlternative> wrongByQuestion;

    @BeforeEach
    void setUp() {
        student = new User("alumno@test.local", "hash", "Alumno");
        ReflectionTestUtils.setField(student, "id", UUID.randomUUID());
        enrollmentId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        exerciseId = UUID.randomUUID();
        byQuestion = new LinkedHashMap<>();
        correctByQuestion = new LinkedHashMap<>();
        wrongByQuestion = new LinkedHashMap<>();
    }

    @Test
    @DisplayName("entrega diez preguntas con cuatro alternativas sin revelar respuestas")
    void buildsQuiz() {
        fixture();
        lenient().when(attempts.findByEnrollmentIdAndExerciseId(enrollmentId, exerciseId))
                .thenReturn(Optional.empty());

        QuizService.QuizView view = service.view(student, "python", itemId);

        assertThat(view.ready()).isTrue();
        assertThat(view.submitted()).isFalse();
        assertThat(view.attempt()).isNull();
        assertThat(view.questions()).hasSize(10);
        assertThat(view.questions()).extracting(QuizService.QuestionView::position)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(view.questions().get(0).alternatives()).hasSize(4);
        assertThat(QuizService.AlternativeView.class.getRecordComponents())
                .extracting("name").containsExactly("id", "label");
    }

    @Test
    @DisplayName("calcula el puntaje en el servidor y guarda el detalle de cada respuesta")
    void scoresAndPersistsSingleAttempt() {
        fixture();
        when(attempts.findByEnrollmentIdAndExerciseId(enrollmentId, exerciseId))
                .thenReturn(Optional.empty());
        when(attempts.saveAndFlush(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            ReflectionTestUtils.setField(attempt, "id", UUID.randomUUID());
            return attempt;
        });
        Map<UUID, UUID> chosen = answersWithSevenCorrect();

        QuizService.AttemptView result = service.submit(student, "python", itemId, chosen);

        assertThat(result.score()).isEqualTo(14);
        assertThat(result.answers()).hasSize(10);
        assertThat(result.answers()).extracting(QuizService.AnswerView::pointsEarned)
                .containsExactly(2, 2, 2, 2, 2, 2, 2, 0, 0, 0);
        ArgumentCaptor<QuizAttempt> attempt = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(attempts).saveAndFlush(attempt.capture());
        assertThat(attempt.getValue().getScore()).isEqualTo(14);
        assertThat(attempt.getValue().getEnrollmentId()).isEqualTo(enrollmentId);
        assertThat(attempt.getValue().getExerciseId()).isEqualTo(exerciseId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuizAttemptAnswer>> saved = (ArgumentCaptor<List<QuizAttemptAnswer>>)
                (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(answers).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(10);
        assertThat(saved.getValue()).extracting(QuizAttemptAnswer::getPointsEarned)
                .containsExactly(2, 2, 2, 2, 2, 2, 2, 0, 0, 0);
    }

    @Test
    @DisplayName("muestra el resultado con la respuesta correcta de cada pregunta")
    void showsDetailedResult() {
        fixture();
        QuizAttempt attempt = new QuizAttempt(enrollmentId, exerciseId, 14);
        ReflectionTestUtils.setField(attempt, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(attempt, "submittedAt",
                java.time.OffsetDateTime.parse("2026-11-25T12:00:00Z"));
        when(attempts.findByEnrollmentIdAndExerciseId(enrollmentId, exerciseId))
                .thenReturn(Optional.of(attempt));
        when(answers.findByAttemptId(attempt.getId())).thenReturn(detailFor(attempt.getId()));

        QuizService.QuizView view = service.view(student, "python", itemId);

        assertThat(view.submitted()).isTrue();
        assertThat(view.attempt().score()).isEqualTo(14);
        assertThat(view.attempt().answers()).hasSize(10);
        QuizService.AnswerView first = view.attempt().answers().get(0);
        assertThat(first.pointsEarned()).isEqualTo(2);
        assertThat(first.selectedLabel()).isEqualTo(first.correctLabel());
        assertThat(first.correctLabel()).isEqualTo("Alternativa 1 de la pregunta 1");
        QuizService.AnswerView last = view.attempt().answers().get(9);
        assertThat(last.pointsEarned()).isZero();
        assertThat(last.selectedLabel()).isNotEqualTo(last.correctLabel());
    }

    @Test
    @DisplayName("no acepta un segundo envío aunque llegue con respuestas distintas")
    void rejectsSecondSubmission() {
        fixture();
        when(attempts.findByEnrollmentIdAndExerciseId(enrollmentId, exerciseId))
                .thenReturn(Optional.of(new QuizAttempt(enrollmentId, exerciseId, 20)));

        assertThatThrownBy(() -> service.submit(student, "python", itemId,
                answersWithSevenCorrect()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
        verify(attempts, never()).saveAndFlush(any(QuizAttempt.class));
        verify(answers, never()).saveAll(any());
    }

    @Test
    @DisplayName("rechaza respuestas faltantes y alternativas de otra pregunta")
    void validatesEveryAnswer() {
        fixture();
        when(attempts.findByEnrollmentIdAndExerciseId(enrollmentId, exerciseId))
                .thenReturn(Optional.empty());
        Map<UUID, UUID> incomplete = answersWithSevenCorrect();
        incomplete.remove(byQuestion.keySet().iterator().next());

        assertThatThrownBy(() -> service.submit(student, "python", itemId, incomplete))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);

        Map<UUID, UUID> foreign = answersWithSevenCorrect();
        UUID firstQuestion = byQuestion.keySet().iterator().next();
        UUID otherQuestion = new ArrayList<>(byQuestion.keySet()).get(1);
        foreign.put(firstQuestion, correctByQuestion.get(otherQuestion).getId());

        assertThatThrownBy(() -> service.submit(student, "python", itemId, foreign))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
        verify(attempts, never()).saveAndFlush(any(QuizAttempt.class));
    }

    @Test
    @DisplayName("no permite enviar un cuestionario sin las diez preguntas cargadas")
    void requiresCompleteQuiz() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = course(courseId);
        Enrollment enrollment = new Enrollment(student, course);
        ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
        CourseModule module = new CourseModule(courseId, "Sesion 1", 0);
        ReflectionTestUtils.setField(module, "id", moduleId);
        ContentItem item = new ContentItem(moduleId, null, "EJERCICIO", exerciseId,
                "Practica", "Consigna", null, null, null, 0, true);
        ReflectionTestUtils.setField(item, "id", itemId);

        when(enrollments.findByUserAndCourseSlug(student, "python"))
                .thenReturn(Optional.of(enrollment));
        when(modules.findById(moduleId)).thenReturn(Optional.of(module));
        when(items.findById(itemId)).thenReturn(Optional.of(item));
        when(questions.findByExerciseIdOrderByPositionAsc(exerciseId)).thenReturn(List.of());
        when(attempts.findByEnrollmentIdAndExerciseId(enrollmentId, exerciseId))
                .thenReturn(Optional.empty());

        QuizService.QuizView view = service.view(student, "python", itemId);
        assertThat(view.ready()).isFalse();

        assertThatThrownBy(() -> service.submit(student, "python", itemId, Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("solo abre cuestionarios de ítems EJERCICIO visibles del curso inscripto")
    void rejectsOtherItems() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = course(courseId);
        Enrollment enrollment = new Enrollment(student, course);
        ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
        ContentItem material = new ContentItem(moduleId, null, "MATERIAL", null,
                "Guía", "Lectura", "https://example.org/guia", null, null, 0, true);
        ReflectionTestUtils.setField(material, "id", itemId);

        when(enrollments.findByUserAndCourseSlug(student, "python"))
                .thenReturn(Optional.of(enrollment));
        when(items.findById(itemId)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> service.view(student, "python", itemId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void fixture() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = course(courseId);
        Enrollment enrollment = new Enrollment(student, course);
        ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
        CourseModule module = new CourseModule(courseId, "Sesion 1", 0);
        ReflectionTestUtils.setField(module, "id", moduleId);
        ContentItem item = new ContentItem(moduleId, null, "EJERCICIO", exerciseId,
                "Practica 1", "Consigna", null, null, null, 0, true);
        ReflectionTestUtils.setField(item, "id", itemId);

        when(enrollments.findByUserAndCourseSlug(student, "python"))
                .thenReturn(Optional.of(enrollment));
        when(modules.findById(moduleId)).thenReturn(Optional.of(module));
        when(items.findById(itemId)).thenReturn(Optional.of(item));

        List<QuizQuestion> questionList = new ArrayList<>();
        List<QuizAlternative> alternativeList = new ArrayList<>();
        for (int position = 1; position <= 10; position++) {
            QuizQuestion question = new QuizQuestion(exerciseId, position,
                    "Pregunta " + position);
            ReflectionTestUtils.setField(question, "id", UUID.randomUUID());
            byQuestion.put(question.getId(), question);
            questionList.add(question);
            for (int option = 1; option <= 4; option++) {
                QuizAlternative alternative = new QuizAlternative(question.getId(), option,
                        "Alternativa " + option + " de la pregunta " + position, option == 1);
                ReflectionTestUtils.setField(alternative, "id", UUID.randomUUID());
                alternativeList.add(alternative);
                if (option == 1) {
                    correctByQuestion.put(question.getId(), alternative);
                } else if (option == 2) {
                    wrongByQuestion.put(question.getId(), alternative);
                }
            }
        }
        when(questions.findByExerciseIdOrderByPositionAsc(exerciseId)).thenReturn(questionList);
        when(alternatives.findByQuestionIdInOrderByQuestionIdAscPositionAsc(anySet()))
                .thenReturn(alternativeList);
    }

    private Map<UUID, UUID> answersWithSevenCorrect() {
        Map<UUID, UUID> chosen = new HashMap<>();
        int index = 0;
        for (UUID questionId : byQuestion.keySet()) {
            QuizAlternative alternative = index < 7
                    ? correctByQuestion.get(questionId) : wrongByQuestion.get(questionId);
            chosen.put(questionId, alternative.getId());
            index++;
        }
        return chosen;
    }

    private List<QuizAttemptAnswer> detailFor(UUID attemptId) {
        List<QuizAttemptAnswer> detail = new ArrayList<>();
        int index = 0;
        for (UUID questionId : byQuestion.keySet()) {
            boolean correct = index < 7;
            QuizAlternative alternative = correct
                    ? correctByQuestion.get(questionId) : wrongByQuestion.get(questionId);
            detail.add(new QuizAttemptAnswer(attemptId, questionId, alternative.getId(),
                    correct ? 2 : 0));
            index++;
        }
        return detail;
    }

    private Course course(UUID courseId) {
        Course course = new Course("python", "Python", "Inicial", 1, true,
                "Programación", "Resumen", null, 0);
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}
