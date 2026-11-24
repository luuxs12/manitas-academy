package com.mundomanitas.api.controller;

import com.mundomanitas.api.exception.PremiumRequiredException;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.CourseCatalog;
import com.mundomanitas.api.service.EnrollmentService;
import com.mundomanitas.api.service.LessonProgressService;
import com.mundomanitas.api.service.VideoPlaybackService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Módulos del área de estudiante.
 *
 * <p>Todas las acciones vuelven al panel: las matrículas, el avance y los
 * certificados se guardan en PostgreSQL y nunca llevan a la portada.</p>
 */
@Controller
@RequestMapping("/panel")
public class StudentController {

    private final AccountService accounts;
    private final CourseCatalog catalog;
    private final EnrollmentService enrollments;
    private final LessonProgressService lessons;
    private final VideoPlaybackService playback;

    public StudentController(
            AccountService accounts,
            CourseCatalog catalog,
            EnrollmentService enrollments,
            LessonProgressService lessons,
            VideoPlaybackService playback
    ) {
        this.accounts = accounts;
        this.catalog = catalog;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.playback = playback;
    }

    @GetMapping
    public String dashboard(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "bienvenida", required = false) String welcome,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        List<Enrollment> mine = enrollments.mine(user);
        long completed = mine.stream().filter(Enrollment::isCompleted).count();
        int progress = mine.isEmpty()
                ? 0
                : (int) Math.round(completed * 100.0 / mine.size());

        Set<String> enrolledSlugs = mine.stream()
                .map(enrollment -> enrollment.getCourse().getSlug())
                .collect(Collectors.toSet());

