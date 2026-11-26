package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Búsqueda global dentro del panel administrativo. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/buscar")
public class AdminSearchController extends AdminBaseController {

    private final AdminCatalogService catalog;

    public AdminSearchController(AdminNotificacionService notifications, AdminCatalogService catalog) {
        super(notifications);
        this.catalog = catalog;
    }

    @GetMapping
    public String search(@RequestParam(name = "q", required = false) String query,
                         @AuthenticationPrincipal AutenticadoUsuario principal,
                         Model model) {
        common(model, "buscar", "Resultados de búsqueda");
        String term = query == null ? "" : query.trim();
        PageRequest page = PageRequest.of(0, 8);
        model.addAttribute("query", term);
        model.addAttribute("users", catalog.users(null, term, page, principal.getId()));
        model.addAttribute("courses", catalog.courses(term, page));
        model.addAttribute("modules", catalog.modules(null, term, page));
        model.addAttribute("videos", catalog.videos(null, term, page));
        model.addAttribute("exercises", catalog.exercises(null, term, page));
        return "admin/buscar";
    }
}
