package com.mundomanitas.api.service;

import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.CourseSection;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.ItemProgress;
import com.mundomanitas.api.model.LessonProgress;
import com.mundomanitas.api.model.QuizAttempt;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.CourseResourceRepository;
import com.mundomanitas.api.repository.CourseSectionRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.ItemProgressRepository;
import com.mundomanitas.api.repository.LessonProgressRepository;
import com.mundomanitas.api.repository.QuizAttemptRepository;
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
public class CourseContentServiceImpl implements CourseContentService {

    private static final Logger log = LoggerFactory.getLogger(CourseContentServiceImpl.class);

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final CourseSectionRepository sections;
    private final ContentItemRepository items;
    private final CourseResourceRepository resources;
    private final EnrollmentRepository enrollments;
    private final LessonProgressRepository lessonProgress;
    private final ItemProgressRepository itemProgress;
    private final QuizAttemptRepository quizAttempts;
    private final VideoRepository videos;
    private final CourseMaterialStorage materials;

    public CourseContentServiceImpl(
            CourseRepository courses,
            CourseModuleRepository modules,
            CourseSectionRepository sections,
            ContentItemRepository items,
            CourseResourceRepository resources,
            EnrollmentRepository enrollments,
            LessonProgressRepository lessonProgress,
            ItemProgressRepository itemProgress,
            QuizAttemptRepository quizAttempts,
            VideoRepository videos,
            CourseMaterialStorage materials
    ) {
        this.courses = courses;
        this.modules = modules;
        this.sections = sections;
        this.items = items;
        this.resources = resources;
        this.enrollments = enrollments;
        this.lessonProgress = lessonProgress;
        this.itemProgress = itemProgress;
        this.quizAttempts = quizAttempts;
        this.videos = videos;
        this.materials = materials;
    }

    @Override
    @Transactional(readOnly = true)
    public Syllabus syllabus(UUID courseId) {
        Course course = courses.findById(courseId).orElseThrow(() -> missing("Curso no encontrado"));
        List<ResourceView> links = resources.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(resource -> new ResourceView(resource.getId(), resource.getTitle(),
                        resource.getUrl()))
                .toList();
        return new Syllabus(course.getSummary(), course.getObjectives(),
                course.getRequirements(), links);
    }

    @Override
    @Transactional(readOnly = true)
    public Outline outline(UUID courseId, UUID enrollmentId, User viewer, boolean includeHidden) {
        Course course = courses.findById(courseId).orElseThrow(() -> missing("Curso no encontrado"));
        boolean locked = !includeHidden && !course.isFree()
                && (viewer == null || !viewer.isPremium());
        Set<UUID> seenVideos = enrollmentId == null ? Set.of()
                : lessonProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId).stream()
                        .map(LessonProgress::getVideoId).collect(Collectors.toSet());
        Set<UUID> seenItems = enrollmentId == null ? Set.of()
                : itemProgress.findByEnrollmentIdOrderByCompletedAtAsc(enrollmentId).stream()
                        .map(ItemProgress::getItemId).collect(Collectors.toSet());
        Set<UUID> quizDone = enrollmentId == null ? Set.of()
                : quizAttempts.findByEnrollmentIdIn(Set.of(enrollmentId)).stream()
                        .map(QuizAttempt::getExerciseId).collect(Collectors.toSet());

