package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Datos comunes de la cabecera del panel administrativo. */
public abstract class AdminBaseController {

    private final AdminNotificationService notifications;

    protected AdminBaseController(AdminNotificationService notifications) {
        this.notifications = notifications;
    }

    protected void common(Model model, String active, String title) {
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        model.addAttribute("active", active);
        model.addAttribute("title", title);
        model.addAttribute("adminSince", since);
        model.addAttribute("adminEvents", notifications.recent(since));
        model.addAttribute("adminUnread", notifications.unread(since));
    }

    protected UUID actorId(@AuthenticationPrincipal AuthenticatedUser principal) {
        return principal.getId();
    }
}
