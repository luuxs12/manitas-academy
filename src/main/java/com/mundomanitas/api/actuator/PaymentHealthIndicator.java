package com.mundomanitas.api.actuator;

import com.mundomanitas.api.service.PaymentService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Informa si la pasarela de pago tiene credenciales cargadas.
 *
 * <p>El nombre del bean es {@code payments}: cuando la pasarela cambie, este
 * indicador seguirá apuntando al contrato y no a una clase concreta.</p>
 */
@Component("payments")
public class PaymentHealthIndicator implements HealthIndicator {

    private final PaymentService payments;

    public PaymentHealthIndicator(PaymentService payments) {
        this.payments = payments;
    }

    @Override
    public Health health() {
        if (!payments.isEnabled()) {
            return Health.unknown()
                    .withDetail("configured", false)
                    .build();
        }
        return Health.up()
                .withDetail("configured", true)
                .withDetail("gateway", "stripe")
                .build();
    }
}
