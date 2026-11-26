package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminQuizForm;
import com.mundomanitas.api.model.Exercise;
import com.mundomanitas.api.model.QuizAlternative;
import com.mundomanitas.api.model.QuizQuestion;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.QuizAlternativeRepository;
import com.mundomanitas.api.repository.QuizAttemptRepository;
import com.mundomanitas.api.repository.QuizQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Carga administrativa del cuestionario")
class AdminQuizServiceTest {

    @Mock private ExerciseRepository exercises;
    @Mock private QuizQuestionRepository questions;
    @Mock private QuizAlternativeRepository alternatives;
    @Mock private QuizAttemptRepository attempts;
    @Mock private AdminAuditService audit;

    private AdminQuizService service;

    @BeforeEach
    void setUp() {
        service = new AdminQuizServiceImpl(exercises, questions, alternatives, attempts, audit);
    }

    @Test
    @DisplayName("guarda diez preguntas de dos puntos con cuatro alternativas y una correcta")
    void createsQuiz() {
        UUID exerciseId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(exercises.findById(exerciseId)).thenReturn(Optional.of(exercise(exerciseId)));
        when(attempts.existsByExerciseId(exerciseId)).thenReturn(false);
        when(questions.findByExerciseIdOrderByPositionAsc(exerciseId)).thenReturn(List.of());
        when(questions.save(any(QuizQuestion.class))).thenAnswer(invocation -> {
            QuizQuestion question = invocation.getArgument(0);
            ReflectionTestUtils.setField(question, "id", UUID.randomUUID());
            return question;
        });

        service.save(exerciseId, validForm(), actor);

        ArgumentCaptor<QuizQuestion> savedQuestions = ArgumentCaptor.forClass(QuizQuestion.class);
        verify(questions, times(10)).save(savedQuestions.capture());
        assertThat(savedQuestions.getAllValues())
                .extracting(QuizQuestion::getPosition)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(savedQuestions.getAllValues()).allSatisfy(question -> {
            assertThat(question.getExerciseId()).isEqualTo(exerciseId);
            assertThat(question.getPoints()).isEqualTo(2);
        });
        verify(alternatives).saveAll(argThat((Iterable<QuizAlternative> saved) -> {
            List<QuizAlternative> values = StreamSupport.stream(saved.spliterator(), false).toList();
            return values.size() == 40
                    && values.stream().filter(QuizAlternative::isCorrect).count() == 10
                    && values.stream().allMatch(alternative -> alternative.getPosition() >= 1
                    && alternative.getPosition() <= 4);
        }));
        verify(audit).record(actor, "CREATE", "CUESTIONARIO", exerciseId, "10 preguntas");
    }

