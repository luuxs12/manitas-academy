package com.mundomanitas.api.service;

import com.google.common.collect.ImmutableSet;
import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.LeccionProgreso;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.LeccionProgresoRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Avance real por video.
 *
 * <p>Solo se marcan videos que existen en PostgreSQL y pertenecen al curso
 * inscripto. El total sale de los videos cargados, no del número declarado
 * en el catálogo.</p>
 */
@Service
public class LeccionProgresoServiceImpl implements LeccionProgresoService {

    private static final Logger log = LoggerFactory.getLogger(LeccionProgresoServiceImpl.class);

    private final LeccionProgresoRepository progreso;
    private final InscripcionRepository enrollments;
    private final VideoRepository videos;
    private final ContenidoItemRepository items;

    public LeccionProgresoServiceImpl(
            LeccionProgresoRepository progreso,
            InscripcionRepository enrollments,
            VideoRepository videos,
            ContenidoItemRepository items
    ) {
        this.progreso = progreso;
        this.enrollments = enrollments;
        this.videos = videos;
        this.items = items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeccionProgreso> forInscripcion(UUID inscripcionId) {
        return progreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId);
    }

    @Override
    @Transactional(readOnly = true)
    public int finalizado(UUID inscripcionId) {
        return (int) progreso.contarPorInscripcionId(inscripcionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> finalizadoVideos(UUID inscripcionId) {
        ImmutableSet.Builder<UUID> resultado = ImmutableSet.builder();
        for (LeccionProgreso item : forInscripcion(inscripcionId)) {
            resultado.add(item.getVideoId());
        }
        return resultado.build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Set<UUID>> finalizadoVideosFor(Set<UUID> inscripcionIds) {
        if (inscripcionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Set<UUID>> found = new HashMap<>();
        for (UUID inscripcionId : inscripcionIds) {
            found.put(inscripcionId, new HashSet<>());
        }
        for (LeccionProgreso item : progreso.buscarPorInscripcionIdIn(inscripcionIds)) {
            found.computeIfAbsent(item.getInscripcionId(), ignored -> new HashSet<>())
                    .add(item.getVideoId());
        }
        Map<UUID, Set<UUID>> resultado = new HashMap<>();
        found.forEach((inscripcionId, videos) ->
                resultado.put(inscripcionId, Set.copyOf(videos)));
        log.info("Videos terminados recuperados en lote para {} matrículas", inscripcionIds.size());
        return resultado;
    }

    /**
     * Marca o desmarca un video. Devuelve true si al hacerlo se terminó
     * el curso completo.
     */
    @Override
    @Transactional
    public boolean toggle(Usuario usuario, String slug, UUID videoId) {
        checkNotNull(usuario, "user");
        checkNotNull(videoId, "videoId");
        Inscripcion inscripcion = enrollments.buscarPorUsuarioAndCursoSlug(usuario, requeridoSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No estás inscripto en ese curso"));
        UUID cursoId = inscripcion.getCurso().getId();

        if (!videos.findById(videoId).isPresent()
                || !videos.existeInCurso(videoId, cursoId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El video no pertenece a este curso");
        }
        ContenidoItem origen = items.buscarPorOrigenTipoAndOrigenId("VIDEO", videoId);
        if (origen != null && !origen.isVisible()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El video no está disponible");
        }
        int total = videos.contarPorCursoId(cursoId);
        if (total == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Este curso todavía no tiene videos");
        }

        LeccionProgreso.Clave key = new LeccionProgreso.Clave(inscripcion.getId(), videoId);
        progreso.findById(key).ifPresentOrElse(
                progreso::delete,
                () -> progreso.save(new LeccionProgreso(inscripcion.getId(), videoId))
        );

        boolean finalizado = progreso.contarPorInscripcionId(inscripcion.getId()) == total;
        log.info("Video actualizado en matrícula {}; curso completo={}",
                inscripcion.getId(), finalizado);
        return finalizado;
    }

    /** Marca todos los videos cuando se usa el acceso directo "Ya terminé". */
    @Override
    @Transactional
    public void completeAll(Inscripcion inscripcion) {
        checkNotNull(inscripcion, "enrollment");
        Set<UUID> existing = finalizadoVideos(inscripcion.getId());
        List<UUID> cursoVideos = videos.buscarIdsPorCursoId(inscripcion.getCurso().getId());
        for (UUID videoId : cursoVideos) {
            if (!existing.contains(videoId)) {
                progreso.save(new LeccionProgreso(inscripcion.getId(), videoId));
            }
        }
        log.debug("Matrícula {} completada con {} videos",
                inscripcion.getId(), cursoVideos.size());
    }

    /** Total de videos terminados por la cuenta, sobre sus matrículas. */
    @Override
    @Transactional(readOnly = true)
    public LeccionTotals totals(List<Inscripcion> mine) {
        int finalizadoVideos = 0;
        int totalVideos = 0;
        for (Inscripcion inscripcion : mine) {
            totalVideos += videos.contarPorCursoId(inscripcion.getCurso().getId());
            finalizadoVideos += finalizado(inscripcion.getId());
        }
        int percent = totalVideos == 0
                ? 0
                : (int) Math.round(finalizadoVideos * 100.0 / totalVideos);
        return new LeccionTotals(finalizadoVideos, totalVideos, percent);
    }

    private static String requeridoSlug(String slug) {
        String clean = StringUtils.trimToNull(slug);
        if (clean == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no encontrado");
        }
        return clean;
    }
}
