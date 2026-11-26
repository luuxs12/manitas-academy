package com.mundomanitas.api.controller;

import com.mundomanitas.api.exception.PremiumRequeridoException;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.CursoCatalog;
import com.mundomanitas.api.service.CursoContenidoService;
import com.mundomanitas.api.service.InscripcionService;
import com.mundomanitas.api.service.LeccionProgresoService;
import com.mundomanitas.api.service.EstudianteCertificadoService;
import com.mundomanitas.api.service.EstudianteContenidoService;
import com.mundomanitas.api.service.VideoReproduccionService;
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
public class EstudianteController {

    private final CuentaService accounts;
    private final CursoCatalog catalog;
    private final InscripcionService enrollments;
    private final LeccionProgresoService lessons;
    private final VideoReproduccionService reproduccion;
    private final EstudianteContenidoService content;
    private final CursoContenidoService estructura;
    private final EstudianteCertificadoService certificates;

    public EstudianteController(
            CuentaService accounts,
            CursoCatalog catalog,
            InscripcionService enrollments,
            LeccionProgresoService lessons,
            VideoReproduccionService reproduccion,
            EstudianteContenidoService content,
            CursoContenidoService estructura,
            EstudianteCertificadoService certificates
    ) {
        this.accounts = accounts;
        this.catalog = catalog;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.reproduccion = reproduccion;
        this.content = content;
        this.estructura = estructura;
        this.certificates = certificates;
    }

    @GetMapping
    public String dashboard(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @RequestParam(name = "bienvenida", required = false) String welcome,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        List<Inscripcion> mine = enrollments.mine(usuario);
        long finalizado = mine.stream().filter(Inscripcion::isFinalizado).count();
        int progreso = mine.isEmpty()
                ? 0
                : (int) Math.round(finalizado * 100.0 / mine.size());

        Set<String> inscriptoSlugs = mine.stream()
                .map(inscripcion -> inscripcion.getCurso().getSlug())
                .collect(Collectors.toSet());

        CursoProgreso cursoProgreso = progresoOf(mine);
        model.addAttribute("enrollments", mine);
        model.addAttribute("next", enrollments.next(usuario));
        model.addAttribute("inscripto", mine.size());
        model.addAttribute("finalizado", finalizado);
        model.addAttribute("progreso", progreso);
        model.addAttribute("doneCounts", cursoProgreso.done());
        model.addAttribute("videoCounts", cursoProgreso.total());
        model.addAttribute("videoProgreso", cursoProgreso.percent());
        model.addAttribute("videoTotals", cursoProgreso.totals());
        model.addAttribute("certificates", usuario.isPremium() ? finalizado : 0);
        model.addAttribute("recommended", catalog.all().stream()
                .filter(curso -> !inscriptoSlugs.contains(curso.getSlug()))
                .limit(3)
                .toList());
        model.addAttribute("welcome", welcome != null);
        return "student/panel";
    }

    @GetMapping("/cursos")
    public String myCourses(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        List<Inscripcion> mine = enrollments.mine(usuario);
        CursoProgreso cursoProgreso = progresoOf(mine);
        model.addAttribute("enrollments", mine);
        model.addAttribute("finalizado", enrollments.finalizado(usuario).size());
        model.addAttribute("videoTotals", cursoProgreso.totals());
        model.addAttribute("doneCounts", cursoProgreso.done());
        model.addAttribute("videoCounts", cursoProgreso.total());
        model.addAttribute("videoProgreso", cursoProgreso.percent());
        model.addAttribute("certificadoSlugs", activoCertificadoSlugs(usuario));
        return "student/cursos";
    }

