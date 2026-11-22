package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.service.MailService;
import com.mundomanitas.api.service.PaymentService;
import com.mundomanitas.api.service.admin.AdminDashboardService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController extends AdminBaseController {

    private final AdminDashboardService dashboard;
    private final MailService mail;
    private final PaymentService payments;

    public AdminController(AdminNotificationService notifications, AdminDashboardService dashboard,
                           MailService mail, PaymentService payments) {
        super(notifications);
        this.dashboard = dashboard;
        this.mail = mail;
        this.payments = payments;
    }

    @GetMapping({"", "/"})
    public String dashboard(@RequestParam(name = "dias", defaultValue = "30") String days,
                            Model model) {
        int selected = dashboard.days(days);
        common(model, "inicio", "Panel Administrativo");
        model.addAttribute("kpis", dashboard.kpis());
        model.addAttribute("chart", dashboard.activity(selected));
        model.addAttribute("popular", dashboard.popular());
        model.addAttribute("recentUsers", dashboard.recentUsers());
        model.addAttribute("system", dashboard.system());
        model.addAttribute("selectedDays", selected);
        return "admin/dashboard";
    }

    @GetMapping("/configuracion")
    public String configuration(Model model) {
        common(model, "configuracion", "Configuración");
        model.addAttribute("system", dashboard.system());
        model.addAttribute("mailEnabled", mail.isEnabled());
        model.addAttribute("stripeEnabled", payments.isEnabled());
        return "admin/configuracion";
    }
}
