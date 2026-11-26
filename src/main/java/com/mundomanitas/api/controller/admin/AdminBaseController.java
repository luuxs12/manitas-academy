package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Datos comunes de la cabecera del panel administrativo. */
public abstract class AdminBaseController {

    private final AdminNotificacionService notifications;

    protected AdminBaseController(AdminNotificacionService notifications) {
        this.notifications = notifications;
    }

    protected void common(Model model, String activo, String titulo) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        model.addAttribute("activo", activo);
        model.addAttribute("titulo", titulo);
        model.addAttribute("adminDesde", since);
        model.addAttribute("adminEvents", notifications.reciente(since));
        model.addAttribute("adminUnread", notifications.unread(since));
    }

    protected UUID actorId(@AuthenticationPrincipal AutenticadoUsuario principal) {
        return principal.getId();
    }

    /** Página de confirmación compartida: el POST lo envía el formulario con CSRF. */
    protected String confirmation(Model model, String activo, String titulo,
                                  String message, String action, String label,
                                  String cancelUrl, List<String> details) {
        common(model, activo, titulo);
        model.addAttribute("confirmTitulo", titulo);
        model.addAttribute("confirmMessage", message);
        model.addAttribute("confirmAction", action);
        model.addAttribute("confirmLabel", label);
        model.addAttribute("cancelUrl", cancelUrl);
        model.addAttribute("confirmDetails", details);
        return "admin/confirmar";
    }
}
