package com.mundomanitas.api.service;

import com.google.common.base.Preconditions;
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
public class CuestionarioServiceImpl implements CuestionarioService {

    private static final Logger log = LoggerFactory.getLogger(CuestionarioServiceImpl.class);
    private static final int QUESTION_COUNT = 10;
    private static final int ALTERNATIVE_COUNT = 4;
    private static final int POINTS_PER_QUESTION = 2;

    private final InscripcionRepository enrollments;
    private final CursoModuloRepository modules;
    private final ContenidoItemRepository items;
    private final CuestionarioPreguntaRepository questions;
    private final CuestionarioAlternativaRepository alternatives;
    private final CuestionarioIntentoRepository attempts;
    private final CuestionarioIntentoRespuestaRepository respuestaRepo;

    public CuestionarioServiceImpl(
            InscripcionRepository enrollments,
            CursoModuloRepository modules,
            ContenidoItemRepository items,
            CuestionarioPreguntaRepository questions,
            CuestionarioAlternativaRepository alternatives,
            CuestionarioIntentoRepository attempts,
            CuestionarioIntentoRespuestaRepository respuestaRepo
    ) {
        this.enrollments = enrollments;
        this.modules = modules;
        this.items = items;
        this.questions = questions;
        this.alternatives = alternatives;
        this.attempts = attempts;
        this.respuestaRepo = respuestaRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public CuestionarioVista view(Usuario usuario, String slug, UUID itemId) {
        EjercicioAccess access = requireEjercicio(usuario, slug, itemId);
        Cargado cargado = load(access.ejercicioId());
        Optional<CuestionarioIntento> existing =
                attempts.buscarPorInscripcionIdAndEjercicioId(access.inscripcionId(), access.ejercicioId());
        IntentoVista resultado = existing.isPresent()
                ? resultado(existing.get(), cargado) : null;
        return new CuestionarioVista(itemId, access.item().getTitulo(),
                StringUtils.defaultString(access.item().getDescripcion()),
                cargado.ready(), existing.isPresent(), resultado, cargado.views());
    }

    @Override
    @Transactional
    public IntentoVista submit(Usuario usuario, String slug, UUID itemId, Map<UUID, UUID> answers) {
        EjercicioAccess access = requireEjercicio(usuario, slug, itemId);
        Cargado cargado = load(access.ejercicioId());
        if (!cargado.ready()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cuestionario todavía no tiene las diez preguntas cargadas");
        }
        if (attempts.buscarPorInscripcionIdAndEjercicioId(
                access.inscripcionId(), access.ejercicioId()).isPresent()) {
            throw alreadySubmitted();
        }
        validateAnswers(answers, cargado);

        int score = 0;
        Map<UUID, UUID> chosenIds = new HashMap<>();
        for (CuestionarioPregunta pregunta : cargado.questions()) {
            CuestionarioAlternativa chosen = cargado.porId().get(answers.get(pregunta.getId()));
            chosenIds.put(pregunta.getId(), chosen.getId());
            score += chosen.isCorrect() ? POINTS_PER_QUESTION : 0;
        }

        CuestionarioIntento intento = new CuestionarioIntento(access.inscripcionId(), access.ejercicioId(), score);
        try {
            attempts.saveAndFlush(intento);
        } catch (DataIntegrityViolationException exception) {
            throw alreadySubmitted();
        }
        List<CuestionarioIntentoRespuesta> detail = new ArrayList<>();
        for (CuestionarioPregunta pregunta : cargado.questions()) {
            CuestionarioAlternativa chosen = cargado.porId().get(chosenIds.get(pregunta.getId()));
            detail.add(new CuestionarioIntentoRespuesta(intento.getId(), pregunta.getId(), chosen.getId(),
                    chosen.isCorrect() ? POINTS_PER_QUESTION : 0));
        }
        respuestaRepo.saveAll(detail);
        respuestaRepo.flush();

        log.info("Cuestionario enviado item={} matrícula={} puntaje={}/20",
                itemId, access.inscripcionId(), score);
        return intentoVista(intento.getScore(), intento.getSubmittedAt(), cargado, answers);
    }

    private EjercicioAccess requireEjercicio(Usuario usuario, String slug, UUID itemId) {
        Preconditions.checkNotNull(usuario, "user");
        Preconditions.checkNotNull(itemId, "itemId");
        Inscripcion inscripcion = enrollments.buscarPorUsuarioAndCursoSlug(usuario, requeridoSlug(slug))
                .orElseThrow(() -> missing("No estás inscripto en ese curso"));
        Curso curso = inscripcion.getCurso();
        if (!curso.isFree() && !usuario.isPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso requiere plan Premium");
        }
        ContenidoItem item = items.findById(itemId)
                .orElseThrow(() -> missing("El ejercicio no existe"));
        if (!item.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ejercicio no está disponible");
        }
        if (!"EJERCICIO".equals(item.getOrigenTipo()) || item.getOrigenId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no es un cuestionario");
        }
        CursoModulo modulo = modules.findById(item.getModuloId())
                .orElseThrow(() -> missing("El módulo no existe"));
        if (!curso.getId().equals(modulo.getCursoId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ejercicio no pertenece a este curso");
        }
        return new EjercicioAccess(inscripcion.getId(), item.getOrigenId(), item);
    }

    private Cargado load(UUID ejercicioId) {
        List<CuestionarioPregunta> preguntaList =
                questions.buscarPorEjercicioIdOrdenarPorPosicionAsc(ejercicioId);
        Map<UUID, CuestionarioPregunta> preguntaPorId = new LinkedHashMap<>();
        for (CuestionarioPregunta pregunta : preguntaList) {
            preguntaPorId.put(pregunta.getId(), pregunta);
        }
        List<CuestionarioAlternativa> alternativaList = preguntaPorId.isEmpty()
                ? List.of()
                : alternatives.buscarPorPreguntaIdInOrdenarPorPreguntaIdAscPosicionAsc(
                        preguntaPorId.keySet());
        Map<UUID, List<CuestionarioAlternativa>> porPregunta = new LinkedHashMap<>();
        Map<UUID, CuestionarioAlternativa> porId = new LinkedHashMap<>();
        Map<UUID, CuestionarioAlternativa> correct = new LinkedHashMap<>();
        for (CuestionarioAlternativa alternativa : alternativaList) {
            porPregunta.computeIfAbsent(alternativa.getPreguntaId(), ignored -> new ArrayList<>())
                    .add(alternativa);
            porId.put(alternativa.getId(), alternativa);
            if (alternativa.isCorrect()) {
                correct.put(alternativa.getPreguntaId(), alternativa);
            }
        }

        List<PreguntaVista> views = preguntaList.stream()
                .map(pregunta -> new PreguntaVista(pregunta.getId(), pregunta.getPosicion(),
                        pregunta.getPrompt(), porPregunta.getOrDefault(pregunta.getId(), List.of())
                        .stream()
                        .map(alternativa -> new AlternativaVista(alternativa.getId(),
                                alternativa.getLabel()))
                        .toList()))
                .toList();
        boolean ready = preguntaList.size() == QUESTION_COUNT
                && preguntaList.stream().allMatch(pregunta -> pregunta.getPoints() == POINTS_PER_QUESTION
                && porPregunta.getOrDefault(pregunta.getId(), List.of()).size() == ALTERNATIVE_COUNT
                && correct.containsKey(pregunta.getId()));
        return new Cargado(preguntaList, porPregunta, porId, correct, views, ready);
    }

    private void validateAnswers(Map<UUID, UUID> answers, Cargado cargado) {
        Set<UUID> expected = cargado.questions().stream()
                .map(CuestionarioPregunta::getId).collect(Collectors.toSet());
        if (answers == null || answers.size() != expected.size()
                || !answers.keySet().equals(expected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Respondé las diez preguntas antes de enviar");
        }
        for (Map.Entry<UUID, UUID> respuesta : answers.entrySet()) {
            CuestionarioAlternativa chosen = cargado.porId().get(respuesta.getValue());
            if (chosen == null || !respuesta.getKey().equals(chosen.getPreguntaId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hay una respuesta que no pertenece a esa pregunta");
            }
        }
    }

    private IntentoVista resultado(CuestionarioIntento intento, Cargado cargado) {
        Map<UUID, CuestionarioIntentoRespuesta> porPregunta = new HashMap<>();
        for (CuestionarioIntentoRespuesta respuesta : respuestaRepo.buscarPorIntentoId(intento.getId())) {
            porPregunta.put(respuesta.getPreguntaId(), respuesta);
        }
        List<RespuestaVista> detail = new ArrayList<>();
        for (CuestionarioPregunta pregunta : cargado.questions()) {
            CuestionarioIntentoRespuesta respuesta = porPregunta.get(pregunta.getId());
            CuestionarioAlternativa selected = respuesta == null
                    ? null : cargado.porId().get(respuesta.getAlternativaId());
            detail.add(new RespuestaVista(pregunta.getPosicion(), pregunta.getPrompt(),
                    label(selected), label(cargado.correct().get(pregunta.getId())),
                    respuesta == null ? 0 : respuesta.getPointsEarned()));
        }
        return new IntentoVista(intento.getScore(), intento.getSubmittedAt(), detail);
    }

    private IntentoVista intentoVista(int score, java.time.OffsetDateTime submittedAt,
                                    Cargado cargado, Map<UUID, UUID> answers) {
        List<RespuestaVista> detail = new ArrayList<>();
        for (CuestionarioPregunta pregunta : cargado.questions()) {
            CuestionarioAlternativa chosen = cargado.porId().get(answers.get(pregunta.getId()));
            detail.add(new RespuestaVista(pregunta.getPosicion(), pregunta.getPrompt(),
                    label(chosen), label(cargado.correct().get(pregunta.getId())),
                    chosen != null && chosen.isCorrect() ? POINTS_PER_QUESTION : 0));
        }
        return new IntentoVista(score, submittedAt, detail);
    }

    private String label(CuestionarioAlternativa alternativa) {
        return alternativa == null ? "Sin respuesta" : alternativa.getLabel();
    }

    private ResponseStatusException alreadySubmitted() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya enviaste este cuestionario; no se puede repetir");
    }

    private ResponseStatusException missing(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private String requeridoSlug(String slug) {
        String clean = StringUtils.trimToNull(slug);
        if (clean == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso es obligatorio");
        }
        return clean;
    }

    private record EjercicioAccess(UUID inscripcionId, UUID ejercicioId, ContenidoItem item) {
    }

    private record Cargado(List<CuestionarioPregunta> questions,
                          Map<UUID, List<CuestionarioAlternativa>> grouped,
                          Map<UUID, CuestionarioAlternativa> porId,
                          Map<UUID, CuestionarioAlternativa> correct,
                          List<PreguntaVista> views,
                          boolean ready) {
    }
}
