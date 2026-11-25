package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminCourseRow;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminOption;
import com.mundomanitas.api.dto.admin.AdminUserForm;
import com.mundomanitas.api.dto.admin.AdminUserRow;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.dto.admin.ExerciseRow;
import com.mundomanitas.api.dto.admin.ModuleRow;
import com.mundomanitas.api.dto.admin.VideoRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD administrativo de cuentas, cursos y contenido, con auditoría. */
public interface AdminCatalogService {

    /** Mejoras pendientes de un curso ya visible; nunca bloquean la publicación. */
    record PublishChecklist(List<String> missing, boolean canPublish) {
    }

    /** Impacto real de borrar un curso, calculado desde la base. */
    record DeletionImpact(int modules, int videos, int exercises,
                          long enrollments, long certificates) {
    }

    String slugFromTitle(String title);

    Page<AdminUserRow> users(String role, String term, Pageable pageable, UUID currentId);

    AdminUserForm userForm(UUID id);

    void updateUser(UUID id, AdminUserForm form, UUID actor);

    Page<AdminCourseRow> courses(String term, Pageable pageable);

    AdminCourseForm courseForm(UUID id);

    UUID createCourse(AdminCourseForm form, UUID actor);

    void updateCourse(UUID id, AdminCourseForm form, UUID actor);

    /** Guarda la portada local y devuelve la ruta anterior para limpiar el archivo. */
    String updateCourseCover(UUID id, String image, UUID actor);

    PublishChecklist publishChecklist(UUID id);

    String courseStatus(UUID id);

    void setPublished(UUID id, boolean published, UUID actor);

    DeletionImpact deletionImpact(UUID id);

    void deleteCourse(UUID id, UUID actor);

    int lessonCount(UUID courseId);

    void moveModule(UUID id, boolean up, UUID actor);

    void moveVideo(UUID id, boolean up, UUID actor);

    Page<ModuleRow> modules(UUID courseId, String term, Pageable pageable);

    AdminModuleForm moduleForm(UUID id);

    UUID createModule(AdminModuleForm form, UUID actor);

    void updateModule(UUID id, AdminModuleForm form, UUID actor);

    void deleteModule(UUID id, UUID actor);

    Page<VideoRow> videos(UUID moduleId, String term, Pageable pageable);

    AdminVideoForm videoForm(UUID id);

    UUID createVideo(AdminVideoForm form, UUID actor);

    void updateVideo(UUID id, AdminVideoForm form, UUID actor);

    void deleteVideo(UUID id, UUID actor);

    Page<ExerciseRow> exercises(UUID moduleId, String term, Pageable pageable);

    AdminExerciseForm exerciseForm(UUID id);

    UUID createExercise(AdminExerciseForm form, UUID actor);

    void updateExercise(UUID id, AdminExerciseForm form, UUID actor);

    void deleteExercise(UUID id, UUID actor);

    List<AdminOption> courseOptions();

    List<AdminOption> moduleOptions();

    Map<UUID, String> userNames();
}
