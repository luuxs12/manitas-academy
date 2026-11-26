package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminEvento;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** Endpoint consultado por el polling de notificaciones del panel. */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/api/notificaciones")
public class AdminNotificacionApiController {

    private final AdminNotificacionService notifications;

    public AdminNotificacionApiController(AdminNotificacionService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public Map<String, Object> reciente(@RequestParam(name = "desde", required = false) String since) {
        OffsetDateTime from = parse(since);
        List<AdminEvento> events = notifications.reciente(from);
        return Map.of(
                "eventos", events,
                "sinLeer", notifications.unread(from),
                "servidor", OffsetDateTime.now().toString());
    }

    private OffsetDateTime parse(String value) {
        if (value == null || value.isBlank()) return OffsetDateTime.now().minusHours(24);
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            return OffsetDateTime.now().minusHours(24);
        }
    }
}
