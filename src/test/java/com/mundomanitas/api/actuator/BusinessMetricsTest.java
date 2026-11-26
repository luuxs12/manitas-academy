package com.mundomanitas.api.actuator;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Métricas de negocio")
class BusinessMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry);

    @Test
    @DisplayName("todas las métricas usan el prefijo del proyecto")
    void publishesPrefixedCounters() {
        metrics.checkoutIniciado("MONTHLY");
        metrics.pagoConfirmed("MONTHLY");
        metrics.pagoFailed();
        metrics.inscripcionCreado();
        metrics.certificadoEmitido();

        assertThat(registry.get("mundomanitas.checkouts.started").counter().count()).isEqualTo(1);
        assertThat(registry.get("mundomanitas.payments.confirmed").counter().count()).isEqualTo(1);
        assertThat(registry.get("mundomanitas.payments.failed").counter().count()).isEqualTo(1);
        assertThat(registry.get("mundomanitas.enrollments.created").counter().count()).isEqualTo(1);
        assertThat(registry.get("mundomanitas.certificates.issued").counter().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getName()).startsWith("mundomanitas."));
    }

    @Test
    @DisplayName("separa las confirmaciones por plan sin revelar datos de la cuenta")
    void tagsPaymentsPorPlan() {
        metrics.pagoConfirmed("MONTHLY");
        metrics.pagoConfirmed("ANNUAL");

        assertThat(registry.get("mundomanitas.payments.confirmed")
                .tag("plan", "MONTHLY").counter().count()).isEqualTo(1);
        assertThat(registry.get("mundomanitas.payments.confirmed")
                .tag("plan", "ANNUAL").counter().count()).isEqualTo(1);
    }
}
