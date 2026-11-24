package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.CourseCoverStorage;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/cursos")
public class AdminCourseController extends AdminBaseController {

    private final AdminCatalogService catalog;
    private final CourseCoverStorage covers;

    public AdminCourseController(AdminNotificationService notifications,
                                 AdminCatalogService catalog,
                                 CourseCoverStorage covers) {
        super(notifications);
        this.catalog = catalog;
        this.covers = covers;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        common(model, "cursos", "Cursos");
        model.addAttribute("page", catalog.courses(query, PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        return "admin/cursos";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        common(model, "cursos", "Nuevo curso");
        model.addAttribute("adminCourseForm", new AdminCourseForm("", "", "", true, "", "", "", 0));
        return "admin/curso-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute AdminCourseForm form,
                         BindingResult result,
                         @AuthenticationPrincipal AuthenticatedUser principal,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) return formView(model, form, "Nuevo curso", null);
        try {
            UUID id = catalog.createCourse(form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El curso se creó correctamente.");
            return "redirect:/admin/cursos/" + id + "/editar";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/cursos/nuevo";
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "cursos", "Editar curso");
            model.addAttribute("adminCourseForm", catalog.courseForm(id));
            addCourseContext(model, id);
            return "admin/curso-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/cursos";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute AdminCourseForm form,
                         BindingResult result,
                         @AuthenticationPrincipal AuthenticatedUser principal,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) return formView(model, form, "Editar curso", id);
        try {
            catalog.updateCourse(id, form, principal.getId());
            redirect.addFlashAttribute("panelOk", "La ficha del curso se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/cursos/" + id + "/editar";
    }

    @PostMapping("/{id}/publicar")
    public String publish(@PathVariable UUID id,
                          @AuthenticationPrincipal AuthenticatedUser principal,
                          RedirectAttributes redirect) {
        changeStatus(id, true, principal.getId(), redirect);
        return "redirect:/admin/cursos/" + id + "/editar";
    }

    @PostMapping("/{id}/despublicar")
    public String unpublish(@PathVariable UUID id,
                            @AuthenticationPrincipal AuthenticatedUser principal,
                            RedirectAttributes redirect) {
        changeStatus(id, false, principal.getId(), redirect);
        return "redirect:/admin/cursos/" + id + "/editar";
    }

    @PostMapping(value = "/{id}/portada", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadCover(@PathVariable UUID id,
                              @RequestParam("portada") MultipartFile file,
                              @AuthenticationPrincipal AuthenticatedUser principal,
                              RedirectAttributes redirect) {
        String stored = null;
        try {
            stored = covers.store(file);
            String previous = catalog.updateCourseCover(id, stored, principal.getId());
            if (previous != null) covers.delete(previous);
            redirect.addFlashAttribute("panelOk", "La portada se guardó en el servidor.");
        } catch (ResponseStatusException exception) {
            discard(stored);
            redirect.addFlashAttribute("panelError", exception.getReason());
        } catch (IllegalArgumentException exception) {
            discard(stored);
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/cursos/" + id + "/editar#portada";
    }

    @GetMapping("/{id}/eliminar")
    public String confirmDelete(@PathVariable UUID id, Model model,
                                RedirectAttributes redirect) {
        try {
            String title = catalog.courseForm(id).title();
            AdminCatalogService.DeletionImpact impact = catalog.deletionImpact(id);
            List<String> details = List.of(
                    "Curso: " + title,
                    "Módulos: " + impact.modules(),
                    "Videos: " + impact.videos(),
                    "Ejercicios: " + impact.exercises(),
                    "Inscripciones: " + impact.enrollments(),
                    "Certificados: " + impact.certificates());
            return confirmation(model, "cursos", "Eliminar curso",
                    "Se borrarán el curso y su contenido. Esta acción no se puede deshacer.",
                    "/admin/cursos/" + id + "/eliminar", "Eliminar curso",
                    "/admin/cursos", details);
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/cursos";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable UUID id,
                         @AuthenticationPrincipal AuthenticatedUser principal,
                         RedirectAttributes redirect) {
        try {
            catalog.deleteCourse(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El curso se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/cursos";
    }

    private String formView(Model model, AdminCourseForm form, String title, UUID id) {
        common(model, "cursos", title);
        model.addAttribute("adminCourseForm", form);
        addCourseContext(model, id);
        return "admin/curso-form";
    }

    private void addCourseContext(Model model, UUID id) {
        if (id == null) return;
        model.addAttribute("courseId", id);
        model.addAttribute("courseStatus", catalog.courseStatus(id));
        model.addAttribute("checklist", catalog.publishChecklist(id));
    }

    private void changeStatus(UUID id, boolean published, UUID actor,
                              RedirectAttributes redirect) {
        try {
            catalog.setPublished(id, published, actor);
            redirect.addFlashAttribute("panelOk", published
                    ? "El curso ya está publicado."
                    : "El curso pasó a borrador.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
    }

    private void discard(String url) {
        if (url != null) covers.delete(url);
    }
}
