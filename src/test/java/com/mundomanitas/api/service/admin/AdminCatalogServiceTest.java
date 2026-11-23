package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminUserForm;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.EnrollmentRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catálogo administrativo")
class AdminCatalogServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private CourseRepository courses;

    @Mock
    private CourseModuleRepository modules;

    @Mock
    private VideoRepository videos;

    @Mock
    private ExerciseRepository exercises;

    @Mock
    private EnrollmentRepository enrollments;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AdminAuditService audit;

    @Mock
    private AdminFormat format;

    private AdminCatalogService service;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogServiceImpl(
                users, courses, modules, videos, exercises, enrollments, encoder, audit, format);
    }

    @Test
    @DisplayName("no crea dos cursos con el mismo identificador")
    void rejectsDuplicateCourseSlug() {
        when(courses.findBySlug("python")).thenReturn(Optional.of(new Course(
                "python", "Python", "Inicial", 10, true, "Programación",
                "Resumen", "/img/cursos/python.jpg", 1)));

        assertThatThrownBy(() -> service.createCourse(form(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");

        verify(courses, never()).save(any());
        verify(audit, never()).record(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("crea el curso y registra la auditoría")
    void createsCourseAndAudits() {
        when(courses.findBySlug("python")).thenReturn(Optional.empty());
        UUID actor = UUID.randomUUID();

        service.createCourse(form(), actor);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).save(captor.capture());
        verify(audit).record(eq(actor), eq("CREATE"), eq("CURSO"),
                eq(captor.getValue().getId()), eq("Python desde cero"));
    }

    @Test
    @DisplayName("no permite cambiar un correo por el de otra cuenta")
    void rejectsDuplicateEmailWhenUpdatingUser() {
        UUID id = UUID.randomUUID();
        User target = mock(User.class);
        User other = mock(User.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        when(users.findById(id)).thenReturn(Optional.of(target));
        when(users.findByEmailIgnoreCase("otra@correo.com")).thenReturn(Optional.of(other));

        AdminUserForm form = new AdminUserForm(
                "Ana", "otra@correo.com", "STUDENT", true, null);

        assertThatThrownBy(() -> service.updateUser(id, form, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correo");

        verify(audit, never()).record(any(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("elimina el curso y registra la auditoría")
    void deletesCourseAndAudits() {
        UUID id = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Course course = new Course("python", "Python", "Inicial", 10, true,
                "Programación", "Resumen", "/img/cursos/python.jpg", 1);
        when(courses.findById(id)).thenReturn(Optional.of(course));

        service.deleteCourse(id, actor);

        verify(courses).delete(course);
        verify(audit).record(actor, "DELETE", "CURSO", id, "Python");
    }

    private AdminCourseForm form() {
        return new AdminCourseForm("python", "Python desde cero", "Inicial", 10, true,
                "Programación", "Variables y ciclos", "/img/cursos/python.jpg", 1);
    }
}
