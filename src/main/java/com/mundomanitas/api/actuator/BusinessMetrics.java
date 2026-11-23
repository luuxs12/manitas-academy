package com.mundomanitas.api.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Contadores de negocio exportados a Prometheus con el prefijo del proyecto.
 *
 * <p>Solo cuentan hechos ocurridos: checkouts iniciados, pagos confirmados o
 * fallidos, inscripciones y certificados. No guardan correos, tokens ni ids de
 * cuenta.</p>
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final Counter paymentsFailed;
    private final Counter enrollmentsCreated;
    private final Counter certificatesIssued;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.paymentsFailed = counter(registry, "mundomanitas.payments.failed",
                "Pagos fallidos o vencidos");
        this.enrollmentsCreated = counter(registry, "mundomanitas.enrollments.created",
                "Matrículas creadas");
        this.certificatesIssued = counter(registry, "mundomanitas.certificates.issued",
                "Certificados emitidos");
    }

    public void checkoutStarted(String plan) {
        Counter.builder("mundomanitas.checkouts.started")
                .description("Checkouts iniciados en la pasarela")
                .tag("plan", plan == null ? "unknown" : plan)
                .register(registry)
                .increment();
    }

    public void paymentConfirmed(String plan) {
        Counter.builder("mundomanitas.payments.confirmed")
                .description("Pagos confirmados por la pasarela")
                .tag("plan", plan == null ? "unknown" : plan)
                .register(registry)
                .increment();
    }

    public void paymentFailed() {
        paymentsFailed.increment();
    }

    public void enrollmentCreated() {
        enrollmentsCreated.increment();
    }

    public void certificateIssued() {
        certificatesIssued.increment();
    }

    private static Counter counter(MeterRegistry registry, String name, String description) {
        return Counter.builder(name).description(description).register(registry);
    }
}
