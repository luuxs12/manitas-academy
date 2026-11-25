package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.CourseResource;
import com.mundomanitas.api.model.CourseSection;
import com.mundomanitas.api.model.Exercise;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.CourseResourceRepository;
import com.mundomanitas.api.repository.CourseSectionRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.VideoRepository;
import com.mundomanitas.api.service.ContentLinks;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Implementación de la carga administrativa de módulos, secciones e ítems. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseContentServiceImpl implements AdminCourseContentService {

    private static final Logger log = LoggerFactory.getLogger(AdminCourseContentServiceImpl.class);

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final CourseSectionRepository sections;
    private final ContentItemRepository items;
    private final CourseResourceRepository resources;
    private final VideoRepository videos;
    private final ExerciseRepository exercises;
    private final AdminAuditService audit;

    public AdminCourseContentServiceImpl(
            CourseRepository courses,
            CourseModuleRepository modules,
            CourseSectionRepository sections,
            ContentItemRepository items,
            CourseResourceRepository resources,
            VideoRepository videos,
            ExerciseRepository exercises,
            AdminAuditService audit
    ) {
        this.courses = courses;
        this.modules = modules;
        this.sections = sections;
        this.items = items;
        this.resources = resources;
        this.videos = videos;
        this.exercises = exercises;
        this.audit = audit;
    }

    @Override
    @Transactional
    public UUID createSection(UUID moduleId, SectionForm form, UUID actor) {
        requireModule(moduleId);
        CourseSection section = new CourseSection(moduleId, requireText(form.title(), "título"),
                form.position(), form.visible());
        sections.save(section);
        audit.record(actor, "CREATE", "SECCION", section.getId(), section.getTitle());
        log.info("Sección creada id={} módulo={} actor={}", section.getId(), moduleId, actor);
        return section.getId();
    }

    @Override
    @Transactional
    public void updateSection(UUID sectionId, SectionForm form, UUID actor) {
        CourseSection section = requireSection(sectionId);
        section.update(requireText(form.title(), "título"), form.position(), form.visible());
        sections.save(section);
        audit.record(actor, "UPDATE", "SECCION", sectionId, section.getTitle());
        log.info("Sección actualizada id={} actor={}", sectionId, actor);
    }

    @Override
    @Transactional
    public void deleteSection(UUID sectionId, UUID actor) {
        CourseSection section = requireSection(sectionId);
        String title = section.getTitle();
        sections.delete(section);
        audit.record(actor, "DELETE", "SECCION", sectionId, title);
        log.info("Sección eliminada id={} actor={}", sectionId, actor);
    }

    @Override
    @Transactional
    public UUID createItem(ItemForm form, UUID actor) {
        checkNotNull(form, "form");
        String type = requireType(form.type());
        String title = requireText(form.title(), "título");
        String url = null;
        String youtubeId = null;
        String prompt = null;
        if ("MATERIAL".equals(type)) {
            url = ContentLinks.https(form.url(), "enlace");
        } else if ("VIDEO".equals(type)) {
            youtubeId = ContentLinks.youtubeId(form.youtubeUrl());
        } else {
            prompt = requireText(form.description(), "consigna");
        }
        requireModule(form.moduleId());
        requireSectionInModule(form.sectionId(), form.moduleId());

        ContentItem item;
        if ("MATERIAL".equals(type)) {
            item = new ContentItem(form.moduleId(), form.sectionId(), type, null, title,
                    StringUtils.trimToNull(form.description()), url, null,
                    form.durationSeconds(), form.position(), form.visible());
            items.save(item);
        } else if ("VIDEO".equals(type)) {
            Video video = new Video(form.moduleId(), title, youtubeId, form.position());
            videos.save(video);
            videos.flush();
            item = items.findBySourceTypeAndSourceId(type, video.getId());
            applyMetadata(item, form);
        } else {
            Exercise exercise = new Exercise(form.moduleId(), title, prompt, form.position());
            exercises.save(exercise);
            exercises.flush();
            item = items.findBySourceTypeAndSourceId(type, exercise.getId());
            applyMetadata(item, form);
        }
        audit.record(actor, "CREATE", "ITEM", item.getId(), title);
        log.info("Ítem creado id={} tipo={} actor={}", item.getId(), type, actor);
        return item.getId();
    }

    @Override
    @Transactional
    public void updateItem(UUID itemId, ItemForm form, UUID actor) {
        checkNotNull(form, "form");
        ContentItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        String type = item.getSourceType();
        String title = requireText(form.title(), "título");
        requireModule(form.moduleId());
        requireSectionInModule(form.sectionId(), form.moduleId());

        if ("MATERIAL".equals(type)) {
            item.updateMaterial(title, StringUtils.trimToNull(form.description()),
                    ContentLinks.https(form.url(), "enlace"));
        } else if ("VIDEO".equals(type)) {
            Video video = videos.findById(item.getSourceId())
                    .orElseThrow(() -> new IllegalArgumentException("Video no encontrado"));
            video.update(title, ContentLinks.youtubeId(form.youtubeUrl()), form.position());
            video.moveTo(form.moduleId());
            videos.save(video);
            videos.flush();
            item = items.findBySourceTypeAndSourceId(type, item.getSourceId());
        } else {
            Exercise exercise = exercises.findById(item.getSourceId())
                    .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado"));
            exercise.update(title, requireText(form.description(), "consigna"), form.position());
            exercise.moveTo(form.moduleId());
            exercises.save(exercise);
            exercises.flush();
            item = items.findBySourceTypeAndSourceId(type, item.getSourceId());
        }
        applyMetadata(item, form);
        items.save(item);
        audit.record(actor, "UPDATE", "ITEM", itemId, title);
        log.info("Ítem actualizado id={} actor={}", itemId, actor);
    }

    @Override
    @Transactional
    public void deleteItem(UUID itemId, UUID actor) {
        ContentItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        String title = item.getTitle();
        if ("VIDEO".equals(item.getSourceType()) && item.getSourceId() != null) {
            videos.findById(item.getSourceId()).ifPresent(videos::delete);
        } else if ("EJERCICIO".equals(item.getSourceType()) && item.getSourceId() != null) {
            exercises.findById(item.getSourceId()).ifPresent(exercises::delete);
        } else {
            items.delete(item);
        }
        audit.record(actor, "DELETE", "ITEM", itemId, title);
        log.info("Ítem eliminado id={} actor={}", itemId, actor);
    }

    @Override
    @Transactional
    public void moveSection(UUID sectionId, boolean up, UUID actor) {
        CourseSection section = requireSection(sectionId);
        List<CourseSection> siblings = sections.findByModuleIdOrderByPositionAsc(section.getModuleId());
        int index = indexOf(siblings, CourseSection::getId, sectionId);
        CourseSection neighbour = neighbour(siblings, index, up);
        if (neighbour == null) return;
        int position = section.getPosition();
        section.setPosition(neighbour.getPosition());
        neighbour.setPosition(position);
        sections.save(section);
        sections.save(neighbour);
        audit.record(actor, "UPDATE", "SECCION", sectionId, up ? "SUBIR" : "BAJAR");
        log.info("Sección reordenada id={} actor={}", sectionId, actor);
    }

    @Override
    @Transactional
    public void moveItem(UUID itemId, boolean up, UUID actor) {
        ContentItem item = items.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        List<ContentItem> siblings = items.findByModuleIdOrderByPositionAsc(item.getModuleId());
        int index = indexOf(siblings, ContentItem::getId, itemId);
        ContentItem neighbour = neighbour(siblings, index, up);
        if (neighbour == null) return;
        int position = item.getPosition();
        item.setPosition(neighbour.getPosition());
        neighbour.setPosition(position);
        syncSourcePosition(item);
        syncSourcePosition(neighbour);
        items.save(item);
        items.save(neighbour);
        audit.record(actor, "UPDATE", "ITEM", itemId, up ? "SUBIR" : "BAJAR");
        log.info("Ítem reordenado id={} actor={}", itemId, actor);
    }

    @Override
    @Transactional
    public void saveSyllabus(UUID courseId, SyllabusForm form, UUID actor) {
        checkNotNull(form, "form");
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));
        course.updateSyllabus(StringUtils.trimToNull(form.objectives()),
                StringUtils.trimToNull(form.requirements()));
        resources.deleteAll(resources.findByCourseIdOrderByPositionAsc(courseId));
        for (ResourceForm resource : form.resources()) {
            resources.save(new CourseResource(courseId, requireText(resource.title(), "título"),
                    ContentLinks.https(resource.url(), "enlace"), resource.position()));
        }
        audit.record(actor, "UPDATE", "SILABO", courseId, "SÍLABO");
        log.info("Sílabo actualizado curso={} actor={}", courseId, actor);
    }

    @Override
    @Transactional
    public UUID createResource(UUID courseId, ResourceForm form, UUID actor) {
        courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));
        CourseResource resource = new CourseResource(courseId,
                requireText(form.title(), "título"),
                ContentLinks.https(form.url(), "enlace"), form.position());
        resources.save(resource);
        audit.record(actor, "CREATE", "RECURSO", resource.getId(), resource.getTitle());
        log.info("Recurso creado id={} curso={} actor={}", resource.getId(), courseId, actor);
        return resource.getId();
    }

    @Override
    @Transactional
    public void deleteResource(UUID resourceId, UUID actor) {
        CourseResource resource = resources.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Recurso no encontrado"));
        String title = resource.getTitle();
        resources.delete(resource);
        audit.record(actor, "DELETE", "RECURSO", resourceId, title);
        log.info("Recurso eliminado id={} actor={}", resourceId, actor);
    }

    private void applyMetadata(ContentItem item, ItemForm form) {
        checkNotNull(item, "El ítem correspondiente no existe en la base");
        item.moveTo(form.moduleId(), form.sectionId());
        item.setMetadata(form.durationSeconds(), form.visible(), form.position());
        items.save(item);
    }

    private void syncSourcePosition(ContentItem item) {
        if (item.getSourceId() == null) return;
        if ("VIDEO".equals(item.getSourceType())) {
            videos.findById(item.getSourceId()).ifPresent(video -> {
                video.setPosition(item.getPosition());
                videos.save(video);
            });
        } else if ("EJERCICIO".equals(item.getSourceType())) {
            exercises.findById(item.getSourceId()).ifPresent(exercise -> {
                exercise.update(exercise.getTitle(), exercise.getPrompt(), item.getPosition());
                exercises.save(exercise);
            });
        }
    }

    private void requireSectionInModule(UUID sectionId, UUID moduleId) {
        if (sectionId == null) return;
        CourseSection section = requireSection(sectionId);
        checkArgument(section.getModuleId().equals(moduleId),
                "La sección no pertenece al módulo");
    }

    private CourseModule requireModule(UUID moduleId) {
        checkNotNull(moduleId, "moduleId");
        return modules.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Módulo no encontrado"));
    }

    private CourseSection requireSection(UUID sectionId) {
        checkNotNull(sectionId, "sectionId");
        return sections.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Sección no encontrada"));
    }

    private String requireType(String type) {
        String value = StringUtils.trimToNull(type);
        checkArgument("VIDEO".equals(value) || "MATERIAL".equals(value)
                || "EJERCICIO".equals(value), "Tipo de ítem no válido");
        return value;
    }

    private String requireText(String value, String field) {
        String text = StringUtils.trimToNull(value);
        checkArgument(text != null, "El campo " + field + " es obligatorio");
        return text;
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
