package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.admin.AdminDashboardService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reportes")
public class AdminReporteController extends AdminBaseController {

    private final AdminDashboardService dashboard;

    public AdminReporteController(AdminNotificacionService notifications,
                                 AdminDashboardService dashboard) {
        super(notifications);
        this.dashboard = dashboard;
    }

    @GetMapping
    public String reports(@RequestParam(name = "dias", defaultValue = "30") String days,
                          Model model) {
        int selected = dashboard.days(days);
        common(model, "reportes", "Reportes");
        model.addAttribute("reporte", dashboard.reports());
        model.addAttribute("grafico", dashboard.actividad(selected));
        model.addAttribute("ingresos", dashboard.ingresos(selected));
        model.addAttribute("selectedDays", selected);
        return "admin/reportes";
    }
}
