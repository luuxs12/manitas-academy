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

/**
 * CRUD administrativo de cuentas, cursos y contenido, con auditoría.
 */
public interface AdminCatalogService {

    Page<AdminUserRow> users(String role, String term, Pageable pageable, UUID currentId);

    AdminUserForm userForm(UUID id);

    void updateUser(UUID id, AdminUserForm form, UUID actor);

    Page<AdminCourseRow> courses(String term, Pageable pageable);

    AdminCourseForm courseForm(UUID id);

    UUID createCourse(AdminCourseForm form, UUID actor);

    void updateCourse(UUID id, AdminCourseForm form, UUID actor);

    void deleteCourse(UUID id, UUID actor);

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
