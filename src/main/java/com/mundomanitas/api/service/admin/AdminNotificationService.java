package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminEvent;
import com.mundomanitas.api.model.AuditLog;
import com.mundomanitas.api.model.Payment;
import com.mundomanitas.api.repository.AuditLogRepository;
import com.mundomanitas.api.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Avisos del panel por polling: solo hechos ocurridos de verdad (auditoría y
 * pagos confirmados). No se generan eventos de ejemplo.
 */
@Service
public class AdminNotificationService {

    private final AuditLogRepository logs;
    private final PaymentRepository payments;
    private final AdminFormat format;

    public AdminNotificationService(
            AuditLogRepository logs, PaymentRepository payments, AdminFormat format) {
        this.logs = logs;
        this.payments = payments;
        this.format = format;
    }

    @Transactional(readOnly = true)
    public List<AdminEvent> recent(OffsetDateTime since) {
        List<TimedEvent> events = new ArrayList<>();
        for (AuditLog log : logs.findTop8ByCreatedAtAfterOrderByCreatedAtDesc(since)) {
            events.add(new TimedEvent(log.getCreatedAt(), new AdminEvent(
                    log.getId().toString(),
                    log.getAction(),
                    "Actividad administrativa",
                    log.getEntity() + ": " + log.getDetail(),
                    format.relative(log.getCreatedAt()))));
        }
        for (Payment payment : payments.findTop5ByStatusOrderByPaidAtDesc("PAID")) {
            if (payment.getPaidAt() == null || payment.getPaidAt().isBefore(since)) continue;
            events.add(new TimedEvent(payment.getPaidAt(), new AdminEvent(
                    payment.getId().toString(),
                    "PAYMENT",
                    "Pago confirmado",
                    payment.getPlan().getLabel() + " · S/ " + payment.getAmountCents() / 100,
                    format.relative(payment.getPaidAt()))));
        }
        return events.stream()
                .sorted(Comparator.comparing(TimedEvent::at).reversed())
                .limit(8)
                .map(TimedEvent::event)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unread(OffsetDateTime since) {
        long audited = logs.countByCreatedAtAfter(since);
        long paid = payments.countByStatusAndPaidAtBetween("PAID", since, OffsetDateTime.now());
        return audited + paid;
    }

    private record TimedEvent(OffsetDateTime at, AdminEvent event) {
    }
}