    @Test
    @DisplayName("reemplaza el cuestionario previo si todavía no hay intentos")
    void replacesQuiz() {
        UUID exerciseId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        QuizQuestion previous = new QuizQuestion(exerciseId, 1, "Pregunta anterior");
        when(exercises.findById(exerciseId)).thenReturn(Optional.of(exercise(exerciseId)));
        when(attempts.existsByExerciseId(exerciseId)).thenReturn(false);
        when(questions.findByExerciseIdOrderByPositionAsc(exerciseId))
                .thenReturn(List.of(previous));
        when(questions.save(any(QuizQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(exerciseId, validForm(), actor);

        verify(questions).deleteAll(List.of(previous));
        verify(questions).flush();
        verify(questions, times(10)).save(any(QuizQuestion.class));
        verify(audit).record(actor, "UPDATE", "CUESTIONARIO", exerciseId, "10 preguntas");
    }

    @Test
    @DisplayName("carga el cuestionario existente para editarlo desde el panel")
    void loadsQuizForEditing() {
        UUID exerciseId = UUID.randomUUID();
        List<QuizQuestion> saved = new ArrayList<>();
        List<QuizAlternative> savedAlternatives = new ArrayList<>();
        for (int position = 1; position <= 10; position++) {
            QuizQuestion question = new QuizQuestion(exerciseId, position, "Pregunta " + position);
            ReflectionTestUtils.setField(question, "id", UUID.randomUUID());
            saved.add(question);
            for (int option = 1; option <= 4; option++) {
                savedAlternatives.add(new QuizAlternative(question.getId(), option,
                        "Opción " + position + "-" + option, option == 2));
            }
        }
        when(exercises.findById(exerciseId)).thenReturn(Optional.of(exercise(exerciseId)));
        when(questions.findByExerciseIdOrderByPositionAsc(exerciseId)).thenReturn(saved);
        when(alternatives.findByQuestionIdInOrderByQuestionIdAscPositionAsc(any()))
                .thenReturn(savedAlternatives);

        AdminQuizForm form = service.form(exerciseId);

        assertThat(form.getQuestions()).hasSize(10);
        assertThat(form.getQuestions().get(0).getPrompt()).isEqualTo("Pregunta 1");
        assertThat(form.getQuestions().get(0).getAlternatives()).hasSize(4);
        assertThat(form.getQuestions().get(0).getCorrectIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza un cuestionario incompleto antes de tocar la base")
    void rejectsIncompleteQuiz() {
        AdminQuizForm form = validForm();
        form.getQuestions().remove(9);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El cuestionario debe tener diez preguntas.");

        verify(exercises, never()).findById(any());
    }

    @Test
    @DisplayName("rechaza una pregunta con menos de cuatro alternativas")
    void rejectsMissingAlternative() {
        AdminQuizForm form = validForm();
        form.getQuestions().get(0).getAlternatives().remove(3);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada pregunta debe tener cuatro alternativas.");
    }

    @Test
    @DisplayName("rechaza más de una respuesta correcta por pregunta")
    void rejectsInvalidCorrectAnswer() {
        AdminQuizForm form = validForm();
        form.getQuestions().get(0).setCorrectIndex(5);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Marcá una sola respuesta correcta por pregunta.");
    }

    @Test
    @DisplayName("rechaza posiciones repetidas")
    void rejectsDuplicatedPositions() {
        AdminQuizForm form = validForm();
        form.getQuestions().get(5).setPosition(1);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Las posiciones deben ir de 1 a 10 sin repetir.");
    }

    @Test
    @DisplayName("rechaza un ejercicio inexistente")
    void rejectsMissingExercise() {
        UUID exerciseId = UUID.randomUUID();
        when(exercises.findById(exerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(exerciseId, validForm(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No existe ese ejercicio.");
    }

    @Test
    @DisplayName("no permite cambiar un cuestionario que ya tiene intentos")
    void rejectsQuizWithAttempts() {
        UUID exerciseId = UUID.randomUUID();
        when(exercises.findById(exerciseId)).thenReturn(Optional.of(exercise(exerciseId)));
        when(attempts.existsByExerciseId(exerciseId)).thenReturn(true);

        assertThatThrownBy(() -> service.save(exerciseId, validForm(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya hay intentos de este cuestionario; no se puede modificar.");

        verify(questions, never()).findByExerciseIdOrderByPositionAsc(any());
    }

    private Exercise exercise(UUID id) {
        Exercise exercise = new Exercise(UUID.randomUUID(), "Ejercicio quiz", "Consigna quiz", 0);
        ReflectionTestUtils.setField(exercise, "id", id);
        return exercise;
    }

    private AdminQuizForm validForm() {
        AdminQuizForm form = new AdminQuizForm();
        for (int position = 1; position <= 10; position++) {
            AdminQuizForm.Question question = new AdminQuizForm.Question();
            question.setPosition(position);
            question.setPrompt("Pregunta " + position);
            question.setCorrectIndex(1);
            for (int option = 1; option <= 4; option++) {
                AdminQuizForm.Alternative alternative = new AdminQuizForm.Alternative();
                alternative.setLabel("Alternativa " + position + "-" + option);
                question.getAlternatives().add(alternative);
            }
            form.getQuestions().add(question);
        }
        return form;
    }
}
