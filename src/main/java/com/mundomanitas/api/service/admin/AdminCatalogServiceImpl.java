package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminFilaCurso;
import com.mundomanitas.api.dto.admin.AdminOpcion;
import com.mundomanitas.api.dto.admin.AdminUsuarioFormulario;
import com.mundomanitas.api.dto.admin.AdminFilaUsuario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.FilaEjercicio;
import com.mundomanitas.api.dto.admin.FilaModulo;
import com.mundomanitas.api.dto.admin.FilaVideo;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Ejercicio;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.EjercicioRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import com.mundomanitas.api.service.YouTubeLink;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Implementación del CRUD administrativo de cuentas, cursos y contenido. */
@Service
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private static final Logger log = LoggerFactory.getLogger(AdminCatalogServiceImpl.class);

    private final UsuarioRepository users;
    private final CursoRepository courses;
    private final CursoModuloRepository modules;
    private final VideoRepository videos;
    private final EjercicioRepository exercises;
    private final InscripcionRepository enrollments;
    private final CertificadoRepository certificates;
    private final PasswordEncoder encoder;
    private final AdminAuditoriaService auditoria;
    private final AdminFormato format;

    public AdminCatalogServiceImpl(
            UsuarioRepository users,
            CursoRepository courses,
            CursoModuloRepository modules,
            VideoRepository videos,
            EjercicioRepository exercises,
            InscripcionRepository enrollments,
            CertificadoRepository certificates,
            PasswordEncoder encoder,
            AdminAuditoriaService auditoria,
            AdminFormato format
    ) {
        this.users = users;
        this.courses = courses;
        this.modules = modules;
        this.videos = videos;
        this.exercises = exercises;
        this.enrollments = enrollments;
        this.certificates = certificates;
        this.encoder = encoder;
        this.auditoria = auditoria;
        this.format = format;
    }

    /* ------------------------------ cuentas ------------------------------ */

    @Transactional(readOnly = true)
    public Page<AdminFilaUsuario> users(String rol, String term, Pageable pageable, UUID actualId) {
        String cleanRol = blankToNull(rol);
        String cleanTerm = blankToNull(term);
        return users.search(cleanRol, cleanTerm, pageable).map(usuario -> new AdminFilaUsuario(
                usuario.getId(), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRol(),
                usuario.getPlan().getLabel(), usuario.isActivo(),
                format.completo(usuario.getFechaCreacion()), usuario.getId().equals(actualId)));
    }

    @Transactional(readOnly = true)
    public AdminUsuarioFormulario usuarioFormulario(UUID id) {
        Usuario usuario = requireUsuario(id);
        return new AdminUsuarioFormulario(usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRol(),
                usuario.isActivo(), "");
    }

    @Transactional
    public void updateUsuario(UUID id, AdminUsuarioFormulario formulario, UUID actor) {
        Usuario usuario = requireUsuario(id);
        users.buscarPorEmailIgnorandoMayusculas(formulario.trimmedEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
                });
        usuario.changeCompletoNombre(formulario.trimmedNombre());
        usuario.changeEmail(formulario.trimmedEmail());
        usuario.changeRol(formulario.rol());
        usuario.changeActivo(formulario.activo());
        if (formulario.contrasena() != null && !formulario.contrasena().isBlank()) {
            usuario.changeContrasenaHash(encoder.encode(formulario.contrasena()));
        }
        auditoria.record(actor, "UPDATE", "USUARIO", id, formulario.trimmedEmail());
        log.info("Usuario actualizado id={} actor={}", id, actor);
    }

    /* ------------------------------ cursos ------------------------------- */

    @Override
    @Transactional(readOnly = true)
    public String slugFromTitulo(String titulo) {
        String slug = StringUtils.trimToEmpty(titulo).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        checkArgument(!slug.isBlank(), "El título no permite generar un identificador.");
        return slug;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFilaCurso> courses(String term, Pageable pageable) {
        String clean = blankToNull(term);
        Page<Curso> page = clean == null
                ? courses.buscarAllPorOrdenarPorPosicionAsc(pageable)
                : courses.buscarPorTituloIgnorandoMayusculasContieneOrSlugIgnorandoMayusculasContiene(
                        clean, clean, pageable);
        Map<UUID, Long> totals = new java.util.HashMap<>();
        for (Object[] row : enrollments.contarPorCurso()) {
            totals.put(UUID.fromString(row[0].toString()), ((Number) row[1]).longValue());
        }
        return page.map(curso -> new AdminFilaCurso(
                curso.getId(), curso.getSlug(), curso.getTitulo(), curso.getCategoria(),
                leccionContar(curso.getId()), curso.getEstado(), curso.priceLabel(),
                curso.getPosicion(), totals.getOrDefault(curso.getId(), 0L), curso.getImagen()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCursoFormulario cursoFormulario(UUID id) {
        Curso curso = requireCurso(id);
        return new AdminCursoFormulario(curso.getSlug(), curso.getTitulo(), curso.getNivel(),
                curso.isFree(), curso.getCategoria(), curso.getResumen(),
                curso.getImagen(), curso.getPosicion());
    }

    @Override
    @Transactional
    public UUID createCurso(AdminCursoFormulario formulario, UUID actor) {
        String slug = blankToNull(formulario.slug()) == null
                ? slugFromTitulo(formulario.titulo()) : formulario.slug().trim();
        checkArgument(courses.buscarPorSlug(slug).isEmpty(),
                "Ya existe un curso con ese identificador.");
        String titulo = requireText(formulario.titulo(), "título");
        String nivel = requireText(formulario.nivel(), "nivel");
        String categoria = requireText(formulario.categoria(), "categoría");
        String resumen = requireText(formulario.resumen(), "resumen");
        Curso curso = new Curso(slug, titulo, nivel, 0, formulario.free(), categoria,
                resumen, blankToNull(formulario.imagen()), formulario.posicion());
        // El panel da por publicada la ficha apenas se crea: el alumnado debe
        // poder verla y matricularse sin un paso de publicación escondido.
        curso.publicar();
        courses.save(curso);
        auditoria.record(actor, "CREATE", "CURSO", curso.getId(), titulo);
        log.info("Curso creado id={} actor={} estado={}", curso.getId(), actor,
                curso.getEstado());
        return curso.getId();
    }

    @Override
    @Transactional
    public void updateCurso(UUID id, AdminCursoFormulario formulario, UUID actor) {
        Curso curso = requireCurso(id);
        String slug = blankToNull(formulario.slug()) == null
                ? slugFromTitulo(formulario.titulo()) : formulario.slug().trim();
        courses.buscarPorSlug(slug).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ya existe un curso con ese identificador.");
                });
        String titulo = requireText(formulario.titulo(), "título");
        curso.update(slug, titulo, requireText(formulario.nivel(), "nivel"), formulario.free(),
                requireText(formulario.categoria(), "categoría"), requireText(formulario.resumen(), "resumen"),
                blankToNull(formulario.imagen()), formulario.posicion());
        auditoria.record(actor, "UPDATE", "CURSO", id, titulo);
        log.info("Curso actualizado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public String updateCursoPortada(UUID id, String imagen, UUID actor) {
        checkArgument(imagen != null
                        && imagen.matches("/uploads/covers/[0-9a-f-]+\\.(jpg|png|webp)"),
                "La portada debe ser una imagen local.");
        Curso curso = requireCurso(id);
        String previous = curso.getImagen();
        curso.changeImagen(imagen);
        auditoria.record(actor, "UPDATE", "CURSO", id, "PORTADA");
        log.info("Portada actualizada id={} actor={} anterior={}", id, actor, previous);
        return previous;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicarLista publicarLista(UUID id) {
        Curso curso = requireCurso(id);
        List<String> pending = new ArrayList<>();
        if (StringUtils.isBlank(curso.getResumen())) pending.add("resumen");
        if (StringUtils.isBlank(curso.getImagen())) pending.add("portada");
        List<CursoModulo> modulesOfCurso = modules.buscarPorCursoIdOrdenarPorPosicionAsc(id);
        if (modulesOfCurso.isEmpty() || modulesOfCurso.stream().noneMatch(modulo ->
                !videos.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId()).isEmpty())) {
            pending.add("video");
        }
        // Publicar ya no depende de estas mejoras: solo se informan en la ficha.
        return new PublicarLista(List.copyOf(pending), true);
    }

    @Override
    @Transactional(readOnly = true)
    public String cursoEstado(UUID id) {
        return requireCurso(id).getEstado();
    }

    @Override
    @Transactional
    public void setPublished(UUID id, boolean published, UUID actor) {
        Curso curso = requireCurso(id);
        if (published) {
            curso.publicar();
        } else {
            curso.unpublish();
        }
        auditoria.record(actor, "UPDATE", "CURSO", id,
                published ? "PUBLICADO" : "BORRADOR");
        log.info("Estado del curso id={} actor={} publicado={}", id, actor, published);
    }

    @Override
    @Transactional(readOnly = true)
    public EliminacionImpacto eliminacionImpacto(UUID id) {
        requireCurso(id);
        List<CursoModulo> modulesOfCurso = modules.buscarPorCursoIdOrdenarPorPosicionAsc(id);
        int videosContar = videos.contarPorCursoId(id);
        int exercisesContar = modulesOfCurso.stream()
                .mapToInt(modulo -> exercises.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId()).size())
                .sum();
        return new EliminacionImpacto(modulesOfCurso.size(), videosContar, exercisesContar,
                enrollments.contarPorCursoId(id), certificates.contarPorCursoId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public int leccionContar(UUID cursoId) {
        return videos.contarPorCursoId(cursoId);
    }

    @Override
    @Transactional
    public void moveModulo(UUID id, boolean up, UUID actor) {
        CursoModulo modulo = modules.findById(id).orElseThrow();
        List<CursoModulo> ordered = new ArrayList<>(
                modules.buscarPorCursoIdOrdenarPorPosicionAsc(modulo.getCursoId()));
        int index = ordered.indexOf(modulo);
        checkArgument(index >= 0, "El módulo no pertenece al curso.");
        int target = up ? index - 1 : index + 1;
        checkArgument(target >= 0 && target < ordered.size(), "No se puede mover más.");
        CursoModulo other = ordered.get(target);
        int posicion = modulo.getPosicion();
        modulo.setPosicion(other.getPosicion());
        other.setPosicion(posicion);
        auditoria.record(actor, "UPDATE", "MODULO", id, up ? "SUBIR" : "BAJAR");
        log.info("Módulo reordenado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public void moveVideo(UUID id, boolean up, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        List<Video> ordered = new ArrayList<>(
                videos.buscarPorModuloIdOrdenarPorPosicionAsc(video.getModuloId()));
        int index = ordered.indexOf(video);
        checkArgument(index >= 0, "El video no pertenece al módulo.");
        int target = up ? index - 1 : index + 1;
        checkArgument(target >= 0 && target < ordered.size(), "No se puede mover más.");
        Video other = ordered.get(target);
        int posicion = video.getPosicion();
        video.setPosicion(other.getPosicion());
        other.setPosicion(posicion);
        auditoria.record(actor, "UPDATE", "VIDEO", id, up ? "SUBIR" : "BAJAR");
        log.info("Video reordenado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public void deleteCurso(UUID id, UUID actor) {
        Curso curso = requireCurso(id);
        EliminacionImpacto impacto = eliminacionImpacto(id);
        checkArgument(impacto.enrollments() == 0,
                "El curso tiene inscritos: despublicalo o archivalo en lugar de borrarlo.");
        String titulo = curso.getTitulo();
        courses.delete(curso);
        auditoria.record(actor, "DELETE", "CURSO", id, titulo);
        log.info("Curso eliminado id={} actor={} impacto={}", id, actor, impacto);
    }

    /* ------------------------------ módulos ------------------------------ */

    @Transactional(readOnly = true)
    public Page<FilaModulo> modules(UUID cursoId, String term, Pageable pageable) {
        Map<UUID, Curso> cursoMap = cursoMap();
        Page<CursoModulo> page = modules.search(cursoId, blankToNull(term), pageable);
        return page.map(modulo -> new FilaModulo(
                modulo.getId(),
                cursoMap.containsKey(modulo.getCursoId())
                        ? cursoMap.get(modulo.getCursoId()).getTitulo()
                        : "Curso eliminado",
                modulo.getTitulo(), modulo.getPosicion(),
                videos.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId()).size(),
                exercises.buscarPorModuloIdOrdenarPorPosicionAsc(modulo.getId()).size()));
    }

    @Transactional(readOnly = true)
    public AdminModuloFormulario moduloFormulario(UUID id) {
        CursoModulo modulo = modules.findById(id).orElseThrow();
        return new AdminModuloFormulario(modulo.getCursoId(), modulo.getTitulo(), modulo.getPosicion());
    }

    @Transactional
    public UUID createModulo(AdminModuloFormulario formulario, UUID actor) {
        requireCurso(formulario.cursoId());
        CursoModulo modulo = new CursoModulo(formulario.cursoId(), formulario.titulo().trim(), formulario.posicion());
        modules.save(modulo);
        auditoria.record(actor, "CREATE", "MODULO", modulo.getId(), formulario.titulo().trim());
        log.info("Módulo creado id={} actor={}", modulo.getId(), actor);
        return modulo.getId();
    }

    @Transactional
    public void updateModulo(UUID id, AdminModuloFormulario formulario, UUID actor) {
        CursoModulo modulo = modules.findById(id).orElseThrow();
        requireCurso(formulario.cursoId());
        modulo.update(formulario.titulo().trim(), formulario.posicion());
        modulo.moveTo(formulario.cursoId());
        auditoria.record(actor, "UPDATE", "MODULO", id, formulario.titulo().trim());
        log.info("Módulo actualizado id={} actor={}", id, actor);
    }

    @Transactional
    public void deleteModulo(UUID id, UUID actor) {
        CursoModulo modulo = modules.findById(id).orElseThrow();
        String titulo = modulo.getTitulo();
        modules.delete(modulo);
        auditoria.record(actor, "DELETE", "MODULO", id, titulo);
        log.info("Módulo eliminado id={} actor={}", id, actor);
    }

    /* ------------------------------- videos ------------------------------ */

    @Override
    @Transactional(readOnly = true)
    public Page<FilaVideo> videos(UUID moduloId, String term, Pageable pageable) {
        Map<UUID, CursoModulo> moduloMap = moduloMap();
        Map<UUID, Curso> cursoMap = cursoMap();
        return videos.search(moduloId, blankToNull(term), pageable).map(video -> {
            CursoModulo modulo = moduloMap.get(video.getModuloId());
            Curso curso = modulo == null ? null : cursoMap.get(modulo.getCursoId());
            return new FilaVideo(video.getId(),
                    modulo == null ? "Módulo eliminado" : modulo.getTitulo(),
                    curso == null ? "Curso eliminado" : curso.getTitulo(),
                    video.getTitulo(),
                    youtubeEnlace(video),
                    video.getPosicion());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVideoFormulario videoFormulario(UUID id) {
        Video video = videos.findById(id).orElseThrow();
        return new AdminVideoFormulario(video.getModuloId(), video.getTitulo(), video.getPosicion(),
                youtubeEnlace(video));
    }

    @Override
    @Transactional
    public UUID createVideo(AdminVideoFormulario formulario, UUID actor) {
        requireModulo(formulario.moduloId());
        Video video = new Video(formulario.moduloId(), requireText(formulario.titulo(), "título"),
                youtubeId(formulario.youtubeUrl()), formulario.posicion());
        videos.save(video);
        videos.flush();
        refreshLessons(video.getModuloId());
        auditoria.record(actor, "CREATE", "VIDEO", video.getId(), formulario.titulo().trim());
        log.info("Video creado id={} actor={} youtube={}", video.getId(), actor,
                video.getYoutubeId());
        return video.getId();
    }

    @Override
    @Transactional
    public void updateVideo(UUID id, AdminVideoFormulario formulario, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        UUID previousModulo = video.getModuloId();
        requireModulo(formulario.moduloId());
        video.update(requireText(formulario.titulo(), "título"),
                youtubeId(formulario.youtubeUrl()), formulario.posicion());
        video.moveTo(formulario.moduloId());
        if (!previousModulo.equals(formulario.moduloId())) {
            refreshLessons(previousModulo);
        }
        refreshLessons(formulario.moduloId());
        auditoria.record(actor, "UPDATE", "VIDEO", id, formulario.titulo().trim());
        log.info("Video actualizado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public void deleteVideo(UUID id, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        UUID moduloId = video.getModuloId();
        String titulo = video.getTitulo();
        videos.delete(video);
        refreshLessons(moduloId);
        auditoria.record(actor, "DELETE", "VIDEO", id, titulo);
        log.info("Video eliminado id={} actor={}", id, actor);
    }

    private String youtubeId(String youtubeUrl) {
        String enlace = AdminVideoFormulario.clean(youtubeUrl);
        String id = enlace == null ? null : YouTubeLink.id(enlace);
        checkArgument(id != null, "El enlace de YouTube no es válido.");
        return id;
    }

    private String youtubeEnlace(Video video) {
        return video.getYoutubeId() == null ? null : "https://youtu.be/" + video.getYoutubeId();
    }

    private void refreshLessons(UUID moduloId) {
        modules.findById(moduloId).ifPresent(modulo -> {
            Curso curso = courses.findById(modulo.getCursoId()).orElse(null);
            if (curso != null) curso.refreshLessons(videos.contarPorCursoId(curso.getId()));
        });
    }

    private String requireText(String value, String field) {
        String clean = StringUtils.trimToNull(value);
        checkArgument(clean != null, "Ingresá el " + field + ".");
        return clean;
    }

    /* ----------------------------- ejercicios ---------------------------- */

    @Transactional(readOnly = true)
    public Page<FilaEjercicio> exercises(UUID moduloId, String term, Pageable pageable) {
        Map<UUID, CursoModulo> moduloMap = moduloMap();
        Map<UUID, Curso> cursoMap = cursoMap();
        return exercises.search(moduloId, blankToNull(term), pageable).map(ejercicio -> {
            CursoModulo modulo = moduloMap.get(ejercicio.getModuloId());
            Curso curso = modulo == null ? null : cursoMap.get(modulo.getCursoId());
            return new FilaEjercicio(ejercicio.getId(),
                    modulo == null ? "Módulo eliminado" : modulo.getTitulo(),
                    curso == null ? "Curso eliminado" : curso.getTitulo(),
                    ejercicio.getTitulo(), ejercicio.getPrompt(), ejercicio.getPosicion());
        });
    }

    @Transactional(readOnly = true)
    public AdminEjercicioFormulario ejercicioFormulario(UUID id) {
        Ejercicio ejercicio = exercises.findById(id).orElseThrow();
        return new AdminEjercicioFormulario(ejercicio.getModuloId(), ejercicio.getTitulo(),
                ejercicio.getPrompt(), ejercicio.getPosicion());
    }

    @Transactional
    public UUID createEjercicio(AdminEjercicioFormulario formulario, UUID actor) {
        requireModulo(formulario.moduloId());
        Ejercicio ejercicio = new Ejercicio(formulario.moduloId(), formulario.titulo().trim(),
                formulario.prompt().trim(), formulario.posicion());
        exercises.save(ejercicio);
        exercises.flush();
        auditoria.record(actor, "CREATE", "EJERCICIO", ejercicio.getId(), formulario.titulo().trim());
        log.info("Ejercicio creado id={} actor={}", ejercicio.getId(), actor);
        return ejercicio.getId();
    }

    @Transactional
    public void updateEjercicio(UUID id, AdminEjercicioFormulario formulario, UUID actor) {
        Ejercicio ejercicio = exercises.findById(id).orElseThrow();
        requireModulo(formulario.moduloId());
        ejercicio.update(formulario.titulo().trim(), formulario.prompt().trim(), formulario.posicion());
        ejercicio.moveTo(formulario.moduloId());
        auditoria.record(actor, "UPDATE", "EJERCICIO", id, formulario.titulo().trim());
        log.info("Ejercicio actualizado id={} actor={}", id, actor);
    }

    @Transactional
    public void deleteEjercicio(UUID id, UUID actor) {
        Ejercicio ejercicio = exercises.findById(id).orElseThrow();
        String titulo = ejercicio.getTitulo();
        exercises.delete(ejercicio);
        auditoria.record(actor, "DELETE", "EJERCICIO", id, titulo);
        log.info("Ejercicio eliminado id={} actor={}", id, actor);
    }

    /* ---------------------------- opciones ------------------------------- */

    @Transactional(readOnly = true)
    public List<AdminOpcion> cursoOptions() {
        return courses.buscarAllPorOrdenarPorPosicionAsc().stream()
                .map(curso -> new AdminOpcion(curso.getId(), curso.getTitulo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOpcion> moduloOptions() {
        Map<UUID, Curso> cursoMap = cursoMap();
        return modules.buscarAllPorOrdenarPorPosicionAsc().stream()
                .map(modulo -> new AdminOpcion(modulo.getId(),
                        (cursoMap.containsKey(modulo.getCursoId())
                                ? cursoMap.get(modulo.getCursoId()).getTitulo()
                                : "Curso eliminado") + " · " + modulo.getTitulo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> usuarioNames() {
        return users.findAll().stream().collect(Collectors.toMap(
                Usuario::getId, Usuario::getNombreCompleto, (a, b) -> a, java.util.LinkedHashMap::new));
    }

    /* ---------------------------- privados ------------------------------- */

    private Usuario requireUsuario(UUID id) {
        checkNotNull(id, "id");
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe esa cuenta."));
    }

    private Curso requireCurso(UUID id) {
        checkNotNull(id, "id");
        return courses.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe ese curso."));
    }

    private void requireModulo(UUID id) {
        checkNotNull(id, "id");
        if (modules.findById(id).isEmpty()) {
            throw new IllegalArgumentException("No existe ese módulo.");
        }
    }

    private Map<UUID, Curso> cursoMap() {
        return courses.findAll().stream().collect(Collectors.toMap(Curso::getId, Function.identity()));
    }

    private Map<UUID, CursoModulo> moduloMap() {
        return modules.findAll().stream().collect(Collectors.toMap(CursoModulo::getId, Function.identity()));
    }

    private String blankToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
