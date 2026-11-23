package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.CurrentUser;
import com.mundomanitas.api.model.Notification;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Carga los datos de la persona autenticada que necesitan las vistas.
 */
public interface CurrentUserService {

    SessionView load(Authentication authentication);

    record SessionView(CurrentUser currentUser, long unreadCount,
                       List<Notification> recentNotifications) {

        public static SessionView empty() {
            return new SessionView(null, 0, List.of());
        }
    }
}
