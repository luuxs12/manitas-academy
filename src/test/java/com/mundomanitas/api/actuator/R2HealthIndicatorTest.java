package com.mundomanitas.api.actuator;

import com.mundomanitas.api.config.R2Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Salud del almacenamiento R2")
class R2HealthIndicatorTest {

    @Test
    @DisplayName("sin credenciales informa estado desconocido sin filtrar secretos")
    void reportsUnknownWhenDisabled() {
        Health health = new R2HealthIndicator(new R2Properties()).health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails().get("configured")).isEqualTo(false);
        assertThat(health.getDetails().values())
                .noneMatch(value -> value.toString().contains("secret"));
    }

    @Test
    @DisplayName("configurado informa UP con el bucket y sin claves")
    void reportsUpWhenConfigured() {
        R2Properties properties = new R2Properties();
        properties.setEndpoint("https://example.r2.cloudflarestorage.com");
        properties.setBucket("mundo-manitas");
        properties.setAccessKey("access");
        properties.setSecretKey("secret");
        properties.setPublicBaseUrl("https://cdn.example.com");

        Health health = new R2HealthIndicator(properties).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails().get("configured")).isEqualTo(true);
        assertThat(health.getDetails().get("bucket")).isEqualTo("mundo-manitas");
        assertThat(health.getDetails()).doesNotContainKey("secretKey");
        assertThat(health.getDetails()).doesNotContainKey("accessKey");
    }
}
