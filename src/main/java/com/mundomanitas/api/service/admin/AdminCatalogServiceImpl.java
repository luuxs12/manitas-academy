package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminCourseRow;
import com.mundomanitas.api.dto.admin.AdminOption;
import com.mundomanitas.api.dto.admin.AdminUserForm;
import com.mundomanitas.api.dto.admin.AdminUserRow;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.ExerciseRow;
import com.mundomanitas.api.dto.admin.ModuleRow;
import com.mundomanitas.api.dto.admin.VideoRow;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Exercise;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.UserRepository;
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

    private final UserRepository users;
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final VideoRepository videos;
    private final ExerciseRepository exercises;
    private final EnrollmentRepository enrollments;
    private final CertificateRepository certificates;
    private final PasswordEncoder encoder;
    private final AdminAuditService audit;
    private final AdminFormat format;

    public AdminCatalogServiceImpl(
            UserRepository users,
            CourseRepository courses,
            CourseModuleRepository modules,
            VideoRepository videos,
            ExerciseRepository exercises,
            EnrollmentRepository enrollments,
            CertificateRepository certificates,
            PasswordEncoder encoder,
            AdminAuditService audit,
            AdminFormat format
    ) {
        this.users = users;
        this.courses = courses;
        this.modules = modules;
        this.videos = videos;
        this.exercises = exercises;
        this.enrollments = enrollments;
        this.certificates = certificates;
        this.encoder = encoder;
        this.audit = audit;
        this.format = format;
    }

    /* ------------------------------ cuentas ------------------------------ */

    @Transactional(readOnly = true)
    public Page<AdminUserRow> users(String role, String term, Pageable pageable, UUID currentId) {
        String cleanRole = blankToNull(role);
        String cleanTerm = blankToNull(term);
        return users.search(cleanRole, cleanTerm, pageable).map(user -> new AdminUserRow(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getPlan().getLabel(), user.isActive(),
                format.full(user.getCreatedAt()), user.getId().equals(currentId)));
    }

    @Transactional(readOnly = true)
    public AdminUserForm userForm(UUID id) {
        User user = requireUser(id);
        return new AdminUserForm(user.getFullName(), user.getEmail(), user.getRole(),
                user.isActive(), "");
    }

    @Transactional
    public void updateUser(UUID id, AdminUserForm form, UUID actor) {
        User user = requireUser(id);
        users.findByEmailIgnoreCase(form.trimmedEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
                });
        user.changeFullName(form.trimmedName());
        user.changeEmail(form.trimmedEmail());
        user.changeRole(form.role());
        user.changeActive(form.active());
        if (form.password() != null && !form.password().isBlank()) {
            user.changePasswordHash(encoder.encode(form.password()));
        }
        audit.record(actor, "UPDATE", "USUARIO", id, form.trimmedEmail());
        log.info("Usuario actualizado id={} actor={}", id, actor);
    }

    /* ------------------------------ cursos ------------------------------- */

    @Override
    @Transactional(readOnly = true)
    public String slugFromTitle(String title) {
        String slug = StringUtils.trimToEmpty(title).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        checkArgument(!slug.isBlank(), "El título no permite generar un identificador.");
        return slug;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminCourseRow> courses(String term, Pageable pageable) {
        String clean = blankToNull(term);
        Page<Course> page = clean == null
                ? courses.findAllByOrderByPositionAsc(pageable)
                : courses.findByTitleIgnoreCaseContainingOrSlugIgnoreCaseContaining(
                        clean, clean, pageable);
        Map<UUID, Long> totals = new java.util.HashMap<>();
        for (Object[] row : enrollments.countByCourse()) {
            totals.put(UUID.fromString(row[0].toString()), ((Number) row[1]).longValue());
        }
        return page.map(course -> new AdminCourseRow(
                course.getId(), course.getSlug(), course.getTitle(), course.getCategory(),
                lessonCount(course.getId()), course.getStatus(), course.priceLabel(),
                course.getPosition(), totals.getOrDefault(course.getId(), 0L), course.getImage()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCourseForm courseForm(UUID id) {
        Course course = requireCourse(id);
        return new AdminCourseForm(course.getSlug(), course.getTitle(), course.getLevel(),
                course.isFree(), course.getCategory(), course.getSummary(),
                course.getImage(), course.getPosition());
    }

    @Override
    @Transactional
    public UUID createCourse(AdminCourseForm form, UUID actor) {
        String slug = blankToNull(form.slug()) == null
                ? slugFromTitle(form.title()) : form.slug().trim();
        checkArgument(courses.findBySlug(slug).isEmpty(),
                "Ya existe un curso con ese identificador.");
        String title = requireText(form.title(), "título");
        String level = requireText(form.level(), "nivel");
        String category = requireText(form.category(), "categoría");
        String summary = requireText(form.summary(), "resumen");
        Course course = new Course(slug, title, level, 0, form.free(), category,
                summary, blankToNull(form.image()), form.position());
        courses.save(course);
        audit.record(actor, "CREATE", "CURSO", course.getId(), title);
        log.info("Curso creado id={} actor={} estado={}", course.getId(), actor,
                course.getStatus());
        return course.getId();
    }

    @Override
    @Transactional
    public void updateCourse(UUID id, AdminCourseForm form, UUID actor) {
        Course course = requireCourse(id);
        String slug = blankToNull(form.slug()) == null
                ? slugFromTitle(form.title()) : form.slug().trim();
        courses.findBySlug(slug).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ya existe un curso con ese identificador.");
                });
        String title = requireText(form.title(), "título");
        course.update(slug, title, requireText(form.level(), "nivel"), form.free(),
                requireText(form.category(), "categoría"), requireText(form.summary(), "resumen"),
                blankToNull(form.image()), form.position());
        audit.record(actor, "UPDATE", "CURSO", id, title);
        log.info("Curso actualizado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public String updateCourseCover(UUID id, String image, UUID actor) {
        checkArgument(image != null
                        && image.matches("/uploads/covers/[0-9a-f-]+\\.(jpg|png|webp)"),
                "La portada debe ser una imagen local.");
        Course course = requireCourse(id);
        String previous = course.getImage();
        course.changeImage(image);
        audit.record(actor, "UPDATE", "CURSO", id, "PORTADA");
        log.info("Portada actualizada id={} actor={} anterior={}", id, actor, previous);
        return previous;
    }

    @Override
    @Transactional(readOnly = true)
    public PublishChecklist publishChecklist(UUID id) {
        Course course = requireCourse(id);
        List<String> missing = new ArrayList<>();
        if (StringUtils.isBlank(course.getSummary())) missing.add("resumen");
        if (StringUtils.isBlank(course.getImage())) missing.add("portada");
        List<CourseModule> modulesOfCourse = modules.findByCourseIdOrderByPositionAsc(id);
        if (modulesOfCourse.isEmpty()) {
            missing.add("módulo");
            missing.add("video");
        } else {
            boolean hasVideo = modulesOfCourse.stream().anyMatch(module ->
                    !videos.findByModuleIdOrderByPositionAsc(module.getId()).isEmpty());
            if (!hasVideo) missing.add("video");
        }
        return new PublishChecklist(List.copyOf(missing), missing.isEmpty());
    }

    @Override
    @Transactional(readOnly = true)
    public String courseStatus(UUID id) {
        return requireCourse(id).getStatus();
    }

    @Override
    @Transactional
    public void setPublished(UUID id, boolean published, UUID actor) {
        Course course = requireCourse(id);
        if (published) {
            PublishChecklist check = publishChecklist(id);
            checkArgument(check.canPublish(), "Falta: " + String.join(", ", check.missing()));
            course.publish();
        } else {
            course.unpublish();
        }
        audit.record(actor, "UPDATE", "CURSO", id,
                published ? "PUBLICADO" : "BORRADOR");
        log.info("Estado del curso id={} actor={} publicado={}", id, actor, published);
    }

    @Override
    @Transactional(readOnly = true)
    public DeletionImpact deletionImpact(UUID id) {
        requireCourse(id);
        List<CourseModule> modulesOfCourse = modules.findByCourseIdOrderByPositionAsc(id);
        int videosCount = videos.countByCourseId(id);
        int exercisesCount = modulesOfCourse.stream()
                .mapToInt(module -> exercises.findByModuleIdOrderByPositionAsc(module.getId()).size())
                .sum();
        return new DeletionImpact(modulesOfCourse.size(), videosCount, exercisesCount,
                enrollments.countByCourseId(id), certificates.countByCourseId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public int lessonCount(UUID courseId) {
        return videos.countByCourseId(courseId);
    }

    @Override
    @Transactional
    public void moveModule(UUID id, boolean up, UUID actor) {
        CourseModule module = modules.findById(id).orElseThrow();
        List<CourseModule> ordered = new ArrayList<>(
                modules.findByCourseIdOrderByPositionAsc(module.getCourseId()));
        int index = ordered.indexOf(module);
        checkArgument(index >= 0, "El módulo no pertenece al curso.");
        int target = up ? index - 1 : index + 1;
        checkArgument(target >= 0 && target < ordered.size(), "No se puede mover más.");
        CourseModule other = ordered.get(target);
        int position = module.getPosition();
        module.setPosition(other.getPosition());
        other.setPosition(position);
        audit.record(actor, "UPDATE", "MODULO", id, up ? "SUBIR" : "BAJAR");
        log.info("Módulo reordenado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public void moveVideo(UUID id, boolean up, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        List<Video> ordered = new ArrayList<>(
                videos.findByModuleIdOrderByPositionAsc(video.getModuleId()));
        int index = ordered.indexOf(video);
        checkArgument(index >= 0, "El video no pertenece al módulo.");
        int target = up ? index - 1 : index + 1;
        checkArgument(target >= 0 && target < ordered.size(), "No se puede mover más.");
        Video other = ordered.get(target);
        int position = video.getPosition();
        video.setPosition(other.getPosition());
        other.setPosition(position);
        audit.record(actor, "UPDATE", "VIDEO", id, up ? "SUBIR" : "BAJAR");
        log.info("Video reordenado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID id, UUID actor) {
        Course course = requireCourse(id);
        DeletionImpact impact = deletionImpact(id);
        checkArgument(impact.enrollments() == 0,
                "El curso tiene inscritos: despublicalo o archivalo en lugar de borrarlo.");
        String title = course.getTitle();
        courses.delete(course);
        audit.record(actor, "DELETE", "CURSO", id, title);
        log.info("Curso eliminado id={} actor={} impacto={}", id, actor, impact);
    }

    /* ------------------------------ módulos ------------------------------ */

    @Transactional(readOnly = true)
    public Page<ModuleRow> modules(UUID courseId, String term, Pageable pageable) {
        Map<UUID, Course> courseMap = courseMap();
        Page<CourseModule> page = modules.search(courseId, blankToNull(term), pageable);
        return page.map(module -> new ModuleRow(
                module.getId(),
                courseMap.containsKey(module.getCourseId())
                        ? courseMap.get(module.getCourseId()).getTitle()
                        : "Curso eliminado",
                module.getTitle(), module.getPosition(),
                videos.findByModuleIdOrderByPositionAsc(module.getId()).size(),
                exercises.findByModuleIdOrderByPositionAsc(module.getId()).size()));
    }

    @Transactional(readOnly = true)
    public AdminModuleForm moduleForm(UUID id) {
        CourseModule module = modules.findById(id).orElseThrow();
        return new AdminModuleForm(module.getCourseId(), module.getTitle(), module.getPosition());
    }

    @Transactional
    public UUID createModule(AdminModuleForm form, UUID actor) {
        requireCourse(form.courseId());
        CourseModule module = new CourseModule(form.courseId(), form.title().trim(), form.position());
        modules.save(module);
        audit.record(actor, "CREATE", "MODULO", module.getId(), form.title().trim());
        log.info("Módulo creado id={} actor={}", module.getId(), actor);
        return module.getId();
    }

    @Transactional
    public void updateModule(UUID id, AdminModuleForm form, UUID actor) {
        CourseModule module = modules.findById(id).orElseThrow();
        requireCourse(form.courseId());
        module.update(form.title().trim(), form.position());
        module.moveTo(form.courseId());
        audit.record(actor, "UPDATE", "MODULO", id, form.title().trim());
        log.info("Módulo actualizado id={} actor={}", id, actor);
    }

    @Transactional
    public void deleteModule(UUID id, UUID actor) {
        CourseModule module = modules.findById(id).orElseThrow();
        String title = module.getTitle();
        modules.delete(module);
        audit.record(actor, "DELETE", "MODULO", id, title);
        log.info("Módulo eliminado id={} actor={}", id, actor);
    }

    /* ------------------------------- videos ------------------------------ */

    @Override
    @Transactional(readOnly = true)
    public Page<VideoRow> videos(UUID moduleId, String term, Pageable pageable) {
        Map<UUID, CourseModule> moduleMap = moduleMap();
        Map<UUID, Course> courseMap = courseMap();
        return videos.search(moduleId, blankToNull(term), pageable).map(video -> {
            CourseModule module = moduleMap.get(video.getModuleId());
            Course course = module == null ? null : courseMap.get(module.getCourseId());
            return new VideoRow(video.getId(),
                    module == null ? "Módulo eliminado" : module.getTitle(),
                    course == null ? "Curso eliminado" : course.getTitle(),
                    video.getTitle(),
                    youtubeLink(video),
                    video.getPosition());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVideoForm videoForm(UUID id) {
        Video video = videos.findById(id).orElseThrow();
        return new AdminVideoForm(video.getModuleId(), video.getTitle(), video.getPosition(),
                youtubeLink(video));
    }

    @Override
    @Transactional
    public UUID createVideo(AdminVideoForm form, UUID actor) {
        requireModule(form.moduleId());
        Video video = new Video(form.moduleId(), requireText(form.title(), "título"),
                youtubeId(form.youtubeUrl()), form.position());
        videos.save(video);
        refreshLessons(video.getModuleId());
        audit.record(actor, "CREATE", "VIDEO", video.getId(), form.title().trim());
        log.info("Video creado id={} actor={} youtube={}", video.getId(), actor,
                video.getYoutubeId());
        return video.getId();
    }

    @Override
    @Transactional
    public void updateVideo(UUID id, AdminVideoForm form, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        UUID previousModule = video.getModuleId();
        requireModule(form.moduleId());
        video.update(requireText(form.title(), "título"),
                youtubeId(form.youtubeUrl()), form.position());
        video.moveTo(form.moduleId());
        if (!previousModule.equals(form.moduleId())) {
            refreshLessons(previousModule);
        }
        refreshLessons(form.moduleId());
        audit.record(actor, "UPDATE", "VIDEO", id, form.title().trim());
        log.info("Video actualizado id={} actor={}", id, actor);
    }

    @Override
    @Transactional
    public void deleteVideo(UUID id, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        UUID moduleId = video.getModuleId();
        String title = video.getTitle();
        videos.delete(video);
        refreshLessons(moduleId);
        audit.record(actor, "DELETE", "VIDEO", id, title);
        log.info("Video eliminado id={} actor={}", id, actor);
    }

    private String youtubeId(String youtubeUrl) {
        String link = AdminVideoForm.clean(youtubeUrl);
        String id = link == null ? null : YouTubeLink.id(link);
        checkArgument(id != null, "El enlace de YouTube no es válido.");
        return id;
    }

    private String youtubeLink(Video video) {
        return video.getYoutubeId() == null ? null : "https://youtu.be/" + video.getYoutubeId();
    }

    private void refreshLessons(UUID moduleId) {
        modules.findById(moduleId).ifPresent(module -> {
            Course course = courses.findById(module.getCourseId()).orElse(null);
            if (course != null) course.refreshLessons(videos.countByCourseId(course.getId()));
        });
    }

    private String requireText(String value, String field) {
        String clean = StringUtils.trimToNull(value);
        checkArgument(clean != null, "Ingresá el " + field + ".");
        return clean;
    }

    /* ----------------------------- ejercicios ---------------------------- */

    @Transactional(readOnly = true)
    public Page<ExerciseRow> exercises(UUID moduleId, String term, Pageable pageable) {
        Map<UUID, CourseModule> moduleMap = moduleMap();
        Map<UUID, Course> courseMap = courseMap();
        return exercises.search(moduleId, blankToNull(term), pageable).map(exercise -> {
            CourseModule module = moduleMap.get(exercise.getModuleId());
            Course course = module == null ? null : courseMap.get(module.getCourseId());
            return new ExerciseRow(exercise.getId(),
                    module == null ? "Módulo eliminado" : module.getTitle(),
                    course == null ? "Curso eliminado" : course.getTitle(),
                    exercise.getTitle(), exercise.getPrompt(), exercise.getPosition());
        });
    }

    @Transactional(readOnly = true)
    public AdminExerciseForm exerciseForm(UUID id) {
        Exercise exercise = exercises.findById(id).orElseThrow();
        return new AdminExerciseForm(exercise.getModuleId(), exercise.getTitle(),
                exercise.getPrompt(), exercise.getPosition());
    }

    @Transactional
    public UUID createExercise(AdminExerciseForm form, UUID actor) {
        requireModule(form.moduleId());
        Exercise exercise = new Exercise(form.moduleId(), form.title().trim(),
                form.prompt().trim(), form.position());
        exercises.save(exercise);
        audit.record(actor, "CREATE", "EJERCICIO", exercise.getId(), form.title().trim());
        log.info("Ejercicio creado id={} actor={}", exercise.getId(), actor);
        return exercise.getId();
    }

    @Transactional
    public void updateExercise(UUID id, AdminExerciseForm form, UUID actor) {
        Exercise exercise = exercises.findById(id).orElseThrow();
        requireModule(form.moduleId());
        exercise.update(form.title().trim(), form.prompt().trim(), form.position());
        exercise.moveTo(form.moduleId());
        audit.record(actor, "UPDATE", "EJERCICIO", id, form.title().trim());
        log.info("Ejercicio actualizado id={} actor={}", id, actor);
    }

    @Transactional
    public void deleteExercise(UUID id, UUID actor) {
        Exercise exercise = exercises.findById(id).orElseThrow();
        String title = exercise.getTitle();
        exercises.delete(exercise);
        audit.record(actor, "DELETE", "EJERCICIO", id, title);
        log.info("Ejercicio eliminado id={} actor={}", id, actor);
    }

    /* ---------------------------- opciones ------------------------------- */

    @Transactional(readOnly = true)
    public List<AdminOption> courseOptions() {
        return courses.findAllByOrderByPositionAsc().stream()
                .map(course -> new AdminOption(course.getId(), course.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminOption> moduleOptions() {
        Map<UUID, Course> courseMap = courseMap();
        return modules.findAllByOrderByPositionAsc().stream()
                .map(module -> new AdminOption(module.getId(),
                        (courseMap.containsKey(module.getCourseId())
                                ? courseMap.get(module.getCourseId()).getTitle()
                                : "Curso eliminado") + " · " + module.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> userNames() {
        return users.findAll().stream().collect(Collectors.toMap(
                User::getId, User::getFullName, (a, b) -> a, java.util.LinkedHashMap::new));
    }

    /* ---------------------------- privados ------------------------------- */

    private User requireUser(UUID id) {
        checkNotNull(id, "id");
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe esa cuenta."));
    }

    private Course requireCourse(UUID id) {
        checkNotNull(id, "id");
        return courses.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe ese curso."));
    }

    private void requireModule(UUID id) {
        checkNotNull(id, "id");
        if (modules.findById(id).isEmpty()) {
            throw new IllegalArgumentException("No existe ese módulo.");
        }
    }

    private Map<UUID, Course> courseMap() {
        return courses.findAll().stream().collect(Collectors.toMap(Course::getId, Function.identity()));
    }

    private Map<UUID, CourseModule> moduleMap() {
        return modules.findAll().stream().collect(Collectors.toMap(CourseModule::getId, Function.identity()));
    }

    private String blankToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
