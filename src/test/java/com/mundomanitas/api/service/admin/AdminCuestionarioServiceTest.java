package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCuestionarioFormulario;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.CuestionarioAlternativa;
import com.mundomanitas.api.model.CuestionarioPregunta;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.CuestionarioAlternativaRepository;
import com.mundomanitas.api.repository.CuestionarioIntentoRepository;
import com.mundomanitas.api.repository.CuestionarioPreguntaRepository;
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
class AdminCuestionarioServiceTest {

    @Mock private EjercicioRepository exercises;
    @Mock private CuestionarioPreguntaRepository questions;
    @Mock private CuestionarioAlternativaRepository alternatives;
    @Mock private CuestionarioIntentoRepository attempts;
    @Mock private AdminAuditoriaService auditoria;

    private AdminCuestionarioService service;

    @BeforeEach
    void setUp() {
        service = new AdminCuestionarioServiceImpl(exercises, questions, alternatives, attempts, auditoria);
    }

    @Test
    @DisplayName("guarda diez preguntas de dos puntos con cuatro alternativas y una correcta")
    void createsCuestionario() {
        UUID ejercicioId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(exercises.findById(ejercicioId)).thenReturn(Optional.of(ejercicio(ejercicioId)));
        when(attempts.existePorEjercicioId(ejercicioId)).thenReturn(false);
        when(questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId)).thenReturn(List.of());
        when(questions.save(any(CuestionarioPregunta.class))).thenAnswer(invocation -> {
            CuestionarioPregunta pregunta = invocation.getArgument(0);
            ReflectionTestUtils.setField(pregunta, "id", UUID.randomUUID());
            return pregunta;
        });

        service.save(ejercicioId, validFormulario(), actor);

