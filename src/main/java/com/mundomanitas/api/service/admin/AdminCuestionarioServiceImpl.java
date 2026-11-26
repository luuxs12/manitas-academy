package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCuestionarioFormulario;
import com.mundomanitas.api.model.CuestionarioAlternativa;
import com.mundomanitas.api.model.CuestionarioPregunta;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.CuestionarioAlternativaRepository;
import com.mundomanitas.api.repository.CuestionarioIntentoRepository;
import com.mundomanitas.api.repository.CuestionarioPreguntaRepository;
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
public class AdminCuestionarioServiceImpl implements AdminCuestionarioService {

    private static final Logger log = LoggerFactory.getLogger(AdminCuestionarioServiceImpl.class);

    private final EjercicioRepository exercises;
    private final CuestionarioPreguntaRepository questions;
    private final CuestionarioAlternativaRepository alternatives;
    private final CuestionarioIntentoRepository attempts;
    private final AdminAuditoriaService auditoria;

    public AdminCuestionarioServiceImpl(EjercicioRepository exercises,
                                CuestionarioPreguntaRepository questions,
                                CuestionarioAlternativaRepository alternatives,
                                CuestionarioIntentoRepository attempts,
                                AdminAuditoriaService auditoria) {
        this.exercises = exercises;
        this.questions = questions;
        this.alternatives = alternatives;
        this.attempts = attempts;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCuestionarioFormulario formulario(UUID ejercicioId) {
        requireEjercicio(ejercicioId);
        List<CuestionarioPregunta> existing = questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId);
        if (existing.size() != 10) return blankFormulario();

        List<UUID> preguntaIds = existing.stream().map(CuestionarioPregunta::getId).toList();
        Map<UUID, List<CuestionarioAlternativa>> porPregunta = alternatives
                .buscarPorPreguntaIdInOrdenarPorPreguntaIdAscPosicionAsc(preguntaIds)
                .stream()
                .collect(Collectors.groupingBy(CuestionarioAlternativa::getPreguntaId));

        AdminCuestionarioFormulario formulario = new AdminCuestionarioFormulario();
        for (CuestionarioPregunta pregunta : existing) {
            AdminCuestionarioFormulario.Pregunta target = new AdminCuestionarioFormulario.Pregunta();
            target.setPosicion(pregunta.getPosicion());
            target.setPrompt(pregunta.getPrompt());
            List<CuestionarioAlternativa> options = porPregunta.getOrDefault(pregunta.getId(), List.of());
            target.setCorrectIndex(options.stream()
                    .filter(CuestionarioAlternativa::isCorrect)
                    .findFirst()
                    .map(CuestionarioAlternativa::getPosicion)
                    .orElse(1));
            for (CuestionarioAlternativa opcion : options) {
                AdminCuestionarioFormulario.Alternativa alternativa = new AdminCuestionarioFormulario.Alternativa();
                alternativa.setLabel(opcion.getLabel());
                target.getAlternatives().add(alternativa);
            }
            formulario.getQuestions().add(target);
        }
        return formulario;
    }

    @Override
    @Transactional
    public void save(UUID ejercicioId, AdminCuestionarioFormulario formulario, UUID actor) {
        validate(formulario);
        requireEjercicio(ejercicioId);
        checkArgument(!attempts.existePorEjercicioId(ejercicioId),
                "Ya hay intentos de este cuestionario; no se puede modificar.");

        List<CuestionarioPregunta> existing = questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId);
        boolean replacing = !existing.isEmpty();
        if (replacing) {
            questions.deleteAll(existing);
            questions.flush();
        }

        List<CuestionarioAlternativa> toSave = new ArrayList<>();
        for (AdminCuestionarioFormulario.Pregunta pregunta : formulario.getQuestions()) {
            CuestionarioPregunta saved = questions.save(new CuestionarioPregunta(
                    ejercicioId, pregunta.getPosicion(), pregunta.getPrompt().trim()));
            List<AdminCuestionarioFormulario.Alternativa> options = pregunta.getAlternatives();
            for (int index = 0; index < options.size(); index++) {
                int posicion = index + 1;
                toSave.add(new CuestionarioAlternativa(saved.getId(), posicion,
                        options.get(index).getLabel().trim(),
                        posicion == pregunta.getCorrectIndex()));
            }
        }
        alternatives.saveAll(toSave);

        auditoria.record(actor, replacing ? "UPDATE" : "CREATE", "CUESTIONARIO", ejercicioId,
                "10 preguntas");
        log.info("Cuestionario guardado exercise={} preguntas={} reemplaza={} actor={}",
                ejercicioId, formulario.getQuestions().size(), replacing, actor);
    }

    private AdminCuestionarioFormulario blankFormulario() {
        AdminCuestionarioFormulario formulario = new AdminCuestionarioFormulario();
        for (int posicion = 1; posicion <= 10; posicion++) {
            AdminCuestionarioFormulario.Pregunta pregunta = new AdminCuestionarioFormulario.Pregunta();
            pregunta.setPosicion(posicion);
            pregunta.setPrompt("");
            pregunta.setCorrectIndex(1);
            for (int opcion = 1; opcion <= 4; opcion++) {
                AdminCuestionarioFormulario.Alternativa alternativa = new AdminCuestionarioFormulario.Alternativa();
                alternativa.setLabel("");
                pregunta.getAlternatives().add(alternativa);
            }
            formulario.getQuestions().add(pregunta);
        }
        return formulario;
    }

    private void validate(AdminCuestionarioFormulario formulario) {
        checkArgument(formulario != null && formulario.getQuestions() != null && formulario.getQuestions().size() == 10,
                "El cuestionario debe tener diez preguntas.");
        Set<Integer> positions = new HashSet<>();
        for (AdminCuestionarioFormulario.Pregunta pregunta : formulario.getQuestions()) {
            checkArgument(pregunta != null && pregunta.getPrompt() != null
                            && !pregunta.getPrompt().isBlank(),
                    "Ingresá el enunciado de la pregunta.");
            checkArgument(pregunta.getPosicion() != null
                            && pregunta.getPosicion() >= 1 && pregunta.getPosicion() <= 10
                            && positions.add(pregunta.getPosicion()),
                    "Las posiciones deben ir de 1 a 10 sin repetir.");
            checkArgument(pregunta.getCorrectIndex() != null
                            && pregunta.getCorrectIndex() >= 1 && pregunta.getCorrectIndex() <= 4,
                    "Marcá una sola respuesta correcta por pregunta.");
            checkArgument(pregunta.getAlternatives() != null
                            && pregunta.getAlternatives().size() == 4,
                    "Cada pregunta debe tener cuatro alternativas.");
            for (AdminCuestionarioFormulario.Alternativa alternativa : pregunta.getAlternatives()) {
                checkArgument(alternativa != null && alternativa.getLabel() != null
                                && !alternativa.getLabel().isBlank(),
                        "Ingresá el texto de la alternativa.");
            }
        }
    }

    private void requireEjercicio(UUID ejercicioId) {
        checkArgument(ejercicioId != null && exercises.findById(ejercicioId).isPresent(),
                "No existe ese ejercicio.");
    }
}
