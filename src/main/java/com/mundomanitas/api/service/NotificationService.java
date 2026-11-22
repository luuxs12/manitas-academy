package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Notification;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Avisos que se escriben cuando ocurre algo real en la cuenta. */
@Service
public class NotificationService {

    public static final String ENROLLMENT = "ENROLLMENT";
    public static final String COMPLETION = "COMPLETION";
    public static final String PAYMENT = "PAYMENT";
    public static final String CERTIFICATE = "CERTIFICATE";
    public static final String SYSTEM = "SYSTEM";

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void notify(User user, String kind, String title, String message, String targetUrl) {
        notifications.save(new Notification(user, kind, title, message, targetUrl));
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(User user) {
        return notifications.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 20));
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notifications.countByUserAndReadAtIsNull(user);
    }

    @Transactional
    public void markRead(User user, UUID id) {
        notifications.findByIdAndUser(id, user).ifPresent(Notification::markRead);
    }

    @Transactional
    public int markAllRead(User user) {
        List<Notification> pending = notifications.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(0, 50));
        int changed = 0;
        for (Notification notification : pending) {
            if (!notification.isRead()) {
                notification.markRead();
                changed++;
            }
        }
        return changed;
    }
}
