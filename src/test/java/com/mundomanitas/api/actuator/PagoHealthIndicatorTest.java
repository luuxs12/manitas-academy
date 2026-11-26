package com.mundomanitas.api.actuator;

import com.mundomanitas.api.service.PagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Salud de la pasarela de pago")
class PagoHealthIndicatorTest {

    @Test
    @DisplayName("sin claves informa estado desconocido")
    void reportsUnknownWhenDisabled() {
        PagoService payments = mock(PagoService.class);
        when(payments.isEnabled()).thenReturn(false);

        Health health = new PagoHealthIndicator(payments).health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails().get("configured")).isEqualTo(false);
    }

    @Test
    @DisplayName("con claves informa UP y la pasarela usada")
    void reportsUpWhenEnabled() {
        PagoService payments = mock(PagoService.class);
        when(payments.isEnabled()).thenReturn(true);

        Health health = new PagoHealthIndicator(payments).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("configured")).isEqualTo(true);
        assertThat(health.getDetails().get("gateway")).isEqualTo("stripe");
    }
}
