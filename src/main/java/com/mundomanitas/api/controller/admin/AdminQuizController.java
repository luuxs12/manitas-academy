package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminQuizForm;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import com.mundomanitas.api.service.admin.AdminQuizService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Carga de preguntas y alternativas de un ejercicio desde el panel administrativo. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/ejercicios/{id}/preguntas")
public class AdminQuizController extends AdminBaseController {

    private final AdminCatalogService catalog;
    private final AdminQuizService quiz;

    public AdminQuizController(AdminNotificationService notifications,
                               AdminCatalogService catalog,
                               AdminQuizService quiz) {
        super(notifications);
        this.catalog = catalog;
        this.quiz = quiz;
    }

    @GetMapping
    public String form(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            return render(id, quiz.form(id), model);
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios";
        }
    }

    @PostMapping
    public String save(@PathVariable UUID id,
                       @Valid @ModelAttribute("adminQuizForm") AdminQuizForm form,
                       BindingResult result,
                       @AuthenticationPrincipal AuthenticatedUser principal,
                       Model model,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) return render(id, form, model);
        try {
            quiz.save(id, form, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El cuestionario se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/ejercicios/" + id + "/preguntas";
    }

    private String render(UUID id, AdminQuizForm form, Model model) {
        common(model, "ejercicios", "Cargar cuestionario");
        model.addAttribute("adminQuizForm", form);
        model.addAttribute("itemId", id);
        model.addAttribute("exerciseTitle", catalog.exerciseForm(id).title());
        return "admin/preguntas-form";
    }
}
