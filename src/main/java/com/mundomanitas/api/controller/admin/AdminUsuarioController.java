package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminUsuarioFormulario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
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
public class AdminUsuarioController extends AdminBaseController {

    private final AdminCatalogService catalog;

    public AdminUsuarioController(AdminNotificacionService notifications, AdminCatalogService catalog) {
        super(notifications);
        this.catalog = catalog;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "rol", required = false) String rol,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @AuthenticationPrincipal AutenticadoUsuario principal,
                       Model model) {
        common(model, "usuarios", "Usuarios");
        model.addAttribute("page", catalog.users(rol, query, PageRequest.of(Math.max(page, 0), 10),
                principal.getId()));
        model.addAttribute("query", query);
        model.addAttribute("rol", rol);
        model.addAttribute("roles", List.of("STUDENT", "ADMIN"));
        return "admin/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String editFormulario(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "usuarios", "Editar usuario");
            model.addAttribute("adminUsuarioFormulario", catalog.usuarioFormulario(id));
            model.addAttribute("usuarioId", id);
            model.addAttribute("roles", List.of("STUDENT", "ADMIN"));
            return "admin/usuario-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute AdminUsuarioFormulario formulario,
                         BindingResult resultado,
                         @AuthenticationPrincipal AutenticadoUsuario principal,
                         Model model,
                         RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            common(model, "usuarios", "Editar usuario");
            model.addAttribute("adminUsuarioFormulario", formulario);
            model.addAttribute("usuarioId", id);
            model.addAttribute("roles", List.of("STUDENT", "ADMIN"));
            return "admin/usuario-form";
        }
        try {
            catalog.updateUsuario(id, formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "Los datos de la cuenta se guardaron.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/usuarios/" + id + "/editar";
    }

}