        model.addAttribute("enrollments", mine);
        model.addAttribute("next", enrollments.next(user));
        model.addAttribute("enrolled", mine.size());
        model.addAttribute("completed", completed);
        model.addAttribute("progress", progress);
        model.addAttribute("videoTotals", lessons.totals(mine));
        model.addAttribute("certificates", user.isPremium() ? completed : 0);
        model.addAttribute("recommended", catalog.all().stream()
                .filter(course -> !enrolledSlugs.contains(course.getSlug()))
                .limit(3)
                .toList());
        model.addAttribute("welcome", welcome != null);
        return "student/panel";
    }

    @GetMapping("/cursos")
    public String myCourses(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        List<Enrollment> mine = enrollments.mine(user);
        Map<UUID, Set<UUID>> doneVideos = new HashMap<>();
        Map<UUID, Integer> videoCounts = new HashMap<>();
        Map<UUID, Integer> videoProgress = new HashMap<>();
        int totalVideos = 0;
        int completedVideos = 0;
        for (Enrollment enrollment : mine) {
            int total = playback.videoCount(enrollment.getCourse().getId());
            int done = lessons.completed(enrollment.getId());
            Set<UUID> doneIds = lessons.completedVideos(enrollment.getId());
            doneVideos.put(enrollment.getId(), doneIds);
            videoCounts.put(enrollment.getId(), total);
            videoProgress.put(enrollment.getId(),
                    total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
            totalVideos += total;
            completedVideos += done;
        }
        int percent = totalVideos == 0
                ? 0
                : (int) Math.round(completedVideos * 100.0 / totalVideos);
        model.addAttribute("enrollments", mine);
        model.addAttribute("completed", enrollments.completed(user).size());
        model.addAttribute("videoTotals",
                new LessonProgressService.LessonTotals(completedVideos, totalVideos, percent));
        model.addAttribute("doneVideos", doneVideos);
        model.addAttribute("videoCounts", videoCounts);
        model.addAttribute("videoProgress", videoProgress);
        return "student/cursos";
    }

    /** Temario y reproductor de un curso inscripto. */
    @GetMapping("/cursos/{slug}")
    public String course(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            @RequestParam(name = "video", required = false) UUID videoId,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        Enrollment enrollment = enrollments.findMine(user, slug);
        List<VideoPlaybackService.CourseVideo> courseVideos =
                playback.courseVideos(enrollment.getCourse().getId());
        Set<UUID> doneVideos = lessons.completedVideos(enrollment.getId());

        VideoPlaybackService.Playback player = null;
        if (videoId != null) {
            try {
                player = playback.play(videoId, enrollment);
            } catch (ResponseStatusException exception) {
                model.addAttribute("panelError", "Ese video no está disponible en este momento.");
            }
        }

        int total = courseVideos.size();
        int done = doneVideos.size();
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("videos", courseVideos);
        model.addAttribute("doneVideos", doneVideos);
        model.addAttribute("selectedId", player == null ? null : videoId);
        model.addAttribute("player", player);
        model.addAttribute("videoCount", total);
        model.addAttribute("doneCount", done);
        model.addAttribute("videoPercent",
                total == 0 ? 0 : (int) Math.round(done * 100.0 / total));
        return "student/curso";
    }

    /** Catálogo dentro del panel: la búsqueda no sale del área de estudiante. */
    @GetMapping("/explorar")
    public String explore(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(name = "q", defaultValue = "") String query,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        Set<String> enrolledSlugs = enrollments.mine(user).stream()
                .map(enrollment -> enrollment.getCourse().getSlug())
                .collect(Collectors.toSet());

        model.addAttribute("courses", catalog.search(query));
        model.addAttribute("areas", catalog.categories());
        model.addAttribute("query", query);
        model.addAttribute("enrolledSlugs", enrolledSlugs);
        return "student/explorar";
    }

    @PostMapping("/cursos/{slug}/inscribir")
    public String enroll(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        Enrollment enrollment = enrollments.enroll(user, slug);
        redirect.addFlashAttribute(
                "panelOk",
                "Te inscribiste en «" + enrollment.getCourse().getTitle() + "»."
        );
        return "redirect:/panel/cursos";
    }

    @PostMapping("/cursos/{slug}/completar")
    public String complete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        Enrollment enrollment = enrollments.complete(user, slug);
        redirect.addFlashAttribute(
                "panelOk",
                "Marcaste «" + enrollment.getCourse().getTitle() + "» como terminado."
        );
        return "redirect:/panel/cursos";
    }

    /** Guarda el avance de un video real del curso inscripto. */
    @PostMapping("/cursos/{slug}/videos/{videoId}")
    public String toggleVideo(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            @PathVariable UUID videoId,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        Enrollment enrollment = enrollments.toggleVideo(user, slug, videoId);
        redirect.addFlashAttribute(
                "panelOk",
                "Avance guardado en «" + enrollment.getCourse().getTitle() + "»."
        );
        return "redirect:/panel/cursos/" + slug + "?video=" + videoId;
    }

    @GetMapping("/certificados")
    public String certificates(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        model.addAttribute("certificates", user.isPremium()
                ? enrollments.completed(user)
                : List.of());
        model.addAttribute("completed", enrollments.completed(user).size());
        return "student/certificados";
    }

    @GetMapping("/certificados/{slug}")
    public String certificate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            Model model,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        Enrollment enrollment = enrollments.findMine(user, slug);
        if (!enrollment.isCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El curso todavía no está terminado");
        }
        if (!user.isPremium()) {
            redirect.addFlashAttribute(
                    "panelError", "Los certificados están incluidos en los planes Premium.");
            return "redirect:/panel/certificados";
        }

        model.addAttribute("enrollment", enrollment);
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("account", user);
        return "student/certificado";
    }

    @GetMapping("/ayuda")
    public String help() {
        return "student/ayuda";
    }

    @ExceptionHandler(PremiumRequiredException.class)
    public String premiumRequired(
            PremiumRequiredException exception,
            RedirectAttributes redirect
    ) {
        redirect.addFlashAttribute(
                "panelError",
                "El curso «" + exception.getCourseTitle() + "» requiere un plan Premium."
        );
        return "redirect:/panel/explorar";
    }
}
