package com.mundomanitas.api.service.admin;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Alta, edición y orden de la estructura de contenido del curso. */
public interface AdminCourseContentService {

    record SectionForm(String title, int position, boolean visible) {
    }

    record ItemForm(UUID moduleId, UUID sectionId, String type, String title, String description,
                    String url, String youtubeUrl, Integer durationSeconds, int position,
                    boolean visible) {
    }

    record DocumentForm(UUID moduleId, UUID sectionId, String title, String description,
                        MultipartFile file, Integer durationSeconds, int position, boolean visible) {
    }

    record ResourceForm(UUID id, String title, String url, int position) {
    }

    record SyllabusForm(String objectives, String requirements, List<ResourceForm> resources) {
    }

    UUID createSection(UUID moduleId, SectionForm form, UUID actor);

    void updateSection(UUID sectionId, SectionForm form, UUID actor);

    void deleteSection(UUID sectionId, UUID actor);

    UUID createItem(ItemForm form, UUID actor);

    UUID createDocument(DocumentForm form, UUID actor);

    void updateItem(UUID itemId, ItemForm form, UUID actor);

    void deleteItem(UUID itemId, UUID actor);

    void moveSection(UUID sectionId, boolean up, UUID actor);

    void moveItem(UUID itemId, boolean up, UUID actor);

    void saveSyllabus(UUID courseId, SyllabusForm form, UUID actor);

    UUID createResource(UUID courseId, ResourceForm form, UUID actor);

    void deleteResource(UUID resourceId, UUID actor);
}
