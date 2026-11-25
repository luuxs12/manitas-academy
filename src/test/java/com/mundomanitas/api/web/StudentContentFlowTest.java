package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.StudentController;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.CourseCatalog;
import com.mundomanitas.api.service.CourseContentService;
import com.mundomanitas.api.service.EnrollmentService;
import com.mundomanitas.api.service.LessonProgressService;
import com.mundomanitas.api.service.StudentContentService;
import com.mundomanitas.api.service.VideoPlaybackService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("Contenido estructurado en la vista del estudiante")
class StudentContentFlowTest {

    private final String slug = "python";
    private UUID actor;
    private UUID courseId;
    private UUID enrollmentId;
    private User student;
    private Course course;
    private Enrollment enrollment;
    private CourseContentService content;
    private VideoPlaybackService playback;
    private CourseCatalog catalog;
    private EnrollmentService enrollments;
    private StudentContentService oldContent;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        courseId = UUID.randomUUID();
        enrollmentId = UUID.randomUUID();

        student = mock(User.class);
        when(student.getId()).thenReturn(actor);
        when(student.getRole()).thenReturn("STUDENT");
        AuthenticatedUser principal = new AuthenticatedUser(student);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        AccountService accounts = mock(AccountService.class);
        catalog = mock(CourseCatalog.class);
        enrollments = mock(EnrollmentService.class);
        LessonProgressService lessons = mock(LessonProgressService.class);
        oldContent = mock(StudentContentService.class);
        content = mock(CourseContentService.class);
        playback = mock(VideoPlaybackService.class);

        course = mock(Course.class);
        when(course.getId()).thenReturn(courseId);
        when(course.getSlug()).thenReturn(slug);
        when(course.getTitle()).thenReturn("Python");

        enrollment = mock(Enrollment.class);
        when(enrollment.getId()).thenReturn(enrollmentId);
        when(enrollment.getCourse()).thenReturn(course);

        when(accounts.require(actor)).thenReturn(student);
        when(enrollments.findMine(student, slug)).thenReturn(enrollment);
        when(lessons.completedVideos(enrollmentId)).thenReturn(Set.of());
        when(content.outline(courseId, enrollmentId, student, false))
                .thenReturn(new CourseContentService.Outline(List.of(), 0, 0, 0, null));

        mvc = MockMvcBuilders.standaloneSetup(new StudentController(
                        accounts, catalog, enrollments, lessons, playback, oldContent, content))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("el panel entrega a cada tarjeta el avance real de su matrícula")
    void dashboardUsesEachEnrollmentProgress() throws Exception {
        UUID dashboardEnrollmentId = UUID.randomUUID();
        Course dashboardCourse = mock(Course.class);
        when(dashboardCourse.getId()).thenReturn(courseId);
        when(dashboardCourse.getSlug()).thenReturn(slug);
        when(dashboardCourse.getTitle()).thenReturn("Python");
        Enrollment dashboardEnrollment = mock(Enrollment.class);
        when(dashboardEnrollment.getId()).thenReturn(dashboardEnrollmentId);
        when(dashboardEnrollment.getCourse()).thenReturn(dashboardCourse);

        when(enrollments.mine(student)).thenReturn(List.of(dashboardEnrollment));
        when(enrollments.next(student)).thenReturn(dashboardEnrollment);
        when(oldContent.progress(dashboardEnrollmentId, courseId))
                .thenReturn(new StudentContentService.Progress(3, 10, 30));
        when(oldContent.totals(List.of(dashboardEnrollment)))
                .thenReturn(new StudentContentService.Progress(3, 10, 30));
        when(catalog.all()).thenReturn(List.of());

        mvc.perform(get("/panel"))
                .andExpect(view().name("student/panel"))
                .andExpect(model().attribute("doneCounts", Map.of(dashboardEnrollmentId, 3)))
                .andExpect(model().attribute("videoCounts", Map.of(dashboardEnrollmentId, 10)))
                .andExpect(model().attribute("videoProgress", Map.of(dashboardEnrollmentId, 30)));

        verify(oldContent).progress(dashboardEnrollmentId, courseId);
    }

    @Test
    @DisplayName("muestra los módulos, secciones e ítems del servicio estructurado")
    void usesStructuredOutline() throws Exception {
        UUID itemId = UUID.randomUUID();
        CourseContentService.ItemView item = new CourseContentService.ItemView(itemId, "MATERIAL",
                "Guía", "Lectura real", "https://example.org/guia", null, null,
                true, false, false, null);
        CourseContentService.Outline outline = new CourseContentService.Outline(
                List.of(new CourseContentService.ModuleView(UUID.randomUUID(), "Unidad 1", true,
                        List.of(), List.of(item), 0)),
                1, 0, 0, itemId);
        when(content.outline(courseId, enrollmentId, student, false)).thenReturn(outline);

        mvc.perform(get("/panel/cursos/{slug}", slug))
                .andExpect(view().name("student/curso"))
                .andExpect(model().attribute("modules", outline.modules()))
                .andExpect(model().attribute("itemCount", 1))
                .andExpect(model().attribute("doneCount", 0))
                .andExpect(model().attribute("itemPercent", 0));

        verify(content).outline(courseId, enrollmentId, student, false);
    }

    @Test
    @DisplayName("abre la clase desde el id real del video guardado en el ítem")
    void opensVideoFromSourceId() throws Exception {
        UUID videoId = UUID.randomUUID();
        VideoPlaybackService.Playback player = new VideoPlaybackService.Playback(
                "Clase 1", "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        when(playback.play(videoId, enrollment)).thenReturn(player);

        mvc.perform(get("/panel/cursos/{slug}", slug).param("video", videoId.toString()))
                .andExpect(model().attribute("player", player))
                .andExpect(model().attribute("selectedId", videoId));

        verify(playback).play(videoId, enrollment);
    }

    @Test
    @DisplayName("guardar y deshacer el avance de materiales pasa por el servicio con la matrícula")
    void marksItemsSeen() throws Exception {
        UUID itemId = UUID.randomUUID();

        mvc.perform(post("/panel/cursos/{slug}/items/{itemId}/visto", slug, itemId)
                        .param("seen", "true"))
                .andExpect(redirectedUrl("/panel/cursos/" + slug))
                .andExpect(flash().attribute("panelOk", "Avance guardado en «Python»."));

        mvc.perform(post("/panel/cursos/{slug}/items/{itemId}/visto", slug, itemId)
                        .param("seen", "false"))
                .andExpect(redirectedUrl("/panel/cursos/" + slug));

        verify(content).markSeen(student, slug, itemId, true);
        verify(content).markSeen(student, slug, itemId, false);
    }
}
