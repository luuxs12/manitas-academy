package com.mundomanitas.api.actuator;

import com.mundomanitas.api.config.R2Properties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Informa si el almacenamiento remoto está configurado.
 *
 * <p>No hace llamadas de red ni expone endpoint, bucket completo ni claves:
 * solo el nombre del bucket, que no es un secreto.</p>
 */
@Component
public class R2HealthIndicator implements HealthIndicator {

    private final R2Properties properties;

    public R2HealthIndicator(R2Properties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.unknown()
                    .withDetail("configured", false)
                    .build();
        }
        return Health.up()
                .withDetail("configured", true)
                .withDetail("bucket", properties.getBucket())
                .build();
    }
}
