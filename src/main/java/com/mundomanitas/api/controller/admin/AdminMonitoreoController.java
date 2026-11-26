package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.admin.AdminNotificacionService;
import com.mundomanitas.api.service.admin.MonitorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Vista de monitoreo en tiempo real del panel administrativo. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
public class AdminMonitoreoController extends AdminBaseController {

    private final MonitorService monitor;

    public AdminMonitoreoController(AdminNotificacionService notifications, MonitorService monitor) {
        super(notifications);
        this.monitor = monitor;
    }

    @GetMapping("/admin/monitoreo")
    public String monitoreo(Model model) {
        common(model, "monitoreo", "Monitoreo");
        model.addAttribute("monitor", monitor.snapshot());
        return "admin/monitoreo";
    }
}