        List<ModuleView> moduleViews = new ArrayList<>();
        List<ItemView> orderedItems = new ArrayList<>();
        for (CourseModule module : modules.findByCourseIdOrderByPositionAsc(courseId)) {
            if (!includeHidden && !module.isVisible()) continue;
            List<CourseSection> moduleSections = sections.findByModuleIdOrderByPositionAsc(module.getId())
                    .stream().filter(section -> includeHidden || section.isVisible()).toList();
            Set<UUID> visibleSections = moduleSections.stream()
                    .map(CourseSection::getId).collect(Collectors.toSet());
            List<ContentItem> moduleItems = items.findByModuleIdOrderByPositionAsc(module.getId())
                    .stream().filter(item -> includeHidden || item.isVisible()).toList();

            List<SectionView> sectionViews = new ArrayList<>();
            for (CourseSection section : moduleSections) {
                List<ItemView> sectionItems = moduleItems.stream()
                        .filter(item -> section.getId().equals(item.getSectionId()))
                        .map(item -> view(item, seenVideos, seenItems, quizDone, locked))
                        .toList();
                sectionItems.forEach(orderedItems::add);
                sectionViews.add(new SectionView(section.getId(), section.getTitle(),
                        section.isVisible(), sectionItems));
            }
            List<ItemView> looseItems = moduleItems.stream()
                    .filter(item -> item.getSectionId() == null
                            || !visibleSections.contains(item.getSectionId()))
                    .map(item -> view(item, seenVideos, seenItems, quizDone, locked))
                    .toList();
            looseItems.forEach(orderedItems::add);

            int done = (int) moduleItems.stream()
                    .filter(item -> isSeen(item, seenVideos, seenItems, quizDone)).count();
            int percent = percent(done, moduleItems.size());
            moduleViews.add(new ModuleView(module.getId(), module.getTitle(), module.isVisible(),
                    List.copyOf(sectionViews), looseItems, percent));
        }