        ArgumentCaptor<CuestionarioPregunta> savedQuestions = ArgumentCaptor.forClass(CuestionarioPregunta.class);
        verify(questions, times(10)).save(savedQuestions.capture());
        assertThat(savedQuestions.getAllValues())
                .extracting(CuestionarioPregunta::getPosicion)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(savedQuestions.getAllValues()).allSatisfy(pregunta -> {
            assertThat(pregunta.getEjercicioId()).isEqualTo(ejercicioId);
            assertThat(pregunta.getPoints()).isEqualTo(2);
        });
        verify(alternatives).saveAll(argThat((Iterable<CuestionarioAlternativa> saved) -> {
            List<CuestionarioAlternativa> values = StreamSupport.stream(saved.spliterator(), false).toList();
            return values.size() == 40
                    && values.stream().filter(CuestionarioAlternativa::isCorrect).count() == 10
                    && values.stream().allMatch(alternativa -> alternativa.getPosicion() >= 1
                    && alternativa.getPosicion() <= 4);
        }));
        verify(auditoria).record(actor, "CREATE", "CUESTIONARIO", ejercicioId, "10 preguntas");
    }

    @Test
    @DisplayName("reemplaza el cuestionario previo si todavía no hay intentos")
    void replacesCuestionario() {
        UUID ejercicioId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        CuestionarioPregunta previous = new CuestionarioPregunta(ejercicioId, 1, "Pregunta anterior");
        when(exercises.findById(ejercicioId)).thenReturn(Optional.of(ejercicio(ejercicioId)));
        when(attempts.existePorEjercicioId(ejercicioId)).thenReturn(false);
        when(questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId))
                .thenReturn(List.of(previous));
        when(questions.save(any(CuestionarioPregunta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(ejercicioId, validFormulario(), actor);

        verify(questions).deleteAll(List.of(previous));
        verify(questions).flush();
        verify(questions, times(10)).save(any(CuestionarioPregunta.class));
        verify(auditoria).record(actor, "UPDATE", "CUESTIONARIO", ejercicioId, "10 preguntas");
    }

    @Test
    @DisplayName("carga el cuestionario existente para editarlo desde el panel")
    void loadsCuestionarioForEditing() {
        UUID ejercicioId = UUID.randomUUID();
        List<CuestionarioPregunta> saved = new ArrayList<>();
        List<CuestionarioAlternativa> savedAlternatives = new ArrayList<>();
        for (int posicion = 1; posicion <= 10; posicion++) {
            CuestionarioPregunta pregunta = new CuestionarioPregunta(ejercicioId, posicion, "Pregunta " + posicion);
            ReflectionTestUtils.setField(pregunta, "id", UUID.randomUUID());
            saved.add(pregunta);
            for (int opcion = 1; opcion <= 4; opcion++) {
                savedAlternatives.add(new CuestionarioAlternativa(pregunta.getId(), opcion,
                        "Opción " + posicion + "-" + opcion, opcion == 2));
            }
        }
        when(exercises.findById(ejercicioId)).thenReturn(Optional.of(ejercicio(ejercicioId)));
        when(questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId)).thenReturn(saved);
        when(alternatives.buscarPorPreguntaIdInOrdenarPorPreguntaIdAscPosicionAsc(any()))
                .thenReturn(savedAlternatives);

        AdminCuestionarioFormulario formulario = service.formulario(ejercicioId);

        assertThat(formulario.getQuestions()).hasSize(10);
        assertThat(formulario.getQuestions().get(0).getPrompt()).isEqualTo("Pregunta 1");
        assertThat(formulario.getQuestions().get(0).getAlternatives()).hasSize(4);
        assertThat(formulario.getQuestions().get(0).getCorrectIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("rechaza un cuestionario incompleto antes de tocar la base")
    void rejectsIncompleteCuestionario() {
        AdminCuestionarioFormulario formulario = validFormulario();
        formulario.getQuestions().remove(9);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), formulario, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El cuestionario debe tener diez preguntas.");

        verify(exercises, never()).findById(any());
    }

    @Test
    @DisplayName("rechaza una pregunta con menos de cuatro alternativas")
    void rejectsMissingAlternativa() {
        AdminCuestionarioFormulario formulario = validFormulario();
        formulario.getQuestions().get(0).getAlternatives().remove(3);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), formulario, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada pregunta debe tener cuatro alternativas.");
    }

    @Test
    @DisplayName("rechaza más de una respuesta correcta por pregunta")
    void rejectsInvalidoCorrectRespuesta() {
        AdminCuestionarioFormulario formulario = validFormulario();
        formulario.getQuestions().get(0).setCorrectIndex(5);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), formulario, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Marcá una sola respuesta correcta por pregunta.");
    }

    @Test
    @DisplayName("rechaza posiciones repetidas")
    void rejectsDuplicatedPositions() {
        AdminCuestionarioFormulario formulario = validFormulario();
        formulario.getQuestions().get(5).setPosicion(1);

        assertThatThrownBy(() -> service.save(UUID.randomUUID(), formulario, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Las posiciones deben ir de 1 a 10 sin repetir.");
    }

    @Test
    @DisplayName("rechaza un ejercicio inexistente")
    void rejectsMissingEjercicio() {
        UUID ejercicioId = UUID.randomUUID();
        when(exercises.findById(ejercicioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(ejercicioId, validFormulario(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No existe ese ejercicio.");
    }

    @Test
    @DisplayName("no permite cambiar un cuestionario que ya tiene intentos")
    void rejectsCuestionarioWithAttempts() {
        UUID ejercicioId = UUID.randomUUID();
        when(exercises.findById(ejercicioId)).thenReturn(Optional.of(ejercicio(ejercicioId)));
        when(attempts.existePorEjercicioId(ejercicioId)).thenReturn(true);

        assertThatThrownBy(() -> service.save(ejercicioId, validFormulario(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya hay intentos de este cuestionario; no se puede modificar.");

        verify(questions, never()).buscarPorEjercicioIdOrdenarPorPosicionAsc(any());
    }

    private Ejercicio ejercicio(UUID id) {
        Ejercicio ejercicio = new Ejercicio(UUID.randomUUID(), "Ejercicio quiz", "Consigna quiz", 0);
        ReflectionTestUtils.setField(ejercicio, "id", id);
        return ejercicio;
    }

    private AdminCuestionarioFormulario validFormulario() {
        AdminCuestionarioFormulario formulario = new AdminCuestionarioFormulario();
        for (int posicion = 1; posicion <= 10; posicion++) {
            AdminCuestionarioFormulario.Pregunta pregunta = new AdminCuestionarioFormulario.Pregunta();
            pregunta.setPosicion(posicion);
            pregunta.setPrompt("Pregunta " + posicion);
            pregunta.setCorrectIndex(1);
            for (int opcion = 1; opcion <= 4; opcion++) {
                AdminCuestionarioFormulario.Alternativa alternativa = new AdminCuestionarioFormulario.Alternativa();
                alternativa.setLabel("Alternativa " + posicion + "-" + opcion);
                pregunta.getAlternatives().add(alternativa);
            }
            formulario.getQuestions().add(pregunta);
        }
        return formulario;
    }
}
