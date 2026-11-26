package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.CursoRecurso;
import com.mundomanitas.api.model.CursoSeccion;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.CursoRecursoRepository;
import com.mundomanitas.api.repository.CursoSeccionRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import com.mundomanitas.api.service.ContenidoEnlaces;
import com.mundomanitas.api.service.CursoMaterialStorage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Implementación de la carga administrativa de módulos, secciones e ítems. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminCursoContenidoServiceImpl implements AdminCursoContenidoService {

    private static final Logger log = LoggerFactory.getLogger(AdminCursoContenidoServiceImpl.class);

    private final CursoRepository courses;
    private final CursoModuloRepository modules;
    private final CursoSeccionRepository sections;
    private final ContenidoItemRepository items;
    private final CursoRecursoRepository resources;
    private final VideoRepository videos;
    private final EjercicioRepository exercises;
    private final AdminAuditoriaService auditoria;
    private final CursoMaterialStorage materials;

    public AdminCursoContenidoServiceImpl(
            CursoRepository courses,
            CursoModuloRepository modules,
            CursoSeccionRepository sections,
            ContenidoItemRepository items,
            CursoRecursoRepository resources,
            VideoRepository videos,
            EjercicioRepository exercises,
            AdminAuditoriaService auditoria,
            CursoMaterialStorage materials
    ) {
        this.courses = courses;
        this.modules = modules;
        this.sections = sections;
        this.items = items;
        this.resources = resources;
        this.videos = videos;
        this.exercises = exercises;
        this.auditoria = auditoria;
        this.materials = materials;
    }

    @Override
    @Transactional
    public UUID createSeccion(UUID moduloId, SeccionFormulario formulario, UUID actor) {
        requireModulo(moduloId);
        CursoSeccion seccion = new CursoSeccion(moduloId, requireText(formulario.titulo(), "título"),
                formulario.posicion(), formulario.visible());
        sections.save(seccion);
        auditoria.record(actor, "CREATE", "SECCION", seccion.getId(), seccion.getTitulo());
        log.info("Sección creada id={} módulo={} actor={}", seccion.getId(), moduloId, actor);
        return seccion.getId();
    }

    @Override
    @Transactional
    public void updateSeccion(UUID seccionId, SeccionFormulario formulario, UUID actor) {
        CursoSeccion seccion = requireSeccion(seccionId);
        seccion.update(requireText(formulario.titulo(), "título"), formulario.posicion(), formulario.visible());
        sections.save(seccion);
        auditoria.record(actor, "UPDATE", "SECCION", seccionId, seccion.getTitulo());
        log.info("Sección actualizada id={} actor={}", seccionId, actor);
    }

    @Override
    @Transactional
    public void deleteSeccion(UUID seccionId, UUID actor) {
        CursoSeccion seccion = requireSeccion(seccionId);
        String titulo = seccion.getTitulo();
        sections.delete(seccion);
        auditoria.record(actor, "DELETE", "SECCION", seccionId, titulo);
        log.info("Sección eliminada id={} actor={}", seccionId, actor);
    }

    @Override
    @Transactional
    public UUID createItem(ItemFormulario formulario, UUID actor) {
        checkNotNull(formulario, "form");
        String type = requireTipo(formulario.type());
        String titulo = requireText(formulario.titulo(), "título");
        String url = null;
        String youtubeId = null;
        String prompt = null;
        if ("MATERIAL".equals(type)) {
            url = ContenidoEnlaces.https(formulario.url(), "enlace");
        } else if ("VIDEO".equals(type) || "VIDEO_LSP".equals(type)) {
            youtubeId = ContenidoEnlaces.youtubeId(formulario.youtubeUrl());
        } else {
            prompt = requireText(formulario.descripcion(), "consigna");
        }
        requireModulo(formulario.moduloId());
        requireSeccionInModulo(formulario.seccionId(), formulario.moduloId());

        ContenidoItem item;
        if ("MATERIAL".equals(type)) {
            item = new ContenidoItem(formulario.moduloId(), formulario.seccionId(), type, null, titulo,
                    StringUtils.trimToNull(formulario.descripcion()), url, null,
                    formulario.duracionSegundos(), formulario.posicion(), formulario.visible());
            items.save(item);
        } else if ("VIDEO".equals(type)) {
            Video video = new Video(formulario.moduloId(), titulo, youtubeId, formulario.posicion());
            videos.save(video);
            videos.flush();
            item = items.buscarPorOrigenTipoAndOrigenId(type, video.getId());
            applyMetadata(item, formulario);
        } else if ("VIDEO_LSP".equals(type)) {
            // La versión en señas vive sola en content_items: no duplica la fila de videos.
            item = new ContenidoItem(formulario.moduloId(), formulario.seccionId(), type, null,
                    titulo, StringUtils.trimToNull(formulario.descripcion()), null, youtubeId,
                    formulario.duracionSegundos(), formulario.posicion(), formulario.visible());
            items.save(item);
        } else {
            Ejercicio ejercicio = new Ejercicio(formulario.moduloId(), titulo, prompt, formulario.posicion());
            exercises.save(ejercicio);
            exercises.flush();
            item = items.buscarPorOrigenTipoAndOrigenId(type, ejercicio.getId());
            applyMetadata(item, formulario);
        }
        auditoria.record(actor, "CREATE", "ITEM", item.getId(), titulo);
        log.info("Ítem creado id={} tipo={} actor={}", item.getId(), type, actor);
        return item.getId();
    }

    @Override
    @Transactional
    public UUID createDocument(DocumentFormulario formulario, UUID actor) {
        checkNotNull(formulario, "form");
        String titulo = requireText(formulario.titulo(), "título");
        requireModulo(formulario.moduloId());
        requireSeccionInModulo(formulario.seccionId(), formulario.moduloId());

        String url = materials.store(formulario.file());
        String originalNombre = originalNombre(formulario);
        ContenidoItem item = new ContenidoItem(formulario.moduloId(), formulario.seccionId(), "MATERIAL", null,
                titulo, StringUtils.trimToNull(formulario.descripcion()), url, originalNombre, null,
                formulario.duracionSegundos(), formulario.posicion(), formulario.visible());
        items.save(item);
        auditoria.record(actor, "CREATE", "ITEM", item.getId(), titulo);
        log.info("Documento creado id={} archivo={} actor={}", item.getId(), originalNombre, actor);
        return item.getId();
    }

    @Override
    @Transactional
    public void updateItem(UUID itemId, ItemFormulario formulario, UUID actor) {
        checkNotNull(formulario, "form");
        ContenidoItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        String type = item.getOrigenTipo();
        String titulo = requireText(formulario.titulo(), "título");
        requireModulo(formulario.moduloId());
        requireSeccionInModulo(formulario.seccionId(), formulario.moduloId());

        if ("MATERIAL".equals(type)) {
            String descripcion = StringUtils.trimToNull(formulario.descripcion());
            String newUrl = StringUtils.trimToNull(formulario.url());
            if (newUrl == null && item.getUrl() != null
                    && item.getUrl().startsWith(CursoMaterialStorage.URL_PREFIX)) {
                item.updateDocument(titulo, descripcion, item.getUrl(), item.getFileName());
            } else {
                if (item.getUrl() != null
                        && item.getUrl().startsWith(CursoMaterialStorage.URL_PREFIX)) {
                    materials.delete(item.getUrl());
                }
                item.updateDocument(titulo, descripcion, ContenidoEnlaces.https(newUrl, "enlace"), null);
            }
        } else if ("VIDEO".equals(type)) {
            Video video = videos.findById(item.getOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Video no encontrado"));
            video.update(titulo, ContenidoEnlaces.youtubeId(formulario.youtubeUrl()), formulario.posicion());
            video.moveTo(formulario.moduloId());
            videos.save(video);
            videos.flush();
            item = items.buscarPorOrigenTipoAndOrigenId(type, item.getOrigenId());
        } else if ("VIDEO_LSP".equals(type)) {
            item.updateVideoLsp(titulo, StringUtils.trimToNull(formulario.descripcion()),
                    ContenidoEnlaces.youtubeId(formulario.youtubeUrl()));
        } else {
            Ejercicio ejercicio = exercises.findById(item.getOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado"));
            ejercicio.update(titulo, requireText(formulario.descripcion(), "consigna"), formulario.posicion());
            ejercicio.moveTo(formulario.moduloId());
            exercises.save(ejercicio);
            exercises.flush();
            item = items.buscarPorOrigenTipoAndOrigenId(type, item.getOrigenId());
        }
        applyMetadata(item, formulario);
        items.save(item);
        auditoria.record(actor, "UPDATE", "ITEM", itemId, titulo);
        log.info("Ítem actualizado id={} actor={}", itemId, actor);
    }

    @Override
    @Transactional
    public void deleteItem(UUID itemId, UUID actor) {
        ContenidoItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        String titulo = item.getTitulo();
        if ("MATERIAL".equals(item.getOrigenTipo())) {
            materials.delete(item.getUrl());
        }
        if ("VIDEO".equals(item.getOrigenTipo()) && item.getOrigenId() != null) {
            videos.findById(item.getOrigenId()).ifPresent(videos::delete);
        } else if ("EJERCICIO".equals(item.getOrigenTipo()) && item.getOrigenId() != null) {
            exercises.findById(item.getOrigenId()).ifPresent(exercises::delete);
        } else {
            items.delete(item);
        }
        auditoria.record(actor, "DELETE", "ITEM", itemId, titulo);
        log.info("Ítem eliminado id={} actor={}", itemId, actor);
    }

    @Override
    @Transactional
    public void moveSeccion(UUID seccionId, boolean up, UUID actor) {
        CursoSeccion seccion = requireSeccion(seccionId);
        List<CursoSeccion> siblings = sections.buscarPorModuloIdOrdenarPorPosicionAsc(seccion.getModuloId());
        int index = indexOf(siblings, CursoSeccion::getId, seccionId);
        CursoSeccion neighbour = neighbour(siblings, index, up);
        if (neighbour == null) return;
        int posicion = seccion.getPosicion();
        seccion.setPosicion(neighbour.getPosicion());
        neighbour.setPosicion(posicion);
        sections.save(seccion);
        sections.save(neighbour);
        auditoria.record(actor, "UPDATE", "SECCION", seccionId, up ? "SUBIR" : "BAJAR");
        log.info("Sección reordenada id={} actor={}", seccionId, actor);
    }

    @Override
    @Transactional
    public void moveItem(UUID itemId, boolean up, UUID actor) {
        ContenidoItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        List<ContenidoItem> siblings = items.buscarPorModuloIdOrdenarPorPosicionAsc(item.getModuloId());
        int index = indexOf(siblings, ContenidoItem::getId, itemId);
        ContenidoItem neighbour = neighbour(siblings, index, up);
        if (neighbour == null) return;
        int posicion = item.getPosicion();
        item.setPosicion(neighbour.getPosicion());
        neighbour.setPosicion(posicion);
        syncOrigenPosicion(item);
        syncOrigenPosicion(neighbour);
        items.save(item);
        items.save(neighbour);
        auditoria.record(actor, "UPDATE", "ITEM", itemId, up ? "SUBIR" : "BAJAR");
        log.info("Ítem reordenado id={} actor={}", itemId, actor);
    }

    @Override
    @Transactional
    public void saveSilabo(UUID cursoId, SilaboFormulario formulario, UUID actor) {
        checkNotNull(formulario, "form");
        Curso curso = courses.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));
        curso.updateSilabo(StringUtils.trimToNull(formulario.objectives()),
                StringUtils.trimToNull(formulario.requirements()));
        resources.deleteAll(resources.buscarPorCursoIdOrdenarPorPosicionAsc(cursoId));
        for (RecursoFormulario recurso : formulario.resources()) {
            resources.save(new CursoRecurso(cursoId, requireText(recurso.titulo(), "título"),
                    ContenidoEnlaces.https(recurso.url(), "enlace"), recurso.posicion()));
        }
        auditoria.record(actor, "UPDATE", "SILABO", cursoId, "SÍLABO");
        log.info("Sílabo actualizado curso={} actor={}", cursoId, actor);
    }

    @Override
    @Transactional
    public UUID createRecurso(UUID cursoId, RecursoFormulario formulario, UUID actor) {
        courses.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));
        CursoRecurso recurso = new CursoRecurso(cursoId,
                requireText(formulario.titulo(), "título"),
                ContenidoEnlaces.https(formulario.url(), "enlace"), formulario.posicion());
        resources.save(recurso);
        auditoria.record(actor, "CREATE", "RECURSO", recurso.getId(), recurso.getTitulo());
        log.info("Recurso creado id={} curso={} actor={}", recurso.getId(), cursoId, actor);
        return recurso.getId();
    }

    @Override
    @Transactional
    public void deleteRecurso(UUID recursoId, UUID actor) {
        CursoRecurso recurso = resources.findById(recursoId)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado"));
        String titulo = recurso.getTitulo();
        resources.delete(recurso);
        auditoria.record(actor, "DELETE", "RECURSO", recursoId, titulo);
        log.info("Recurso eliminado id={} actor={}", recursoId, actor);
    }

    private void applyMetadata(ContenidoItem item, ItemFormulario formulario) {
        checkNotNull(item, "El ítem correspondiente no existe en la base");
        item.moveTo(formulario.moduloId(), formulario.seccionId());
        item.setMetadata(formulario.duracionSegundos(), formulario.visible(), formulario.posicion());
        items.save(item);
    }

    private void syncOrigenPosicion(ContenidoItem item) {
        if (item.getOrigenId() == null) return;
        if ("VIDEO".equals(item.getOrigenTipo())) {
            videos.findById(item.getOrigenId()).ifPresent(video -> {
                video.setPosicion(item.getPosicion());
                videos.save(video);
            });
        } else if ("EJERCICIO".equals(item.getOrigenTipo())) {
            exercises.findById(item.getOrigenId()).ifPresent(ejercicio -> {
                ejercicio.update(ejercicio.getTitulo(), ejercicio.getPrompt(), item.getPosicion());
                exercises.save(ejercicio);
            });
        }
    }

    private void requireSeccionInModulo(UUID seccionId, UUID moduloId) {
        if (seccionId == null) return;
        CursoSeccion seccion = requireSeccion(seccionId);
        checkArgument(seccion.getModuloId().equals(moduloId),
                "La sección no pertenece al módulo");
    }

    private CursoModulo requireModulo(UUID moduloId) {
        checkNotNull(moduloId, "moduleId");
        return modules.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("Módulo no encontrado"));
    }

    private CursoSeccion requireSeccion(UUID seccionId) {
        checkNotNull(seccionId, "sectionId");
        return sections.findById(seccionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
    }

    private String requireTipo(String type) {
        String value = StringUtils.trimToNull(type);
        checkArgument("VIDEO".equals(value) || "VIDEO_LSP".equals(value)
                || "MATERIAL".equals(value) || "EJERCICIO".equals(value),
                "Tipo de ítem no válido");
        return value;
    }

    private String requireText(String value, String field) {
        String text = StringUtils.trimToNull(value);
        checkArgument(text != null, "El campo " + field + " es obligatorio");
        return text;
    }

    private String originalNombre(DocumentFormulario formulario) {
        String original = formulario.file() == null ? null : formulario.file().getOriginalFilename();
        if (original == null || original.isBlank()) return "documento";
        try {
            return Path.of(original.replace('\\', '/')).getFileName().toString();
        } catch (InvalidPathException exception) {
            return "documento";
        }
    }

    private <T, ID> int indexOf(List<T> values, java.util.function.Function<T, ID> id, ID target) {
        for (int i = 0; i < values.size(); i++) {
            if (id.apply(values.get(i)).equals(target)) return i;
        }
        throw new IllegalArgumentException("No se encontró el elemento a reordenar");
    }

    private <T> T neighbour(List<T> values, int index, boolean up) {
        int target = up ? index - 1 : index + 1;
        return target < 0 || target >= values.size() ? null : values.get(target);
    }
}
