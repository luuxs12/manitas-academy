package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.admin.AdminAuditoriaService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/auditoria")
public class AdminAuditoriaController extends AdminBaseController {

    private final AdminAuditoriaService auditoria;

    public AdminAuditoriaController(AdminNotificacionService notifications, AdminAuditoriaService auditoria) {
        super(notifications);
        this.auditoria = auditoria;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        common(model, "auditoria", "Logs de auditoría");
        model.addAttribute("page", auditoria.page(PageRequest.of(Math.max(page, 0), 15)));
        return "admin/auditoria";
    }
}
