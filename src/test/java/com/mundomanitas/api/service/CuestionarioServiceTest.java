package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.CuestionarioAlternativa;
import com.mundomanitas.api.model.CuestionarioIntento;
import com.mundomanitas.api.model.CuestionarioIntentoRespuesta;
import com.mundomanitas.api.model.CuestionarioPregunta;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.CuestionarioAlternativaRepository;
import com.mundomanitas.api.repository.CuestionarioIntentoRespuestaRepository;
import com.mundomanitas.api.repository.CuestionarioIntentoRepository;
import com.mundomanitas.api.repository.CuestionarioPreguntaRepository;
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
class CuestionarioServiceTest {

    @Mock private InscripcionRepository enrollments;
    @Mock private CursoModuloRepository modules;
    @Mock private ContenidoItemRepository items;
    @Mock private CuestionarioPreguntaRepository questions;
    @Mock private CuestionarioAlternativaRepository alternatives;
    @Mock private CuestionarioIntentoRepository attempts;
    @Mock private CuestionarioIntentoRespuestaRepository answers;

    @InjectMocks private CuestionarioServiceImpl service;

    private Usuario estudiante;
    private UUID inscripcionId;
    private UUID itemId;
    private UUID ejercicioId;
    private Map<UUID, CuestionarioPregunta> porPregunta;
    private Map<UUID, CuestionarioAlternativa> correctPorPregunta;
    private Map<UUID, CuestionarioAlternativa> incorrectaPorPregunta;

    @BeforeEach
    void setUp() {
        estudiante = new Usuario("alumno@test.local", "hash", "Alumno");
        ReflectionTestUtils.setField(estudiante, "id", UUID.randomUUID());
        inscripcionId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        ejercicioId = UUID.randomUUID();
        porPregunta = new LinkedHashMap<>();
        correctPorPregunta = new LinkedHashMap<>();
        incorrectaPorPregunta = new LinkedHashMap<>();
    }

    @Test
    @DisplayName("entrega diez preguntas con cuatro alternativas sin revelar respuestas")
    void buildsCuestionario() {
        fixture();
        lenient().when(attempts.buscarPorInscripcionIdAndEjercicioId(inscripcionId, ejercicioId))
                .thenReturn(Optional.empty());

        CuestionarioService.CuestionarioVista view = service.view(estudiante, "python", itemId);

        assertThat(view.ready()).isTrue();
        assertThat(view.submitted()).isFalse();
        assertThat(view.intento()).isNull();
        assertThat(view.questions()).hasSize(10);
        assertThat(view.questions()).extracting(CuestionarioService.PreguntaVista::posicion)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(view.questions().get(0).alternatives()).hasSize(4);
        assertThat(CuestionarioService.AlternativaVista.class.getRecordComponents())
                .extracting("name").containsExactly("id", "label");
    }