    /** Temario y reproductor de un curso inscripto. */
    @GetMapping("/cursos/{slug}")
    public String curso(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @RequestParam(name = "video", required = false) UUID videoId,
            @RequestParam(name = "material", required = false) UUID materialId,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        Inscripcion inscripcion = enrollments.buscarMine(usuario, slug);
        UUID cursoId = inscripcion.getCurso().getId();
        CursoContenidoService.Esquema esquema =
                estructura.esquema(cursoId, inscripcion.getId(), usuario, false);
        Set<UUID> doneVideos = lessons.finalizadoVideos(inscripcion.getId());

        VideoReproduccionService.Reproduccion player = null;
        if (videoId != null) {
            try {
                player = reproduccion.play(videoId, inscripcion);
            } catch (ResponseStatusException exception) {
                model.addAttribute("panelError", "Ese video no está disponible en este momento.");
            }
        }

        CursoContenidoService.MaterialVista material = null;
        if (materialId != null) {
            try {
                material = estructura.vistaMaterial(usuario, slug, materialId);
            } catch (ResponseStatusException exception) {
                model.addAttribute("panelError", "Ese documento no está disponible en este momento.");
            }
        }

        model.addAttribute("inscripcion", inscripcion);
        model.addAttribute("curso", inscripcion.getCurso());
        model.addAttribute("modules", esquema.modules());
        model.addAttribute("itemHeadings", silaboHeadings(esquema));
        model.addAttribute("doneVideos", doneVideos);
        model.addAttribute("selectedId", player == null ? null : videoId);
        model.addAttribute("selectedItemId", material == null ? null : material.id());
        model.addAttribute("player", player);
        model.addAttribute("material", material);
        model.addAttribute("itemContar", esquema.totalItems());
        model.addAttribute("doneContar", esquema.doneItems());
        model.addAttribute("itemPercent", esquema.percent());
        model.addAttribute("certificadoSlugs", activoCertificadoSlugs(usuario));
        return "student/curso";
    }

    /** Catálogo dentro del panel: la búsqueda no sale del área de estudiante. */
    @GetMapping("/explorar")
    public String explore(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @RequestParam(name = "q", defaultValue = "") String query,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        Set<String> inscriptoSlugs = enrollments.mine(usuario).stream()
                .map(inscripcion -> inscripcion.getCurso().getSlug())
                .collect(Collectors.toSet());

        model.addAttribute("courses", catalog.search(query));
        model.addAttribute("areas", catalog.categories());
        model.addAttribute("query", query);
        model.addAttribute("inscriptoSlugs", inscriptoSlugs);
        return "student/explorar";
    }

    @PostMapping("/cursos/{slug}/inscribir")
    public String enroll(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        Inscripcion inscripcion = enrollments.enroll(usuario, slug);
        redirect.addFlashAttribute(
                "panelOk",
                "Te inscribiste en «" + inscripcion.getCurso().getTitulo() + "»."
        );
        return "redirect:/panel/cursos";
    }

    @PostMapping("/cursos/{slug}/completar")
    public String complete(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        Inscripcion inscripcion = enrollments.complete(usuario, slug);
        redirect.addFlashAttribute(
                "panelOk",
                "Marcaste «" + inscripcion.getCurso().getTitulo() + "» como terminado."
        );
        return "redirect:/panel/cursos";
    }

    /** Guarda el avance de un video real del curso inscripto. */
    @PostMapping("/cursos/{slug}/videos/{videoId}")
    public String toggleVideo(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @PathVariable UUID videoId,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        Inscripcion inscripcion = enrollments.toggleVideo(usuario, slug, videoId);
        redirect.addFlashAttribute(
                "panelOk",
                "Avance guardado en «" + inscripcion.getCurso().getTitulo() + "»."
        );
        return "redirect:/panel/cursos/" + slug + "?video=" + videoId;
    }

    /** Guarda el avance de materiales y ejercicios del temario estructurado. */
    @PostMapping("/cursos/{slug}/items/{itemId}/visto")
    public String markItem(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @PathVariable UUID itemId,
            @RequestParam(name = "visto", defaultValue = "true") boolean visto,
            RedirectAttributes redirect
    ) {
        Usuario usuario = accounts.require(principal.getId());
        String titulo = enrollments.buscarMine(usuario, slug).getCurso().getTitulo();
        estructura.markVisto(usuario, slug, itemId, visto);
        redirect.addFlashAttribute("panelOk", "Avance guardado en «" + titulo + "».");
        return "redirect:/panel/cursos/" + slug;
    }

    /** Descarga privada de un documento subido al temario del curso inscripto. */
    @GetMapping("/cursos/{slug}/materiales/{itemId}/descargar")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> descargaMaterial(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @PathVariable UUID itemId
    ) {
        Usuario usuario = accounts.require(principal.getId());
        CursoContenidoService.MaterialDescarga descarga =
                estructura.descargaMaterial(usuario, slug, itemId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.safeFileNombre() + "\"")
                .contentType(MediaType.parseMediaType(descarga.contenidoTipo()))
                .body(descarga.bytes());
    }

