package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.CursoSeccion;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.ItemProgreso;
import com.mundomanitas.api.model.LeccionProgreso;
import com.mundomanitas.api.model.CuestionarioIntento;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.CursoRecursoRepository;
import com.mundomanitas.api.repository.CursoSeccionRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.ItemProgresoRepository;
import com.mundomanitas.api.repository.LeccionProgresoRepository;
import com.mundomanitas.api.repository.CuestionarioIntentoRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/** Implementación del temario, el sílabo y el avance por ítem. */
@Service
public class CursoContenidoServiceImpl implements CursoContenidoService {

    private static final Logger log = LoggerFactory.getLogger(CursoContenidoServiceImpl.class);
    private static final int QUIZ_MAX_SCORE = 20;

    private final CursoRepository courses;
    private final CursoModuloRepository modules;
    private final CursoSeccionRepository sections;
    private final ContenidoItemRepository items;
    private final CursoRecursoRepository resources;
    private final InscripcionRepository enrollments;
    private final LeccionProgresoRepository leccionProgreso;
    private final ItemProgresoRepository itemProgreso;
    private final CuestionarioIntentoRepository cuestionarioAttempts;
    private final EjercicioRepository exercises;
    private final VideoRepository videos;
    private final CursoMaterialStorage materials;

    public CursoContenidoServiceImpl(
            CursoRepository courses,
            CursoModuloRepository modules,
            CursoSeccionRepository sections,
            ContenidoItemRepository items,
            CursoRecursoRepository resources,
            InscripcionRepository enrollments,
            LeccionProgresoRepository leccionProgreso,
            ItemProgresoRepository itemProgreso,
            CuestionarioIntentoRepository cuestionarioAttempts,
            EjercicioRepository exercises,
            VideoRepository videos,
            CursoMaterialStorage materials
    ) {
        this.courses = courses;
        this.modules = modules;
        this.sections = sections;
        this.items = items;
        this.resources = resources;
        this.enrollments = enrollments;
        this.leccionProgreso = leccionProgreso;
        this.itemProgreso = itemProgreso;
        this.cuestionarioAttempts = cuestionarioAttempts;
        this.exercises = exercises;
        this.videos = videos;
        this.materials = materials;
    }

