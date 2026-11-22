package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.AccountService;
import com.mundomanitas.api.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Avisos escritos por inscripciones, finalizaciones y pagos reales. */
@Controller
@RequestMapping("/panel/notificaciones")
public class NotificationController {

    private final AccountService accounts;
    private final NotificationService notifications;

    public NotificationController(AccountService accounts, NotificationService notifications) {
        this.accounts = accounts;
        this.notifications = notifications;
    }

    @GetMapping
    public String list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            Model model
    ) {
        User user = accounts.require(principal.getId());
        model.addAttribute("notifications", notifications.recent(user));
        model.addAttribute("unread", notifications.unreadCount(user));
        return "student/notificaciones";
    }

    @PostMapping("/{id}/leer")
    public String read(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            RedirectAttributes redirect
    ) {
        notifications.markRead(accounts.require(principal.getId()), id);
        redirect.addFlashAttribute("panelOk", "El aviso se marcó como leído.");
        return "redirect:/panel/notificaciones";
    }

    @PostMapping("/leer-todas")
    public String readAll(
            @AuthenticationPrincipal AuthenticatedUser principal,
            RedirectAttributes redirect
    ) {
        int changed = notifications.markAllRead(accounts.require(principal.getId()));
        redirect.addFlashAttribute(
                "panelOk", changed == 0 ? "No había avisos nuevos." : "Marcaste todos los avisos como leídos.");
        return "redirect:/panel/notificaciones";
    }
}
