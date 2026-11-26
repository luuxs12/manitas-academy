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
    private final Counter enrollmentsCreado;
    private final Counter certificatesEmitido;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.paymentsFailed = counter(registry, "mundomanitas.payments.failed",
                "Pagos fallidos o vencidos");
        this.enrollmentsCreado = counter(registry, "mundomanitas.enrollments.created",
                "Matrículas creadas");
        this.certificatesEmitido = counter(registry, "mundomanitas.certificates.issued",
                "Certificados emitidos");
    }

    public void checkoutIniciado(String plan) {
        Counter.builder("mundomanitas.checkouts.started")
                .description("Checkouts iniciados en la pasarela")
                .tag("plan", plan == null ? "unknown" : plan)
                .register(registry)
                .increment();
    }

    public void pagoConfirmed(String plan) {
        Counter.builder("mundomanitas.payments.confirmed")
                .description("Pagos confirmados por la pasarela")
                .tag("plan", plan == null ? "unknown" : plan)
                .register(registry)
                .increment();
    }

    public void pagoFailed() {
        paymentsFailed.increment();
    }

    public void inscripcionCreado() {
        enrollmentsCreado.increment();
    }

    public void certificadoEmitido() {
        certificatesEmitido.increment();
    }

    private static Counter counter(MeterRegistry registry, String name, String descripcion) {
        return Counter.builder(name).description(descripcion).register(registry);
    }
}
