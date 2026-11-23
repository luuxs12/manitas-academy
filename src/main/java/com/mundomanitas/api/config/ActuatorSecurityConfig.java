package com.mundomanitas.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Acceso de Prometheus a Actuator con un usuario dedicado.
 *
 * <p>Solo existe cuando {@code MONITOR_PASSWORD} está definido. Nunca se
 * comparte la cuenta administrativa del sitio y la contraseña se guarda hasheada
 * con el mismo codificador que las contraseñas de los usuarios.</p>
 */
@Configuration
@ConditionalOnExpression("'${app.monitoring.password:}' != ''")
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain actuatorSecurityFilterChain(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            @Value("${app.monitoring.username:monitor}") String username,
            @Value("${app.monitoring.password}") String password
    ) throws Exception {
        UserDetailsService monitoringUsers = new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(password))
                        .roles("MONITOR")
                        .build()
        );
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(monitoringUsers);
        provider.setPasswordEncoder(passwordEncoder);

        http
                .securityMatcher("/actuator/**")
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("MONITOR"))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
