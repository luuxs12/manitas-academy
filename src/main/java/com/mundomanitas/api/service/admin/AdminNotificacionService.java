package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminEvento;
import com.mundomanitas.api.model.AuditoriaRegistro;
import com.mundomanitas.api.model.Pago;
import com.mundomanitas.api.repository.AuditoriaRegistroRepository;
import com.mundomanitas.api.repository.PagoRepository;
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
public class AdminNotificacionService {

    private final AuditoriaRegistroRepository logs;
    private final PagoRepository payments;
    private final AdminFormato format;

    public AdminNotificacionService(
            AuditoriaRegistroRepository logs, PagoRepository payments, AdminFormato format) {
        this.logs = logs;
        this.payments = payments;
        this.format = format;
    }

    @Transactional(readOnly = true)
    public List<AdminEvento> reciente(OffsetDateTime since) {
        List<TemporizadoEvento> events = new ArrayList<>();
        for (AuditoriaRegistro log : logs.buscarTop8PorFechaCreacionDespuesOrdenarPorFechaCreacionDesc(since)) {
            events.add(new TemporizadoEvento(log.getFechaCreacion(), new AdminEvento(
                    log.getId().toString(),
                    log.getAction(),
                    "Actividad administrativa",
                    log.getEntity() + ": " + log.getDetail(),
                    format.relative(log.getFechaCreacion()))));
        }
        for (Pago pago : payments.buscarTop5PorEstadoOrdenarPorFechaPagoDesc("PAID")) {
            if (pago.getFechaPago() == null || pago.getFechaPago().isBefore(since)) continue;
            events.add(new TemporizadoEvento(pago.getFechaPago(), new AdminEvento(
                    pago.getId().toString(),
                    "PAYMENT",
                    "Pago confirmado",
                    pago.getPlan().getLabel() + " · S/ " + pago.getAmountCents() / 100,
                    format.relative(pago.getFechaPago()))));
        }
        return events.stream()
                .sorted(Comparator.comparing(TemporizadoEvento::at).reversed())
                .limit(8)
                .map(TemporizadoEvento::evento)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unread(OffsetDateTime since) {
        long audited = logs.contarPorFechaCreacionDespues(since);
        long pagado = payments.contarPorEstadoAndFechaPagoEntre("PAID", since, OffsetDateTime.now());
        return audited + pagado;
    }

    private record TemporizadoEvento(OffsetDateTime at, AdminEvento evento) {
    }
}
