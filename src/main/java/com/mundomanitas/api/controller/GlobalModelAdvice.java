package com.mundomanitas.api.controller;

import com.mundomanitas.api.dto.CurrentUser;
import com.mundomanitas.api.model.Notification;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Expone la persona autenticada y sus avisos a todas las vistas del panel.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository users;
    private final NotificationService notifications;

    public GlobalModelAdvice(UserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    private User current(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return users.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(Authentication authentication) {
        User user = current(authentication);
        if (user == null) return null;
        return new CurrentUser(
                user.getFullName(), user.getEmail(), user.getPlan(),
                user.getAvatarPath(), user.getTheme(), user.getRole());
    }

    @ModelAttribute("unreadCount")
    public long unreadCount(Authentication authentication) {
        User user = current(authentication);
        return user == null ? 0 : notifications.unreadCount(user);
    }

    @ModelAttribute("recentNotifications")
    public List<Notification> recentNotifications(Authentication authentication) {
        User user = current(authentication);
        return user == null ? List.of() : notifications.recent(user);
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
