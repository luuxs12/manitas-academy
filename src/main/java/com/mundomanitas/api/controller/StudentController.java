package com.mundomanitas.api.controller;

import com.mundomanitas.api.exception.PremiumRequiredException;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.CourseCatalog;
import com.mundomanitas.api.service.CourseContentService;
import com.mundomanitas.api.service.EnrollmentService;
import com.mundomanitas.api.service.LessonProgressService;
import com.mundomanitas.api.service.StudentCertificateService;
import com.mundomanitas.api.service.StudentContentService;
import com.mundomanitas.api.service.VideoPlaybackService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final StudentContentService content;
    private final CourseContentService structure;
    private final StudentCertificateService certificates;

    public StudentController(
            AccountService accounts,
            CourseCatalog catalog,
            EnrollmentService enrollments,
            LessonProgressService lessons,
            VideoPlaybackService playback,
            StudentContentService content,
            CourseContentService structure,
            StudentCertificateService certificates
    ) {
        this.accounts = accounts;
        this.catalog = catalog;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.playback = playback;
        this.content = content;
        this.structure = structure;
        this.certificates = certificates;
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

        CourseProgress courseProgress = progressOf(mine);
        model.addAttribute("enrollments", mine);
        model.addAttribute("next", enrollments.next(user));
        model.addAttribute("enrolled", mine.size());
        model.addAttribute("completed", completed);
        model.addAttribute("progress", progress);
        model.addAttribute("doneCounts", courseProgress.done());
        model.addAttribute("videoCounts", courseProgress.total());
        model.addAttribute("videoProgress", courseProgress.percent());
        model.addAttribute("videoTotals", courseProgress.totals());
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
        CourseProgress courseProgress = progressOf(mine);
        model.addAttribute("enrollments", mine);
        model.addAttribute("completed", enrollments.completed(user).size());
        model.addAttribute("videoTotals", courseProgress.totals());
        model.addAttribute("doneCounts", courseProgress.done());
        model.addAttribute("videoCounts", courseProgress.total());
        model.addAttribute("videoProgress", courseProgress.percent());
        model.addAttribute("certificateSlugs", activeCertificateSlugs(user));
        return "student/cursos";
    }

    /** Temario y reproductor de un curso inscripto. */
    @GetMapping("/cursos/{slug}")
    public String course(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            @RequestParam(name = "video", required = false) UUID videoId,
            @RequestParam(name = "material", required = false) UUID materialId,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        Enrollment enrollment = enrollments.findMine(user, slug);
        UUID courseId = enrollment.getCourse().getId();
        CourseContentService.Outline outline =
                structure.outline(courseId, enrollment.getId(), user, false);
        Set<UUID> doneVideos = lessons.completedVideos(enrollment.getId());

        VideoPlaybackService.Playback player = null;
        if (videoId != null) {
            try {
                player = playback.play(videoId, enrollment);
            } catch (ResponseStatusException exception) {
                model.addAttribute("panelError", "Ese video no está disponible en este momento.");
            }
        }

        CourseContentService.MaterialView material = null;
        if (materialId != null) {
            try {
                material = structure.viewMaterial(user, slug, materialId);
            } catch (ResponseStatusException exception) {
                model.addAttribute("panelError", "Ese documento no está disponible en este momento.");
            }
        }

        model.addAttribute("enrollment", enrollment);
        model.addAttribute("course", enrollment.getCourse());
        model.addAttribute("modules", outline.modules());
        model.addAttribute("itemHeadings", syllabusHeadings(outline));
        model.addAttribute("doneVideos", doneVideos);
        model.addAttribute("selectedId", player == null ? null : videoId);
        model.addAttribute("selectedItemId", material == null ? null : material.id());
        model.addAttribute("player", player);
        model.addAttribute("material", material);
        model.addAttribute("itemCount", outline.totalItems());
        model.addAttribute("doneCount", outline.doneItems());
        model.addAttribute("itemPercent", outline.percent());
        model.addAttribute("certificateSlugs", activeCertificateSlugs(user));
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

    /** Guarda el avance de materiales y ejercicios del temario estructurado. */
    @PostMapping("/cursos/{slug}/items/{itemId}/visto")
    public String markItem(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            @PathVariable UUID itemId,
            @RequestParam(name = "seen", defaultValue = "true") boolean seen,
            RedirectAttributes redirect
    ) {
        User user = accounts.require(principal.getId());
        String title = enrollments.findMine(user, slug).getCourse().getTitle();
        structure.markSeen(user, slug, itemId, seen);
        redirect.addFlashAttribute("panelOk", "Avance guardado en «" + title + "».");
        return "redirect:/panel/cursos/" + slug;
    }

    /** Descarga privada de un documento subido al temario del curso inscripto. */
    @GetMapping("/cursos/{slug}/materiales/{itemId}/descargar")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> downloadMaterial(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            @PathVariable UUID itemId
    ) {
        User user = accounts.require(principal.getId());
        CourseContentService.MaterialDownload download =
                structure.downloadMaterial(user, slug, itemId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.safeFileName() + "\"")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.bytes());
    }

    /** PDF privado para el visor del panel; la validación de acceso vive en el servicio. */
    @GetMapping("/cursos/{slug}/materiales/{itemId}/ver")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> viewPdf(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            @PathVariable UUID itemId
    ) {
        User user = accounts.require(principal.getId());
        CourseContentService.MaterialDownload pdf = structure.readPdf(user, slug, itemId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + pdf.safeFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.bytes());
    }

    @GetMapping("/certificados")
    public String certificates(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        model.addAttribute("certificates", certificates.available(user));
        model.addAttribute("completed", enrollments.completed(user).size());
        return "student/certificados";
    }

    @GetMapping("/certificados/{slug}")
    public String certificate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String slug,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        StudentCertificateService.Issued certificate = certificates.findActive(user, slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No tenés un certificado vigente de ese curso"));

        model.addAttribute("certificate", certificate);
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

    /**
     * Primer ítem de cada tipo dentro de cada sección. La vista lo usa para
     * rotular "Clase en video", "Material de estudio" y "Práctica" sin
     * duplicar etiquetas ni perder el orden real del temario.
     */
    private Map<UUID, String> syllabusHeadings(CourseContentService.Outline outline) {
        Map<UUID, String> headings = new LinkedHashMap<>();
        for (CourseContentService.ModuleView module : outline.modules()) {
            for (CourseContentService.SectionView section : module.sections()) {
                markFirstOfEachKind(headings, section.items());
            }
            markFirstOfEachKind(headings, module.items());
        }
        return headings;
    }

    private void markFirstOfEachKind(Map<UUID, String> headings,
                                     List<CourseContentService.ItemView> items) {
        Set<String> seen = new HashSet<>();
        for (CourseContentService.ItemView item : items) {
            String label = switch (item.type()) {
                case "VIDEO" -> "Clase en video";
                case "MATERIAL" -> "Material de estudio";
                case "EJERCICIO" -> "Práctica";
                default -> null;
            };
            if (label != null && seen.add(label)) {
                headings.put(item.id(), label);
            }
        }
    }

    /** Slugs con certificado vigente, para no ofrecer links que dan 404. */
    private List<String> activeCertificateSlugs(User user) {
        return certificates.available(user).stream()
                .map(StudentCertificateService.Issued::courseSlug)
                .toList();
    }

    /** Avance por matrícula: el panel y el listado comparten una sola consulta. */
    private CourseProgress progressOf(List<Enrollment> mine) {
        Map<UUID, Integer> done = new HashMap<>();
        Map<UUID, Integer> total = new HashMap<>();
        Map<UUID, Integer> percent = new HashMap<>();
        int completedVideos = 0;
        int allVideos = 0;
        for (Map.Entry<UUID, StudentContentService.Progress> entry
                : content.progressFor(mine).entrySet()) {
            UUID enrollmentId = entry.getKey();
            StudentContentService.Progress progress = entry.getValue();
            done.put(enrollmentId, progress.done());
            total.put(enrollmentId, progress.total());
            percent.put(enrollmentId, progress.percent());
            completedVideos += progress.done();
            allVideos += progress.total();
        }
        int overall = allVideos == 0
                ? 0
                : (int) Math.round(completedVideos * 100.0 / allVideos);
        LessonProgressService.LessonTotals totals =
                new LessonProgressService.LessonTotals(completedVideos, allVideos, overall);
        return new CourseProgress(done, total, percent, totals);
    }

    private record CourseProgress(
            Map<UUID, Integer> done,
            Map<UUID, Integer> total,
            Map<UUID, Integer> percent,
            LessonProgressService.LessonTotals totals) {}
}
