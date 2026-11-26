package com.mundomanitas.api.controller;

import com.mundomanitas.api.service.ActualUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone la persona autenticada y sus avisos a todas las vistas del panel.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final ActualUsuarioService usuarioActual;

    public GlobalModelAdvice(ActualUsuarioService usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    @ModelAttribute
    public void addSesion(Authentication authentication, Model model) {
        ActualUsuarioService.SesionVista session = usuarioActual.load(authentication);
        model.addAttribute("usuarioActual", session.usuarioActual());
        model.addAttribute("unreadContar", session.unreadContar());
        model.addAttribute("recienteNotifications", session.recienteNotifications());
    }

    /**
     * Resuelve el token CSRF antes de escribir la respuesta. El modal de
     * cuenta vive al final de páginas largas: si el token se pidiera recién
     * al renderizar sus formularios, la respuesta ya estaría enviada y no se
     * podría abrir la sesión que guarda el token.
     */
    @ModelAttribute
    public void materializeCsrfToken(HttpServletRequest request) {
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
            token.getToken();
        }
    }
}
