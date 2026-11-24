package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/cursos")
public class AdminCourseController extends AdminBaseController {

    private final AdminCatalogService catalog;

    public AdminCourseController(AdminNotificationService notifications, AdminCatalogService catalog) {
        super(notifications);
        this.catalog = catalog;
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
        model.addAttribute("adminCourseForm", new AdminCourseForm("", "", "Nivel inicial", 1,
                true, "", "", "/img/cursos/python.jpg", 0));
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
            model.addAttribute("courseId", id);
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
        if (id != null) model.addAttribute("courseId", id);
        return "admin/curso-form";
    }
}
