package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.CorreoService;
import com.mundomanitas.api.service.PagoService;
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
@RequestMapping("/admin")
public class AdminController extends AdminBaseController {

    private final AdminDashboardService dashboard;
    private final CorreoService correo;
    private final PagoService payments;

    public AdminController(AdminNotificacionService notifications, AdminDashboardService dashboard,
                           CorreoService correo, PagoService payments) {
        super(notifications);
        this.dashboard = dashboard;
        this.correo = correo;
        this.payments = payments;
    }

    @GetMapping({"", "/"})
    public String dashboard(@RequestParam(name = "dias", defaultValue = "30") String days,
                            Model model) {
        int selected = dashboard.days(days);
        common(model, "inicio", "Panel Administrativo");
        model.addAttribute("kpis", dashboard.kpis());
        model.addAttribute("grafico", dashboard.actividad(selected));
        model.addAttribute("popular", dashboard.popular());
        model.addAttribute("recienteUsers", dashboard.recienteUsers());
        model.addAttribute("sistema", dashboard.sistema());
        model.addAttribute("selectedDays", selected);
        return "admin/dashboard";
    }

    @GetMapping("/configuracion")
    public String configuration(Model model) {
        common(model, "configuracion", "Configuración");
        model.addAttribute("sistema", dashboard.sistema());
        model.addAttribute("correoEnabled", correo.isEnabled());
        model.addAttribute("stripeEnabled", payments.isEnabled());
        return "admin/configuracion";
    }
}
