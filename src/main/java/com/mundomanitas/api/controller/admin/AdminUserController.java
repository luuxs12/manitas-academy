package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminUserForm;
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

import java.util.List;
import java.util.UUID;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUserController extends AdminBaseController {

    private final AdminCatalogService catalog;

    public AdminUserController(AdminNotificationService notifications, AdminCatalogService catalog) {
        super(notifications);
        this.catalog = catalog;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "rol", required = false) String role,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @AuthenticationPrincipal AuthenticatedUser principal,
                       Model model) {
        common(model, "usuarios", "Usuarios");
        model.addAttribute("page", catalog.users(role, query, PageRequest.of(Math.max(page, 0), 10),
                principal.getId()));
        model.addAttribute("query", query);
        model.addAttribute("role", role);
        model.addAttribute("roles", List.of("STUDENT", "ADMIN"));
        return "admin/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "usuarios", "Editar usuario");
            model.addAttribute("adminUserForm", catalog.userForm(id));
            model.addAttribute("userId", id);
            model.addAttribute("roles", List.of("STUDENT", "ADMIN"));
            return "admin/usuario-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute AdminUserForm form,
                         BindingResult result,
                         @AuthenticationPrincipal AuthenticatedUser principal,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            common(model, "usuarios", "Editar usuario");
            model.addAttribute("adminUserForm", form);
            model.addAttribute("userId", id);
            model.addAttribute("roles", List.of("STUDENT", "ADMIN"));
            return "admin/usuario-form";
        }
        try {
            catalog.updateUser(id, form, principal.getId());
            redirect.addFlashAttribute("panelOk", "Los datos de la cuenta se guardaron.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/usuarios/" + id + "/editar";
    }

}
