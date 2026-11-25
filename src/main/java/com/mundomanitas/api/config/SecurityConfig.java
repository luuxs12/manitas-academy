package com.mundomanitas.api.config;

import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.security.InMemoryRateLimiter;
import com.mundomanitas.api.security.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import java.time.Duration;

/**
 * Seguridad de las cuentas.
 *
 * <p>El sitio público sigue abierto; el login se apoya en la tabla
 * {@code users} y en contraseñas cifradas con BCrypt.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe una cuenta con ese correo"));
    }

    @Bean
    RateLimiter rateLimiter(
            @Value("${app.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds
    ) {
        return new InMemoryRateLimiter(maxAttempts, Duration.ofSeconds(windowSeconds));
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            @Value("${app.security.remember-me-key:mundo-manitas-dev}") String rememberMeKey
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/pagos/stripe/webhook").permitAll()
                        // Sin usuario de monitoreo, Actuator queda solo para ADMIN.
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        // Paneles aislados: ADMIN nunca entra al área de estudiante.
                        .requestMatchers("/panel", "/panel/**").hasRole("STUDENT")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/?login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean admin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                            response.sendRedirect(admin ? "/admin" : "/panel");
                        })
                        .failureUrl("/?login&error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // Cerrar sesión devuelve a la portada con el acceso abierto;
                        // no se muestra una pantalla de login separada.
                        .logoutSuccessUrl("/?login")
                        .permitAll())
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(60 * 60 * 24 * 14)
                        .userDetailsService(userDetailsService))
                .headers(headers -> headers
                        .referrerPolicy(policy -> policy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(policy -> policy.policy(
                                "camera=(), geolocation=(), microphone=(), payment=()")))
                // Stripe firma el cuerpo: no puede enviar el token CSRF del sitio.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/pagos/stripe/webhook"));

        return http.build();
    }
}
