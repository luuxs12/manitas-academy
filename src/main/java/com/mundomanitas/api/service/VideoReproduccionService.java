package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Entrega de clases de YouTube al área de estudiante. */
@Service
public class VideoReproduccionService {

    private final VideoRepository videos;
    private final CursoModuloRepository modules;
    private final ContenidoItemRepository items;

    public VideoReproduccionService(VideoRepository videos, CursoModuloRepository modules,
                                ContenidoItemRepository items) {
        this.videos = videos;
        this.modules = modules;
        this.items = items;
    }

    /** Dirección de incrustación construida en el servidor desde el ID guardado. */
    public record Reproduccion(String titulo, String url) {
    }

    /** Tarjeta del temario en orden de módulos y lecciones. */
    public record CursoVideo(UUID id, UUID moduloId, String moduloTitulo, String titulo) {
    }

    @Transactional(readOnly = true)
    public Reproduccion play(UUID videoId, Inscripcion inscripcion) {
        if (inscripcion == null || inscripcion.getCurso() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada");
        }
        Video video = videos.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Video no encontrado"));
        if (!videos.existeInCurso(videoId, inscripcion.getCurso().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El video no pertenece a este curso");
        }
        ContenidoItem origen = items.buscarPorOrigenTipoAndOrigenId("VIDEO", videoId);
        if (origen != null && !origen.isVisible()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "La clase no está disponible");
        }
        String url = YouTubeLink.embedUrl(video.getYoutubeId());
        if (url == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "La clase todavía no tiene un video de YouTube");
        }
        return new Reproduccion(video.getTitulo(), url);
    }

    @Transactional(readOnly = true)
    public List<CursoVideo> cursoVideos(UUID cursoId) {
        List<CursoVideo> cards = new ArrayList<>();
        for (CursoModulo modulo : modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)) {
            for (Video video : videos.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId())) {
                cards.add(new CursoVideo(video.getId(), modulo.getId(),
                        modulo.getTitulo(), video.getTitulo()));
            }
        }
        return cards;
    }

    @Transactional(readOnly = true)
    public int videoContar(UUID cursoId) {
        return videos.contarPorCursoId(cursoId);
    }
}