    @Test
    @DisplayName("calcula el puntaje en el servidor y guarda el detalle de cada respuesta")
    void scoresAndPersistsSingleIntento() {
        fixture();
        when(attempts.buscarPorInscripcionIdAndEjercicioId(inscripcionId, ejercicioId))
                .thenReturn(Optional.empty());
        when(attempts.saveAndFlush(any(CuestionarioIntento.class))).thenAnswer(invocation -> {
            CuestionarioIntento intento = invocation.getArgument(0);
            ReflectionTestUtils.setField(intento, "id", UUID.randomUUID());
            return intento;
        });
        Map<UUID, UUID> chosen = answersWithSevenCorrect();

        CuestionarioService.IntentoVista resultado = service.submit(estudiante, "python", itemId, chosen);

        assertThat(resultado.score()).isEqualTo(14);
        assertThat(resultado.answers()).hasSize(10);
        assertThat(resultado.answers()).extracting(CuestionarioService.RespuestaVista::pointsEarned)
                .containsExactly(2, 2, 2, 2, 2, 2, 2, 0, 0, 0);
        ArgumentCaptor<CuestionarioIntento> intento = ArgumentCaptor.forClass(CuestionarioIntento.class);
        verify(attempts).saveAndFlush(intento.capture());
        assertThat(intento.getValue().getScore()).isEqualTo(14);
        assertThat(intento.getValue().getInscripcionId()).isEqualTo(inscripcionId);
        assertThat(intento.getValue().getEjercicioId()).isEqualTo(ejercicioId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CuestionarioIntentoRespuesta>> saved = (ArgumentCaptor<List<CuestionarioIntentoRespuesta>>)
                (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(answers).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(10);
        assertThat(saved.getValue()).extracting(CuestionarioIntentoRespuesta::getPointsEarned)
                .containsExactly(2, 2, 2, 2, 2, 2, 2, 0, 0, 0);
    }

    @Test
    @DisplayName("muestra el resultado con la respuesta correcta de cada pregunta")
    void showsDetailedResultado() {
        fixture();
        CuestionarioIntento intento = new CuestionarioIntento(inscripcionId, ejercicioId, 14);
        ReflectionTestUtils.setField(intento, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(intento, "submittedAt",
                java.time.OffsetDateTime.parse("2026-11-25T12:00:00Z"));
        when(attempts.buscarPorInscripcionIdAndEjercicioId(inscripcionId, ejercicioId))
                .thenReturn(Optional.of(intento));
        when(answers.buscarPorIntentoId(intento.getId())).thenReturn(detailFor(intento.getId()));

        CuestionarioService.CuestionarioVista view = service.view(estudiante, "python", itemId);

        assertThat(view.submitted()).isTrue();
        assertThat(view.intento().score()).isEqualTo(14);
        assertThat(view.intento().answers()).hasSize(10);
        CuestionarioService.RespuestaVista first = view.intento().answers().get(0);
        assertThat(first.pointsEarned()).isEqualTo(2);
        assertThat(first.selectedLabel()).isEqualTo(first.correctLabel());
        assertThat(first.correctLabel()).isEqualTo("Alternativa 1 de la pregunta 1");
        CuestionarioService.RespuestaVista last = view.intento().answers().get(9);
        assertThat(last.pointsEarned()).isZero();
        assertThat(last.selectedLabel()).isNotEqualTo(last.correctLabel());
    }

    @Test
    @DisplayName("no acepta un segundo envío aunque llegue con respuestas distintas")
    void rejectsSecondSubmission() {
        fixture();
        when(attempts.buscarPorInscripcionIdAndEjercicioId(inscripcionId, ejercicioId))
                .thenReturn(Optional.of(new CuestionarioIntento(inscripcionId, ejercicioId, 20)));

        assertThatThrownBy(() -> service.submit(estudiante, "python", itemId,
                answersWithSevenCorrect()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
        verify(attempts, never()).saveAndFlush(any(CuestionarioIntento.class));
        verify(answers, never()).saveAll(any());
    }

    @Test
    @DisplayName("rechaza respuestas faltantes y alternativas de otra pregunta")
    void validatesEveryRespuesta() {
        fixture();
        when(attempts.buscarPorInscripcionIdAndEjercicioId(inscripcionId, ejercicioId))
                .thenReturn(Optional.empty());
        Map<UUID, UUID> incomplete = answersWithSevenCorrect();
        incomplete.remove(porPregunta.keySet().iterator().next());

        assertThatThrownBy(() -> service.submit(estudiante, "python", itemId, incomplete))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);

        Map<UUID, UUID> foreign = answersWithSevenCorrect();
        UUID firstPregunta = porPregunta.keySet().iterator().next();
        UUID otherPregunta = new ArrayList<>(porPregunta.keySet()).get(1);
        foreign.put(firstPregunta, correctPorPregunta.get(otherPregunta).getId());

        assertThatThrownBy(() -> service.submit(estudiante, "python", itemId, foreign))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
        verify(attempts, never()).saveAndFlush(any(CuestionarioIntento.class));
    }

    @Test
    @DisplayName("no permite enviar un cuestionario sin las diez preguntas cargadas")
    void requiresCompleteCuestionario() {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        Curso curso = curso(cursoId);
        Inscripcion inscripcion = new Inscripcion(estudiante, curso);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
        CursoModulo modulo = new CursoModulo(cursoId, "Sesion 1", 0);
        ReflectionTestUtils.setField(modulo, "id", moduloId);
        ContenidoItem item = new ContenidoItem(moduloId, null, "EJERCICIO", ejercicioId,
                "Practica", "Consigna", null, null, null, 0, true);
        ReflectionTestUtils.setField(item, "id", itemId);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python"))
                .thenReturn(Optional.of(inscripcion));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo));
        when(items.findById(itemId)).thenReturn(Optional.of(item));
        when(questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId)).thenReturn(List.of());
        when(attempts.buscarPorInscripcionIdAndEjercicioId(inscripcionId, ejercicioId))
                .thenReturn(Optional.empty());

        CuestionarioService.CuestionarioVista view = service.view(estudiante, "python", itemId);
        assertThat(view.ready()).isFalse();

        assertThatThrownBy(() -> service.submit(estudiante, "python", itemId, Map.of()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("solo abre cuestionarios de ítems EJERCICIO visibles del curso inscripto")
    void rejectsOtherItems() {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        Curso curso = curso(cursoId);
        Inscripcion inscripcion = new Inscripcion(estudiante, curso);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
        ContenidoItem material = new ContenidoItem(moduloId, null, "MATERIAL", null,
                "Guía", "Lectura", "https://example.org/guia", null, null, 0, true);
        ReflectionTestUtils.setField(material, "id", itemId);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python"))
                .thenReturn(Optional.of(inscripcion));
        when(items.findById(itemId)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> service.view(estudiante, "python", itemId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void fixture() {
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();
        Curso curso = curso(cursoId);
        Inscripcion inscripcion = new Inscripcion(estudiante, curso);
        ReflectionTestUtils.setField(inscripcion, "id", inscripcionId);
        CursoModulo modulo = new CursoModulo(cursoId, "Sesion 1", 0);
        ReflectionTestUtils.setField(modulo, "id", moduloId);
        ContenidoItem item = new ContenidoItem(moduloId, null, "EJERCICIO", ejercicioId,
                "Practica 1", "Consigna", null, null, null, 0, true);
        ReflectionTestUtils.setField(item, "id", itemId);

        when(enrollments.buscarPorUsuarioAndCursoSlug(estudiante, "python"))
                .thenReturn(Optional.of(inscripcion));
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo));
        when(items.findById(itemId)).thenReturn(Optional.of(item));

        List<CuestionarioPregunta> preguntaList = new ArrayList<>();
        List<CuestionarioAlternativa> alternativaList = new ArrayList<>();
        for (int posicion = 1; posicion <= 10; posicion++) {
            CuestionarioPregunta pregunta = new CuestionarioPregunta(ejercicioId, posicion,
                    "Pregunta " + posicion);
            ReflectionTestUtils.setField(pregunta, "id", UUID.randomUUID());
            porPregunta.put(pregunta.getId(), pregunta);
            preguntaList.add(pregunta);
            for (int opcion = 1; opcion <= 4; opcion++) {
                CuestionarioAlternativa alternativa = new CuestionarioAlternativa(pregunta.getId(), opcion,
                        "Alternativa " + opcion + " de la pregunta " + posicion, opcion == 1);
                ReflectionTestUtils.setField(alternativa, "id", UUID.randomUUID());
                alternativaList.add(alternativa);
                if (opcion == 1) {
                    correctPorPregunta.put(pregunta.getId(), alternativa);
                } else if (opcion == 2) {
                    incorrectaPorPregunta.put(pregunta.getId(), alternativa);
                }
            }
        }
        when(questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId)).thenReturn(preguntaList);
        when(alternatives.buscarPorPreguntaIdInOrdenarPorPreguntaIdAscPosicionAsc(anySet()))
                .thenReturn(alternativaList);
    }

    private Map<UUID, UUID> answersWithSevenCorrect() {
        Map<UUID, UUID> chosen = new HashMap<>();
        int index = 0;
        for (UUID preguntaId : porPregunta.keySet()) {
            CuestionarioAlternativa alternativa = index < 7
                    ? correctPorPregunta.get(preguntaId) : incorrectaPorPregunta.get(preguntaId);
            chosen.put(preguntaId, alternativa.getId());
            index++;
        }
        return chosen;
    }

    private List<CuestionarioIntentoRespuesta> detailFor(UUID intentoId) {
        List<CuestionarioIntentoRespuesta> detail = new ArrayList<>();
        int index = 0;
        for (UUID preguntaId : porPregunta.keySet()) {
            boolean correct = index < 7;
            CuestionarioAlternativa alternativa = correct
                    ? correctPorPregunta.get(preguntaId) : incorrectaPorPregunta.get(preguntaId);
            detail.add(new CuestionarioIntentoRespuesta(intentoId, preguntaId, alternativa.getId(),
                    correct ? 2 : 0));
            index++;
        }
        return detail;
    }

    private Curso curso(UUID cursoId) {
        Curso curso = new Curso("python", "Python", "Inicial", 1, true,
                "Programación", "Resumen", null, 0);
        ReflectionTestUtils.setField(curso, "id", cursoId);
        return curso;
    }
}