        int done = (int) orderedItems.stream().filter(ItemView::seen).count();
        UUID next = orderedItems.stream()
                .filter(item -> !item.seen() && !item.locked())
                .map(ItemView::id).findFirst().orElse(null);
        return new Outline(List.copyOf(moduleViews), orderedItems.size(), done,
                percent(done, orderedItems.size()), next);
    }

    @Override
    @Transactional(readOnly = true)
    public Progress progress(UUID enrollmentId, UUID courseId) {
        Outline outline = outline(courseId, enrollmentId, viewer(enrollmentId), false);
        return new Progress(outline.doneItems(), outline.totalItems(), outline.percent());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID nextPending(UUID courseId, UUID enrollmentId) {
        return outline(courseId, enrollmentId, viewer(enrollmentId), false).nextPending();
    }

    @Override
    @Transactional
    public void markSeen(User user, String slug, UUID itemId, boolean seen) {
        ItemAccess access = requireAccessibleItem(user, slug, itemId);
        Enrollment enrollment = access.enrollment();
        ContentItem item = access.item();
        Course course = enrollment.getCourse();

        if ("EJERCICIO".equals(item.getSourceType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ejercicio solo se marca como visto al rendir el cuestionario");
        }
        if ("VIDEO".equals(item.getSourceType())) {
            UUID videoId = item.getSourceId();
            if (videoId == null || !videos.existsInCourse(videoId, course.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El video no pertenece a este curso");
            }
            LessonProgress.Key key = new LessonProgress.Key(enrollment.getId(), videoId);
            boolean done = lessonProgress.findById(key).isPresent();
            if (seen && !done) {
                lessonProgress.save(new LessonProgress(enrollment.getId(), videoId));
            } else if (!seen && done) {
                lessonProgress.deleteById(key);
            }
        } else {
            ItemProgress.Key key = new ItemProgress.Key(enrollment.getId(), itemId);
            boolean done = itemProgress.findById(key).isPresent();
            if (seen && !done) {
                itemProgress.save(new ItemProgress(enrollment.getId(), itemId));
            } else if (!seen && done) {
                itemProgress.deleteById(key);
            }
        }
        log.info("Ítem actualizado item={} matrícula={} visto={}", itemId, enrollment.getId(), seen);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDownload downloadMaterial(User user, String slug, UUID itemId) {
        ItemAccess access = requireAccessibleItem(user, slug, itemId);
        ContentItem item = access.item();
        if (!"MATERIAL".equals(item.getSourceType())
                || item.getUrl() == null
                || !item.getUrl().startsWith(CourseMaterialStorage.URL_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no tiene un documento descargable");
        }
        String url = item.getUrl();
        MaterialDownload download = new MaterialDownload(
                fileNameOf(item, url), materials.contentType(url), materials.read(url));
        log.info("Documento descargado item={} matrícula={} actor={}",
                itemId, access.enrollment().getId(), user.getId());
        return download;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialView viewMaterial(User user, String slug, UUID itemId) {
        ItemAccess access = requirePdfItem(user, slug, itemId);
        ContentItem item = access.item();
        String url = item.getUrl();
        MaterialView view = new MaterialView(item.getId(), item.getTitle(),
                fileNameOf(item, url), materials.contentType(url));
        log.info("PDF abierto en el visor item={} matrícula={} actor={}",
                itemId, access.enrollment().getId(), user.getId());
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDownload readPdf(User user, String slug, UUID itemId) {
        ItemAccess access = requirePdfItem(user, slug, itemId);
        ContentItem item = access.item();
        String url = item.getUrl();
        MaterialDownload pdf = new MaterialDownload(fileNameOf(item, url),
                materials.contentType(url), materials.read(url));
        log.info("PDF servido al visor item={} matrícula={} actor={}",
                itemId, access.enrollment().getId(), user.getId());
        return pdf;
    }

    /** La misma entrada privada que la descarga, limitada a PDFs reales. */
    private ItemAccess requirePdfItem(User user, String slug, UUID itemId) {
        ItemAccess access = requireAccessibleItem(user, slug, itemId);
        ContentItem item = access.item();
        String url = item.getUrl();
        if (!"MATERIAL".equals(item.getSourceType()) || url == null
                || !url.startsWith(CourseMaterialStorage.URL_PREFIX)
                || !"application/pdf".equals(materials.contentType(url))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no tiene un PDF para abrir");
        }
        return access;
    }

    private String fileNameOf(ContentItem item, String url) {
        return item.getFileName() == null ? url.substring(url.lastIndexOf('/') + 1)
                : item.getFileName();
    }

    private ItemAccess requireAccessibleItem(User user, String slug, UUID itemId) {
        checkNotNull(user, "user");
        checkNotNull(itemId, "itemId");
        Enrollment enrollment = enrollments.findByUserAndCourseSlug(user, requiredSlug(slug))
                .orElseThrow(() -> missing("No estás inscripto en ese curso"));
        Course course = enrollment.getCourse();
        if (!course.isFree() && !user.isPremium()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso requiere plan Premium");
        }
        ContentItem item = items.findById(itemId)
                .orElseThrow(() -> missing("El ítem no existe"));
        if (!item.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El ítem no está disponible");
        }
        CourseModule module = modules.findById(item.getModuleId())
                .orElseThrow(() -> missing("El módulo no existe"));
        if (!course.getId().equals(module.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El ítem no pertenece a este curso");
        }
        return new ItemAccess(enrollment, item);
    }

    private ItemView view(ContentItem item, Set<UUID> seenVideos, Set<UUID> seenItems,
                          Set<UUID> quizDone, boolean locked) {
        boolean done = "EJERCICIO".equals(item.getSourceType()) && item.getSourceId() != null
                && quizDone.contains(item.getSourceId());
        return new ItemView(item.getId(), item.getSourceType(), item.getTitle(),
                item.getDescription(), item.getUrl(), item.getYoutubeId(),
                item.getDurationSeconds(), item.isVisible(),
                isSeen(item, seenVideos, seenItems, quizDone), locked, item.getSourceId(),
                item.getFileName(), done);
    }

    private boolean isSeen(ContentItem item, Set<UUID> seenVideos, Set<UUID> seenItems,
                           Set<UUID> quizDone) {
        if ("VIDEO".equals(item.getSourceType())) {
            return seenVideos.contains(item.getSourceId());
        }
        if ("EJERCICIO".equals(item.getSourceType())) {
            return item.getSourceId() != null && quizDone.contains(item.getSourceId());
        }
        return seenItems.contains(item.getId());
    }

    private int percent(int done, int total) {
        return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private User viewer(UUID enrollmentId) {
        if (enrollmentId == null) return null;
        return enrollments.findById(enrollmentId).map(Enrollment::getUser).orElse(null);
    }

    private String requiredSlug(String slug) {
        String value = StringUtils.trimToNull(slug);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso es obligatorio");
        }
        return value;
    }

    private ResponseStatusException missing(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record ItemAccess(Enrollment enrollment, ContentItem item) {
    }
}
