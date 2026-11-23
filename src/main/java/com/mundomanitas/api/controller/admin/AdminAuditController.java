package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.admin.AdminAuditService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
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
public class AdminAuditController extends AdminBaseController {

    private final AdminAuditService audit;

    public AdminAuditController(AdminNotificationService notifications, AdminAuditService audit) {
        super(notifications);
        this.audit = audit;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        common(model, "auditoria", "Logs de auditoría");
        model.addAttribute("page", audit.page(PageRequest.of(Math.max(page, 0), 15)));
        return "admin/auditoria";
    }
}
