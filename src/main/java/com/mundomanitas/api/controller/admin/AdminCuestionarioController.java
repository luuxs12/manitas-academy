package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCuestionarioFormulario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import com.mundomanitas.api.service.admin.AdminCuestionarioService;
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
public class AdminCuestionarioController extends AdminBaseController {

    private final AdminCatalogService catalog;
    private final AdminCuestionarioService cuestionario;

    public AdminCuestionarioController(AdminNotificacionService notifications,
                               AdminCatalogService catalog,
                               AdminCuestionarioService cuestionario) {
        super(notifications);
        this.catalog = catalog;
        this.cuestionario = cuestionario;
    }

    @GetMapping
    public String formulario(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            return render(id, cuestionario.formulario(id), model);
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios";
        }
    }

    @PostMapping
    public String save(@PathVariable UUID id,
                       @Valid @ModelAttribute("adminCuestionarioFormulario") AdminCuestionarioFormulario formulario,
                       BindingResult resultado,
                       @AuthenticationPrincipal AutenticadoUsuario principal,
                       Model model,
                       RedirectAttributes redirect) {
        if (resultado.hasErrors()) return render(id, formulario, model);
        try {
            cuestionario.save(id, formulario, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El cuestionario se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/ejercicios/" + id + "/preguntas";
    }

    private String render(UUID id, AdminCuestionarioFormulario formulario, Model model) {
        common(model, "ejercicios", "Cargar cuestionario");
        model.addAttribute("adminCuestionarioFormulario", formulario);
        model.addAttribute("itemId", id);
        model.addAttribute("ejercicioTitulo", catalog.ejercicioFormulario(id).titulo());
        return "admin/preguntas-form";
    }
}
