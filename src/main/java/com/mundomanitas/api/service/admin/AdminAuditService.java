package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminEvent;
import com.mundomanitas.api.model.AuditLog;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.AuditLogRepository;
import com.mundomanitas.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Escribe y consulta la auditoría de acciones administrativas. */
@Service
public class AdminAuditService {

    private final AuditLogRepository logs;
    private final UserRepository users;
    private final AdminFormat format;

    public AdminAuditService(AuditLogRepository logs, UserRepository users, AdminFormat format) {
        this.logs = logs;
        this.users = users;
        this.format = format;
    }

    @Transactional
    public void record(UUID actor, String action, String entity, UUID entityId, String detail) {
        logs.save(new AuditLog(actor, action, entity, entityId, detail));
    }

    @Transactional(readOnly = true)
    public Page<AdminEvent> page(Pageable pageable) {
        Map<UUID, String> names = new HashMap<>();
        for (User user : users.findAll()) {
            names.put(user.getId(), user.getFullName());
        }
        return logs.findAllByOrderByCreatedAtDesc(pageable).map(log ->
                new AdminEvent(
                        log.getId().toString(),
                        log.getAction(),
                        names.getOrDefault(log.getActorId(), "Usuario eliminado"),
                        log.getEntity() + ": " + log.getDetail(),
                        format.full(log.getCreatedAt())));
    }
}
