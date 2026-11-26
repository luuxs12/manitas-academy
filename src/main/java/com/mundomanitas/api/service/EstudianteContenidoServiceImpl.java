package com.mundomanitas.api.service;

import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Implementación del temario y el avance reales de una matrícula. */
@Service
public class EstudianteContenidoServiceImpl implements EstudianteContenidoService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteContenidoServiceImpl.class);

    private final CursoModuloRepository modules;
    private final VideoRepository videos;
    private final EjercicioRepository exercises;
    private final LeccionProgresoService lessons;

    public EstudianteContenidoServiceImpl(CursoModuloRepository modules,
                                     VideoRepository videos,
                                     EjercicioRepository exercises,
                                     LeccionProgresoService lessons) {
        this.modules = modules;
        this.videos = videos;
        this.exercises = exercises;
        this.lessons = lessons;
    }

    @Override
    @Transactional(readOnly = true)
    public Esquema esquema(UUID cursoId) {
        List<ModuloEsquema> resultado = new ArrayList<>();
        for (CursoModulo modulo : modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)) {
            List<VideoEsquema> classes = videos.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId())
                    .stream()
                    .map(video -> new VideoEsquema(video.getId(), video.getTitulo()))
                    .toList();
            List<EjercicioEsquema> tasks = exercises.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId())
                    .stream()
                    .map(ejercicio -> new EjercicioEsquema(
                            ejercicio.getId(), ejercicio.getTitulo(), ejercicio.getPrompt()))
                    .toList();
            resultado.add(new ModuloEsquema(modulo.getId(), modulo.getTitulo(), classes, tasks));
        }
        int count = resultado.stream().mapToInt(modulo -> modulo.videos().size()).sum();
        return new Esquema(List.copyOf(resultado), count);
    }

    @Override
    @Transactional(readOnly = true)
    public Progreso progreso(UUID inscripcionId, UUID cursoId) {
        List<UUID> actual = videos.buscarIdsPorCursoId(cursoId);
        Set<UUID> done = lessons.finalizadoVideos(inscripcionId);
        int finalizado = (int) actual.stream().filter(done::contains).count();
        int total = actual.size();
        int percent = total == 0 ? 0 : (int) Math.round(finalizado * 100.0 / total);
        return new Progreso(finalizado, total, percent);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Progreso> progresoFor(List<Inscripcion> mine) {
        if (mine.isEmpty()) {
            return Map.of();
        }
        Set<UUID> inscripcionIds = mine.stream()
                .map(Inscripcion::getId)
                .collect(Collectors.toSet());
        Set<UUID> cursoIds = mine.stream()
                .map(inscripcion -> inscripcion.getCurso().getId())
                .collect(Collectors.toSet());

        Map<UUID, Set<UUID>> videosPorCurso = new HashMap<>();
        for (VideoRepository.CursoVideo row : videos.buscarCursoVideos(cursoIds)) {
            videosPorCurso.computeIfAbsent(row.getCursoId(), ignored -> new HashSet<>())
                    .add(row.getVideoId());
        }
        Map<UUID, Set<UUID>> donePorInscripcion = lessons.finalizadoVideosFor(inscripcionIds);

        Map<UUID, Progreso> resultado = new HashMap<>();
        for (Inscripcion inscripcion : mine) {
            UUID id = inscripcion.getId();
            Set<UUID> actual = videosPorCurso.getOrDefault(inscripcion.getCurso().getId(), Set.of());
            Set<UUID> done = donePorInscripcion.getOrDefault(id, Set.of());
            int finalizado = (int) actual.stream().filter(done::contains).count();
            int total = actual.size();
            int percent = total == 0 ? 0 : (int) Math.round(finalizado * 100.0 / total);
            resultado.put(id, new Progreso(finalizado, total, percent));
        }
        log.info("Avance calculado en lote para {} matrículas y {} cursos",
                mine.size(), cursoIds.size());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Progreso totals(List<Inscripcion> mine) {
        int done = 0;
        int total = 0;
        for (Progreso actual : progresoFor(mine).values()) {
            done += actual.done();
            total += actual.total();
        }
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        return new Progreso(done, total, percent);
    }
}
