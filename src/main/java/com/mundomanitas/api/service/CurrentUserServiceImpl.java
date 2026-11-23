package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.CurrentUser;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de la carga del usuario actual para las vistas. */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository users;
    private final NotificationService notifications;

    public CurrentUserServiceImpl(UserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @Override
    @Transactional(readOnly = true)
    public SessionView load(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return SessionView.empty();
        }

        return users.findById(principal.getId())
                .map(this::session)
                .orElseGet(SessionView::empty);
    }

    private SessionView session(User user) {
        return new SessionView(
                new CurrentUser(
                        user.getFullName(),
                        user.getEmail(),
                        user.getPlan(),
                        user.getAvatarPath(),
                        user.getTheme(),
                        user.getRole()),
                notifications.unreadCount(user),
                notifications.recent(user)
        );
    }
}