    /** PDF privado para el visor del panel; la validación de acceso vive en el servicio. */
    @GetMapping("/cursos/{slug}/materiales/{itemId}/ver")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> vistaPdf(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            @PathVariable UUID itemId
    ) {
        Usuario usuario = accounts.require(principal.getId());
        CursoContenidoService.MaterialDescarga pdf = estructura.leidoPdf(usuario, slug, itemId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + pdf.safeFileNombre() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.bytes());
    }

    @GetMapping("/certificados")
    public String certificates(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        model.addAttribute("certificates", certificates.disponible(usuario));
        model.addAttribute("finalizado", enrollments.finalizado(usuario).size());
        return "student/certificados";
    }

    @GetMapping("/certificados/{slug}")
    public String certificado(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable String slug,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        EstudianteCertificadoService.Emitido certificado = certificates.buscarActivo(usuario, slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No tenés un certificado vigente de ese curso"));

        model.addAttribute("certificado", certificado);
        model.addAttribute("cuenta", usuario);
        return "student/certificado";
    }

    @GetMapping("/ayuda")
    public String help() {
        return "student/ayuda";
    }

    @ExceptionHandler(PremiumRequeridoException.class)
    public String premiumRequerido(
            PremiumRequeridoException exception,
            RedirectAttributes redirect
    ) {
        redirect.addFlashAttribute(
                "panelError",
                "El curso «" + exception.getCursoTitulo() + "» requiere un plan Premium."
        );
        return "redirect:/panel/explorar";
    }

    /**
     * Primer ítem de cada tipo dentro de cada sección. La vista lo usa para
     * rotular "Clase en video", "Material de estudio" y "Práctica" sin
     * duplicar etiquetas ni perder el orden real del temario.
     */
    private Map<UUID, String> silaboHeadings(CursoContenidoService.Esquema esquema) {
        Map<UUID, String> headings = new LinkedHashMap<>();
        for (CursoContenidoService.ModuloVista modulo : esquema.modules()) {
            for (CursoContenidoService.SeccionVista seccion : modulo.sections()) {
                markFirstOfEachKind(headings, seccion.items());
            }
            markFirstOfEachKind(headings, modulo.items());
        }
        return headings;
    }

    private void markFirstOfEachKind(Map<UUID, String> headings,
                                     List<CursoContenidoService.ItemVista> items) {
        Set<String> visto = new HashSet<>();
        for (CursoContenidoService.ItemVista item : items) {
            String label = switch (item.type()) {
                case "VIDEO" -> "Clase en video";
                case "MATERIAL" -> "Material de estudio";
                case "EJERCICIO" -> "Práctica";
                default -> null;
            };
            if (label != null && visto.add(label)) {
                headings.put(item.id(), label);
            }
        }
    }

    /** Slugs con certificado vigente, para no ofrecer enlaces que dan 404. */
    private List<String> activoCertificadoSlugs(Usuario usuario) {
        return certificates.disponible(usuario).stream()
                .map(EstudianteCertificadoService.Emitido::cursoSlug)
                .toList();
    }

    /** Avance por matrícula: el panel y el listado comparten una sola consulta. */
    private CursoProgreso progresoOf(List<Inscripcion> mine) {
        Map<UUID, Integer> done = new HashMap<>();
        Map<UUID, Integer> total = new HashMap<>();
        Map<UUID, Integer> percent = new HashMap<>();
        int finalizadoVideos = 0;
        int allVideos = 0;
        for (Map.Entry<UUID, EstudianteContenidoService.Progreso> entry
                : content.progresoFor(mine).entrySet()) {
            UUID inscripcionId = entry.getKey();
            EstudianteContenidoService.Progreso progreso = entry.getValue();
            done.put(inscripcionId, progreso.done());
            total.put(inscripcionId, progreso.total());
            percent.put(inscripcionId, progreso.percent());
            finalizadoVideos += progreso.done();
            allVideos += progreso.total();
        }
        int overall = allVideos == 0
                ? 0
                : (int) Math.round(finalizadoVideos * 100.0 / allVideos);
        LeccionProgresoService.LeccionTotals totals =
                new LeccionProgresoService.LeccionTotals(finalizadoVideos, allVideos, overall);
        return new CursoProgreso(done, total, percent, totals);
    }

    private record CursoProgreso(
            Map<UUID, Integer> done,
            Map<UUID, Integer> total,
            Map<UUID, Integer> percent,
            LeccionProgresoService.LeccionTotals totals) {}
}
