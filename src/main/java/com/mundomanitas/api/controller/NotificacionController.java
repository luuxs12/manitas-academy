package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.NotificacionService;
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
public class NotificacionController {

    private final CuentaService accounts;
    private final NotificacionService notifications;

    public NotificacionController(CuentaService accounts, NotificacionService notifications) {
        this.accounts = accounts;
        this.notifications = notifications;
    }

    @GetMapping
    public String list(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            Model model
    ) {
        Usuario usuario = accounts.require(principal.getId());
        model.addAttribute("notifications", notifications.reciente(usuario));
        model.addAttribute("unread", notifications.unreadContar(usuario));
        return "student/notificaciones";
    }

    @PostMapping("/{id}/leer")
    public String read(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            @PathVariable UUID id,
            RedirectAttributes redirect
    ) {
        notifications.markLeido(accounts.require(principal.getId()), id);
        redirect.addFlashAttribute("panelOk", "El aviso se marcó como leído.");
        return "redirect:/panel/notificaciones";
    }

    @PostMapping("/leer-todas")
    public String leidoAll(
            @AuthenticationPrincipal AutenticadoUsuario principal,
            RedirectAttributes redirect
    ) {
        int changed = notifications.markAllLeido(accounts.require(principal.getId()));
        redirect.addFlashAttribute(
                "panelOk", changed == 0 ? "No había avisos nuevos." : "Marcaste todos los avisos como leídos.");
        return "redirect:/panel/notificaciones";
    }
}