    @Override
    @Transactional(readOnly = true)
    public Silabo silabo(UUID cursoId) {
        Curso curso = courses.findById(cursoId).orElseThrow(() -> missing("Curso no encontrado"));
        List<RecursoVista> enlaces = resources.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId).stream()
                .map(recurso -> new RecursoVista(recurso.getId(), recurso.getTitulo(),
                        recurso.getUrl()))
                .toList();
        return new Silabo(curso.getResumen(), curso.getObjectives(),
                curso.getRequirements(), enlaces);
    }

    @Override
    @Transactional(readOnly = true)
    public Esquema esquema(UUID cursoId, UUID inscripcionId, Usuario viewer, boolean includeHidden) {
        Curso curso = courses.findById(cursoId).orElseThrow(() -> missing("Curso no encontrado"));
        boolean bloqueado = !includeHidden && !curso.isFree()
                && (viewer == null || !viewer.isPremium());
        Set<UUID> vistoVideos = inscripcionId == null ? Set.of()
                : leccionProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId).stream()
                        .map(LeccionProgreso::getVideoId).collect(Collectors.toSet());
        Set<UUID> vistoItems = inscripcionId == null ? Set.of()
                : itemProgreso.buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(inscripcionId).stream()
                        .map(ItemProgreso::getItemId).collect(Collectors.toSet());
        // Solo 20/20 marca el ejercicio como visto; un intento fallido se puede repetir.
        Set<UUID> cuestionarioDone = inscripcionId == null ? Set.of()
                : cuestionarioAttempts.buscarPorInscripcionIdIn(Set.of(inscripcionId)).stream()
                        .filter(intento -> intento.getScore() >= QUIZ_MAX_SCORE)
                        .map(CuestionarioIntento::getEjercicioId).collect(Collectors.toSet());

        boolean usaLsp = viewer != null && viewer.isUsaLsp();
        List<ModuloVista> moduloViews = new ArrayList<>();
        List<ItemVista> orderedItems = new ArrayList<>();
        for (CursoModulo modulo : modules.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId)) {
            if (!includeHidden && !modulo.isVisible()) continue;
            List<CursoSeccion> moduloSections = sections.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId())
                    .stream().filter(seccion -> includeHidden || seccion.isVisible()).toList();
            Set<UUID> visibleSections = moduloSections.stream()
                    .map(CursoSeccion::getId).collect(Collectors.toSet());
            List<ContenidoItem> moduloItems = items.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId())
                    .stream()
                    .filter(item -> includeHidden || item.isVisible())
                    .filter(item -> visiblePara(item, usaLsp, includeHidden))
                    .toList();

            List<SeccionVista> seccionViews = new ArrayList<>();
            for (CursoSeccion seccion : moduloSections) {
                List<ItemVista> seccionItems = moduloItems.stream()
                        .filter(item -> seccion.getId().equals(item.getSeccionId()))
                        .map(item -> view(item, vistoVideos, vistoItems, cuestionarioDone, bloqueado))
                        .toList();
                seccionItems.forEach(orderedItems::add);
                seccionViews.add(new SeccionVista(seccion.getId(), seccion.getTitulo(),
                        seccion.isVisible(), seccionItems));
            }
            List<ItemVista> looseItems = moduloItems.stream()
                    .filter(item -> item.getSeccionId() == null
                            || !visibleSections.contains(item.getSeccionId()))
                    .map(item -> view(item, vistoVideos, vistoItems, cuestionarioDone, bloqueado))
                    .toList();
            looseItems.forEach(orderedItems::add);

            int done = (int) moduloItems.stream()
                    .filter(item -> isVisto(item, vistoVideos, vistoItems, cuestionarioDone)).count();
            int percent = percent(done, moduloItems.size());
            moduloViews.add(new ModuloVista(modulo.getId(), modulo.getTitulo(), modulo.isVisible(),
                    List.copyOf(seccionViews), looseItems, percent));
        }

        int done = (int) orderedItems.stream().filter(ItemVista::visto).count();
        UUID next = orderedItems.stream()
                .filter(item -> !item.visto() && !item.bloqueado())
                .map(ItemVista::id).findFirst().orElse(null);
        return new Esquema(List.copyOf(moduloViews), orderedItems.size(), done,
                percent(done, orderedItems.size()), next);
    }

    @Override
    @Transactional(readOnly = true)
    public Progreso progreso(UUID inscripcionId, UUID cursoId) {
        Esquema esquema = esquema(cursoId, inscripcionId, viewer(inscripcionId), false);
        return new Progreso(esquema.doneItems(), esquema.totalItems(), esquema.percent());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID nextPending(UUID cursoId, UUID inscripcionId) {
        return esquema(cursoId, inscripcionId, viewer(inscripcionId), false).nextPending();
    }

    @Override
    @Transactional
    public void markVisto(Usuario usuario, String slug, UUID itemId, boolean visto) {
        ItemAccess access = requireAccessibleItem(usuario, slug, itemId);
        Inscripcion inscripcion = access.inscripcion();
        ContenidoItem item = access.item();
        Curso curso = inscripcion.getCurso();

        if ("EJERCICIO".equals(item.getOrigenTipo()) && marcarAlRendir(item)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este ejercicio se marca como visto al rendir el cuestionario");
        }
        if ("VIDEO".equals(item.getOrigenTipo())) {
            UUID videoId = item.getOrigenId();
            if (videoId == null || !videos.existeInCurso(videoId, curso.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El video no pertenece a este curso");
            }
            LeccionProgreso.Clave key = new LeccionProgreso.Clave(inscripcion.getId(), videoId);
            boolean done = leccionProgreso.findById(key).isPresent();
            if (visto && !done) {
                leccionProgreso.save(new LeccionProgreso(inscripcion.getId(), videoId));
            } else if (!visto && done) {
                leccionProgreso.deleteById(key);
            }
        } else {
            ItemProgreso.Clave key = new ItemProgreso.Clave(inscripcion.getId(), itemId);
            boolean done = itemProgreso.findById(key).isPresent();
            if (visto && !done) {
                itemProgreso.save(new ItemProgreso(inscripcion.getId(), itemId));
            } else if (!visto && done) {
                itemProgreso.deleteById(key);
            }
        }
        log.info("Ítem actualizado item={} matrícula={} visto={}", itemId, inscripcion.getId(), visto);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDescarga descargaMaterial(Usuario usuario, String slug, UUID itemId) {
        ItemAccess access = requireAccessibleItem(usuario, slug, itemId);
        ContenidoItem item = access.item();
        if (!"MATERIAL".equals(item.getOrigenTipo())
                || item.getUrl() == null
                || !item.getUrl().startsWith(CursoMaterialStorage.URL_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no tiene un documento descargable");
        }
        String url = item.getUrl();
        MaterialDescarga descarga = new MaterialDescarga(
                fileNombreOf(item, url), materials.contenidoTipo(url), materials.read(url));
        log.info("Documento descargado item={} matrícula={} actor={}",
                itemId, access.inscripcion().getId(), usuario.getId());
        return descarga;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialVista vistaMaterial(Usuario usuario, String slug, UUID itemId) {
        ItemAccess access = requirePdfItem(usuario, slug, itemId);
        ContenidoItem item = access.item();
        String url = item.getUrl();
        MaterialVista view = new MaterialVista(item.getId(), item.getTitulo(),
                fileNombreOf(item, url), materials.contenidoTipo(url));
        log.info("PDF abierto en el visor item={} matrícula={} actor={}",
                itemId, access.inscripcion().getId(), usuario.getId());
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public VideoItemVista videoItem(Usuario usuario, String slug, UUID itemId) {
        ItemAccess access = requireAccessibleItem(usuario, slug, itemId);
        ContenidoItem item = access.item();
        boolean usaLsp = usuario != null && usuario.isUsaLsp();
        String tipo = item.getOrigenTipo();

        if ("VIDEO_LSP".equals(tipo)) {
            if (!usaLsp) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La versión en lengua de señas no está disponible para esta cuenta");
            }
            if (item.getYoutubeId() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La clase en lengua de señas todavía no tiene video");
            }
            return new VideoItemVista(item.getId(), null, item.getYoutubeId(),
                    item.getTitulo(), true);
        }
        if ("VIDEO".equals(tipo)) {
            if (usaLsp) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Esta cuenta ve las clases en lengua de señas");
            }
            UUID videoId = item.getOrigenId();
            UUID cursoId = access.inscripcion().getCurso().getId();
            if (videoId == null || !videos.existeInCurso(videoId, cursoId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El video no pertenece a este curso");
            }
            Video video = videos.findById(videoId)
                    .orElseThrow(() -> missing("Video no encontrado"));
            return new VideoItemVista(item.getId(), videoId, video.getYoutubeId(),
                    video.getTitulo(), false);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "El índice seleccionado no tiene video");
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDescarga leidoPdf(Usuario usuario, String slug, UUID itemId) {
        ItemAccess access = requirePdfItem(usuario, slug, itemId);
        ContenidoItem item = access.item();
        String url = item.getUrl();
        MaterialDescarga pdf = new MaterialDescarga(fileNombreOf(item, url),
                materials.contenidoTipo(url), materials.read(url));
        log.info("PDF servido al visor item={} matrícula={} actor={}",
                itemId, access.inscripcion().getId(), usuario.getId());
        return pdf;
    }

    /** La misma entrada privada que la descarga, limitada a PDFs reales. */
    private ItemAccess requirePdfItem(Usuario usuario, String slug, UUID itemId) {
        ItemAccess access = requireAccessibleItem(usuario, slug, itemId);
        ContenidoItem item = access.item();
        String url = item.getUrl();
        if (!"MATERIAL".equals(item.getOrigenTipo()) || url == null
                || !url.startsWith(CursoMaterialStorage.URL_PREFIX)
                || !"application/pdf".equals(materials.contenidoTipo(url))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no tiene un PDF para abrir");
        }
        return access;
    }

    private String fileNombreOf(ContenidoItem item, String url) {
        return item.getFileName() == null ? url.substring(url.lastIndexOf('/') + 1)
                : item.getFileName();
    }

    private ItemAccess requireAccessibleItem(Usuario usuario, String slug, UUID itemId) {
        checkNotNull(usuario, "user");
        checkNotNull(itemId, "itemId");
        Inscripcion inscripcion = enrollments.buscarPorUsuarioAndCursoSlug(usuario, requeridoSlug(slug))
                .orElseThrow(() -> missing("No estás inscripto en ese curso"));
        Curso curso = inscripcion.getCurso();
        if (!curso.isFree() && !usuario.isPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso requiere plan Premium");
        }
        ContenidoItem item = items.findById(itemId)
                .orElseThrow(() -> missing("El ítem no existe"));
        if (!item.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El ítem no está disponible");
        }
        CursoModulo modulo = modules.findById(item.getModuloId())
                .orElseThrow(() -> missing("El módulo no existe"));
        if (!curso.getId().equals(modulo.getCursoId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no pertenece a este curso");
        }
        return new ItemAccess(inscripcion, item);
    }

    /** Las cuentas con LSP ven solo las versiones en señas; el resto, los videos normales. */
    private boolean visiblePara(ContenidoItem item, boolean usaLsp, boolean includeHidden) {
        if (includeHidden) return true;
        if ("VIDEO_LSP".equals(item.getOrigenTipo())) return usaLsp;
        return !("VIDEO".equals(item.getOrigenTipo()) && usaLsp);
    }

    /** Un ejercicio se marca solo al rendir o a mano, según lo configuró el administrador. */
    private boolean marcarAlRendir(ContenidoItem item) {
        if (item.getOrigenId() == null) return true;
        return exercises.findById(item.getOrigenId())
                .map(Ejercicio::isMarcarAlRendir)
                .orElse(true);
    }

    private ItemVista view(ContenidoItem item, Set<UUID> vistoVideos, Set<UUID> vistoItems,
                          Set<UUID> cuestionarioDone, boolean bloqueado) {
        boolean done = "EJERCICIO".equals(item.getOrigenTipo()) && marcarAlRendir(item)
                && item.getOrigenId() != null && cuestionarioDone.contains(item.getOrigenId());
        return new ItemVista(item.getId(), item.getOrigenTipo(), item.getTitulo(),
                item.getDescripcion(), item.getUrl(), item.getYoutubeId(),
                item.getDuracionSegundos(), item.isVisible(),
                isVisto(item, vistoVideos, vistoItems, cuestionarioDone), bloqueado, item.getOrigenId(),
                item.getFileName(), done);
    }

    private boolean isVisto(ContenidoItem item, Set<UUID> vistoVideos, Set<UUID> vistoItems,
                           Set<UUID> cuestionarioDone) {
        if ("VIDEO".equals(item.getOrigenTipo())) {
            return vistoVideos.contains(item.getOrigenId());
        }
        if ("EJERCICIO".equals(item.getOrigenTipo()) && marcarAlRendir(item)) {
            return item.getOrigenId() != null && cuestionarioDone.contains(item.getOrigenId());
        }
        return vistoItems.contains(item.getId());
    }

    private int percent(int done, int total) {
        return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private Usuario viewer(UUID inscripcionId) {
        if (inscripcionId == null) return null;
        return enrollments.findById(inscripcionId).map(Inscripcion::getUsuario).orElse(null);
    }

    private String requeridoSlug(String slug) {
        String value = StringUtils.trimToNull(slug);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso es obligatorio");
        }
        return value;
    }

    private ResponseStatusException missing(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record ItemAccess(Inscripcion inscripcion, ContenidoItem item) {
    }
}
