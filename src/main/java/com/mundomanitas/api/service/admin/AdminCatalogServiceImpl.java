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
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import com.mundomanitas.api.service.R2StorageService;
import com.mundomanitas.api.service.YouTubeLink;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final PasswordEncoder encoder;
    private final AdminAuditService audit;
    private final AdminFormat format;
    private final R2StorageService storage;

    public AdminCatalogServiceImpl(
            UserRepository users,
            CourseRepository courses,
            CourseModuleRepository modules,
            VideoRepository videos,
            ExerciseRepository exercises,
            EnrollmentRepository enrollments,
            PasswordEncoder encoder,
            AdminAuditService audit,
            AdminFormat format,
            R2StorageService storage
    ) {
        this.users = users;
        this.courses = courses;
        this.modules = modules;
        this.videos = videos;
        this.exercises = exercises;
        this.enrollments = enrollments;
        this.encoder = encoder;
        this.audit = audit;
        this.format = format;
        this.storage = storage;
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
                course.getLessons(), course.priceLabel(), course.getPosition(),
                totals.getOrDefault(course.getId(), 0L), course.getImage()));
    }

    @Transactional(readOnly = true)
    public AdminCourseForm courseForm(UUID id) {
        Course course = requireCourse(id);
        return new AdminCourseForm(course.getSlug(), course.getTitle(), course.getLevel(),
                course.getLessons(), course.isFree(), course.getCategory(), course.getSummary(),
                course.getImage(), course.getPosition());
    }

    @Transactional
    public UUID createCourse(AdminCourseForm form, UUID actor) {
        if (courses.findBySlug(form.slug()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un curso con ese identificador.");
        }
        Course course = new Course(form.slug(), form.title(), form.level(), form.lessons(),
                form.free(), form.category(), form.summary(), form.image(), form.position());
        courses.save(course);
        audit.record(actor, "CREATE", "CURSO", course.getId(), form.title());
        log.info("Curso creado id={} actor={}", course.getId(), actor);
        return course.getId();
    }

    @Transactional
    public void updateCourse(UUID id, AdminCourseForm form, UUID actor) {
        Course course = requireCourse(id);
        courses.findBySlug(form.slug()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ya existe un curso con ese identificador.");
                });
        course.update(form.slug(), form.title(), form.level(), form.lessons(), form.free(),
                form.category(), form.summary(), form.image(), form.position());
        audit.record(actor, "UPDATE", "CURSO", id, form.title());
        log.info("Curso actualizado id={} actor={}", id, actor);
    }

    @Transactional
    public void deleteCourse(UUID id, UUID actor) {
        Course course = requireCourse(id);
        String title = course.getTitle();
        courses.delete(course);
        audit.record(actor, "DELETE", "CURSO", id, title);
        log.info("Curso eliminado id={} actor={}", id, actor);
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
                    sourceLabel(video),
                    video.getPosition());
        });
    }

    @Transactional(readOnly = true)
    public AdminVideoForm videoForm(UUID id) {
        Video video = videos.findById(id).orElseThrow();
        return new AdminVideoForm(video.getModuleId(), video.getTitle(), video.getUrl(),
                video.getPosition(), video.getObjectKey(), video.getSubtitleKey(),
                video.getDurationSeconds(), video.getTranscript(),
                youtubeLink(video));
    }

    @Transactional
    public UUID createVideo(AdminVideoForm form, UUID actor) {
        requireModule(form.moduleId());
        Video video = new Video(form.moduleId(), form.title().trim(),
                AdminVideoForm.clean(form.url()), form.position(),
                AdminVideoForm.clean(form.objectKey()),
                AdminVideoForm.clean(form.subtitleKey()),
                form.durationSeconds() == null ? 0 : form.durationSeconds(),
                AdminVideoForm.clean(form.transcript()),
                youtubeId(form.youtubeUrl()));
        videos.save(video);
        audit.record(actor, "CREATE", "VIDEO", video.getId(), form.title().trim());
        log.info("Video creado id={} actor={}", video.getId(), actor);
        return video.getId();
    }

    @Transactional
    public void updateVideo(UUID id, AdminVideoForm form, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        requireModule(form.moduleId());
        video.update(form.title().trim(), AdminVideoForm.clean(form.url()), form.position(),
                AdminVideoForm.clean(form.objectKey()),
                AdminVideoForm.clean(form.subtitleKey()),
                form.durationSeconds() == null ? 0 : form.durationSeconds(),
                AdminVideoForm.clean(form.transcript()),
                youtubeId(form.youtubeUrl()));
        video.moveTo(form.moduleId());
        audit.record(actor, "UPDATE", "VIDEO", id, form.title().trim());
        log.info("Video actualizado id={} actor={}", id, actor);
    }

    @Transactional
    public void deleteVideo(UUID id, UUID actor) {
        Video video = videos.findById(id).orElseThrow();
        String title = video.getTitle();
        videos.delete(video);
        storage.deleteByKey(video.getObjectKey());
        storage.deleteByKey(video.getSubtitleKey());
        storage.deleteByUrl(video.getUrl());
        audit.record(actor, "DELETE", "VIDEO", id, title);
        log.info("Video eliminado id={} actor={}", id, actor);
    }

    private String youtubeId(String youtubeUrl) {
        String link = AdminVideoForm.clean(youtubeUrl);
        if (link == null) return null;
        String id = YouTubeLink.id(link);
        if (id == null) throw new IllegalArgumentException("El enlace de YouTube no es válido.");
        return id;
    }

    private String youtubeLink(Video video) {
        String id = YouTubeLink.id(video.getYoutubeId());
        return id == null ? null : "https://youtu.be/" + id;
    }

    private String sourceLabel(Video video) {
        String link = youtubeLink(video);
        if (link != null) return link;
        return StringUtils.trimToNull(video.getUrl()) != null ? video.getUrl() : video.getObjectKey();
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
